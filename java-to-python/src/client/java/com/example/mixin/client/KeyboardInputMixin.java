package com.example.mixin.client;

import com.example.ExampleMod;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

@Mixin(MinecraftClient.class)
public class KeyboardInputMixin {
    private static Socket keyboardSocket;
    private static BufferedOutputStream keyboardOutputStream;
    private static boolean keyboardIsConnected = false;
    private static final Object keyboardLock = new Object();
    private static boolean isStreaming = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        
        synchronized (keyboardLock) {
            if (!keyboardIsConnected) {
                try {
                    keyboardSocket = new Socket("localhost", 12347);
                    keyboardOutputStream = new BufferedOutputStream(keyboardSocket.getOutputStream());
                    keyboardIsConnected = true;
                    ExampleMod.LOGGER.info("Connected to keyboard data server!");
                } catch (IOException e) {
                    ExampleMod.LOGGER.error("Failed to connect to keyboard server: " + e.getMessage());
                    return;
                }
            }

            try {
                // Get key states using GLFW key codes
                boolean w = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_W);
                boolean s = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_S);
                boolean a = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_A);
                boolean d = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_D);
                boolean space = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_SPACE);
                boolean leftShift = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_LEFT_SHIFT);
                
                // Check J and K keys
                boolean j = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_J);
                boolean k = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_K);
                
                if (j && !isStreaming) {
                    isStreaming = true;
                    ExampleMod.LOGGER.info("Stream started");
                } else if (k && isStreaming) {
                    isStreaming = false;
                    ExampleMod.LOGGER.info("Stream stopped");
                }

                // Pack key states into an integer
                int keyState = 0;
                if (w) keyState |= 1;
                if (s) keyState |= 2;
                if (a) keyState |= 4;
                if (d) keyState |= 8;
                if (space) keyState |= 16;
                if (leftShift) keyState |= 32;
                if (isStreaming) keyState |= 64; // Use bit 6 for streaming state

                // Send key state data
                byte[] data = new byte[4];
                putInt(data, 0, keyState);
                keyboardOutputStream.write(data);
                keyboardOutputStream.flush();

                if (keyState > 0) {
                    ExampleMod.LOGGER.info(String.format("Sent keyboard state: %d", keyState));
                }
            } catch (IOException e) {
                ExampleMod.LOGGER.error("Error sending keyboard data: " + e.getMessage());
                keyboardIsConnected = false;
                try {
                    if (keyboardOutputStream != null) keyboardOutputStream.close();
                    if (keyboardSocket != null) keyboardSocket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private static void putInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte)(value >> 24);
        bytes[offset + 1] = (byte)(value >> 16);
        bytes[offset + 2] = (byte)(value >> 8);
        bytes[offset + 3] = (byte)value;
    }
} 