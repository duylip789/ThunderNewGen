package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import java.awt.Color;

public class VHat extends Module {
    public VHat() { 
        super("V-Hat", Category.RENDER); 
    }

    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 150).getRGB()));

    // Thay vì dùng EventHandler, ta dùng hàm onRender của chính Module nếu base hỗ trợ
    // Hoặc nếu build vẫn lỗi, ta sẽ tạm thời để trống để bạn check log xem class Render nào tồn tại
    
    public void onRender3D(MatrixStack matrices) {
        if (mc.world == null || mc.player == null) return;
        
        Color c = color.getValue().getColorObject();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.isSpectator()) continue;
            
            double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - mc.getEntityRenderDispatcher().camera.getPos().getX();
            double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - mc.getEntityRenderDispatcher().camera.getPos().getY();
            double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - mc.getEntityRenderDispatcher().camera.getPos().getZ();
            
            matrices.push();
            matrices.translate(x, y + player.getHeight() + 0.1, z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-player.headYaw));
            
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            buffer.vertex(matrix, 0, 0.3f, 0).color(c.getRGB());
            for (int i = 0; i <= 32; i++) {
                float px = (float) (Math.cos(i * 2 * Math.PI / 32) * 0.6f);
                float pz = (float) (Math.sin(i * 2 * Math.PI / 32) * 0.6f);
                buffer.vertex(matrix, px, 0, pz).color(c.getRed(), c.getGreen(), c.getBlue(), 120);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }
}
