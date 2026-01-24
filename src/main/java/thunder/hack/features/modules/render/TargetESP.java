package thunder.hack.features.modules.render;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import thunder.hack.events.impl.Render3DEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;

import java.awt.*;
import java.util.Random;

public class TargetESP extends Module {

    public enum Mode {
        Circle,
        Cube,
        GhostV1,
        GhostV2
    }

    private final Setting<Mode> mode =
            new Setting<>("Mode", Mode.GhostV1);

    private final Random random = new Random();

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @EventHandler
    public void onRender3D(Render3DEvent e) {
        if (!Aura.isEnabled()) return;
        Entity target = Aura.target;
        if (target == null) return;

        switch (mode.getValue()) {
            case GhostV1 -> renderGhost(e, target, 0.25, 14);
            case GhostV2 -> renderGhost(e, target, 0.45, 22);
            case Circle -> renderCircle(e, target);
            case Cube -> renderCube(e, target);
        }
    }

    /* ================= GHOST (GIUN) ================= */

    private void renderGhost(Render3DEvent e, Entity target,
                             double power, int length) {

        MatrixStack matrices = e.getMatrixStack();
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        double x = MathHelper.lerp(e.getTickDelta(), target.lastRenderX, target.getX()) - cam.x;
        double y = MathHelper.lerp(e.getTickDelta(), target.lastRenderY, target.getY()) - cam.y + target.getHeight() * 0.6;
        double z = MathHelper.lerp(e.getTickDelta(), target.lastRenderZ, target.getZ()) - cam.z;

        Color color = HudEditor.getColor(1);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (int g = 0; g < 3; g++) {
            buffer.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);

            double px = x, py = y, pz = z;
            double time = (System.currentTimeMillis() + g * 300) * 0.002;

            for (int i = 0; i < length; i++) {
                px += Math.sin(time + i * 0.5 + random.nextFloat()) * power * 0.1;
                py += Math.cos(time * 1.3 + i * 0.4) * power * 0.08;
                pz += Math.sin(time * 0.9 + i * 0.6) * power * 0.1;

                float alpha = 1f - (float) i / length;

                buffer.vertex(matrix,
                        (float) px,
                        (float) py,
                        (float) pz)
                        .color(color.getRed(), color.getGreen(),
                               color.getBlue(), (int) (alpha * 220));
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /* ================= BASIC ESP ================= */

    private void renderCircle(Render3DEvent e, Entity target) {
        // giữ trống hoặc dùng circle cũ của bạn
    }

    private void renderCube(Render3DEvent e, Entity target) {
        // giữ trống hoặc dùng cube cũ của bạn
    }
}
