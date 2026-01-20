package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility; // Đảm bảo có import này
import java.util.Comparator;
import java.util.stream.StreamSupport;

public class Aura extends Module {
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6f);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20);
    public final Setting<AttackHand> attackHand = new Setting<>("AttackHand", AttackHand.MainHand);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);

    public static Entity target;
    public float rotationYaw, rotationPitch;
    public Box resolvedBox;
    private int hitTicks;
    private final Timer pauseTimer = new Timer();

    public Aura() { super("Aura", Category.COMBAT); }

    public void pause() { pauseTimer.reset(); }

    public boolean isAboveWater() {
        return mc.player.isSubmergedInWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER;
    }

    public float getAttackCooldown() {
        return mc.player.getAttackCooldownProgress(0.5f);
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if ((mc.player.isUsingItem() && pauseWhileEating.getValue()) || !pauseTimer.passedMs(500)) return;

        target = findTarget();
        if (target != null) {
            float[] rotations = Managers.PLAYER.calcAngle(target.getEyePos());
            rotationYaw = rotations[0];
            rotationPitch = rotations[1];

            if (clientLook.getValue()) {
                mc.player.setYaw(rotationYaw);
                mc.player.setPitch(rotationPitch);
            }

            if (hitTicks <= 0 && getAttackCooldown() >= 0.9f) attack();
        }
        if (hitTicks > 0) hitTicks--;
    }

    private void attack() {
        int slot = switchMode.getValue() == Switch.Silent ? mc.player.getInventory().selectedSlot : -1;
        if (switchMode.getValue() != Switch.None) {
            int weapon = InventoryUtility.getSwordHotBar().slot();
            if (weapon != -1) InventoryUtility.switchTo(weapon);
        }

        mc.interactionManager.attackEntity(mc.player, target);
        if (attackHand.getValue() == AttackHand.MainHand) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.player.swingHand(Hand.OFF_HAND);
        
        hitTicks = 10;
        if (slot != -1) InventoryUtility.switchTo(slot);
    }

    private Entity findTarget() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false)
            .filter(ent -> ent instanceof LivingEntity && ent != mc.player && ent.isAlive() && mc.player.distanceTo(ent) <= attackRange.getValue())
            .filter(ent -> !(ent instanceof PlayerEntity p) || !Managers.FRIEND.isFriend(p))
            .min(Comparator.comparingDouble(ent -> mc.player.squaredDistanceTo((Entity) ent))).orElse(null);
    }

    @Override public void onRender3D(MatrixStack stack) {}

    public static class Position {
        public double x, y, z;
        public int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { return ticks++ > ModuleManager.aura.backTicks.getValue(); }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2 }
    public enum WallsBypass { Off, V1, V2 }
}
