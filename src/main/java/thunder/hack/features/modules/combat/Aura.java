package thunder.hack.features.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.block.Blocks;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.Timer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Aura extends Module {
    // --- ENUMS (Bắt buộc cho các module khác và Mixin) ---
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, Liquid }
    public enum Mode { None, Track, Smooth }
    public enum Switch { None, Normal, Silent }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }

    // --- SETTINGS ---
    public final Setting<Mode> rotationMode = new Setting<>("Rotation", Mode.Track);
    public final Setting<Switch> switchMode = new Setting<>("Switch", Switch.Normal);
    public final Setting<Float> attackRange = new Setting<>("Range", 3.5f, 1f, 6f);
    public final Setting<Boolean> autoCrit = new Setting<>("StrictCrit", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", false);

    public final Setting<SettingGroup> sgVisual = new Setting<>("Visuals", new SettingGroup(true, 0));
    public final Setting<Boolean> renderGhost = new Setting<>("GhostSlash", true).addToGroup(sgVisual);
    public final Setting<ColorSetting> ghostColor = new Setting<>("Color", new ColorSetting(new Color(160, 100, 255, 200).getRGB())).addToGroup(sgVisual);
    public final Setting<Float> slashLength = new Setting<>("Length", 3.5f, 1.0f, 6.0f).addToGroup(sgVisual);
    public final Setting<Float> slashWidth = new Setting<>("Width", 0.6f, 0.1f, 2.0f).addToGroup(sgVisual);
    public final Setting<Boolean> seeThrough = new Setting<>("SeeThrough", true).addToGroup(sgVisual);

    // --- BIẾN TOÀN CỤC ---
    public static Entity target;
    public float rotationPitch, rotationYaw;
    public Box resolvedBox;
    private final Timer pauseTimer = new Timer();
    private final List<GhostSlashData> slashes = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    public Aura() { super("Aura", Category.COMBAT); }

    // --- CÁC HÀM TIỆN ÍCH CHO MODULE KHÁC (PearlChaser, TriggerBot...) ---
    public void pause() { pauseTimer.reset(); }
    public float getAttackCooldown() { return mc.player.getAttackCooldownProgress(0.5f); }
    public boolean isAboveWater() { return mc.world.getBlockState(mc.player.getBlockPos().down()).isOf(Blocks.WATER); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(500)) return;

        target = findTarget();
        slashes.removeIf(s -> (s.age += 0.04f) > 1.0f);

        if (target != null) {
            float[] rotations = calculateAngle(target.getEyePos());
            rotationYaw = rotations[0];
            rotationPitch = rotations[1];

            if (rotationMode.getValue() != Mode.None) {
                mc.player.setYaw(rotationYaw);
                mc.player.setPitch(rotationPitch);
            }

            if (getAttackCooldown() >= 0.95f) {
                if (!autoCrit.getValue() || (mc.player.fallDistance > 0 && !mc.player.isOnGround())) {
                    mc.interactionManager.attackEntity(mc.player, target);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    if (renderGhost.getValue()) addSlash(target);
                }
            }
        }
    }

    private void addSlash(Entity target) {
        float[] p = {0f, 35f, -35f, 15f, -15f};
        float pitch = p[random.nextInt(p.length)];
        float yaw = random.nextFloat() * 360f;
        slashes.add(new GhostSlashData(target.getPos().add(0, target.getHeight() * 0.6, 0), yaw, pitch));
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        if (slashes.isEmpty()) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        if (seeThrough.getValue()) { RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); }

        for (GhostSlashData s : slashes) {
            stack.push();
            Vec3d c = mc.getEntityRenderDispatcher().camera.getPos();
            stack.translate(s.pos.x - c.x, s.pos.y - c.y, s.pos.z - c.z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s.yaw));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s.pitch));

            Matrix4f mat = stack.peek().getPositionMatrix();
            
            // XỬ LÝ RENDER THEO MAPPING 1.20.5+
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

            Color colorObj = ghostColor.getValue().getColorObject();
            float alpha = (colorObj.getAlpha() / 255f) * (1.0f - s.age);
            float l = slashLength.getValue();
            float w = slashWidth.getValue();

            for (int j = 0; j <= 20; j++) {
                float t = (float) j / 20;
                float x = MathUtility.lerp(-l, l, t);
                float fade = (float) Math.sin(t * Math.PI);
                int aCore = (int) (alpha * fade * 255);
                float curW = w * fade;

                // FIX LỖI #99: Dùng lại next() thay vì endVertex()
                buffer.vertex(mat, x, -curW/2, 0).color(colorObj.getRed(), colorObj.getGreen(), colorObj.getBlue(), 0).next();
                buffer.vertex(mat, x, 0, 0).color(colorObj.getRed(), colorObj.getGreen(), colorObj.getBlue(), aCore).next();
                buffer.vertex(mat, x, curW/2, 0).color(colorObj.getRed(), colorObj.getGreen(), colorObj.getBlue(), 0).next();
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            stack.pop();
        }
        if (seeThrough.getValue()) { RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); }
        RenderSystem.disableBlend();
    }

    private Entity findTarget() {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof LivingEntity living && e != mc.player && living.isAlive()) {
                if (e instanceof PlayerEntity p && Managers.FRIEND.isFriend(p)) continue;
                if (mc.player.distanceTo(e) <= attackRange.getValue()) targets.add(living);
            }
        }
        targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
        return targets.isEmpty() ? null : targets.get(0);
    }

    private float[] calculateAngle(Vec3d target) {
        Vec3d eyes = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        double dX = target.x - eyes.x;
        double dY = (target.y - eyes.y) * -1.0;
        double dZ = target.z - eyes.z;
        double dist = Math.sqrt(dX * dX + dZ * dZ);
        return new float[]{(float) Math.toDegrees(Math.atan2(dZ, dX)) - 90.0f, (float) Math.toDegrees(Math.atan2(dY, dist))};
    }

    // --- CLASS POSITION CHO MIXIN ---
    public static class Position {
        public double x, y, z;
        public long time;
        public Position(double x, double y, double z) { 
            this.x = x; this.y = y; this.z = z; this.time = System.currentTimeMillis();
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public boolean shouldRemove() { return System.currentTimeMillis() - time > 1000; }
    }

    private static class GhostSlashData {
        Vec3d pos; float yaw, pitch, age;
        public GhostSlashData(Vec3d pos, float yaw, float pitch) { this.pos = pos; this.yaw = yaw; this.pitch = pitch; this.age = 0; }
    }
}
