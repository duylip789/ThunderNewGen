package thunder.hack.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.render.EventRender3D;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.settings.impl.ModeSetting;
import thunder.hack.utility.render.ColorUtils;
import thunder.hack.utility.render.Render3DUtils;
import com.google.common.eventbus.Subscribe;

import java.awt.*;

public class TargetESP extends Module {
    // Dịch sang Tiếng Anh
    private final ModeSetting mode = new ModeSetting("Mode", "Ghosts", "Ghosts", "Marker", "Marker2", "Circle");

    private final float[] SCALE_CACHE = new float[101];
    private float animationVal = 0f; // Thay thế EaseInOutQuad bằng biến float đơn giản để đỡ lỗi thiếu thư viện
    private Entity lastTarget = null;
    private double scale = 0.0D;

    // Textures (Bạn cần đảm bảo file ảnh tồn tại trong assets, hoặc thay bằng ảnh có sẵn của Thunder)
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
        // Lấy target từ Aura của ThunderHack
        Entity currentTarget = ModuleManager.aura.target;

        // Logic Animation đơn giản hóa để hợp với Module system
        if (currentTarget != null) {
            animationVal = MathHelper.lerp(event.getTickDelta() * 0.1f, animationVal, 1f);
        } else {
            animationVal = MathHelper.lerp(event.getTickDelta() * 0.1f, animationVal, 0f);
        }

        if (currentTarget != null) {
            lastTarget = currentTarget;
        }

        if (lastTarget != null && animationVal > 0.01f) {
            if (mode.is("Marker") || mode.is("Marker2")) {
                render(lastTarget, event.getTickDelta());
            } else if (mode.is("Ghosts")) {
                renderGhosts(14, 8, 1.8f, 3f, lastTarget);
            } else if (mode.is("Circle")) {
                cicle(lastTarget, event.getMatrixStack(), event.getTickDelta());
            }
        }
    }

    public void renderGhosts(int espLength, int factor, float shaking, float amplitude, Entity target) {
        if (target == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;
        
        // ThunderHack không có RayTraceUtil.getHitProgress mặc định, nên mình để tạm logic check khoảng cách
        float hitProgress = (mc.player.distanceTo(target) < 3) ? 1.0f : 0f; 
        float delta = mc.getRenderTickCounter().getTickDelta(true);
        
        Vec3d camPos = camera.getPos();
        double tX = MathHelper.lerp(delta, target.prevX, target.getX()) - camPos.x;
        double tY = MathHelper.lerp(delta, target.prevY, target.getY()) - camPos.y;
        double tZ = MathHelper.lerp(delta, target.prevZ, target.getZ()) - camPos.z;
        float age = target.age + delta; // Thay thế interpolateFloat

        boolean canSee = mc.player.canSee(target);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, FIREFLY);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        
        if (canSee) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        } else {
            RenderSystem.disableDepthTest();
        }

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float ghostAlpha = animationVal;

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
                int baseColor;
                if (hitProgress > 0) {
                    baseColor = Color.RED.getRGB();
                } else {
                    // Dùng ColorUtils của ThunderHack
                    baseColor = ColorUtils.getColor((int) (180 * offset)).getRGB();
                }

                int color = applyOpacity(baseColor, offset * ghostAlpha);

                float scale = SCALE_CACHE[Math.min((int)(offset * 100), 100)];
                buffer.vertex(matrix, -scale,  scale, 0).texture(0f, 1f).color(color);
                buffer.vertex(matrix,  scale,  scale, 0).texture(1f, 1f).color(color);
                buffer.vertex(matrix,  scale, -scale, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix, -scale, -scale, 0).texture(0f, 0).color(color);
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end()); // Cập nhật cách gọi Draw cho Fabric mới

        if (canSee) {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.disableBlend();
    }

    private void cicle(Entity target, MatrixStack matrices, float tickDelta) {
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX()) - camPos.x;
        double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ()) - camPos.z;
        double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) - camPos.y + Math.min(Math.sin(System.currentTimeMillis() / 400.0) + 0.95, target.getHeight());

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int baseColor = ColorUtils.getColor(360).getRGB();
        float r = ((baseColor >> 16) & 0xFF) / 255f;
        float g = ((baseColor >> 8) & 0xFF) / 255f;
        float b = (baseColor & 0xFF) / 255f;

        float alpha = animationVal;

        float radius = target.getWidth() * 0.8f;

        for (float i = 0; i <= Math.PI * 2 + (Math.PI * 5 / 100); i += Math.PI * 5 / 100) {
            double vecX = x + radius * Math.cos(i);
            double vecZ = z + radius * Math.sin(i);

            buffer.vertex(matrix, (float) vecX, (float) (y - Math.cos(System.currentTimeMillis() / 400.0) / 2), (float) vecZ).color(r, g, b, 0.01f * alpha);
            buffer.vertex(matrix, (float) vecX, (float) y, (float) vecZ).color(r, g, b, 1f * alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void render(Entity target, float delta) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return;

        float hitProgress = (mc.player.distanceTo(target) < 3) ? 1.0f : 0f;
        Vec3d camPos = camera.getPos();
        double tX = MathHelper.lerp(delta, target.prevX, target.getX()) - camPos.x;
        double tY = MathHelper.lerp(delta, target.prevY, target.getY()) - camPos.y;
        double tZ = MathHelper.lerp(delta, target.prevZ, target.getZ()) - camPos.z;
        
        MatrixStack matrices = setupMatrices(camera, target, delta, tX, tY, tZ);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        if (mode.is("Marker")) {
            RenderSystem.setShaderTexture(0, MARKER);
        }
        if (mode.is("Marker2")) {
            RenderSystem.setShaderTexture(0, MARKER2);
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        float alpha = animationVal;
        
        // Fix màu sắc dùng ColorUtils của Thunder
        int[] baseColors = hitProgress > 0 ? 
            new int[]{Color.RED.getRGB(), ColorUtils.getColor(0).getRGB(), Color.RED.getRGB(), ColorUtils.getColor(270).getRGB()} : 
            new int[]{ColorUtils.getColor(90).getRGB(), ColorUtils.getColor(0).getRGB(), ColorUtils.getColor(180).getRGB(), ColorUtils.getColor(270).getRGB()};

        drawQuad(matrix, applyAlphaToColors(baseColors, alpha));
        
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private MatrixStack setupMatrices(Camera camera, Entity target, float delta, double tX, double tY, double tZ) {
        MatrixStack matrices = new MatrixStack();
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw + 180f));
        matrices.translate(tX, tY + target.getEyeHeight(target.getPose()) / 2f, tZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(1f)); // Simplified rotation

        float radians = (float) Math.toRadians(System.currentTimeMillis() % 3600 / 5f);
        matrices.multiplyPositionMatrix(new Matrix4f().rotate(radians, 0, 0, 1));
        matrices.translate(-0.75, -0.75, -0.01);
        return matrices;
    }

    private int[] applyAlphaToColors(int[] colors, float alpha) {
        int[] out = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            Color color = new Color(colors[i]);
            out[i] = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alpha)).getRGB();
        }
        return out;
    }

    private void drawQuad(Matrix4f matrix, int[] colors) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(matrix,0,1.5f,0).texture(0f,1f).color(colors[0]);
        buffer.vertex(matrix,1.5f,1.5f,0).texture(1f,1f).color(colors[1]);
        buffer.vertex(matrix,1.5f,0,0).texture(1f,0).color(colors[2]);
        buffer.vertex(matrix,0,0,0).texture(0f,0).color(colors[3]);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
  }
             
