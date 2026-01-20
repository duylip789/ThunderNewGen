package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Aura extends Module {
    // --- SETTINGS GỐC (Giữ nguyên để không lỗi liên kết) ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f);
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track);
    
    // ESP MỚI: Ghost (Màu xanh nước biển riêng, size tùy chỉnh)
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.Ghost);
    public final Setting<SettingGroup> espGroup = new Setting<>("ESPSettings", new SettingGroup(false, 0), v -> esp.is(ESP.Ghost));
    public final Setting<Float> ghostSize = new Setting<>("GhostSize", 1.8f, 0.5f, 4.0f, v -> esp.is(ESP.Ghost)).addToGroup(espGroup);
    public final Setting<ColorSetting> colorGhost = new Setting<>("ColorGhost", new ColorSetting(new Color(0, 150, 255, 180).getRGB()), v -> esp.is(ESP.Ghost)).addToGroup(espGroup);

    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f);
    public final Setting<Boolean> prediction = new Setting<>("Prediction", true);

    public final Setting<SettingGroup> targets = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("Players", true).addToGroup(targets);
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 10, 0, 20);

    // --- VARIABLES ---
    public static Entity target;
    public float rotationYaw;
    public float rotationPitch;
    private int hitTicks;
    private final Timer pauseTimer = new Timer();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(500)) return;
        updateTarget();
        if (target == null) return;

        // --- HVH LOGIC: Hit nhanh hơn bằng cách ưu tiên HurtTime (Liquid Style) ---
        boolean readyToAttack = false;
        if (target instanceof LivingEntity living) {
            if (living.hurtTime <= 2 || getAttackCooldown() >= attackCooldown.getValue()) {
                readyToAttack = true;
            }
        }

        calcRotations(readyToAttack);

        if (readyToAttack && hitTicks <= 0) {
            attack();
        }
        hitTicks--;
    }

    private void attack() {
        if (target == null) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        hitTicks = 10; 
    }

    private void calcRotations(boolean ready) {
        if (target == null) return;
        Vec3d targetVec = target.getEyePos();
        
        // Găm tâm đón đầu hướng chạy
        if (prediction.getValue()) {
            targetVec = targetVec.add((target.getX() - target.prevX) * 1.5, 0, (target.getZ() - target.prevZ) * 1.5);
        }
        
        float[] angles = MathUtility.calculateAngle(mc.player.getEyePos(), targetVec);
        rotationYaw = angles[0];
        rotationPitch = angles[1];
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (target != null && rotationMode.is(Mode.Track)) {
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
        }
    }

    public void onRender3D(MatrixStack stack) {
        if (target instanceof LivingEntity living && esp.is(ESP.Ghost)) {
            // Sử dụng màu colorGhost riêng biệt
            Render3DEngine.drawTargetEsp(stack, living); 
        }
    }

    private void updateTarget() {
        List<LivingEntity> entities = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof LivingEntity living && ent != mc.player && ent.isAlive()) {
                if (mc.player.distanceTo(ent) <= attackRange.getValue() + aimRange.getValue()) {
                    entities.add(living);
                }
            }
        }
        target = entities.stream().min(Comparator.comparing(e -> mc.player.distanceTo(e))).orElse(null);
    }

    public float getAttackCooldown() {
        return mc.player.getAttackCooldownProgress(0.5f);
    }

    // --- CÁC ĐỊNH NGHĨA PHỤ TRỢ (Để fix lỗi build hệ thống) ---

    public static class Position {
        private final double x, y, z;
        private int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { return ticks++ > ModuleManager.aura.backTicks.getValue(); }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum ESP { Off, Ghost, ThunderHackV2 }
}
