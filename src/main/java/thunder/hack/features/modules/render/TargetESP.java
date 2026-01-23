package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.setting.Setting;
import java.awt.Color;

public class TargetESP extends Module {

    private final Setting<Color> color = new Setting<>("Color", new Color(255, 255, 255, 200));
    private final Setting<Float> size = new Setting<>("Size", 1.0f, 0.1f, 2.0f);

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        // Fix lỗi conversion: Dùng trực tiếp ModuleManager.aura
        if (ModuleManager.aura == null || ModuleManager.aura.target == null) return;

        Entity target = ModuleManager.aura.target;
        renderTargetCircle(stack, target);
    }

    private void renderTargetCircle(MatrixStack stack, Entity entity) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        stack.push();

        // FIX 1.21: Minecraft 1.21 dùng RenderTickCounter thay vì getTickDelta() trực tiếp
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);

        double x = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickDelta - mc.getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickDelta - mc.getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickDelta - mc.getEntityRenderDispatcher().camera.getPos().z;

        stack.translate(x, y + 0.1, z);

        float angle = (float) (System.currentTimeMillis() % 2000) / 2000f;
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle * 360f));

        // FIX 1.21: Cách sử dụng Tessellator và BufferBuilder mới hoàn toàn
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);

        Color c = color.getValue();
        float radius = size.getValue();

        for (int i = 0; i <= 360; i += 5) {
            double radians = Math.toRadians(i);
            double cx = Math.cos(radians) * radius;
            double cz = Math.sin(radians) * radius;
            bufferBuilder.vertex(stack.peek().getPositionMatrix(), (float)cx, 0, (float)cz)
                    .color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        }

        // FIX 1.21: Dùng BufferRenderer.drawWithGlobalProgram cho 1.21
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        stack.pop();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
