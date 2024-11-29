package com.example.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ExampleMixin {
	private int tickCounter = 0;

	@Inject(at = @At("HEAD"), method = "tick")
	private void onTick(CallbackInfo info) {
		tickCounter++;
		if (tickCounter > 20) {
			tickCounter = 1;
		}
		
		MinecraftServer server = (MinecraftServer) (Object) this;
		// server.getPlayerManager().broadcast(
		// 	Text.literal("Tick Counter: " + tickCounter + "/20"),
		// 	false
		// );
	}
}