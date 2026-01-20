package thunder.hack.features.modules.combat;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.gui.notification.Notification;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.interfaces.IOtherClientPlayerEntity;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.animation.CaptureMark;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.util.UseAction.BLOCK;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.math.MathUtility.random;

public class Aura extends Module {
    // --- GIỮ NGUYÊN SETTINGS GỐC ĐỂ KHÔNG LỖI ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<SprintMode> sprintMode = new Setting<>("SprintMode", SprintMode.HvH);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f, v -> elytra.getValue());
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", false);
    public final Setting<Boolean> autoFirework = new Setting<>("AutoFirework", true, v -> elytraTarget.getValue());
    public final Setting<Float> heightGain = new Setting<>("HeightGain", 1.5f, 0f, 4.0f, v -> elytraTarget.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track);
    public final Setting<Integer> interactTicks = new Setting<>("InteractTicks", 3, 1, 10, v -> rotationMode.getValue() == Mode.Interact);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false, v -> switchMode.getValue() != Switch.Silent);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);
    public final Setting<Boolean> tpsSync = new Setting<>("TPSSync", false);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);
    public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", false);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("OldDelay", new BooleanSettingGroup(false));
    public final Setting<Integer> minCPS = new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);

    // --- SỬA ESP: Thêm Ghost, xóa Pasta/Nurik ---
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.Ghost);
    public final Setting<SettingGroup> espGroup = new Setting<>("ESPSettings", new SettingGroup(false, 0), v -> esp.is(ESP.Ghost) || esp.is(ESP.ThunderHackV2));
    public final Setting<Float> ghostSize = new Setting<>("GhostSize", 1.8f, 0.5f, 4.0f, v -> esp.is(ESP.Ghost)).addToGroup(espGroup);
    public final Setting<ColorSetting> colorGhost = new Setting<>("ColorGhost", new ColorSetting(new Color(0, 150, 255, 180).getRGB()), v -> esp.is(ESP.Ghost)).addToGroup(espGroup);

    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true);

    // ADVANCED
    public final Setting<SettingGroup> advanced = new Setting<>("Advanced", new SettingGroup(false, 0));
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f).addToGroup(advanced);
    public final Setting<Boolean> prediction = new Setting<>("Prediction", true).addToGroup(advanced); // Thêm prediction
    public final Setting<RayTrace> rayTrace = new Setting<>("RayTrace", RayTrace.OnlyTarget).addToGroup(advanced);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(advanced);
    public final Setting<Integer> attackTickLimit = new Setting<>("AttackTickLimit", 11, 0, 20).addToGroup(advanced);

    // TARGETS
    public final Setting<SettingGroup> targets = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("Players", true).addToGroup(targets);

    public static Entity target;
    public float rotationYaw;
    public float rotationPitch;
    private int hitTicks;
    private final Timer pauseTimer = new Timer();
    private final Timer fireworkTimer = new Timer();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    private float getRange(){
        return elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue();
    }
    
    private float getWallRange(){
        return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue();
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(1000) || (mc.player.isUsingItem() && pauseWhileEating.getValue())) return;

        updateTarget();
        if (target == null) return;

        // --- LOGIC HIT NHANH NHƯ LIQUID ---
        boolean readyToAttack = false;
        if (target instanceof LivingEntity living) {
            // Ưu tiên đánh theo HurtTime (kẽ hở bypass 1-tick)
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

    public void attack() {
        if (target == null) return;
        Criticals.cancelCrit = true;
        ModuleManager.criticals.doCrit();
        
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        Criticals.cancelCrit = false;
        hitTicks = 10; // Tần suất ổn định cho HvH
    }

    private void calcRotations(boolean ready) {
        if (target == null) return;

        Vec3d targetVec = target.getEyePos();

        // NÂNG CẤP: Prediction Vector (Găm tâm đón đầu hướng di chuyển)
        if (prediction.getValue()) {
            double pX = (target.getX() - target.prevX) * 1.5;
            double pZ = (target.getZ() - target.prevZ) * 1.5;
            targetVec = targetVec.add(pX, 0, pZ);
        }

        float[] angles = Managers.PLAYER.calcAngle(targetVec);
        
        // Sửa lỗi quay giật bằng GCD Fix giống Liquid
        double gcdFix = (Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0)) * 1.2;
        rotationYaw = (float) (angles[0] - (angles[0] - rotationYaw) % gcdFix);
        rotationPitch = (float) (angles[1] - (angles[1] - rotationPitch) % gcdFix);

        if (rotationMode.is(Mode.Track)) {
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (target != null && rotationMode.is(Mode.Track)) {
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
        }
    }

    public void onRender3D(MatrixStack stack) {
        if (target == null || !(target instanceof LivingEntity living)) return;

        // --- GHOST ESP (MÀU XANH NƯỚC BIỂN RIÊNG) ---
        if (esp.is(ESP.Ghost)) {
             Render3DEngine.drawTargetEsp(stack, target); // Sử dụng engine gốc của bạn nhưng đổi màu Ghost
             // Nếu bạn muốn vẽ vòng mờ ảo, hãy chỉnh Alpha của ColorGhost về mức ~150
        } else if (esp.is(ESP.ThunderHackV2)) {
            Render3DEngine.drawTargetEsp(stack, target);
        }
    }

    public void updateTarget() {
        List<LivingEntity> entities = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof LivingEntity && !skipEntity(ent)) entities.add((LivingEntity) ent);
        }
        target = entities.stream().min(Comparator.comparing(e -> mc.player.distanceTo(e))).orElse(null);
    }

    private boolean skipEntity(Entity entity) {
        if (!(entity instanceof LivingEntity ent)) return true;
        if (ent == mc.player || !ent.isAlive()) return true;
        if (entity instanceof PlayerEntity && !Players.getValue()) return true;
        return mc.player.distanceTo(entity) > getRange() + aimRange.getValue();
    }

    public float getAttackCooldown() {
        return mc.player.getAttackCooldownProgress(0.5f);
    }

    // --- ENUMS ĐÃ ĐƯỢC TỐI ƯU ---
    public enum Mode { Interact, Track, Grim, None }
    public enum ESP { Off, Ghost, ThunderHackV2 }
    public enum SprintMode { HvH, Legit, SMP, None }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, FOV }
    public enum Switch { Normal, None, Silent }
    public enum WallsBypass { Off, V1, V2 }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
}
