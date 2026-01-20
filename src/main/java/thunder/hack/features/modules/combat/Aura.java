package thunder.hack.features.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Aura extends Module {
    // --- NHÓM COMBAT (HIT + SPRINT) ---
    public final Setting<SettingGroup> combatGroup = new Setting<>("Combat", new SettingGroup(false, 0));
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f).addToGroup(combatGroup);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(combatGroup);
    public final Setting<SprintMode> sprintMode = new Setting<>("Sprint Mode", SprintMode.Keep).addToGroup(combatGroup);
    public final Setting<Boolean> elytraTarget = new Setting<>("Elytra Target", false).addToGroup(combatGroup);

    // --- NHÓM VISUALS (ESP MỜ ẢO CHUẨN KQQ) ---
    public final Setting<SettingGroup> visualGroup = new Setting<>("Visuals", new SettingGroup(false, 0));
    public final Setting<ESPMode> espMode = new Setting<>("ESP Mode", ESPMode.Liquid).addToGroup(visualGroup);
    public final Setting<ColorSetting> slashColor = new Setting<>("Slash Color", new ColorSetting(new Color(180, 150, 255, 200).getRGB())).addToGroup(visualGroup);
    public final Setting<Float> slashSize = new Setting<>("Slash Size", 1.3f, 0.5f, 2.5f).addToGroup(visualGroup);
    public final Setting<Float> slashSpeed = new Setting<>("Slash Speed", 1.5f, 0.1f, 5.0f).addToGroup(visualGroup);

    public static Entity target;
    private float slashAnim;
    private int hitTicks;
    private final Timer pauseTimer = new Timer();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        updateTarget();
        if (target == null) return;

        // --- SPRINT MODE ---
        if (sprintMode.getValue() == SprintMode.Keep) {
            mc.player.setSprinting(true);
        }

        // --- LOGIC HIT & FULL CRIT ---
        // Chỉ đánh khi đang rơi (fallDistance) hoặc Creative để đảm bảo luôn Crit (HvH Meta)
        boolean isFalling = mc.player.fallDistance > 0.05f && !mc.player.isOnGround();
        
        if (isFalling || mc.player.getAbilities().creativeMode) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= attackCooldown.getValue()) {
                if (hitTicks <= 0) {
                    doAttack();
                    hitTicks = 10; // Tốc độ hit ổn định để bypass anti-cheat
                }
            }
        }
        hitTicks--;
    }

    private void doAttack() {
        if (target == null) return;
        
        // Hỗ trợ Criticals module
        Criticals.cancelCrit = true;
        ModuleManager.criticals.doCrit();
        
        // Gửi packet tấn công và Swing tay
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        Criticals.cancelCrit = false;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (target != null) {
            // Rotations mượt mà về phía mục tiêu
            float[] angles = Managers.PLAYER.calcAngle(target.getEyePos());
            mc.player.setYaw(angles[0]);
            mc.player.setPitch(angles[1]);
        }
    }

    // --- RENDER ESP LIQUID MỜ ẢO (KQQ STYLE) ---
    @Override
    public void onRender3D(MatrixStack stack) {
        if (target instanceof LivingEntity living && espMode.getValue() == ESPMode.Liquid) {
            renderKQQBlurrySlash(stack, living);
        }
    }

    private void renderKQQBlurrySlash(MatrixStack stack, LivingEntity entity) {
        slashAnim += slashSpeed.getValue() * 15f;
        
        // Nội suy tọa độ để ESP không bị lag theo mục tiêu
        double x = entity.prevX + (entity.getX() - entity.prevX) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = entity.prevY + (entity.getY() - entity.prevY) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY() + entity.getHeight() / 2;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        Color color = new Color(slashColor.getValue().getColor());
        stack.push();
        stack.translate(x, y, z);
        
        // HIỆU ỨNG DẸT: Trục Y mỏng (0.15) tạo cảm giác lưỡi kiếm vụt qua mờ ảo
        stack.scale(1.0f, 0.15f, 1.0f); 

        // Vẽ 2 tầng vệt chém đan chéo (Cross-Slash)
        drawArc(stack, slashAnim, slashSize.getValue(), color, 45f);
        drawArc(stack, -slashAnim * 0.85f, slashSize.getValue() * 0.9f, color, -45f);
        
        stack.pop();
    }

    private void drawArc(MatrixStack stack, float rotation, float radius, Color color, float tilt) {
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));
        Matrix4f matrix = stack.peek().getPositionMatrix();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();

        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        
        // Vẽ cung 160 độ để tạo độ "vút" (Slash trail)
        for (int i = 0; i <= 160; i += 8) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle) * radius;
            float sin = (float) Math.sin(angle) * radius;
            
            // Đuôi mờ dần về cuối (Alpha Gradient)
            int alpha = (int) (color.getAlpha() * (1.0f - (i / 160.0f)));
            
            // Vẽ dải Neon Glow: Đỉnh dưới đậm, đỉnh trên tàng hình (Alpha 0)
            buffer.vertex(matrix, cos, -0.1f, sin).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).next();
            buffer.vertex(matrix, cos, 0.7f, sin).color(color.getRed(), color.getGreen(), color.getBlue(), 0).next();
        }
        
        // FIX LỖI BUILD: Render chuẩn 1.20+
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        stack.pop();
    }

    private void updateTarget() {
        List<LivingEntity> list = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof LivingEntity living) || ent == mc.player || !ent.isAlive()) continue;
            
            // Lọc Elytra Target (Có thể Set Bind trong menu)
            if (elytraTarget.getValue() && !living.isFallFlying()) continue;
            
            if (mc.player.distanceTo(ent) <= attackRange.getValue()) list.add(living);
        }
        target = list.stream().min(Comparator.comparing(e -> mc.player.distanceTo(e))).orElse(null);
    }

    // --- ENUMS & CLASSES HỆ THỐNG ---
    public void pause() { pauseTimer.reset(); }
    public enum SprintMode { None, Keep }
    public enum ESPMode { Off, Liquid }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    
    public static class Position {
        public double x, y, z;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }
}
