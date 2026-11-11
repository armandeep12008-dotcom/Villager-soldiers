package com.villagersoldiers;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** One spawn egg per SoldierType. Right-click a block to spawn that unit. */
public final class ModItems {
    private static final List<Item> EGGS = new ArrayList<>();

    static class SoldierEgg extends Item {
        private final SoldierType type;
        SoldierEgg(Properties props, SoldierType type) { super(props); this.type = type; }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
            BlockPos spot = ctx.getClickedPos().relative(ctx.getClickedFace());
            type.spawn(level, Vec3.atBottomCenterOf(spot));
            if (ctx.getPlayer() != null) ctx.getItemInHand().consume(1, ctx.getPlayer());
            return InteractionResult.CONSUME;
        }
    }

    public static void init() {
        for (SoldierType t : SoldierType.values()) {
            String id = t.name().toLowerCase() + "_egg";
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                    Identifier.fromNamespaceAndPath(VillagerSoldiers.MOD_ID, id));
            Item egg = Registry.register(BuiltInRegistries.ITEM, key,
                    new SoldierEgg(new Item.Properties().setId(key), t));
            EGGS.add(egg);
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(output -> EGGS.forEach(egg -> output.accept(egg)));
    }
    private ModItems() {}
}
