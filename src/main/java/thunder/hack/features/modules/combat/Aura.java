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
import thunder.hack.utility.player.InventoryUtility;

import java.util.Comparator;

public class Aura extends Module {
    // --- MAIN ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false, v -> switchMode.getValue() != Switch.Silent);

    // --- ATTACK SETTINGS (Bao gồm ESP và Crit) ---
    public final Setting<SettingGroup> attackSettings = new Setting<>("Attack Settings", new SettingGroup(false, 0));
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.ThunderHack).addToGroup(attackSettings);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true)).addToGroup(attackSettings);
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true).addToGroup(attackSettings);
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
    public final Setting<Boolean> ignoreInvisible = new Setting<>("IgnoreInvisible", false).addToGroup(targetsGroup);

    public static Entity target;
    private float lastYaw, lastPitch;

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    @EventHandler
    public void onSync(EventSync event) {
        target = findTarget();
        if (target == null) return;

        // Rotation Logic (GCD Fix + Smoothing)
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

        // Attack Logic (HVH Sprint Reset)
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
        if (smartCrit.getValue().isEnabled() && onlySpace.getValue() && mc.player.isOnGround()) return false;
        
        float cd = attackCooldown.getValue();
        if (tpsSync.getValue()) cd *= (20.0f / Managers.TICK.getTPS());
        return mc.player.getAttackCooldownProgress(0.5f) >= cd;
    }

    private Entity findTarget() {
        float r = elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue();
        return mc.world.getEntities().stream()
                .filter(e -> e != mc.player && e.isAlive() && mc.player.distanceTo(e) <= r)
                .filter(this::isProperTarget)
                .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
                .orElse(null);
    }

    private boolean isProperTarget(Entity e) {
        if (e instanceof PlayerEntity && !Players.getValue()) return false;
        if (e instanceof SlimeEntity && !Slimes.getValue()) return false;
        if (e instanceof VillagerEntity && !Villagers.getValue()) return false;
        if (e instanceof AnimalEntity && !Animals.getValue()) return false;
        if (e instanceof HostileEntity && !hostiles.getValue()) return false;
        if ((e instanceof FireballEntity || e instanceof ShulkerBulletEntity) && !Projectiles.getValue()) return false;
        if (e.isInvisible() && ignoreInvisible.getValue()) return false;
        return true;
    }

    private float[] getHVHRotations(Entity entity) {
        Vec3d targetPos = entity.getBoundingBox().getCenter();
        double diffX = targetPos.x - mc.player.getX();
        double diffY = targetPos.y - mc.player.getEyePos().y;
        double diffZ = targetPos.z - mc.player.getZ();
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
        
        // GCD Fix
        double sens = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double gcd = Math.pow(sens, 3.0) * 1.2;
        return new float[]{(float) (yaw - (yaw % gcd)), (float) (pitch - (pitch % gcd))};
    }

    public enum SprintMode { Off, Normal, HVH }
    public enum Mode { Track, Interact, Grim, None }
    public enum Switch { Normal, None, Silent }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, FOV }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
  
