package net.wqrvq.buddy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.wqrvq.buddy.entity.BuddyEntity;
import net.wqrvq.buddy.item.BuddySummonItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuddyMod implements ModInitializer {
	public static final String MOD_ID = "buddy";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final RegistryKey<EntityType<?>> BUDDY_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "buddy"));

	public static final EntityType<BuddyEntity> BUDDY = Registry.register(
			Registries.ENTITY_TYPE,
			BUDDY_KEY,
			EntityType.Builder.create(BuddyEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.6f, 1.95f)
					.maxTrackingRange(10)
					.build(BUDDY_KEY)
	);

	private static final RegistryKey<Item> BUDDY_CHARM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "buddy_charm"));

	public static final Item BUDDY_CHARM = Registry.register(
			Registries.ITEM,
			BUDDY_CHARM_KEY,
			new BuddySummonItem(new Item.Settings().maxCount(16).registryKey(BUDDY_CHARM_KEY))
	);

	@Override
	public void onInitialize() {
		FabricDefaultAttributeRegistry.register(BUDDY, BuddyEntity.createBuddyAttributes());

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(BUDDY_CHARM));

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (world.isClient) {
				return;
			}
			Box searchBox = new Box(pos).expand(16.0);
			for (BuddyEntity buddy : world.getEntitiesByType(TypeFilter.instanceOf(BuddyEntity.class), searchBox,
					b -> b.isTamed() && player.equals(b.getOwner()))) {
				buddy.tryAssistMining(state.getBlock(), player);
			}
		});

		LOGGER.info("Buddy mod loaded - 마인크래프트 친구가 준비됐습니다");
	}
}
