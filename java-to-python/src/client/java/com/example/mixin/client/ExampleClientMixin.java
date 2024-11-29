package com.example.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

@Mixin(MinecraftClient.class)
public class ExampleClientMixin {
	private Socket socket;
	private boolean isConnected = false;
	private int tickCounter = 0;
	
	// Reusable buffers
	private ByteBuffer pixelBuffer;
	private ByteArrayOutputStream imageOutputStream;
	private BufferedOutputStream socketOutputStream;
	private BufferedImage frameImage;
	private Integer previousFrameHash;
	private ImageWriter jpegWriter;
	private ImageWriteParam jpegParams;
	private byte[] pixelsBytes;
	private byte[] imgData;
	private byte[] base64Buffer;
	private StringBuilder messageBuilder;
	
	// Constants
	private static final int BUFFER_SIZE = 32768; // Increased buffer size
	private static final int TICK_INTERVAL = 2;
	private static final int MAX_FRAME_SIZE = 131072; // Increased to 128KB for full res
	private static final float SCALE = 0.75f; // Adjust this value to change scaling (0.5 = half size)
	private static final long FRAME_TIME_TARGET = 33; // ~30 FPS
	private static final int BASE64_BUFFER_SIZE = (MAX_FRAME_SIZE * 4) / 3 + 4; // Base64 expansion factor
	
	private long lastFrameTime = 0;
	private long lastProcessedTime = 0;
	
	@Inject(at = @At("HEAD"), method = "render")
	private void onRender(CallbackInfo info) {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastProcessedTime < FRAME_TIME_TARGET) {
			return;
		}
		lastProcessedTime = currentTime;
		
		try {
			if (!isConnected) {
				try {
					socket = new Socket("localhost", 12345);
					socketOutputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);
					isConnected = true;
				} catch (IOException e) {
					return;
				}
			}

			MinecraftClient client = MinecraftClient.getInstance();
			Framebuffer framebuffer = client.getFramebuffer();
			
			// Get the actual game window dimensions instead of framebuffer dimensions
			int width = client.getWindow().getFramebufferWidth();
			int height = client.getWindow().getFramebufferHeight();
			
			// Calculate scaled dimensions
			int scaledWidth = Math.max(1, (int)(width * SCALE));
			int scaledHeight = Math.max(1, (int)(height * SCALE));
			
			if (pixelBuffer == null || pixelBuffer.capacity() < width * height * 4) {
				pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);
				pixelsBytes = new byte[width * height * 4];
				imageOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
				frameImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_3BYTE_BGR);
				imgData = ((DataBufferByte) frameImage.getRaster().getDataBuffer()).getData();
				
				// Initialize JPEG writer with quality settings
				jpegWriter = ImageIO.getImageWritersByFormatName("jpg").next();
				jpegParams = jpegWriter.getDefaultWriteParam();
				jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				jpegParams.setCompressionQuality(0.6f);
				base64Buffer = new byte[BASE64_BUFFER_SIZE];
				messageBuilder = new StringBuilder(BASE64_BUFFER_SIZE + 1);
			}

			pixelBuffer.clear();
			framebuffer.beginRead();
			
			// Read from the actual game viewport
			GL11.glReadBuffer(GL11.GL_FRONT);
			GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
			
			framebuffer.endRead();

			pixelBuffer.get(pixelsBytes);
			
			// Process pixels with scaling
			int widthRatio = (width << 16) / scaledWidth;
			int heightRatio = (height << 16) / scaledHeight;

			for (int y = 0; y < scaledHeight; y++) {
				int sourceY = ((height - 1 - ((y * heightRatio) >> 16)) * width) * 4;
				int targetY = y * scaledWidth * 3;
				
				for (int x = 0; x < scaledWidth; x++) {
					int sourceX = ((x * widthRatio) >> 16) * 4;
					int sourceIndex = sourceY + sourceX;
					int targetIndex = targetY + (x * 3);
					
					imgData[targetIndex] = pixelsBytes[sourceIndex];     // B
					imgData[targetIndex + 1] = pixelsBytes[sourceIndex + 1]; // G
					imgData[targetIndex + 2] = pixelsBytes[sourceIndex + 2]; // R
				}
			}

			// Optimized JPEG encoding
			imageOutputStream.reset();
			jpegWriter.setOutput(new MemoryCacheImageOutputStream(imageOutputStream));
			jpegWriter.write(null, new IIOImage(frameImage, null, null), jpegParams);
			byte[] currentFrame = imageOutputStream.toByteArray();
			
			if (currentFrame.length > MAX_FRAME_SIZE) {
				return;
			}

			int newHash = Arrays.hashCode(currentFrame);
			if (previousFrameHash != null && newHash == previousFrameHash) {
				return;
			}
			previousFrameHash = newHash;

			messageBuilder.setLength(0);
			messageBuilder.append(Base64.getEncoder().encodeToString(currentFrame)).append('\n');
			socketOutputStream.write(messageBuilder.toString().getBytes());
			socketOutputStream.flush();

		} catch (IOException e) {
			isConnected = false;
			try {
				if (socketOutputStream != null) socketOutputStream.close();
				if (socket != null) socket.close();
			} catch (IOException ignored) {}
		}
		
		lastFrameTime = currentTime;
	}

	private int getRGBFromBytes(byte[] pixels, int offset) {
		int r = pixels[offset + 0] & 0xFF;
		int g = pixels[offset + 1] & 0xFF;
		int b = pixels[offset + 2] & 0xFF;
		return (r << 16) | (g << 8) | b;
	}
}

