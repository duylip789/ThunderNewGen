package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.Render3DEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import meteordevelopment.orbit.EventHandler;

import java.awt.Color;

public class VHat extends Module {

    public VHat() {
        super("V-Hat", Category.RENDER);
    }

    // --- CÀI ĐẶT (SETTINGS) ---
    public final Setting<Boolean> friendsOnly = new Setting<>("Friend", false);
    public final Setting<Boolean> syncColor = new Setting<>("Sync Color", true);
    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 150).getRGB()), v -> !syncColor.getValue());
    
    // Độ rộng và độ cao của nón
    public final Setting<Float> scale = new Setting<>("Scale", 0.6f, 0.1f, 1.0f);
    public final Setting<Float> height = new Setting<>("Height", 0.3f, 0.1f, 1.0f);

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        // Xử lý màu sắc
        Color finalColor = syncColor.getValue() ? HudEditor.getColor(1) : color.getValue().getColorObject();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.isSpectator()) continue;

            // Logic Friend: Nếu bật thì chỉ hiện cho mình và bạn bè
            if (friendsOnly.getValue()) {
                boolean isMe = (player == mc.player);
                boolean isFriend = Managers.FRIEND.isFriend(player.getName().getString());
                if (!isMe && !isFriend) continue;
            }

            drawHat(event.getMatrixStack(), player, event.getTickDelta(), finalColor);
        }
    }

    private void drawHat(MatrixStack matrices, PlayerEntity player, float tickDelta, Color c) {
        // Nội suy tọa độ để nón di chuyển mượt mà theo người chơi
        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - mc.gameRenderer.getCamera().getPos().getX();
        double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - mc.gameRenderer.getCamera().getPos().getY();
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - mc.gameRenderer.getCamera().getPos().getZ();

        float yaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        matrices.push();
        
        // Đặt nón lên đầu
        matrices.translate(x, y + player.getHeight() + 0.1, z);
        
        // Xoay theo hướng đầu người chơi
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float radius = scale.getValue();
        float coneHeight = height.getValue();

        // 1. Vẽ thân nón (Nón lá hình nón)
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        
        // Đỉnh nón
        buffer.vertex(matrix, 0, coneHeight, 0)
              .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

        // Đáy nón
        for (int i = 0; i <= 32; i++) {
            double angle = i * 2 * Math.PI / 32;
            float px = (float) (Math.cos(angle) * radius);
            float pz = (float) (Math.sin(angle) * radius);
            buffer.vertex(matrix, px, 0, pz)
                  .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 2);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // 2. Vẽ viền nón (Outline) cho sắc nét
        RenderSystem.lineWidth(2.0f);
        BufferBuilder lineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 32; i++) {
            double angle = i * 2 * Math.PI / 32;
            float px = (float) (Math.cos(angle) * radius);
            float pz = (float) (Math.sin(angle) * radius);
            lineBuffer.vertex(matrix, px, 0, pz)
                      .color(c.getRed(), c.getGreen(), c.getBlue(), 255);
        }
        BufferRenderer.drawWithGlobalProgram(lineBuffer.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
