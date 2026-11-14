package net.wqrvq.buddy.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class BuddyEntity extends TameableEntity {
	private static final String[] GREETINGS = {
			"오늘도 같이 놀자!",
			"뒤는 내가 지켜줄게.",
			"블록 캐는 거 도와줄까?",
			"조심해, 근처에 몹이 있어!",
			"우리 팀워크 최고다.",
			"어디로 갈까?"
	};

	public BuddyEntity(EntityType<? extends TameableEntity> entityType, World world) {
		super(entityType, world);
		this.setTamed(false, false);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(2, new SitGoal(this));
		this.goalSelector.add(3, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.add(4, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
		this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(8, new LookAroundGoal(this));

		this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
		this.targetSelector.add(2, new AttackWithOwnerGoal(this));
		this.targetSelector.add(3, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false,
				(entity, world) -> !(entity instanceof CreeperEntity)));
	}

	public static DefaultAttributeContainer.Builder createBuddyAttributes() {
		return AnimalEntity.createMobAttributes()
				.add(EntityAttributes.MAX_HEALTH, 30.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.3)
				.add(EntityAttributes.ATTACK_DAMAGE, 4.0)
				.add(EntityAttributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return false;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);

		if (this.isTamed() && this.isOwner(player)) {
			if (!this.getWorld().isClient) {
				if (player.isSneaking()) {
					this.setSitting(!this.isSitting());
				} else if (!stack.isEmpty()) {
					this.equipFromPlayer(player, stack);
				} else {
					String line = GREETINGS[Random.create(this.getWorld().getTime() + this.getId()).nextInt(GREETINGS.length)];
					player.sendMessage(Text.literal("[" + this.getDisplayName().getString() + "] " + line), false);
				}
			}
			return ActionResult.SUCCESS;
		}

		if (!this.isTamed() && stack.isOf(Items.BREAD) && !this.getWorld().isClient) {
			stack.decrement(1);
			this.setOwner(player);
			this.setTamed(true, true);
			this.navigation.stop();
			this.setTarget(null);
			this.setSitting(false);
			this.getWorld().sendEntityStatus(this, (byte) 7);
			player.sendMessage(Text.literal("이제부터 " + player.getName().getString() + "님과 함께할게요!"), false);
			return ActionResult.SUCCESS;
		}

		return super.interactMob(player, hand);
	}

	private void equipFromPlayer(PlayerEntity player, ItemStack stack) {
		EquipmentSlot slot = this.getPreferredEquipmentSlot(stack);
		ItemStack previous = this.getEquippedStack(slot);
		this.equipStack(slot, stack.copyWithCount(1));
		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		if (!previous.isEmpty()) {
			player.giveItemStack(previous);
		}
	}

	private static final int MINING_ASSIST_RADIUS = 4;

	public boolean tryAssistMining(Block targetBlock, PlayerEntity owner) {
		if (this.isSitting() || !(this.getWorld() instanceof ServerWorld serverWorld)) {
			return false;
		}

		BlockPos center = this.getBlockPos();
		BlockPos bestPos = null;
		double bestDistance = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.iterate(
				center.add(-MINING_ASSIST_RADIUS, -MINING_ASSIST_RADIUS, -MINING_ASSIST_RADIUS),
				center.add(MINING_ASSIST_RADIUS, MINING_ASSIST_RADIUS, MINING_ASSIST_RADIUS))) {
			BlockState state = serverWorld.getBlockState(pos);
			if (!state.isOf(targetBlock) || state.getHardness(serverWorld, pos) < 0) {
				continue;
			}
			double distance = pos.getSquaredDistance(center);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestPos = pos.toImmutable();
			}
		}

		if (bestPos == null) {
			return false;
		}

		BlockState state = serverWorld.getBlockState(bestPos);
		BlockEntity blockEntity = serverWorld.getBlockEntity(bestPos);
		ItemStack tool = this.getEquippedStack(EquipmentSlot.MAINHAND);
		List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, bestPos, blockEntity, this, tool);

		if (!serverWorld.breakBlock(bestPos, false, this, 512)) {
			return false;
		}

		for (ItemStack drop : drops) {
			owner.giveItemStack(drop);
		}
		return true;
	}

	@Override
	public boolean canBeLeashed() {
		return this.isTamed() && super.canBeLeashed();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_VILLAGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
		return SoundEvents.ENTITY_VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_VILLAGER_DEATH;
	}

	@Override
	public BuddyEntity createChild(net.minecraft.server.world.ServerWorld world, net.minecraft.entity.passive.PassiveEntity entity) {
		return null;
	}
}
