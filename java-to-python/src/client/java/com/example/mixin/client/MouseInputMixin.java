package com.example.mixin.client;

import com.example.ExampleMod;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

@Mixin(MinecraftClient.class)
public class MouseInputMixin {
    private static Socket mouseSocket;
    private static BufferedOutputStream mouseOutputStream;
    private static boolean mouseIsConnected = false;
    private static final Object mouseLock = new Object();
    private static double lastTickX = 0;
    private static double lastTickY = 0;
    private static boolean firstTick = true;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.mouse == null) return;
        
        Mouse mouse = client.mouse;
        long handle = client.getWindow().getHandle();
        double currentX = mouse.getX();
        double currentY = mouse.getY();
        
        synchronized (mouseLock) {
            if (!mouseIsConnected) {
                try {
                    mouseSocket = new Socket("localhost", 12346);
                    mouseOutputStream = new BufferedOutputStream(mouseSocket.getOutputStream());
                    mouseIsConnected = true;
                    ExampleMod.LOGGER.info("Connected to mouse data server!");
                } catch (IOException e) {
                    ExampleMod.LOGGER.error("Failed to connect to mouse server: " + e.getMessage());
                    return;
                }
            }
            
            try {
                // Initialize last positions on first tick
                if (firstTick) {
                    lastTickX = currentX;
                    lastTickY = currentY;
                    firstTick = false;
                    return;
                }
                
                // Calculate deltas since last tick
                double dx = currentX - lastTickX;
                double dy = currentY - lastTickY;
                
                // Update last positions for next tick
                lastTickX = currentX;
                lastTickY = currentY;
                
                // Get button states using Mouse methods instead of GLFW
                boolean leftButton = mouse.wasLeftButtonClicked();
                boolean rightButton = mouse.wasRightButtonClicked();
                
                // Pack button states into an integer
                int buttonState = 0;
                if (leftButton) buttonState |= 1;
                if (rightButton) buttonState |= 2;
                
                // Send movement and button data
                byte[] data = new byte[12];
                putFloat(data, 0, (float)dx);
                putFloat(data, 4, (float)dy);
                putInt(data, 8, buttonState);
                
                mouseOutputStream.write(data);
                mouseOutputStream.flush();
                
                if (dx != 0 || dy != 0 || buttonState != 0) {
                    ExampleMod.LOGGER.info(String.format("Sent mouse data: dx=%.2f, dy=%.2f, buttons=%d", dx, dy, buttonState));
                }
            } catch (IOException e) {
                ExampleMod.LOGGER.error("Error sending mouse data: " + e.getMessage());
                mouseIsConnected = false;
                try {
                    if (mouseOutputStream != null) mouseOutputStream.close();
                    if (mouseSocket != null) mouseSocket.close();
                } catch (IOException ignored) {}
            }
        }
    }
    
    private static void putFloat(byte[] bytes, int offset, float value) {
        int bits = Float.floatToIntBits(value);
        bytes[offset] = (byte)(bits >> 24);
        bytes[offset + 1] = (byte)(bits >> 16);
        bytes[offset + 2] = (byte)(bits >> 8);
        bytes[offset + 3] = (byte)bits;
    }
    
    private static void putInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte)(value >> 24);
        bytes[offset + 1] = (byte)(value >> 16);
        bytes[offset + 2] = (byte)(value >> 8);
        bytes[offset + 3] = (byte)value;
    }
} 