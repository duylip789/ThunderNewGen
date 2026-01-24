package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.core.ModuleManager;

import java.util.ArrayList;
import java.util.List;

public class Aura extends Module {
    // --- MAIN SETTINGS ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);

    // --- ATTACK SETTINGS (Gộp ESP và Crit) ---
    public final Setting<SettingGroup> attackSettings = new Setting<>("Attack Settings", new SettingGroup(false, 0));
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.ThunderHack).addToGroup(attackSettings);
    public final Setting<Integer> espLength = new Setting<>("ESPLength", 14, 1, 40, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(attackSettings);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true)).addToGroup(attackSettings);
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(attackSettings);
    public final Setting<AttackHand> attackHand = new Setting<>("AttackHand", AttackHand.MainHand).addToGroup(attackSettings);
    public final Setting<SprintMode> sprint = new Setting<>("Sprint", SprintMode.HVH).addToGroup(attackSettings);

    // --- ROTATION SETTINGS ---
    public final Setting<SettingGroup> rotationSettings = new Setting<>("Rotation Settings", new SettingGroup(false, 0));
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track).addToGroup(rotationSettings);
    public final Setting<Integer> maxYawStep = new Setting<>("MaxYawStep", 75, 1, 180).addToGroup(rotationSettings);
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f).addToGroup(rotationSettings);

    // --- TARGETS ---
    public final Setting<SettingGroup> targetsGroup = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("Players", true).addToGroup(targetsGroup);
    public final Setting<Boolean> hostiles = new Setting<>("Hostiles", true).addToGroup(targetsGroup);
    public final Setting<Boolean> Slimes = new Setting<>("Slimes", true).addToGroup(targetsGroup);
    public final Setting<Boolean> Villagers = new Setting<>("Villagers", false).addToGroup(targetsGroup);
    public final Setting<Boolean> Animals = new Setting<>("Animals", false).addToGroup(targetsGroup);
    public final Setting<Boolean> Projectiles = new Setting<>("Projectiles", true).addToGroup(targetsGroup);

    // --- BIẾN PHỤC VỤ MIXIN & RESOLVER (BẮT BUỘC) ---
    public float rotationYaw, rotationPitch;
    public Box resolvedBox;
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20);

    public static Entity target;
    private final Timer fireworkTimer = new Timer();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    @EventHandler
    public void onSync(EventSync event) {
        target = findTarget();
        if (target == null) return;

        // Logic Xoay Tâm
        float[] rots = getHVHRotations(target);
        this.rotationYaw = rots[0];
        this.rotationPitch = rots[1];

        // Ép xoay tâm (Dùng biến event.yaw trực tiếp vì bản cũ không có setYaw)
        event.yaw = rots[0];
        event.pitch = rots[1];

        // Logic Attack + Sprint Reset
        if (canAttack()) {
            // SPRINT RESET LOGIC (Ép sát thương Crit)
            if (sprint.getValue() == SprintMode.HVH) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }

            // Thực hiện đòn đánh
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(attackHand.getValue() == AttackHand.OffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);

            // START SPRINT lại ngay sau khi đánh
            if (sprint.getValue() == SprintMode.HVH) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
        }
    }

    private boolean canAttack() {
        if (!(target instanceof LivingEntity)) return false;
        // Chỉ crit khi đang rơi (nếu OnlyCrit bật)
        if (onlySpace.getValue() && (mc.player.isOnGround() || mc.player.fallDistance < 0.12f)) return false;
        
        float cd = attackCooldown.getValue();
        return mc.player.getAttackCooldownProgress(0.5f) >= cd;
    }

    private Entity findTarget() {
        Entity best = null;
        double minDistance = attackRange.getValue() + aimRange.getValue();

        // Duyệt loop để tránh lỗi .stream()
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            
            if (e instanceof PlayerEntity && !Players.getValue()) continue;
            if (e instanceof SlimeEntity && !Slimes.getValue()) continue;
            if (e instanceof HostileEntity && !hostiles.getValue()) continue;
            if (e instanceof VillagerEntity && !Villagers.getValue()) continue;
            if (e instanceof AnimalEntity && !Animals.getValue()) continue;
            if ((e instanceof FireballEntity || e instanceof ShulkerBulletEntity) && !Projectiles.getValue()) continue;

            double dist = mc.player.distanceTo(e);
            if (dist < minDistance) {
                minDistance = dist;
                best = e;
            }
        }
        return best;
    }

    private float[] getHVHRotations(Entity entity) {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d targetPos = entity.getBoundingBox().getCenter();
        double diffX = targetPos.x - eyes.x;
        double diffY = targetPos.y - eyes.y;
        double diffZ = targetPos.z - eyes.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
        return new float[]{yaw, pitch};
    }

    // --- CLASS CHO BACKTRACK (MIXIN CẦN) ---
    public static class Position {
        public double x, y, z;
        public int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { 
            return ticks++ > ModuleManager.aura.backTicks.getValue(); 
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    public enum SprintMode { Off, Normal, HVH }
    public enum Mode { Track, Interact, Grim, None }
    public enum Switch { Normal, None, Silent }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, ThunderHackV2 }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum WallsBypass { Off, V1, V2 }
    }
                                                    
