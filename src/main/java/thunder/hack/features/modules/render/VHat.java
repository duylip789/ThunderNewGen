package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;

import java.awt.*;

public class VHat extends Module {

    public VHat() {
        super("V-Hat", Category.RENDER);
    }

    public final Setting<Boolean> friendsOnly = new Setting<>("Friend", false);
    public final Setting<Boolean> syncColor = new Setting<>("SyncColor", true);
    public final Setting<ColorSetting> color = new Setting<>(
            "Color",
            new ColorSetting(new Color(255, 255, 255, 160).getRGB()),
            v -> !syncColor.getValue()
    );
    public final Setting<Float> scale = new Setting<>("Scale", 0.6f, 0.1f, 1.2f);
    public final Setting<Float> height = new Setting<>("Height", 0.35f, 0.1f, 1.2f);

    /**
     * ThunderNewGen gọi hàm này trực tiếp trong renderer
     * KHÔNG override – KHÔNG event – KHÔNG annotation
     */
    public void onRender(MatrixStack matrices, float tickDelta) {
        if (mc.player == null || mc.world == null) return;

        Color finalColor = syncColor.getValue()
                ? HudEditor.getColor(1)
                : color.getValue().getColorObject();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.isSpectator()) continue;

            if (friendsOnly.getValue()) {
                if (player != mc.player &&
                        !Managers.FRIEND.isFriend(player.getName().getString()))
                    continue;
            }

            drawHat(matrices, player, tickDelta, finalColor);
        }
    }

    private void drawHat(MatrixStack matrices, PlayerEntity player, float tickDelta, Color c) {
        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX())
                - mc.gameRenderer.getCamera().getPos().getX();
        double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY())
                - mc.gameRenderer.getCamera().getPos().getY();
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ())
                - mc.gameRenderer.getCamera().getPos().getZ();

        float yaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);

        matrices.push();
        matrices.translate(x, y + player.getHeight() + 0.05f, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tess = Tessellator.getInstance();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float r = scale.getValue();
        float h = height.getValue();
        int steps = 36;

        // Cone body
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(matrix, 0f, h, 0f)
                .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha())
                .next();

        for (int i = 0; i <= steps; i++) {
            double a = i * Math.PI * 2 / steps;
            float px = (float) (Math.cos(a) * r);
            float pz = (float) (Math.sin(a) * r);
            buf.vertex(matrix, px, 0f, pz)
                    .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 2)
                    .next();
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // Outline
        RenderSystem.lineWidth(1.6f);
        BufferBuilder line = tess.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            double a = i * Math.PI * 2 / steps;
            float px = (float) (Math.cos(a) * r);
            float pz = (float) (Math.sin(a) * r);
            line.vertex(matrix, px, 0f, pz)
                    .color(c.getRed(), c.getGreen(), c.getBlue(), 255)
                    .next();
        }
        BufferRenderer.drawWithGlobalProgram(line.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
