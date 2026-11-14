package net.wqrvq.buddy.item;

import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.wqrvq.buddy.BuddyMod;
import net.wqrvq.buddy.entity.BuddyEntity;

public class BuddySummonItem extends Item {
	public BuddySummonItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);

		if (!world.isClient && world instanceof ServerWorld serverWorld) {
			BuddyEntity buddy = BuddyMod.BUDDY.create(serverWorld, null, player.getBlockPos(), SpawnReason.MOB_SUMMONED, false, false);
			if (buddy != null) {
				Vec3d spawnPos = player.getPos().add(player.getRotationVector().multiply(2.0));
				buddy.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), 0.0f);
				buddy.setOwner(player);
				buddy.setTamed(true, true);
				serverWorld.spawnEntity(buddy);
				player.sendMessage(Text.literal("친구가 소환됐어요! 우클릭으로 대화하고, 웅크린 채 우클릭하면 앉아요."), false);
				stack.decrement(1);
			}
		}

		return ActionResult.SUCCESS;
	}
}
