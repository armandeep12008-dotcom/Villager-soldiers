package com.villagersoldiers;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> SOLDIER_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VillagerSoldiers.MOD_ID, "soldier"));

    public static final EntityType<SoldierEntity> SOLDIER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, SOLDIER_KEY,
            EntityType.Builder.of(SoldierEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).eyeHeight(1.62f).clientTrackingRange(10).build(SOLDIER_KEY));

    public static void init() {
        FabricDefaultAttributeRegistry.register(SOLDIER, SoldierEntity.createSoldierAttributes());
    }
    private ModEntities() {}
}
