package com.villagersoldiers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Villager soldier. Extends Villager purely so the vanilla villager renderer can be reused.
 * Brain/trading are bypassed; behaviour is goal-based.
 *
 * INVENTORY LAYOUT (27 slots, shown as a chest GUI on right-click):
 *   0 main hand | 1 off hand | 2 head | 3 chest | 4 legs | 5 feet | 6..26 spare / ammo
 * Whatever you put in slots 0-5 is equipped immediately; ammo is read from 6..26.
 */
public class SoldierEntity extends Villager {
    public static final int INV_SIZE = 27;
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private final SimpleContainer gear = new SimpleContainer(INV_SIZE);
    private SoldierType.Role role = SoldierType.Role.SWORD_SHIELD;
    private boolean syncing = false;

    public SoldierEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        gear.addListener(c -> syncEquipment());
    }

    public static AttributeSupplier.Builder createSoldierAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    // ---------- role / inventory ----------
    public void setRole(SoldierType.Role role) {
        this.role = role;
        ItemStack[] kit = SoldierType.defaultKit(role);
        for (int i = 0; i < kit.length; i++) gear.setItem(i, kit[i]);
        syncEquipment();
    }
    public SimpleContainer getGear() { return gear; }

    private void syncEquipment() {
        if (syncing) return;
        syncing = true;
        for (int i = 0; i < SLOTS.length; i++) {
            setItemSlot(SLOTS[i], gear.getItem(i).copy());
            setDropChance(SLOTS[i], 0f); // inventory drops handled in dropCustomDeathLoot
        }
        syncing = false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> ChestMenu.threeRows(id, inv, gear),
                Component.literal("Soldier (" + role.name().toLowerCase() + ")")));
        }
        return InteractionResult.SUCCESS;   // suppresses villager trading screen
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource src, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, src, killedByPlayer);
        for (int i = 0; i < gear.getContainerSize(); i++) {
            ItemStack s = gear.getItem(i);
            if (!s.isEmpty()) spawnAtLocation(level, s);
        }
        gear.clearContent();
    }

    // ---------- persistence ----------
    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("SoldierRole", role.name());
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < gear.getContainerSize(); i++) items.add(gear.getItem(i));
        out.store("SoldierGear", ItemStack.OPTIONAL_CODEC.listOf(), items);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        in.getString("SoldierRole").ifPresent(s -> { try { role = SoldierType.Role.valueOf(s); } catch (Exception ignored) {} });
        in.read("SoldierGear", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < Math.min(list.size(), gear.getContainerSize()); i++) gear.setItem(i, list.get(i));
        });
        syncEquipment();
    }

    // ---------- AI ----------
    @Override
    protected void customServerAiStep(ServerLevel level) {
        // Intentionally NOT calling super: skips the villager Brain (sleep, panic, trading...).
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SoldierAttackGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    // ---------- combat ----------
    boolean takeAmmo(Item ammo) {
        for (int i = 6; i < gear.getContainerSize(); i++) {
            ItemStack s = gear.getItem(i);
            if (s.is(ammo)) { s.shrink(1); gear.setChanged(); return true; }
        }
        return false;
    }

    /** One goal for all roles: approach, then melee or shoot depending on role. */
    static class SoldierAttackGoal extends Goal {
        private final SoldierEntity s;
        private int cooldown;
        SoldierAttackGoal(SoldierEntity s) { this.s = s; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            LivingEntity t = s.getTarget();
            return t != null && t.isAlive();
        }

        private double range() {
            return switch (s.role) {
                case BOW, CROSSBOW -> 18; case ARTILLERY -> 45; default -> 2.2; };
        }

        @Override public void tick() {
            LivingEntity t = s.getTarget();
            if (t == null) return;
            s.getLookControl().setLookAt(t, 30f, 30f);
            double dist = s.distanceTo(t);
            // If mounted, steer the mount (soldiers can't drive it themselves)
            Mob mover = s.getVehicle() instanceof Mob m ? m : s;
            boolean ranged = s.role != SoldierType.Role.SWORD_SHIELD && s.role != SoldierType.Role.SPEAR_SHIELD;
            if (dist > range() * (ranged ? 0.8 : 1.0)) mover.getNavigation().moveTo(t, 1.2);
            else if (ranged) mover.getNavigation().stop();

            if (cooldown > 0) { cooldown--; return; }
            switch (s.role) {
                case SWORD_SHIELD, SPEAR_SHIELD -> {
                    if (dist <= (s.role == SoldierType.Role.SPEAR_SHIELD ? 3.2 : 2.2)) {
                        s.swing(InteractionHand.MAIN_HAND);
                        s.doHurtTarget((ServerLevel) s.level(), t);
                        cooldown = 20;
                    }
                }
                case BOW, CROSSBOW -> {
                    if (dist <= range() && s.hasLineOfSight(t) && s.takeAmmo(Items.ARROW)) {
                        ItemStack weapon = s.getMainHandItem();
                        AbstractArrow a = ProjectileUtil.getMobArrow(s, new ItemStack(Items.ARROW), 1.0f, weapon);
                        double dx = t.getX() - s.getX(), dz = t.getZ() - s.getZ();
                        double dy = t.getY(0.3333) - a.getY() + Math.sqrt(dx * dx + dz * dz) * 0.2;
                        a.shoot(dx, dy, dz, 1.6f, 6f);
                        s.level().addFreshEntity(a);
                        cooldown = s.role == SoldierType.Role.BOW ? 25 : 40;
                    }
                }
                case ARTILLERY -> {
                    if (dist <= range() && dist > 8 && s.takeAmmo(Items.FIRE_CHARGE)) {
                        Vec3 d = new Vec3(t.getX() - s.getX(), t.getEyeY() - s.getEyeY() + dist * 0.05, t.getZ() - s.getZ()).normalize();
                        LargeFireball shell = new LargeFireball(s.level(), s, d.scale(1.5), 2);
                        shell.setPos(s.getX() + d.x, s.getEyeY() + d.y, s.getZ() + d.z);
                        s.level().addFreshEntity(shell);
                        cooldown = 100;
                    }
                }
            }
        }
    }
}
