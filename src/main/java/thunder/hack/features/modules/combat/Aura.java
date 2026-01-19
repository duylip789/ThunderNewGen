package thunder.hack.features.modules.combat;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.gui.notification.Notification;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.interfaces.IOtherClientPlayerEntity;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.Color;
import java.util.*;

import static net.minecraft.util.UseAction.BLOCK;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.math.MathUtility.random;

public class Aura extends Module {
    // --- SETTINGS CHÍNH ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("WallRange", 3.1f, 0f, 6.0f);
    public final Setting<Mode> rotationMode = new Setting<>("Rotation", Mode.Track);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    
    // Yêu cầu của bạn: Chế độ Sprint đa năng
    public final Setting<SprintMode> sprintMode = new Setting<>("SprintMode", SprintMode.HvH);

    // NewGen ESP Settings (Giống ảnh 1000189659.jpg)
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.NewGen);
    public final Setting<Color> newGenColor = new Setting<>("ESPColor", new Color(255, 255, 255, 200), v -> esp.is(ESP.NewGen));
    public final Setting<Integer> newGenPoints = new Setting<>("ESPPoints", 15, 1, 50, v -> esp.is(ESP.NewGen));
    public final Setting<Float> newGenSpeed = new Setting<>("ESPSpeed", 0.15f, 0.01f, 1.0f, v -> esp.is(ESP.NewGen));

    // Nâng cao để tối ưu HvH
    public final Setting<SettingGroup> advanced = new Setting<>("Advanced", new SettingGroup(false, 0));
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(advanced);
    public final Setting<Integer> hvhTimer = new Setting<>("HvHTimer", 10, 1, 20, v -> sprintMode.is(SprintMode.HvH)).addToGroup(advanced);

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
        
        if (target == null) {
            soulParticles.clear();
            return;
        }

        // --- LOGIC SPRINT MODE (HVH & SMP) ---
        handleSprintMode();

        // --- LOGIC TẤN CÔNG (HVH OPTIMIZED) ---
        if (canAttack()) {
            attack();
        }

        // --- HIỆU ỨNG NEWGEN ESP ---
        if (esp.is(ESP.NewGen)) {
            if (soulParticles.size() < newGenPoints.getValue()) {
                soulParticles.add(new SoulParticle(target.getPos().add(0, 1, 0)));
            }
            soulParticles.forEach(p -> p.tick(target, newGenSpeed.getValue()));
        }
    }

    private void handleSprintMode() {
        if (target == null) return;
        switch (sprintMode.getValue()) {
            case HvH -> {
                // Tối ưu bám hitbox và chém nhanh, ép sprint liên tục để đạt tốc độ di chuyển cao
                if (mc.player.forwardSpeed > 0 || mc.player.sidewaysSpeed > 0) {
                    mc.player.setSprinting(true);
                }
            }
            case SMP -> {
                // Tối ưu damage: Chỉ sprint khi chạm đất, giúp các cú chém khi đang rơi luôn là Crit
                if (mc.player.fallDistance > 0.08 && !mc.player.isOnGround()) {
                    mc.player.setSprinting(false); // Dừng sprint để đảm bảo Crit dmg to nhất
                } else {
                    mc.player.setSprinting(true);
                }
            }
            case Legit -> {
                if (mc.player.forwardSpeed <= 0) mc.player.setSprinting(false);
            }
        }
    }

    public void attack() {
        // Switch vũ khí nhanh nhất
        int prevSlot = -1;
        SearchInvResult swordResult = InventoryUtility.getSwordHotBar();
        if (swordResult.found()) {
            prevSlot = mc.player.getInventory().selectedSlot;
            swordResult.switchTo();
        }

        // Gửi packet tấn công
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        // Timer tối ưu cho HvH: Giảm cooldown để hit nhanh hơn đối thủ
        hitTicks = sprintMode.is(SprintMode.HvH) ? hvhTimer.getValue() : 11;
        
        if (prevSlot != -1) InventoryUtility.switchTo(prevSlot);
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (target != null && rotationMode.is(Mode.Track)) {
            // Lock-on mục tiêu gắt gao cho HvH
            float[] rots = Managers.PLAYER.calcAngle(target.getEyePos());
            rotationYaw = rots[0];
            rotationPitch = rots[1];
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
        }
    }

    public void onRender3D(MatrixStack stack) {
        if (target == null || !esp.is(ESP.NewGen)) return;
        
        // Vẽ hiệu ứng tia linh hồn bám đuổi (NewGen ESP)
        for (SoulParticle p : soulParticles) {
            // Sử dụng drawBoxOutline siêu nhỏ để làm hạt sáng mà không bị lỗi tham số Sphere
            Box pBox = new Box(p.pos.x - 0.02, p.pos.y - 0.02, p.pos.z - 0.02, p.pos.x + 0.02, p.pos.y + 0.02, p.pos.z + 0.02);
            Render3DEngine.drawBoxOutline(pBox, newGenColor.getValue(), 1.5f);
            
            if (p.trail.size() > 1) {
                for (int i = 0; i < p.trail.size() - 1; i++) {
                    Render3DEngine.drawLine(p.trail.get(i), p.trail.get(i + 1), newGenColor.getValue());
                }
            }
        }
    }

    // --- CÁC HÀM PHỤ TRỢ FIX LỖI "CANNOT FIND SYMBOL" ---
    public void pause() { pauseTimer.reset(); }
    public boolean isAboveWater() { 
        return mc.player.isSubmergedInWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER; 
    }
    
    private boolean canAttack() {
        float cooldown = ((float) ((ILivingEntity) mc.player).getLastAttackedTicks()) / (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0);
        return cooldown >= attackCooldown.getValue() && mc.player.distanceTo(target) <= attackRange.getValue();
    }

    public Entity findTarget() {
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity && e != mc.player && e.isAlive() && mc.player.distanceTo(e) <= attackRange.getValue() + 1) return e;
        }
        return null;
    }

    // Hạt hiệu ứng NewGen
    private static class SoulParticle {
        public Vec3d pos;
        public List<Vec3d> trail = new ArrayList<>();
        private final Random rand = new Random();
        public SoulParticle(Vec3d start) { this.pos = start; }
        public void tick(Entity target, float speed) {
            trail.add(pos);
            if (trail.size() > 12) trail.remove(0);
            Vec3d targetVec = target.getPos().add(0, target.getHeight() / 1.5f, 0);
            Vec3d moveDir = targetVec.subtract(pos).normalize().multiply(speed);
            pos = pos.add(moveDir).add((rand.nextFloat() - 0.5) * 0.1, (rand.nextFloat() - 0.5) * 0.1, (rand.nextFloat() - 0.5) * 0.1);
        }
    }

    public enum Mode { Track, None }
    public enum ESP { Off, NewGen, ThunderHack }
    public enum SprintMode { Legit, None, SMP, HvH }
}
