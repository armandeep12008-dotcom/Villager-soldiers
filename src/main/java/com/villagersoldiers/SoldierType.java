package com.villagersoldiers;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Every unit. All soldiers are real Guard Villagers guards (same AI, model, textures,
 * inventory screen, follow/patrol buttons) - we only choose their gear and mount.
 */
public enum SoldierType {
    INFANTRY_SWORD, INFANTRY_SPEAR, INFANTRY_BOW, INFANTRY_CROSSBOW,
    HORSE_SWORD, HORSE_SPEAR,
    CAMEL_GUARDS,
    GHAST_SQUAD;

    public enum Role { SWORD_SHIELD, SPEAR_SHIELD, BOW, CROSSBOW }

    public void spawn(ServerLevel level, Vec3 pos) {
        switch (this) {
            case INFANTRY_SWORD -> guard(level, pos, Role.SWORD_SHIELD, null);
            case INFANTRY_SPEAR -> guard(level, pos, Role.SPEAR_SHIELD, null);
            case INFANTRY_BOW -> guard(level, pos, Role.BOW, null);
            case INFANTRY_CROSSBOW -> guard(level, pos, Role.CROSSBOW, null);
            case HORSE_SWORD, HORSE_SPEAR -> {
                Mob horse = mount(level, pos, "horse");
                guard(level, pos, this == HORSE_SWORD ? Role.SWORD_SHIELD : Role.SPEAR_SHIELD, horse);
            }
            case CAMEL_GUARDS -> {
                Mob camel = mount(level, pos, "camel");
                // seat 1: melee guard, seat 2: ranged guard
                guard(level, pos, level.getRandom().nextBoolean() ? Role.SWORD_SHIELD : Role.SPEAR_SHIELD, camel);
                guard(level, pos, level.getRandom().nextBoolean() ? Role.BOW : Role.CROSSBOW, camel);
            }
            case GHAST_SQUAD -> {
                Mob ghast = mount(level, pos.add(0, 2, 0), "happy_ghast");
                Item harness = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_harness"));
                ghast.setItemSlot(EquipmentSlot.BODY, new ItemStack(harness));
                // 4 archers: 2 bows + 2 crossbows
                guard(level, pos, Role.BOW, ghast);
                guard(level, pos, Role.CROSSBOW, ghast);
                guard(level, pos, Role.BOW, ghast);
                guard(level, pos, Role.CROSSBOW, ghast);
            }
        }
    }

    // ---------- helpers ----------
    private static Mob mount(ServerLevel level, Vec3 pos, String id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id));
        Entity e = type.create(level, EntitySpawnReason.COMMAND);
        Mob m = (Mob) e;
        m.setPos(pos);
        m.setPersistenceRequired();
        if (m instanceof AbstractHorse h) {
            h.setTamed(true);
            h.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        }
        level.addFreshEntity(m);
        return m;
    }

    private static void guard(ServerLevel level, Vec3 pos, Role role, Mob mount) {
        EntityType<?> type = GuardVillagers.GUARD_VILLAGER;
        Entity e = type.create(level, EntitySpawnReason.COMMAND);
        GuardEntity g = (GuardEntity) e;
        g.setPos(pos);
        g.setGuardVariant(GuardEntity.getRandomTypeForBiome(level, BlockPos.containing(pos)));
        equip(g, role);
        g.setPersistenceRequired();
        level.addFreshEntity(g);
        if (mount != null) g.startRiding(mount, true, true);
    }

    private static void equip(GuardEntity g, Role role) {
        g.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        g.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        g.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        g.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        switch (role) {
            case SWORD_SHIELD -> { hand(g, Items.IRON_SWORD); off(g, Items.SHIELD); }
            case SPEAR_SHIELD -> { hand(g, Items.IRON_SPEAR); off(g, Items.SHIELD); }
            case BOW -> hand(g, Items.BOW);
            case CROSSBOW -> hand(g, Items.CROSSBOW);
        }
    }
    private static void hand(GuardEntity g, Item i) { g.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(i)); }
    private static void off(GuardEntity g, Item i) { g.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(i)); }
}
