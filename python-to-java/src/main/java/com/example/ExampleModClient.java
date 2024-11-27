package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import com.example.network.ActionReceiver;

public class ExampleModClient implements ClientModInitializer {
    private static KeyBinding automationKey;
    private static int currentPattern = 0;
    private static final int PATTERN_COUNT = 3;
    
    @Override
    public void onInitializeClient() {
        // Start the action receiver
        ActionReceiver.startListener();

        // Register the tick event to handle automation
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                // Reset all inputs first
                resetAllInputs(client);

                // Apply actions based on received commands
                applyReceivedActions(client);
            }
        });
    }

    private void resetAllInputs(net.minecraft.client.MinecraftClient client) {
        client.options.jumpKey.setPressed(false);
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
    }

    private void applyReceivedActions(net.minecraft.client.MinecraftClient client) {
        // Apply each action based on the received state
        client.options.jumpKey.setPressed(ActionReceiver.shouldActivate("jump"));
        client.options.forwardKey.setPressed(ActionReceiver.shouldActivate("forward"));
        client.options.backKey.setPressed(ActionReceiver.shouldActivate("back"));
        client.options.leftKey.setPressed(ActionReceiver.shouldActivate("left"));
        client.options.rightKey.setPressed(ActionReceiver.shouldActivate("right"));
        client.options.sprintKey.setPressed(ActionReceiver.shouldActivate("sprint"));
        client.options.attackKey.setPressed(ActionReceiver.shouldActivate("attack"));
        client.options.useKey.setPressed(ActionReceiver.shouldActivate("use"));

        // Handle rotation with dx/dy values
        float dx = ActionReceiver.getFloatAction("dx", 0.0f);
        float dy = ActionReceiver.getFloatAction("dy", 0.0f);
        if (dx != 0.0f || dy != 0.0f) {
            client.player.setYaw(client.player.getYaw() + dx);
            client.player.setPitch(Math.max(-90.0f, Math.min(90.0f, client.player.getPitch() + dy)));
        }
    }
} 