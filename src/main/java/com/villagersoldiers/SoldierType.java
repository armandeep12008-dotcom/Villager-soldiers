package com.villagersoldiers;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Every unit in the mod. A "unit" can be 1..5 soldiers plus a mount. */
public enum SoldierType {
    INFANTRY_SWORD, INFANTRY_SPEAR, INFANTRY_ARCHER,
    HORSE_SWORD, HORSE_SPEAR,
    CAMEL_GUARDS,
    GHAST_SQUAD_BOW, GHAST_SQUAD_CROSSBOW,
    ARTILLERY;

    /** Role of a single soldier; decides loadout + AI. */
    public enum Role { SWORD_SHIELD, SPEAR_SHIELD, BOW, CROSSBOW, ARTILLERY }

    public void spawn(ServerLevel level, Vec3 pos) {
        switch (this) {
            case INFANTRY_SWORD -> soldier(level, pos, Role.SWORD_SHIELD, null);
            case INFANTRY_SPEAR -> soldier(level, pos, Role.SPEAR_SHIELD, null);
            case INFANTRY_ARCHER -> soldier(level, pos, Role.BOW, null);
            case ARTILLERY -> soldier(level, pos, Role.ARTILLERY, null);
            case HORSE_SWORD, HORSE_SPEAR -> {
                Horse horse = EntityType.HORSE.create(level, EntitySpawnReason.COMMAND);
                prepMount(level, horse, pos);
                soldier(level, pos, this == HORSE_SWORD ? Role.SWORD_SHIELD : Role.SPEAR_SHIELD, horse);
            }
            case CAMEL_GUARDS -> {
                Camel camel = EntityType.CAMEL.create(level, EntitySpawnReason.COMMAND);
                prepMount(level, camel, pos);
                // Camels carry 2: melee guard + ranged guard
                soldier(level, pos, level.random.nextBoolean() ? Role.SWORD_SHIELD : Role.SPEAR_SHIELD, camel);
                soldier(level, pos, level.random.nextBoolean() ? Role.BOW : Role.CROSSBOW, camel);
            }
            case GHAST_SQUAD_BOW, GHAST_SQUAD_CROSSBOW -> {
                Mob ghast = EntityType.HAPPY_GHAST.create(level, EntitySpawnReason.COMMAND);
                prepMount(level, ghast, pos.add(0, 3, 0));
                for (int i = 0; i < 4; i++) // happy ghast seats 4
                    soldier(level, pos, this == GHAST_SQUAD_BOW ? Role.BOW : Role.CROSSBOW, ghast);
            }
        }
    }

    private static void prepMount(ServerLevel level, Mob mount, Vec3 pos) {
        mount.setPos(pos);
        mount.setPersistenceRequired();
        // TODO(26.2): tame/saddle/harness. Horse: setTamed(true) + SADDLE equipment slot.
        // Happy ghast: BODY slot harness item. Verify exact names in your IDE.
        level.addFreshEntity(mount);
    }

    private static void soldier(ServerLevel level, Vec3 pos, Role role, Mob mount) {
        SoldierEntity s = ModEntities.SOLDIER.create(level, EntitySpawnReason.COMMAND);
        s.setPos(pos);
        s.setRole(role);                 // fills the inventory -> equipment
        s.setPersistenceRequired();
        level.addFreshEntity(s);
        if (mount != null) s.startRiding(mount, true);
    }

    static ItemStack[] defaultKit(Role role) {
        // [mainhand, offhand, head, chest, legs, feet, spare...]
        Item head = Items.IRON_HELMET, chest = Items.IRON_CHESTPLATE, legs = Items.IRON_LEGGINGS, feet = Items.IRON_BOOTS;
        return switch (role) {
            case SWORD_SHIELD -> kit(Items.IRON_SWORD, Items.SHIELD, head, chest, legs, feet);
            case SPEAR_SHIELD -> kit(Items.IRON_SPEAR, Items.SHIELD, head, chest, legs, feet);
            case BOW -> withAmmo(kit(Items.BOW, null, head, chest, legs, feet), new ItemStack(Items.ARROW, 64));
            case CROSSBOW -> withAmmo(kit(Items.CROSSBOW, null, head, chest, legs, feet), new ItemStack(Items.ARROW, 64));
            case ARTILLERY -> withAmmo(kit(Items.FLINT_AND_STEEL, null, Items.IRON_HELMET, null, null, null),
                    new ItemStack(Items.FIRE_CHARGE, 32));
        };
    }
    private static ItemStack[] kit(Item... items) {
        ItemStack[] out = new ItemStack[6];
        for (int i = 0; i < 6; i++) out[i] = items[i] == null ? ItemStack.EMPTY : new ItemStack(items[i]);
        return out;
    }
    private static ItemStack[] withAmmo(ItemStack[] base, ItemStack ammo) {
        ItemStack[] out = java.util.Arrays.copyOf(base, 7);
        out[6] = ammo;
        return out;
    }
}
