package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.EnumSetting;
import thunder.hack.features.modules.client.HudEditor;

import java.awt.*;

public class TargetESP extends Module {

    public enum Mode {
        NewGen
    }

    private final Setting<Mode> mode =
            new EnumSetting<>("Mode", Mode.NewGen);

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @Override
    public void onRender(MatrixStack matrices, float tickDelta) {
        if (mc.world == null || mc.player == null) return;

        Aura aura = Module.getModule(Aura.class);
        if (aura == null) return;

        LivingEntity target = aura.getTarget();
        if (target == null) return;

        if (mode.getValue() == Mode.NewGen) {
            renderNewGen(matrices, target, tickDelta);
        }
    }

    // ================== NEWGEN ESP ==================

    private void renderNewGen(MatrixStack matrices, LivingEntity target, float tickDelta) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        double x = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX()) - cam.x;
        double y = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) + target.getHeight() * 0.6 - cam.y;
        double z = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ()) - cam.z;

        Color c = HudEditor.getColor(1);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        matrices.push();
        matrices.translate(x, y, z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        long time = System.currentTimeMillis();

        for (int i = 0; i < 3; i++) {
            double seed = i * 1000;
            double t = (time + seed) * 0.002;

            for (int p = 0; p < 18; p++) {
                double prog = p / 18.0;

                double ox = Math.sin(t + prog * 6) * 0.6;
                double oy = Math.cos(t * 1.3 + prog * 5) * 0.4;
                double oz = Math.sin(t * 1.7 + prog * 4) * 0.6;

                float alpha = (float) (1.0 - prog) * 180f;

                buffer.vertex(matrix,
                        (float) ox,
                        (float) oy,
                        (float) oz
                ).color(
                        c.getRed(),
                        c.getGreen(),
                        c.getBlue(),
                        (int) alpha
                );
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
