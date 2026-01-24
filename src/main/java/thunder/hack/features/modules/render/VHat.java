package thunder.hack.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.render.Render3DEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;

import java.awt.Color;

public class VHat extends Module {

    public VHat() {
        super("V-Hat", Category.RENDER);
    }

    public final Setting<Boolean> friendsOnly = new Setting<>("Friend", false);
    public final Setting<Boolean> syncColor = new Setting<>("Sync Color", true);
    public final Setting<ColorSetting> color =
            new Setting<>("Color", new ColorSetting(new Color(255, 255, 255, 150).getRGB()),
                    v -> !syncColor.getValue());
    public final Setting<Float> scale = new Setting<>("Scale", 0.6f, 0.1f, 1.0f);
    public final Setting<Float> height = new Setting<>("Height", 0.3f, 0.1f, 1.0f);

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        Color finalColor = syncColor.getValue()
                ? HudEditor.getColor(1)
                : color.getValue().getColorObject();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isInvisible() || player.isSpectator()) continue;

            if (friendsOnly.getValue()) {
                if (player != mc.player && !Managers.FRIEND.isFriend(player.getName().getString()))
                    continue;
            }

            drawHat(event.getMatrixStack(), player, event.getTickDelta(), finalColor);
        }
    }

    private void drawHat(MatrixStack matrices, PlayerEntity player, float tickDelta, Color c) {
        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX())
                - mc.gameRenderer.getCamera().getPos().x;
        double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY())
                - mc.gameRenderer.getCamera().getPos().y;
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ())
                - mc.gameRenderer.getCamera().getPos().z;

        float yaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());

        matrices.push();
        matrices.translate(x, y + player.getHeight(), z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float radius = scale.getValue();
        float coneHeight = height.getValue();

        buffer.vertex(matrix, 0, coneHeight, 0)
                .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());

        for (int i = 0; i <= 32; i++) {
            double angle = i * Math.PI * 2 / 32;
            float px = (float) (Math.cos(angle) * radius);
            float pz = (float) (Math.sin(angle) * radius);
            buffer.vertex(matrix, px, 0, pz)
                    .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 2);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.5f);
        BufferBuilder line = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 32; i++) {
            double angle = i * Math.PI * 2 / 32;
            float px = (float) (Math.cos(angle) * radius);
            float pz = (float) (Math.sin(angle) * radius);
            line.vertex(matrix, px, 0, pz)
                    .color(c.getRed(), c.getGreen(), c.getBlue(), 255);
        }
        BufferRenderer.drawWithGlobalProgram(line.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
