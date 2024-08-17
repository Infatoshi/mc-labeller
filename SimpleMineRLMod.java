package com.user.recorder;

import com.google.gson.Gson;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod("recorder")
public class SimpleMineRLMod {
    private Random random = new Random();
    private SimpleServer server;
    private Gson gson;
    private static final int SCALE_FACTOR = 2; // Downscale factor
    
    private Set<Integer> pressedKeys = new HashSet<>();
    private Set<Integer> pressedMouseButtons = new HashSet<>();
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public SimpleMineRLMod() {
        MinecraftForge.EVENT_BUS.register(this);
        gson = new Gson();
        server = new SimpleServer(12345); // Choose an appropriate port
        new Thread(() -> server.start()).start(); // Start server in a new thread
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                server.updatePlayerPosition(player.getX(), player.getY(), player.getZ());
                server.updateScreenCapture(captureScreen());
                server.updateMouseMovement(getMouseMovement());
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            pressedKeys.add(event.getKey());
            if (event.getKey() == 46) { // C key
                int x = random.nextInt(100001); // 0 to 100000
                int y = random.nextInt(11) + 110; // 110 to 120
                int z = random.nextInt(100001); // 0 to 100000
                String command = String.format("/tp %d %d %d", x, y, z);
                Minecraft.getInstance().player.chat(command);
            }
        
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            pressedKeys.remove(event.getKey());
        }
        server.updateKeystrokes(pressedKeys);
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        int button = event.getButton();
        int action = event.getAction();
        
        if (action == GLFW.GLFW_PRESS) {
            pressedMouseButtons.add(button);
        } else if (action == GLFW.GLFW_RELEASE) {
            pressedMouseButtons.remove(button);
        }
        server.updateMouseButtons(pressedMouseButtons);
    }

    private String getMouseMovement() {
        Minecraft mc = Minecraft.getInstance();
        double currentX = mc.mouseHandler.xpos();
        double currentY = mc.mouseHandler.ypos();
        
        double dx = currentX - lastMouseX;
        double dy = currentY - lastMouseY;
        
        lastMouseX = currentX;
        lastMouseY = currentY;
        
        return String.format("%.2f,%.2f", dx, dy);
    }

    private String captureScreen() {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        int scaledWidth = width / SCALE_FACTOR;
        int scaledHeight = height / SCALE_FACTOR;

        GL11.glReadBuffer(GL11.GL_FRONT);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int i = (x + (width * y)) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                image.setRGB(x, height - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImage.createGraphics();
        g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(scaledImage, "jpg", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
    
        // Check if the base64 string is empty
        if (base64Image.isEmpty()) {
            System.out.println("Warning: Captured screen is empty");
            return "";
        }
        
        return base64Image;
    }

    private class SimpleServer {
        private int port;
        private ExecutorService pool;
        private volatile String playerPosition = "0,0,0";
        private volatile String screenCapture = "";
        private volatile String keystrokes = "[]";
        private volatile String mouseButtons = "[]";
        private volatile String mouseMovement = "0.00,0.00";

        public SimpleServer(int port) {
            this.port = port;
            this.pool = Executors.newFixedThreadPool(10);
        }

        public void updatePlayerPosition(double x, double y, double z) {
            this.playerPosition = String.format("%.2f,%.2f,%.2f", x, y, z);
        }

        public void updateScreenCapture(String capture) {
            this.screenCapture = capture;
        }

        public void updateKeystrokes(Set<Integer> keys) {
            this.keystrokes = gson.toJson(keys);
        }

        public void updateMouseButtons(Set<Integer> buttons) {
            this.mouseButtons = gson.toJson(buttons);
        }

        public void updateMouseMovement(String movement) {
            this.mouseMovement = movement;
        }

        public void start() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Server is listening on port " + port);

                while (true) {
                    Socket socket = serverSocket.accept();
                    pool.execute(new ClientHandler(socket));
                }
            } catch (Exception e) {
                System.out.println("Server exception: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private class ClientHandler implements Runnable {
            private Socket socket;

            public ClientHandler(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                try (
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
                ) {
                    while (!socket.isClosed()) {
                        String data = gson.toJson(new DataPacket(playerPosition, screenCapture, keystrokes, mouseButtons, mouseMovement));
                        out.println(data);
                        Thread.sleep(50); // Send data every 50ms (20 times per sec)
                    }
                } catch (Exception e) {
                    System.out.println("Client handler exception: " + e.getMessage());
                }
            }
        }
    }

    private class DataPacket {
        String position;
        String screen;
        String keystrokes;
        String mouseButtons;
        String mouseMovement;

        DataPacket(String position, String screen, String keystrokes, String mouseButtons, String mouseMovement) {
            this.position = position;
            this.screen = screen;
            this.keystrokes = keystrokes;
            this.mouseButtons = mouseButtons;
            this.mouseMovement = mouseMovement;
        }
    }
}
