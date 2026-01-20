package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import java.util.Comparator;

public class Aura extends Module {
    // --- SETTINGS (Giữ nguyên tên biến để các file khác gọi không lỗi) ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6f);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20);
    public final Setting<AttackHand> attackHand = new Setting<>("AttackHand", AttackHand.MainHand);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);

    // --- BIẾN PUBLIC (Bắt buộc cho Mixin) ---
    public static Entity target;
    public float rotationYaw, rotationPitch;
    public Box resolvedBox;

    private int hitTicks;

    public Aura() { super("Aura", Category.COMBAT); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (mc.player.isUsingItem() && pauseWhileEating.getValue()) return;

        target = findTarget();
        if (target != null) {
            // Cập nhật rotation cho Mixin/ClientLook
            float[] rotations = Managers.PLAYER.calcAngle(target.getEyePos());
            rotationYaw = rotations[0];
            rotationPitch = rotations[1];

            if (clientLook.getValue()) {
                mc.player.setYaw(rotationYaw);
                mc.player.setPitch(rotationPitch);
            }

            if (canAttack()) attack();
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
        swingHand();
        hitTicks = 10;

        if (slot != -1) InventoryUtility.switchTo(slot);
    }

    private boolean canAttack() {
        return hitTicks <= 0 && mc.player.getAttackCooldownProgress(0.5f) >= 0.9f;
    }

    private void swingHand() {
        if (attackHand.getValue() == AttackHand.MainHand) mc.player.swingHand(Hand.MAIN_HAND);
        else if (attackHand.getValue() == AttackHand.OffHand) mc.player.swingHand(Hand.OFF_HAND);
    }

    private Entity findTarget() {
        return mc.world.getEntities().stream()
            .filter(ent -> ent instanceof LivingEntity && ent != mc.player && ent.isAlive() && mc.player.distanceTo(ent) <= attackRange.getValue())
            .filter(ent -> !(ent instanceof PlayerEntity p) || !Managers.FRIEND.isFriend(p))
            .min(Comparator.comparingDouble(ent -> mc.player.squaredDistanceTo(ent))).orElse(null);
    }

    @Override public void onRender3D(MatrixStack stack) {}

    // --- CẤU TRÚC PHỤ TRỢ (Giữ nguyên để fix lỗi Mixin) ---
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
