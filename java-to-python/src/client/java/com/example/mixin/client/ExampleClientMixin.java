package com.example.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
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
	private byte[] previousFrame;
	private ImageWriter jpegWriter;
	private ImageWriteParam jpegParams;
	
	// Constants
	private static final int BUFFER_SIZE = 32768; // Increased buffer size
	private static final int TICK_INTERVAL = 2;
	private static final int MAX_FRAME_SIZE = 131072; // Increased to 128KB for full res
	private static final float SCALE = 0.75f; // Adjust this value to change scaling (0.5 = half size)
	
	private long lastFrameTime = 0;
	
	@Inject(at = @At("HEAD"), method = "render")
	private void onRender(CallbackInfo info) {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastFrameTime < 16) return;
		
		if (++tickCounter % TICK_INTERVAL != 0) return;
		
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
			int width = framebuffer.textureWidth;
			int height = framebuffer.textureHeight;
			
			// Calculate scaled dimensions
			int scaledWidth = Math.max(1, (int)(width * SCALE));
			int scaledHeight = Math.max(1, (int)(height * SCALE));
			
			if (pixelBuffer == null || pixelBuffer.capacity() < width * height * 4) {
				pixelBuffer = ByteBuffer.allocateDirect(width * height * 4);
				imageOutputStream = new ByteArrayOutputStream(BUFFER_SIZE);
				frameImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_3BYTE_BGR);
				
				// Initialize JPEG writer with quality settings
				jpegWriter = ImageIO.getImageWritersByFormatName("jpg").next();
				jpegParams = jpegWriter.getDefaultWriteParam();
				jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				jpegParams.setCompressionQuality(0.6f);
			}

			pixelBuffer.clear();
			framebuffer.beginRead();
			GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
			framebuffer.endRead();

			byte[] pixelsBytes = new byte[width * height * 4];
			pixelBuffer.get(pixelsBytes);
			
			// Calculate center crop offsets
			int startX = (width - scaledWidth) / 2;
			int startY = (height - scaledHeight) / 2;
			
			// Direct byte array access for faster pixel processing
			byte[] imgData = ((DataBufferByte) frameImage.getRaster().getDataBuffer()).getData();
			
			// Process pixels with scaling and center crop
			for (int y = 0; y < scaledHeight; y++) {
				for (int x = 0; x < scaledWidth; x++) {
					int sourceX = startX + x;
					int sourceY = startY + y;
					int sourceIndex = ((sourceY * width) + sourceX) * 4;
					int targetIndex = (y * scaledWidth + x) * 3;
					
					imgData[targetIndex] = pixelsBytes[sourceIndex + 2];     // B
					imgData[targetIndex + 1] = pixelsBytes[sourceIndex + 1]; // G
					imgData[targetIndex + 2] = pixelsBytes[sourceIndex];     // R
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

			if (previousFrame != null && Arrays.equals(previousFrame, currentFrame)) {
				return;
			}
			previousFrame = currentFrame;

			String base64Image = Base64.getEncoder().encodeToString(currentFrame);
			socketOutputStream.write((base64Image + "\n").getBytes());
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

