package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.Render3DEvent; // BỎ CHỮ .render Ở GIỮA
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import meteordevelopment.orbit.EventHandler;
import java.awt.Color;

public class VHat extends Module {
    public VHat() { 
        super("V-Hat", Category.RENDER); 
    }

    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 150).getRGB()));

    @EventHandler
    public void onRender3D(Render3DEvent event) { // DÙNG Render3DEvent (Viết hoa)
        if (mc.world == null || mc.player == null) return;
        
        Color c = color.getValue().getColorObject();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.isSpectator()) continue;
            
            double x = MathHelper.lerp(event.getTickDelta(), player.lastRenderX, player.getX()) - mc.gameRenderer.getCamera().getPos().getX();
            double y = MathHelper.lerp(event.getTickDelta(), player.lastRenderY, player.getY()) - mc.gameRenderer.getCamera().getPos().getY();
            double z = MathHelper.lerp(event.getTickDelta(), player.lastRenderZ, player.getZ()) - mc.gameRenderer.getCamera().getPos().getZ();
            
            MatrixStack matrices = event.getMatrixStack();
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
