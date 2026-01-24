package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import thunder.hack.events.impl.RenderWorldLastEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Aura;

import java.awt.*;

public class TargetESP extends Module {

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    private final MinecraftClient mc = MinecraftClient.getInstance();

    @EventHandler
    public void onRender(RenderWorldLastEvent event) {
        if (mc.world == null || mc.player == null) return;

        if (!Aura.INSTANCE.isEnabled()) return;
        LivingEntity target = Aura.INSTANCE.getTarget();
        if (target == null) return;

        renderGhostESP(event.getMatrixStack(), target, event.getTickDelta());
    }

    // ================= GHOST ESP =================
    private void renderGhostESP(MatrixStack matrices, LivingEntity target, float tickDelta) {
        Camera cam = mc.gameRenderer.getCamera();

        double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX()) - cam.getPos().x;
        double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) - cam.getPos().y + target.getHeight() * 0.6;
        double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ()) - cam.getPos().z;

        matrices.push();
        matrices.translate(x, y, z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Color base = HudEditor.getColor(1);
        float time = (mc.world.getTime() + tickDelta) * 0.15f;

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // 3 con "giun"
        for (int w = 0; w < 3; w++) {
            float seed = w * 100f;

            for (int i = 0; i < 25; i++) {
                float progress = i / 25f;

                float radius = 0.6f * (1f - progress);
                float angleX = MathHelper.sin(time + seed + i * 0.3f) * radius;
                float angleY = MathHelper.sin(time * 1.3f + seed + i * 0.2f) * 0.4f;
                float angleZ = MathHelper.cos(time + seed + i * 0.25f) * radius;

                int alpha = (int) (180 * (1f - progress));

                buffer.vertex(mat, angleX, angleY, angleZ)
                        .color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
