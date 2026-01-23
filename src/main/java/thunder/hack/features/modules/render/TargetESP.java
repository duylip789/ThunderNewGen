package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;
import thunder.hack.features.modules.Module;
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
        [span_0](start_span)[span_1](start_span)// Kiểm tra an toàn để tránh NoClassDefFoundError như trong log[span_0](end_span)[span_1](end_span)
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
        
        // Di chuyển matrix đến vị trí của target
        double x = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().z;
        
        stack.translate(x, y + 0.1, z);
        
        // Hiệu ứng xoay tròn cho gl4es mượt hơn
        float speed = (float) (System.currentTimeMillis() % 2000) / 2000f;
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(speed * 360f));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        // Bắt đầu vẽ vòng tròn bằng GL_LINE_STRIP (tương thích gl4es tốt nhất)
        bufferBuilder.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        
        Color c = color.getValue();
        float radius = size.getValue();

        for (int i = 0; i <= 360; i += 5) {
            double radians = Math.toRadians(i);
            double circleX = Math.cos(radians) * radius;
            double circleZ = Math.sin(radians) * radius;
            bufferBuilder.vertex(stack.peek().getPositionMatrix(), (float)circleX, 0, (float)circleZ)
                    .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).next();
        }

        tessellator.draw();
        
        stack.pop();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
