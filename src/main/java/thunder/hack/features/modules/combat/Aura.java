package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
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

import java.util.Comparator;

public class Aura extends Module {
    // --- MAIN ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);

    // --- ATTACK SETTINGS (ESP + Crit + Cooldown) ---
    public final Setting<SettingGroup> attackSettings = new Setting<>("Attack Settings", new SettingGroup(false, 0));
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.ThunderHack).addToGroup(attackSettings);
    public final Setting<Integer> espLength = new Setting<>("ESPLength", 14, 1, 40, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(attackSettings);
    public final Setting<Integer> espFactor = new Setting<>("ESPFactor", 8, 1, 20, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(attackSettings);
    public final Setting<Float> espShaking = new Setting<>("ESPShaking", 1.8f, 1.5f, 10f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(attackSettings);
    public final Setting<Float> espAmplitude = new Setting<>("ESPAmplitude", 3f, 0.1f, 8f, v -> esp.getValue() == ESP.ThunderHackV2).addToGroup(attackSettings);

    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true)).addToGroup(attackSettings);
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    
    public final Setting<Boolean> tpsSync = new Setting<>("TPSSync", false).addToGroup(attackSettings);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(attackSettings);
    public final Setting<AttackHand> attackHand = new Setting<>("AttackHand", AttackHand.MainHand).addToGroup(attackSettings);

    // --- ROTATION SETTINGS ---
    public final Setting<SettingGroup> rotationSettings = new Setting<>("Rotation Settings", new SettingGroup(false, 0));
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track).addToGroup(rotationSettings);
    public final Setting<Integer> minYawStep = new Setting<>("MinYawStep", 65, 1, 180).addToGroup(rotationSettings);
    public final Setting<Integer> maxYawStep = new Setting<>("MaxYawStep", 75, 1, 180).addToGroup(rotationSettings);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false).addToGroup(rotationSettings);
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f).addToGroup(rotationSettings);
    public final Setting<RayTrace> rayTrace = new Setting<>("RayTrace", RayTrace.OnlyTarget).addToGroup(rotationSettings);

    public final Setting<SprintMode> sprint = new Setting<>("Sprint", SprintMode.HVH);
    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);

    // --- TARGETS ---
    public final Setting<SettingGroup> targetsGroup = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("Players", true).addToGroup(targetsGroup);
    public final Setting<Boolean> Mobs = new Setting<>("Mobs", true).addToGroup(targetsGroup);
    public final Setting<Boolean> Animals = new Setting<>("Animals", true).addToGroup(targetsGroup);
    public final Setting<Boolean> Villagers = new Setting<>("Villagers", true).addToGroup(targetsGroup);
    public final Setting<Boolean> Slimes = new Setting<>("Slimes", true).addToGroup(targetsGroup);
    public final Setting<Boolean> hostiles = new Setting<>("Hostiles", true).addToGroup(targetsGroup);
    public final Setting<Boolean> Projectiles = new Setting<>("Projectiles", true).addToGroup(targetsGroup);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true).addToGroup(targetsGroup);
    
    // Thêm Setting này để Mixin không bị lỗi
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20);

    public static Entity target;
    private float lastYaw, lastPitch;
    private final Timer fireworkTimer = new Timer();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    @EventHandler
    public void onSync(EventSync event) {
        target = findTarget();
        if (target == null) return;

        float[] rots = getHVHRotations(target);
        float yawDiff = MathHelper.wrapDegrees(rots[0] - lastYaw);
        float yawStep = MathHelper.clamp(yawDiff, -maxYawStep.getValue(), maxYawStep.getValue());
        if (Math.abs(yawStep) < minYawStep.getValue()) yawStep = MathHelper.clamp(yawStep, -minYawStep.getValue(), minYawStep.getValue());

        lastYaw += yawStep;
        lastPitch = rots[1];

        event.setYaw(lastYaw);
        event.setPitch(lastPitch);
        if (clientLook.getValue()) {
            mc.player.setYaw(lastYaw);
            mc.player.setPitch(lastPitch);
        }

        // Elytra Target Logic
        if (elytraTarget.getValue() && target instanceof PlayerEntity pl && pl.isFallFlying()) {
            if (fireworkTimer.passedMs(500)) {
                int fw = InventoryUtility.getItemSlot(Items.FIREWORK_ROCKET);
                if (fw != -1) {
                    int old = mc.player.getInventory().selectedSlot;
                    InventoryUtility.switchTo(fw);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    InventoryUtility.switchTo(old);
                    fireworkTimer.reset();
                }
            }
        }

        // Attack Logic
        if (canAttack()) {
            if (sprint.getValue() == SprintMode.HVH) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(attackHand.getValue() == AttackHand.OffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
            if (sprint.getValue() == SprintMode.HVH) {
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
        }
    }

    private boolean canAttack() {
        if (!(target instanceof LivingEntity)) return false;
        if (smartCrit.getValue().isEnabled() && onlySpace.getValue() && (mc.player.isOnGround() || mc.player.fallDistance < 0.1f)) return false;
        float cd = attackCooldown.getValue();
        if (tpsSync.getValue()) cd *= (20.0f / Managers.TICK.getTPS());
        return mc.player.getAttackCooldownProgress(0.5f) >= cd;
    }

    private Entity findTarget() {
        float r = elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue();
        return mc.world.getEntities().stream()
                .filter(e -> e != mc.player && e.isAlive() && mc.player.distanceTo(e) <= r + aimRange.getValue())
                .filter(this::isProperTarget)
                .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                .orElse(null);
    }

    private boolean isProperTarget(Entity e) {
        if (e instanceof PlayerEntity) return Players.getValue();
        if (e instanceof SlimeEntity) return Slimes.getValue();
        if (e instanceof HostileEntity) return hostiles.getValue();
        if (e instanceof VillagerEntity) return Villagers.getValue();
        if (e instanceof AnimalEntity) return Animals.getValue();
        if (e instanceof FireballEntity || e instanceof ShulkerBulletEntity) return Projectiles.getValue();
        return false;
    }

    private float[] getHVHRotations(Entity entity) {
        Vec3d targetPos = entity.getBoundingBox().getCenter();
        double diffX = targetPos.x - mc.player.getX();
        double diffY = targetPos.y - mc.player.getEyePos().y;
        double diffZ = targetPos.z - mc.player.getZ();
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        return new float[]{(float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F, (float) -Math.toDegrees(Math.atan2(diffY, diffXZ))};
    }

    // --- CÁC CLASS/ENUM CẦN THIẾT ĐỂ FIX LỖI BUILD MIXIN ---
    public static class Position {
        private double x, y, z;
        private int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { return ticks++ > 20; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Mode { Interact, Track, Grim, None }
    public enum SprintMode { Off, Normal, HVH }
    public enum Switch { Normal, None, Silent }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, FOV }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, ThunderHackV2 }
                    }
    
