package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Aura extends Module {

    // ================= MAIN =================
    public final Setting<Float> range = new Setting<>("Range", 3.1f, 1.0f, 6.0f);
    public final Setting<Float> aimRange = new Setting<>("AimRange", 2.0f, 0.0f, 6.0f);
    public final Setting<Float> elytraRangeValue = new Setting<>("ElytraRange", 0.5f, 0.0f, 6.0f);
    public final Setting<Float> elytraAimRange = new Setting<>("ElytraAimRange", 32.0f, 0.0f, 64.0f);
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);

    public final Setting<Boolean> igoneWall = new Setting<>("IgoneWall", true);
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off);
    public final Setting<Switch> autoWeapon = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false);

    // ================= ATTACK =================
    public final Setting<SettingGroup> attackSettings =
            new Setting<>("Attack Settings", new SettingGroup(false, 0));

    public final Setting<BooleanSettingGroup> smartCrit =
            new Setting<>("SmartCrit", new BooleanSettingGroup(true)).addToGroup(attackSettings);

    public final Setting<Boolean> onlySpace =
            new Setting<>("OnlyCrit", false).addToGroup(smartCrit);

    public final Setting<Boolean> autoJump =
            new Setting<>("AutoJump", false).addToGroup(smartCrit);

    public final Setting<Boolean> shieldBreaker =
            new Setting<>("ShieldBreaker", true).addToGroup(attackSettings);

    public final Setting<Boolean> pauseWhileEating =
            new Setting<>("PauseWhileEating", false).addToGroup(attackSettings);

    public final Setting<BooleanSettingGroup> oldDelay =
            new Setting<>("OldDelay", new BooleanSettingGroup(false)).addToGroup(attackSettings);

    public final Setting<Integer> minCPS =
            new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);

    public final Setting<Integer> maxCPS =
            new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);

    public final Setting<Float> attackCooldown =
            new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(attackSettings);

    public final Setting<Float> attackRange =
            new Setting<>("AttackRange", 3.0f, 0.0f, 6.0f).addToGroup(attackSettings);

    // ================= ROTATION =================
    public final Setting<SettingGroup> rotationSettings =
            new Setting<>("Rotation Settings", new SettingGroup(false, 0));

    public final Setting<Mode> rotationMode =
            new Setting<>("RotationMode", Mode.Track).addToGroup(rotationSettings);

    public final Setting<RayTrace> rayTrace =
            new Setting<>("RayTrace", RayTrace.OnlyTarget).addToGroup(rotationSettings);

    public final Setting<Boolean> clientLook =
            new Setting<>("ClientLook", false).addToGroup(rotationSettings);

    public final Setting<Boolean> lockTarget =
            new Setting<>("LockTarget", true).addToGroup(rotationSettings);

    public final Setting<Integer> minYawStep =
            new Setting<>("MinYawStep", 65, 1, 180).addToGroup(rotationSettings);

    public final Setting<Integer> maxYawStep =
            new Setting<>("MaxYawStep", 75, 1, 180).addToGroup(rotationSettings);

    public final Setting<String> sprint = new Setting<>("Sprint", "HVH");
    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);

    // ================= TARGETS =================
    public final Setting<SettingGroup> targetsGroup =
            new Setting<>("Targets", new SettingGroup(false, 0));

    public final Setting<Boolean> players =
            new Setting<>("Players", true).addToGroup(targetsGroup);

    public final Setting<Boolean> mobs =
            new Setting<>("Mobs", true).addToGroup(targetsGroup);

    public final Setting<Boolean> ignoreCreative =
            new Setting<>("IgnoreCreative", true).addToGroup(targetsGroup);

    public final Setting<Boolean> elytraTarget =
            new Setting<>("ElytraTarget", false).addToGroup(targetsGroup);

    public final Setting<Switch> switchMode =
            new Setting<>("SwitchMode", Switch.Normal).addToGroup(attackSettings);

    // ================= VISUAL (ESP) =================
    public final Setting<SettingGroup> visual =
            new Setting<>("Visual", new SettingGroup(false, 0));

    public final Setting<ESPMode> espMode =
            new Setting<>("ESPMode", ESPMode.Ghost).addToGroup(visual);

    public final Setting<Color> espColor =
            new Setting<>("ESPColor", new Color(255, 80, 80, 160)).addToGroup(visual);

    public final Setting<Float> espWidth =
            new Setting<>("ESPWidth", 2.0f, 0.5f, 5.0f).addToGroup(visual);

    // ================= RUNTIME =================
    public static Entity target;
    public float rotationPitch, rotationYaw;
    private final Timer attackTimer = new Timer();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    // ================= ATTACK =================
    @EventHandler
    public void onSync(EventSync e) {
        target = findTarget();
        if (target == null) return;

        if (attackTimer.passedMs(600)) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            attackTimer.reset();
        }
    }

    // ================= ESP RENDER =================
    @Override
    public void onRender3D(MatrixStack stack) {
        if (espMode.getValue() == ESPMode.Off) return;
        if (target == null || mc.player == null || mc.world == null) return;

        Box box = target.getBoundingBox();
        Color c = espColor.getValue();

        if (espMode.getValue() == ESPMode.Box) {
            Render3DEngine.drawBoxOutline(box, c, espWidth.getValue());
            Render3DEngine.drawFilledBox(stack, box, Render2DEngine.injectAlpha(c, 80));
        }

        if (espMode.getValue() == ESPMode.Ghost) {
            Render3DEngine.drawTargetEsp(stack, target);
        }
    }

    // ================= TARGET SELECT =================
    private Entity findTarget() {
        List<Entity> entities = new ArrayList<>();
        mc.world.getEntities().forEach(entities::add);

        List<Entity> targets = entities.stream()
                .filter(e -> e instanceof PlayerEntity)
                .filter(e -> e != mc.player)
                .filter(Entity::isAlive)
                .filter(e -> mc.player.distanceTo(e) <= range.getValue())
                .sorted(Comparator.comparingDouble(mc.player::distanceTo))
                .toList();

        return targets.isEmpty() ? null : targets.get(0);
    }

    // =============================
    // FIX TARGET ESP / POSITION HISTORY
    // =============================
    public static class Position {
        public final double x;
        public final double y;
        public final double z;
        private final long time;

        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = System.currentTimeMillis();
        }

        public Vec3d toVec3d() {
            return new Vec3d(x, y, z);
        }

        public boolean shouldRemove() {
            // giữ lịch sử vị trí ~1s (có thể chỉnh)
            return System.currentTimeMillis() - time > 1000L;
        }
    }

    // =============================
    // API COMPAT FIX (DO NOT TOUCH CORE)
    // =============================

    /** 
     * Dùng cho PearlChaser / AutoBuff
     * Chỉ pause logic bên ngoài, không ảnh hưởng core
     */
    public void pause() {
        // noop – giữ tương thích module
    }

    /**
     * Dùng cho TriggerBot / FakePlayer
     * Trả cooldown vanilla an toàn
     */
    public float getAttackCooldown() {
        if (mc.player == null) return 1.0f;
        return mc.player.getAttackCooldownProgress(0.0f);
    }

    /**
     * Dùng cho TriggerBot
     * Check player có đứng trên nước không
     */
    public boolean isAboveWater() {
        if (mc.player == null || mc.world == null) return false;
        return mc.player.isTouchingWater();
    }

    // ================= ENUM =================
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, FOV }
    public enum Mode { Track, Interact, Grim, None }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Switch { Normal, None, Silent }
    public enum WallsBypass { Off, V1, V2 }
    public enum ESPMode { Off, Box, Ghost }
}
