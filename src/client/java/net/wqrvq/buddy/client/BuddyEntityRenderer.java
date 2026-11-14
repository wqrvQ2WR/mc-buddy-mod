package net.wqrvq.buddy.client;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.Identifier;
import net.wqrvq.buddy.BuddyMod;
import net.wqrvq.buddy.entity.BuddyEntity;

public class BuddyEntityRenderer extends BipedEntityRenderer<BuddyEntity, BipedEntityRenderState, BipedEntityModel<BipedEntityRenderState>> {
	private static final Identifier TEXTURE = Identifier.of(BuddyMod.MOD_ID, "textures/entity/buddy.png");

	public BuddyEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
		this.addFeature(new ArmorFeatureRenderer<>(this,
				new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_INNER_ARMOR)),
				new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE_OUTER_ARMOR)),
				context.getEquipmentRenderer()));
		this.addFeature(new HeldItemFeatureRenderer<>(this));
	}

	@Override
	public BipedEntityRenderState createRenderState() {
		return new BipedEntityRenderState();
	}

	@Override
	public Identifier getTexture(BipedEntityRenderState state) {
		return TEXTURE;
	}
}
