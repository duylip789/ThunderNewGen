package thunder.hack.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.EnumSetting;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.Random;

public class TargetESP extends Module {

    public enum Mode {
        NewGen
    }

    private final Setting<Mode> mode =
            new EnumSetting<>("Mode", Mode.NewGen);

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @Override
    public void onRender(MatrixStack matrices, float tickDelta) {
        if (!ModuleManager.aura.isEnabled()) return;

        LivingEntity target = Aura.target;
        if (target == null || target.isRemoved()) return;

        if (mode.getValue() == Mode.NewGen) {
            renderGhost(target, tickDelta);
        }
    }

    /* ================= GHOST ESP ================= */

    private void renderGhost(LivingEntity target, float tickDelta) {
        Vec3d basePos = Render3DEngine.getEntityRenderPos(target, tickDelta);

        float time = (System.currentTimeMillis() % 10_000L) / 1000f;

        Color color = ModuleManager.hud.getColor(1);

        for (int i = 0; i < 3; i++) {
            float angle = time * 2f + i * 2.1f;
            float radius = 0.6f + randomOffset(i) * 0.3f;

            double x = basePos.x + Math.cos(angle) * radius;
            double z = basePos.z + Math.sin(angle) * radius;
            double y = basePos.y + 0.8f
                    + Math.sin(time * 3 + i) * 0.4f;

            drawWormTrail(
                    basePos.x, basePos.y + 0.9, basePos.z,
                    x, y, z,
                    color
            );
        }
    }

    /* ============== GIUN / WORM TRAIL ============== */

    private void drawWormTrail(
            double sx, double sy, double sz,
            double ex, double ey, double ez,
            Color color
    ) {
        int segments = 12;

        for (int i = 0; i < segments; i++) {
            float t1 = i / (float) segments;
            float t2 = (i + 1) / (float) segments;

            double x1 = MathHelper.lerp(t1, sx, ex);
            double y1 = MathHelper.lerp(t1, sy, ey);
            double z1 = MathHelper.lerp(t1, sz, ez);

            double x2 = MathHelper.lerp(t2, sx, ex);
            double y2 = MathHelper.lerp(t2, sy, ey);
            double z2 = MathHelper.lerp(t2, sz, ez);

            float alpha = 0.6f * (1f - t1);

            Render3DEngine.drawLine(
                    x1, y1, z1,
                    x2, y2, z2,
                    new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            (int) (alpha * 255)
                    ),
                    1.5f
            );
        }
    }

    private float randomOffset(int seed) {
        Random r = new Random(seed * 9999L);
        return r.nextFloat() - 0.5f;
    }
}
