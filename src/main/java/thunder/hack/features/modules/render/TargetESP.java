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
        // Lấy Aura an toàn để không bị crash khi khởi động
        Aura aura = ModuleManager.get(Aura.class);
        if (aura == null || aura.target == null) return;

        Entity target = aura.target;
        renderTargetCircle(stack, target);
    }

    private void renderTargetCircle(MatrixStack stack, Entity entity) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        stack.push();

        // Tính toán vị trí nội suy (mượt mà khi entity di chuyển)
        double x = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().x;
        double y = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().y;
        double z = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * mc.getTickDelta() - mc.getEntityRenderDispatcher().camera.getPos().z;

        stack.translate(x, y + 0.1, z); // Nâng lên 0.1 để không bị chìm dưới đất

        // Hiệu ứng xoay tròn mượt mà
        float angle = (float) (System.currentTimeMillis() % 2000) / 2000f;
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle * 360f));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        // Bắt đầu vẽ vòng tròn 3D bằng LINE_STRIP (Phù hợp GL4ES Mobile & PC)
        bufferBuilder.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);

        Color c = color.getValue();
        float radius = size.getValue();

        for (int i = 0; i <= 360; i += 5) {
            double radians = Math.toRadians(i);
            double cx = Math.cos(radians) * radius;
            double cz = Math.sin(radians) * radius;
            bufferBuilder.vertex(stack.peek().getPositionMatrix(), (float)cx, 0, (float)cz)
                    .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).next();
        }

        tessellator.draw();

        stack.pop();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
