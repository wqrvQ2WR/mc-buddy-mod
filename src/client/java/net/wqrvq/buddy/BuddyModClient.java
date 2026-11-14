package net.wqrvq.buddy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.wqrvq.buddy.client.BuddyEntityRenderer;

public class BuddyModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(BuddyMod.BUDDY, BuddyEntityRenderer::new);
	}
}
