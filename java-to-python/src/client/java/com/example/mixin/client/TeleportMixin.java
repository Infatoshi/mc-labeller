package com.example.mixin.client;

import com.example.ExampleMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.random.Random;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class TeleportMixin {
    private static boolean wasLPressed = false;
    private static boolean wasPPressed = false;
    private static boolean isHoldingPlayer = false;
    private static final int TELEPORT_RANGE = 1000;
    private static final int HOLD_HEIGHT = 150;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        long handle = client.getWindow().getHandle();
        boolean isLPressed = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_L);
        boolean isPPressed = InputUtil.isKeyPressed(handle, InputUtil.GLFW_KEY_P);
        
        // Handle P key to release player
        if (isPPressed && !wasPPressed && isHoldingPlayer) {
            isHoldingPlayer = false;
            // client.player.sendMessage(Text.literal("Released from hold position"), false);
        }
        
        // Keep player at HOLD_HEIGHT while waiting
        if (isHoldingPlayer) {
            client.player.setPosition(client.player.getX(), HOLD_HEIGHT, client.player.getZ());
            client.player.fallDistance = 0; // Reset fall distance while being held
        }

        if (isLPressed && !wasLPressed) {
            // Generate random x,z coordinates
            Random random = Random.create();
            double x = random.nextBetween(-TELEPORT_RANGE, TELEPORT_RANGE);
            double z = random.nextBetween(-TELEPORT_RANGE, TELEPORT_RANGE);
            
            // Teleport logic
            client.player.setPosition(x, HOLD_HEIGHT, z);
            isHoldingPlayer = true;
            // String message = String.format("Teleported to X: %.2f, Y: %d, Z: %.2f", x, HOLD_HEIGHT, z);
            // client.player.sendMessage(Text.literal(message), false);
        }
        
        wasLPressed = isLPressed;
        wasPPressed = isPPressed;
    }
} 