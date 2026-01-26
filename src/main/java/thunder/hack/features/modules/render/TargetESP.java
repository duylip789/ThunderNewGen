package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

// --- FIX IMPORT QUAN TRỌNG ---
import thunder.hack.core.ModuleManager; 
// Nếu lỗi dòng trên, thử: import thunder.hack.core.manager.ModuleManager;

import thunder.hack.events.impl.EventRender3D; 
// Nếu lỗi dòng trên, thử: import thunder.hack.events.impl.Render3DEvent;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.impl.ModeSetting; // Sửa settings -> setting
import thunder.hack.utility.render.Render2DUtil; // Sửa Utils -> Util
import thunder.hack.utility.render.Render3DUtil;
import com.google.common.eventbus.Subscribe;

import java.awt.*;

public class TargetESP extends Module {
    // Sửa đường dẫn Setting
    private final ModeSetting mode = new ModeSetting("Mode", "Ghosts", "Ghosts", "Marker", "Marker2", "Circle");

    private final float[] SCALE_CACHE = new float[101];
    private float animationVal = 0f;
    private Entity lastTarget = null;

    private static final Identifier FIREFLY = new Identifier("thunderhack", "textures/firefly.png");
    private static final Identifier MARKER = new Identifier("thunderhack", "textures/marker.png");
    private static final Identifier MARKER2 = new Identifier("thunderhack", "textures/marker2.png");

    public TargetESP() {
        super("TargetESP", "Beautiful indicator on your target", Category.RENDER);
        addSettings(mode);
        for (int i = 0; i <= 100; i++) SCALE_CACHE[i] = Math.max(0.28f * (i / 100f), 0.2f);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        // Lấy Target từ Aura
        Entity currentTarget = ModuleManager.aura.target;

        if (currentTarget != null) {
            animationVal = MathHelper.lerp(event.getTickDelta() * 0.1f, animationVal, 1f);
            lastTarget = currentTarget;
        } else {
            animationVal = MathHelper.lerp(event.getTickDelta() * 0.1f, animationVal, 0f);
        }

        if (lastTarget != null && animationVal > 0.01f) {
            if (mode.getValue().equals("Marker") || mode.getValue().equals("Marker2")) {
                renderMarker(lastTarget, event.getTickDelta());
            } else if (mode.getValue().equals("Ghosts")) {
                renderGhosts(14, 8, 1.8f, 3f, lastTarget);
            } else if (mode.getValue().equals("Circle")) {
                renderCircle(lastTarget, event.getMatrixStack(), event.getTickDelta());
            }
        }
    }

    public void renderGhosts(int espLength, int factor, float shaking, float amplitude, Entity target) {
        if (target == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;
        
        float delta = mc.getTickDelta();
        Vec3d camPos = camera.getPos();
        double tX = MathHelper.lerp(delta, target.prevX, target.getX()) - camPos.x;
        double tY = MathHelper.lerp(delta, target.prevY, target.getY()) - camPos.y;
        double tZ = MathHelper.lerp(delta, target.prevZ, target.getZ()) - camPos.z;
        float age = target.age + delta;

        boolean canSee = mc.player.canSee(target);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, FIREFLY);
        
        // 1.21 Dùng GameRenderer cho an toàn, tránh lỗi ShaderProgramKeys
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i <= espLength; i++) {
                float offset = (float) i / espLength;
                double radians = Math.toRadians(((i / 1.5f + age) * factor + j * 120) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(age * 2.5f + i * (j + 1)) * amplitude) / shaking;

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180f));
                matrices.translate(tX + Math.cos(radians) * target.getWidth(), tY + 1 + sinQuad, tZ + Math.sin(radians) * target.getWidth());
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

                Matrix4f matrix = matrices.peek().getPositionMatrix();
                
                // Lấy màu từ Render2DUtil thay vì ColorUtils
                int baseColor = Render2DUtil.getHUDColor().getRGB();
                int color = applyOpacity(baseColor, offset * animationVal);

                float scale = SCALE_CACHE[Math.min((int)(offset * 100), 100)];
                
                // Cấu trúc Vertex 1.21
                buffer.vertex(matrix, -scale,  scale, 0).texture(0f, 1f).color(color).next();
                buffer.vertex(matrix,  scale,  scale, 0).texture(1f, 1f).color(color).next();
                buffer.vertex(matrix,  scale, -scale, 0).texture(1f, 0).color(color).next();
                buffer.vertex(matrix, -scale, -scale, 0).texture(0f, 0).color(color).next();
            }
        }
        
        BufferRenderer.drawWithShader(buffer.end());

        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableBlend();
    }

    private void renderCircle(Entity target, MatrixStack matrices, float tickDelta) {
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX()) - camPos.x;
        double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ()) - camPos.z;
        double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) - camPos.y;

        double floatingY = y + (Math.sin(System.currentTimeMillis() / 400.0) + 1.0) * (target.getHeight() / 2.0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int colorRGB = Render2DUtil.getHUDColor().getRGB();
        float r = ((colorRGB >> 16) & 0xFF) / 255f;
        float g = ((colorRGB >> 8) & 0xFF) / 255f;
        float b = (colorRGB & 0xFF) / 255f;

        float radius = target.getWidth() * 1.0f;

        for (float i = 0; i <= Math.PI * 2.1; i += Math.PI / 20) {
            double vecX = x + radius * Math.cos(i);
            double vecZ = z + radius * Math.sin(i);

            buffer.vertex(matrix, (float) vecX, (float) (floatingY - 0.2f), (float) vecZ).color(r, g, b, 0.0f).next();
            buffer.vertex(matrix, (float) vecX, (float) floatingY, (float) vecZ).color(r, g, b, 0.7f * animationVal).next();
        }

        BufferRenderer.drawWithShader(buffer.end());
        
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderMarker(Entity target, float delta) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;

        Vec3d camPos = camera.getPos();
        double tX = MathHelper.lerp(delta, target.prevX, target.getX()) - camPos.x;
        double tY = MathHelper.lerp(delta, target.prevY, target.getY()) - camPos.y;
        double tZ = MathHelper.lerp(delta, target.prevZ, target.getZ()) - camPos.z;
        
        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
        matrices.translate(tX, tY + target.getHeight() / 2f, tZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(1f));

        float radians = (float) Math.toRadians(System.currentTimeMillis() % 3600 / 5f);
        matrices.multiplyPositionMatrix(new Matrix4f().rotate(radians, 0, 0, 1));
        matrices.translate(-0.5, -0.5, 0);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        if (mode.getValue().equals("Marker")) {
            RenderSystem.setShaderTexture(0, MARKER);
        } else {
            RenderSystem.setShaderTexture(0, MARKER2);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        int color = Render2DUtil.getHUDColor().getRGB();
        int colorWithAlpha = applyOpacity(color, animationVal);

        buffer.vertex(matrix, 0, 1, 0).texture(0f, 1f).color(colorWithAlpha).next();
        buffer.vertex(matrix, 1, 1, 0).texture(1f, 1f).color(colorWithAlpha).next();
        buffer.vertex(matrix, 1, 0, 0).texture(1f, 0).color(colorWithAlpha).next();
        buffer.vertex(matrix, 0, 0, 0).texture(0f, 0).color(colorWithAlpha).next();
        
        BufferRenderer.drawWithShader(buffer.end());
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static int applyOpacity(int color, float opacity) {
        Color c = new Color(color);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * opacity)).getRGB();
    }
}
