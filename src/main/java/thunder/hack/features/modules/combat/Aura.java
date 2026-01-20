package thunder.hack.features.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.RotationAxis;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Aura extends Module {
    // --- COMBAT SETTINGS ---
    public final Setting<Float> range = new Setting<>("Range", 3.5f, 1f, 6f);
    public final Setting<Boolean> autoCrit = new Setting<>("StrictCrit", true);
    public final Setting<Boolean> rotate = new Setting<>("Rotate", true);
    public final Setting<Float> wallRange = new Setting<>("WallRange", 3.0f, 1f, 6f);

    // --- VISUAL SETTINGS (MENU ESP) ---
    // Khai báo kiểu này để chắc chắn hiện Menu trong ClickGUI của bạn
    public final Setting<SettingGroup> sgVisual = new Setting<>("Visuals", new SettingGroup(true, 0));

    public final Setting<Boolean> renderGhost = new Setting<>("RenderGhost", true).addToGroup(sgVisual);
    public final Setting<ColorSetting> ghostColor = new Setting<>("Color", new ColorSetting(new Color(160, 100, 255, 200).getRGB())).addToGroup(sgVisual);
    public final Setting<Float> ghostSpeed = new Setting<>("FadeSpeed", 1.0f, 0.1f, 3.0f).addToGroup(sgVisual);
    public final Setting<Float> slashLength = new Setting<>("SlashLength", 3.5f, 1.0f, 6.0f).addToGroup(sgVisual);
    public final Setting<Float> slashWidth = new Setting<>("SlashWidth", 0.6f, 0.1f, 2.0f).addToGroup(sgVisual);
    public final Setting<Boolean> seeThrough = new Setting<>("ThroughWalls", true).addToGroup(sgVisual);

    // --- VARIABLES ---
    public static Entity target;
    private final List<GhostSlash> slashes = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        target = null;
        slashes.clear();
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        // 1. Tìm mục tiêu
        target = findTarget();
        if (target == null) return;

        // 2. Quay đầu
        if (rotate.getValue()) {
            float[] rotations = calculateAngle(target.getEyePos());
            mc.player.setYaw(rotations[0]);
            mc.player.setPitch(rotations[1]);
        }

        // 3. Logic Tấn công (Crit & Timing)
        if (shouldAttack()) {
            attackTarget();
        }
    }

    // --- LOGIC COMBAT ---
    private Entity findTarget() {
        List<LivingEntity> potentialTargets = new ArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof LivingEntity living && ent != mc.player && living.isAlive()) {
                if (ent instanceof PlayerEntity && Managers.FRIEND.isFriend((PlayerEntity) ent)) continue;
                
                float dist = mc.player.distanceTo(ent);
                if (dist <= range.getValue()) {
                    if (!mc.player.canSee(ent) && dist > wallRange.getValue()) continue;
                    potentialTargets.add(living);
                }
            }
        }
        potentialTargets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
        return potentialTargets.isEmpty() ? null : potentialTargets.get(0);
    }

    private boolean shouldAttack() {
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f) return false;
        if (autoCrit.getValue()) {
            boolean isFalling = mc.player.fallDistance > 0;
            boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava();
            boolean onGround = mc.player.isOnGround();
            if (inLiquid || mc.player.isClimbing()) return true;
            return isFalling && !onGround;
        }
        return true;
    }

    private void attackTarget() {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        // Kích hoạt hiệu ứng Ghost Slash
        if (renderGhost.getValue()) {
            addGhostSlash(target);
        }
    }

    // --- RENDER GHOST SLASH (VISUALS) ---
    private void addGhostSlash(Entity target) {
        float[] pitches = {0f, 35f, -35f, 15f, -15f}; // Các góc chém đa dạng
        float pitch = pitches[random.nextInt(pitches.length)];
        float yaw = random.nextFloat() * 360f;
        Vec3d pos = target.getPos().add(0, target.getHeight() * 0.6, 0);
        slashes.add(new GhostSlash(pos, yaw, pitch));
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        if (slashes.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        if (seeThrough.getValue()) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Dùng vòng lặp ngược để tránh lỗi khi đang xóa
        for (int i = slashes.size() - 1; i >= 0; i--) {
            GhostSlash s = slashes.get(i);
            s.age += 0.03f * ghostSpeed.getValue(); // Tốc độ mờ
            
            if (s.age >= 1.0f) {
                slashes.remove(i);
                continue;
            }

            stack.push();
            Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
            stack.translate(s.pos.x - cam.x, s.pos.y - cam.y, s.pos.z - cam.z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s.yaw));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s.pitch));

            Matrix4f mat = stack.peek().getPositionMatrix();
            buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

            Color c = ghostColor.getValue().getColorObject();
            float baseAlpha = (c.getAlpha() / 255f) * (1.0f - s.age);
            float len = slashLength.getValue();
            float wid = slashWidth.getValue();

            int steps = 20; 
            for (int j = 0; j <= steps; j++) {
                float t = (float) j / steps; 
                float x = MathUtility.lerp(-len, len, t);
                
                // Hiệu ứng bầu ở giữa, nhọn 2 đầu (Shape Fade)
                float shapeFade = (float) Math.sin(t * Math.PI); 
                
                int alphaCore = (int) (baseAlpha * shapeFade * 255);
                float currentWidth = wid * shapeFade; 

                // Vẽ 3 điểm để tạo dải màu Glow (giữa đậm, biên mờ)
                buffer.vertex(mat, x, -currentWidth/2, 0).color(c.getRed(), c.getGreen(), c.getBlue(), 0).next();
                buffer.vertex(mat, x, 0, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alphaCore).next();
                buffer.vertex(mat, x, currentWidth/2, 0).color(c.getRed(), c.getGreen(), c.getBlue(), 0).next();
            }

            tessellator.draw();
            stack.pop();
        }

        if (seeThrough.getValue()) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
        RenderSystem.disableBlend();
    }

    private float[] calculateAngle(Vec3d targetPos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        double dX = targetPos.x - eyesPos.x;
        double dY = (targetPos.y - eyesPos.y) * -1.0D;
        double dZ = targetPos.z - eyesPos.z;
        double dist = Math.sqrt(dX * dX + dZ * dZ);
        float yaw = (float) Math.toDegrees(Math.atan2(dZ, dX)) - 90.0F;
        float pitch = (float) Math.toDegrees(Math.atan2(dY, dist));
        return new float[]{yaw, pitch};
    }

    private static class GhostSlash {
        Vec3d pos; float yaw, pitch, age;
        public GhostSlash(Vec3d pos, float yaw, float pitch) {
            this.pos = pos; this.yaw = yaw; this.pitch = pitch; this.age = 0;
        }
    }
}
