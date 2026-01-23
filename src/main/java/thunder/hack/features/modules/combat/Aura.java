package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Aura extends Module {
    // --- CÀI ĐẶT CHÍNH (THEO THỨ TỰ TRONG ẢNH) ---
    public final Setting<Float> range = new Setting<>("Range", 3.1f, 1.0f, 6.0f);
    public final Setting<Float> aimRange = new Setting<>("AimRange", 2.0f, 0.0f, 6.0f);
    public final Setting<Float> elytraRangeValue = new Setting<>("ElytraRange", 0.5f, 0.0f, 6.0f);
    public final Setting<Float> elytraAimRange = new Setting<>("ElytraAimRange", 32.0f, 0.0f, 64.0f);
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    
    public final Setting<Boolean> igoneWall = new Setting<>("IgoneWall", true); 
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off);
    public final Setting<Switch> autoWeapon = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false);

    // --- NHÓM ATTACK SETTINGS ---
    public final Setting<SettingGroup> attackSettings = new Setting<>("Attack Settings", new SettingGroup(false, 0));
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true)).addToGroup(attackSettings);
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true).addToGroup(attackSettings);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false).addToGroup(attackSettings);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("OldDelay", new BooleanSettingGroup(false)).addToGroup(attackSettings);
    public final Setting<Integer> minCPS = new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(attackSettings);
    public final Setting<Float> attackRange = new Setting<>("AttackRange", 3.0f, 0.0f, 6.0f).addToGroup(attackSettings);

    // --- NHÓM ROTATION SETTINGS ---
    public final Setting<SettingGroup> rotationSettings = new Setting<>("Rotation Settings", new SettingGroup(false, 0));
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track).addToGroup(rotationSettings);
    public final Setting<RayTrace> rayTrace = new Setting<>("RayTrace", RayTrace.OnlyTarget).addToGroup(rotationSettings);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false).addToGroup(rotationSettings);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true).addToGroup(rotationSettings);
    public final Setting<Integer> minYawStep = new Setting<>("MinYawStep", 65, 1, 180).addToGroup(rotationSettings);
    public final Setting<Integer> maxYawStep = new Setting<>("MaxYawStep", 75, 1, 180).addToGroup(rotationSettings);

    public final Setting<String> sprint = new Setting<>("Sprint", "HVH");
    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);

    // --- NHÓM TARGETS ---
    public final Setting<SettingGroup> targetsGroup = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> players = new Setting<>("Players", true).addToGroup(targetsGroup);
    public final Setting<Boolean> mobs = new Setting<>("Mobs", true).addToGroup(targetsGroup);
    public final Setting<Boolean> ignoreCreative = new Setting<>("IgnoreCreative", true).addToGroup(targetsGroup);

    // --- BIẾN CẦN THIẾT ĐỂ FIX LỖI CÁC MODULE KHÁC ---
    public static Entity target;
    public float rotationPitch, rotationYaw;
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", false).addToGroup(targetsGroup);
    public final Setting<Switch> switchMode = new Setting<>("SwitchMode", Switch.Normal).addToGroup(attackSettings);
    
    private final Timer attackTimer = new Timer();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    // Các hàm phụ trợ để fix lỗi PearlChaser, TriggerBot, FakePlayer...
    public void pause() {}
    
    public float getAttackCooldown() {
        return mc.player.getAttackCooldownProgress(0.5f);
    }

    public boolean isAboveWater() {
        return mc.player.isSubmergedInWater() || mc.player.isInSwimmingPose();
    }

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

    private Entity findTarget() {
        // Fix lỗi stream() bằng cách convert Iterable sang List trước
        List<Entity> allEntities = new ArrayList<>();
        mc.world.getEntities().forEach(allEntities::add);

        List<Entity> targets = allEntities.stream()
                filter(entity -> entity instanceof PlayerEntity && entity != mc.player && entity.isAlive())
                .filter(entity -> mc.player.distanceTo(entity) <= range.getValue())
                .toList();

        if (sort.getValue() == Sort.LowestDistance) {
            targets = targets.stream()
                    .sorted(Comparator.comparingDouble(mc.player::distanceTo))
                    .toList();
        }
        return targets.isEmpty() ? null : targets.get(0);
    }

    public enum Sort { LowestDistance, HighestDistance, LowestHealth, FOV }
    public enum Mode { Track, Interact, Grim, None }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Switch { Normal, None, Silent }
    public enum WallsBypass { Off, V1, V2 }

    public static class Position {
        public double x, y, z;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        
        // Thêm hàm này để fix lỗi MixinEntityLiving
        public boolean shouldRemove() {
            return false;
        }
    }
}
