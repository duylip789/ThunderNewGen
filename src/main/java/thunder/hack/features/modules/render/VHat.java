package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor; // Import để lấy màu Sync
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import java.awt.Color;

public class VHat extends Module {
    public VHat() { 
        super("V-Hat", Category.RENDER); 
    }

    // Các Setting mới
    public final Setting<Boolean> others = new Setting<>("Others", false); // Mặc định tắt cho đỡ lag
    public final Setting<Boolean> sync = new Setting<>("Sync", true);      // Mặc định bật đồng bộ màu
    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(0, 181, 23, 200).getRGB()), v -> !sync.getValue());

    public void onRender3D(MatrixStack matrices) {
        if (mc.world == null || mc.player == null) return;
        
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        
        // Xử lý logic màu sắc
        Color c;
        if (sync.getValue()) {
            // Lấy màu từ HudEditor (tự động theo Frosbut, Fade, v.v.)
            c = HudEditor.getColor(1); 
        } else {
            // Dùng màu tùy chỉnh
            c = color.getValue().getColorObject();
        }

        // Tăng độ đậm (Alpha) để nón nhìn rõ hơn
        int alphaTop = 240; // Đỉnh nón rất đậm
        int alphaBot = 180; // Vành nón hơi nhạt hơn xíu để tạo khối 3D

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.isSpectator()) continue;
            
            // LOGIC: Nếu tắt 'Others' thì bỏ qua người khác, chỉ vẽ bản thân
            if (!others.getValue() && player != mc.player) continue;

            // LOGIC: Ẩn khi nhìn góc nhìn thứ nhất (First Person) để không che màn hình
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            
            // Tính toán nội suy tọa độ (Interpolation)
            double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - mc.getEntityRenderDispatcher().camera.getPos().getX();
            double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - mc.getEntityRenderDispatcher().camera.getPos().getY();
            double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - mc.getEntityRenderDispatcher().camera.getPos().getZ();
            
            float interpolatedHeadYaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);

            matrices.push();
            
            // Đặt vị trí sát đầu: getHeight() + 0.05 là vừa đẹp
            matrices.translate(x, y + player.getHeight() + 0.05, z); 
            
            // Xoay theo hướng nhìn của đầu
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-interpolatedHeadYaw));
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableCull(); 
            RenderSystem.depthMask(false); 

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            // THÔNG SỐ NÓN TO ĐẸP
            float radius = 0.65f; // Bán kính
            float height = 0.35f; // Chiều cao nón (thấp xuống chút cho giống mũ tai bèo/mũ cối)
            
            // Đỉnh nón (Tâm)
            buffer.vertex(matrix, 0, height, 0).color(c.getRed(), c.getGreen(), c.getBlue(), alphaTop);
            
            // Vành nón (Vòng tròn đáy)
            for (int i = 0; i <= 36; i++) { 
                float px = (float) (Math.cos(i * 2 * Math.PI / 36) * radius);
                float pz = (float) (Math.sin(i * 2 * Math.PI / 36) * radius);
                
                // Vẽ đáy
                buffer.vertex(matrix, px, 0, pz).color(c.getRed(), c.getGreen(), c.getBlue(), alphaBot);
            }
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }
}
