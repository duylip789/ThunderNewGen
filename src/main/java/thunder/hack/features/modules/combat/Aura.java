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
import thunder.hack.setting.impl.BooleanSettingGroup;
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

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.util.UseAction.BLOCK;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.math.MathUtility.random;

public class Aura extends Module {
    // --- BASIC SETTINGS ---
    public final Setting<Float> attackRange = new Setting<>("range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("through walls range", 3.1f, 0f, 6.0f);
    public final Setting<Mode> rotationMode = new Setting<>("rotation mode", Mode.Track);
    public final Setting<SprintMode> sprintMode = new Setting<>("sprint mode", SprintMode.HvH);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("smart crit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("only crit", false).addToGroup(smartCrit);

    // --- NEWGEN ESP SETTINGS ---
    public final Setting<ESP> esp = new Setting<>("esp", ESP.NewGen);
    public final Setting<Color> newGenColor = new Setting<>("newgen color", new Color(255, 255, 255, 200), v -> esp.is(ESP.NewGen));
    public final Setting<Integer> newGenPoints = new Setting<>("newgen points", 20, 1, 50, v -> esp.is(ESP.NewGen));
    public final Setting<Float> newGenSpeed = new Setting<>("newgen speed", 0.15f, 0.01f, 1.0f, v -> esp.is(ESP.NewGen));

    // --- ADVANCED & HVH ---
    public final Setting<SettingGroup> advanced = new Setting<>("advanced", new SettingGroup(false, 0));
    public final Setting<Integer> backTicks = new Setting<>("back ticks", 4, 1, 20).addToGroup(advanced);
    public final Setting<Float> attackCooldown = new Setting<>("attack cooldown", 0.9f, 0.5f, 1f).addToGroup(advanced);
    public final Setting<Integer> hvhTimer = new Setting<>("hvh timer", 10, 1, 20, v -> sprintMode.is(SprintMode.HvH)).addToGroup(advanced);
    public final Setting<Boolean> shieldBreaker = new Setting<>("shield breaker", true).addToGroup(advanced);
    public final Setting<RayTrace> rayTrace = new Setting<>("ray trace", RayTrace.OnlyTarget).addToGroup(advanced);
    public final Setting<Resolver> resolver = new Setting<>("resolver", Resolver.Off).addToGroup(advanced);

    public static Entity target;
    public float rotationYaw, rotationPitch;
    private int hitTicks;
    private final Timer pauseTimer = new Timer();
    private final List<SoulParticle> soulParticles = new ArrayList<>();
    private static boolean wasTargeted = false;

    public Aura() { super("Aura", Category.COMBAT); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(1000)) return;
        target = findTarget();
        if (target == null) { soulParticles.clear(); return; }

        handleSprintLogic();

        if (canAttack()) {
            attack();
        }

        if (esp.is(ESP.NewGen)) {
            if (soulParticles.size() < newGenPoints.getValue()) {
                soulParticles.add(new SoulParticle(target.getEyePos()));
            }
            soulParticles.forEach(p -> p.tick(target, newGenSpeed.getValue()));
        }
    }

    private void handleSprintLogic() {
        switch (sprintMode.getValue()) {
            case HvH -> {
                if (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0) mc.player.setSprinting(true);
            }
            case SMP -> {
                if (mc.player.fallDistance > 0.1f && !mc.player.isOnGround()) mc.player.setSprinting(false);
                else mc.player.setSprinting(true);
            }
            case Legit -> { if (mc.player.forwardSpeed <= 0) mc.player.setSprinting(false); }
        }
    }

    public void attack() {
        int prevSlot = -1;
        SearchInvResult swordResult = InventoryUtility.getSwordHotBar();
        if (swordResult.found()) { prevSlot = mc.player.getInventory().selectedSlot; swordResult.switchTo(); }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        hitTicks = sprintMode.is(SprintMode.HvH) ? hvhTimer.getValue() : 11;
        if (prevSlot != -1) InventoryUtility.switchTo(prevSlot);
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (target != null && rotationMode.is(Mode.Track)) {
            float[] rots = Managers.PLAYER.calcAngle(target.getEyePos());
            rotationYaw = rots[0]; rotationPitch = rots[1];
            mc.player.setYaw(rotationYaw); mc.player.setPitch(rotationPitch);
        }
    }

    public void onRender3D(MatrixStack stack) {
        if (target == null) return;
        if (esp.is(ESP.NewGen)) {
            for (SoulParticle p : soulParticles) {
                // Fix lỗi render: Dùng drawLine vẽ điểm thay cho Sphere
                Vec3d point = p.pos;
                Render3DEngine.drawLine(point.add(0,0.01,0), point.add(0,-0.01,0), newGenColor.getValue());
                if (p.trail.size() > 1) {
                    for (int i = 0; i < p.trail.size() - 1; i++) {
                        Render3DEngine.drawLine(p.trail.get(i), p.trail.get(i + 1), newGenColor.getValue());
                    }
                }
            }
        } else if (esp.is(ESP.ThunderHack)) {
            Render3DEngine.drawTargetEsp(stack, target);
        }
    }

    // --- CÁC HÀM BẮT BUỘC ĐỂ KHÔNG LỖI SYMBOL ---
    public void pause() { pauseTimer.reset(); }
    public boolean isAboveWater() { return mc.player.isSubmergedInWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER; }
    private boolean canAttack() { return mc.player.getAttackCooldownProgress(0.5f) >= attackCooldown.getValue() && mc.player.distanceTo(target) <= attackRange.getValue(); }
    private Entity findTarget() {
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity && e != mc.player && e.isAlive() && mc.player.distanceTo(e) <= attackRange.getValue() + 1) return e;
        }
        return null;
    }

    // --- TOÀN BỘ ENUM VÀ CLASS PHỤ TRỢ (GIỮ NGUYÊN ĐỂ MIXIN KHÔNG LỖI) ---
    public static class Position {
        private double x, y, z; private int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { return ticks++ > ModuleManager.aura.backTicks.getValue(); }
        public double getX() { return x; } public double getY() { return y; } public double getZ() { return z; }
    }

    private static class SoulParticle {
        public Vec3d pos; public List<Vec3d> trail = new ArrayList<>(); private final Random rand = new Random();
        public SoulParticle(Vec3d start) { this.pos = start; }
        public void tick(Entity target, float speed) {
            trail.add(pos); if (trail.size() > 15) trail.remove(0);
            Vec3d targetVec = target.getPos().add(0, target.getHeight() / 1.5f, 0);
            Vec3d moveDir = targetVec.subtract(pos).normalize().multiply(speed);
            pos = pos.add(moveDir).add((rand.nextFloat() - 0.5) * 0.12, (rand.nextFloat() - 0.5) * 0.12, (rand.nextFloat() - 0.5) * 0.12);
        }
    }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, NewGen }
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
    public enum SprintMode { Legit, None, SMP, HvH }
}
