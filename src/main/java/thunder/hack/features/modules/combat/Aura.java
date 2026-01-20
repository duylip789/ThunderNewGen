package thunder.hack.features.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Aura extends Module {
    // --- SETTINGS (Giữ nguyên cấu trúc Thunder gốc) ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.95f, 0.5f, 1f);

    // --- LIQUID SLASH ESP 100% KHANGQUIQUAI ---
    public final Setting<ColorSetting> slashColor = new Setting<>("SlashColor", new ColorSetting(new Color(255, 255, 255, 255).getRGB()));
    public final Setting<Float> slashSize = new Setting<>("SlashSize", 1.5f, 0.5f, 3.0f);
    public final Setting<Float> slashSpeed = new Setting<>("SlashSpeed", 1.5f, 0.1f, 4.0f);

    // --- TARGETS ---
    public final Setting<SettingGroup> targets = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> players = new Setting<>("Players", true).addToGroup(targets);
    public final Setting<Boolean> mobs = new Setting<>("Mobs", true).addToGroup(targets);
    public final Setting<Boolean> animals = new Setting<>("Animals", false).addToGroup(targets);
    public final Setting<Boolean> slimes = new Setting<>("Slimes", false).addToGroup(targets);

    public static Entity target;
    private float slashAnim;
    private int hitTicks;
    private final Timer pauseTimer = new Timer();

    public Aura() { super("Aura", Category.COMBAT); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        updateTarget();
        if (target == null) return;

        // ÉP FULL CRIT: Sát thương to nhất, gõ phát nào nổ vệt đỏ phát đó
        boolean isFalling = mc.player.fallDistance > 0.05f && !mc.player.isOnGround();
        
        if (isFalling || mc.player.getAbilities().creativeMode || mc.player.isSubmergedInWater()) {
            if (mc.player.getAttackCooldownProgress(0.5f) >= attackCooldown.getValue()) {
                if (hitTicks <= 0) {
                    attack();
                    hitTicks = 9; // Tốc độ hit dính LiquidBounce
                }
            }
        }
        hitTicks--;
    }

    private void attack() {
        if (target == null) return;
        Criticals.cancelCrit = true;
        ModuleManager.criticals.doCrit(); 
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        Criticals.cancelCrit = false;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (target != null) {
            float[] angles = Managers.PLAYER.calcAngle(target.getEyePos());
            mc.player.setYaw(angles[0]);
            mc.player.setPitch(angles[1]);
        }
    }

    public void onRender3D(MatrixStack stack) {
        if (target instanceof LivingEntity living) {
            renderKhangSlash(stack, living);
        }
    }

    // --- LOGIC VẼ SLASH GIỐNG KHANGQUIQUAI 100% ---
    private void renderKhangSlash(MatrixStack stack, LivingEntity entity) {
        slashAnim += slashSpeed.getValue() * 10f;
        
        double x = entity.prevX + (entity.getX() - entity.prevX) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double y = entity.prevY + (entity.getY() - entity.prevY) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getY() + entity.getHeight() / 2;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * Render3DEngine.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        Color color = slashColor.getValue().getColor();
        
        stack.push();
        stack.translate(x, y, z);

        // KhangQuiquai thường có 2-3 vệt đan chéo cực mượt
        drawSlashLayer(stack, slashAnim, slashSize.getValue(), color, 45f);   // Tia chính
        drawSlashLayer(stack, -slashAnim * 0.7f, slashSize.getValue() * 0.8f, color, -45f); // Tia phụ
        
        stack.pop();
    }

    private void drawSlashLayer(MatrixStack stack, float rotation, float radius, Color color, float tilt) {
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));

        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.defaultBlendFunc();

        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        
        // Vẽ vệt chém theo cung tròn 180 độ để tạo độ dài
        for (int i = 0; i <= 180; i += 5) {
            float angle = (float) Math.toRadians(i);
            float cos = (float) Math.cos(angle) * radius;
            float sin = (float) Math.sin(angle) * radius;
            
            // Alpha mờ dần từ đầu đến cuối vệt để tạo hiệu ứng Trail (đuôi)
            int alpha = (int) (color.getAlpha() * (1.0f - (i / 180.0f)));
            
            // Vẽ dải Neon mỏng
            buffer.vertex(matrix, cos, -0.02f, sin).color(color.getRed(), color.getGreen(), color.getBlue(), alpha).next();
            buffer.vertex(matrix, cos, 0.12f, sin).color(color.getRed(), color.getGreen(), color.getBlue(), 0).next();
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        stack.pop();
    }

    private void updateTarget() {
        List<LivingEntity> list = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof LivingEntity living) || ent == mc.player || !ent.isAlive()) continue;
            if (ent instanceof PlayerEntity && !players.getValue()) continue;
            if (ent instanceof HostileEntity && !mobs.getValue()) continue;
            if (ent instanceof AnimalEntity && !animals.getValue()) continue;
            if (ent instanceof SlimeEntity && !slimes.getValue()) continue;

            if (mc.player.distanceTo(ent) <= attackRange.getValue() + aimRange.getValue()) list.add(living);
        }
        target = list.stream().min(Comparator.comparing(e -> mc.player.distanceTo(e))).orElse(null);
    }

    // --- FIX LỖI BUILD (Cần thiết) ---
    public void pause() { pauseTimer.reset(); }
    public boolean isAboveWater() { return mc.player.isSubmergedInWater(); }
    public Box resolvedBox;
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", false);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 10, 0, 20);
    public static class Position {
        public Position(double x, double y, double z) {}
        public boolean shouldRemove() { return true; }
    }
    public enum Mode { Track, None }
    public enum Switch { Normal, None, Silent }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, Ghost }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
}
