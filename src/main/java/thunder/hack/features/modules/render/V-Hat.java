package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventRender3D;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.ColorSetting;
import thunder.hack.setting.Setting;
import meteordevelopment.orbit.EventHandler;

import java.awt.Color;

public class VHat extends Module {

    public VHat() {
        super("V-Hat", Category.RENDER);
    }

    // --- CÀI ĐẶT (SETTINGS) ---

    // Chế độ chỉ hiển thị cho bạn bè
    public final Setting<Boolean> friendsOnly = new Setting<>("Friend", false);

    // Chế độ đồng bộ màu với HUD Editor
    public final Setting<Boolean> syncColor = new Setting<>("Sync Color", true);

    // Màu tùy chỉnh (chỉ hiện khi tắt Sync Color)
    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 150).getRGB()), v -> !syncColor.getValue());

    // Tùy chỉnh kích thước nón lá
    public final Setting<Float> scale = new Setting<>("Scale", 0.6f, 0.1f, 1.0f); // Độ rộng
    public final Setting<Float> height = new Setting<>("Height", 0.3f, 0.1f, 1.0f); // Độ cao

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        // Xử lý màu sắc: Nếu Sync bật -> Lấy màu HUD, nếu tắt -> Lấy màu custom
        Color finalColor;
        if (syncColor.getValue()) {
            finalColor = HudEditor.getColor(1); 
        } else {
            finalColor = color.getValue().getColorObject();
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            // Bỏ qua người chơi đang tàng hình hoặc chế độ khán giả
            if (player.isInvisible() || player.isSpectator()) continue;

            // Logic Friend Only:
            // Nếu bật -> Chỉ hiện nón cho Bản thân (mc.player) và Bạn bè
            if (friendsOnly.getValue()) {
                boolean isMe = (player == mc.player);
                boolean isFriend = ThunderHack.friendManager.isFriend(player.getName().getString());
                
                if (!isMe && !isFriend) continue; 
            }

            drawHat(event.getMatrixStack(), player, event.getTickDelta(), finalColor);
        }
    }

    private void drawHat(MatrixStack matrices, PlayerEntity player, float tickDelta, Color c) {
        // 1. Tính toán vị trí nội suy (Interpolation) để mượt mà
        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - mc.gameRenderer.getCamera().getPos().getX();
        double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - mc.gameRenderer.getCamera().getPos().getY();
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - mc.gameRenderer.getCamera().getPos().getZ();

        // 2. Tính toán góc xoay đầu
        float yaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        matrices.push();
        
        // Dịch chuyển matrix đến vị trí đỉnh đầu của player
        matrices.translate(x, y + player.getHeight(), z);
        
        // Xoay nón theo hướng nhìn của đầu
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        // Thiết lập Render System cho 1.21
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull(); // Để nhìn thấy cả mặt trong của nón
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        
        // --- VẼ HÌNH NÓN (Nón Lá) ---
        // Sử dụng TRIANGLE_FAN: Vẽ rẻ quạt từ đỉnh nón xuống đáy
        // Lưu ý: Trong 1.21, Tessellator.begin() trả về BufferBuilder
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float radius = scale.getValue();
        float coneHeight = height.getValue();

        // Đỉnh nón (Tâm ở trên cao)
        buffer.vertex(matrix, 0, coneHeight, 0)
              .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

        // Vẽ vòng tròn đáy nón
        for (int i = 0; i <= 32; i++) {
            double angle = i * 2 * Math.PI / 32;
            float px = (float) (Math.cos(angle) * radius);
            float pz = (float) (Math.sin(angle) * radius);

            buffer.vertex(matrix, px, 0, pz)
                  .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 2); // Làm mờ phần đáy chút cho đẹp
        }
        
        // Kết thúc vẽ hình nón
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // --- VẼ VIỀN NÓN (Outline) ---
        // Vẽ thêm một đường viền đậm để nón sắc nét hơn
        RenderSystem.lineWidth(1.5f);
        
        // Dùng DEBUG_LINES hoặc LINE_STRIP cho viền
        BufferBuilder lineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= 32; i++) {
            double angle = i * 2 * Math.PI / 32;
            float px = (float) (Math.cos(angle) * radius);
            float pz = (float) (Math.sin(angle) * radius);

            lineBuffer.vertex(matrix, px, 0, pz)
                      .color(c.getRed(), c.getGreen(), c.getBlue(), 255); // Viền đậm (Alpha 255)
        }
        
        BufferRenderer.drawWithGlobalProgram(lineBuffer.end());

        // Reset trạng thái render
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }
          }

