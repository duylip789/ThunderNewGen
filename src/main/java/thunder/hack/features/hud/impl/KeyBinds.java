package thunder.hack.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.Random;

public class TargetESP extends Module {

    public enum Mode {
        NewGen
    }

    private final Setting<Mode> mode =
            new Setting<>("Mode", Mode.NewGen);

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @Override
    public void onRender3D(MatrixStack matrices, float tickDelta) {
        if (!Aura.isEnabled()) return;

        Entity ent = Aura.target;
        if (!(ent instanceof LivingEntity target)) return;

        if (mode.getValue() == Mode.NewGen) {
            renderGhost(target, tickDelta);
        }
    }

    /* ================= GHOST ESP ================= */

    private void renderGhost(LivingEntity target, float tickDelta) {
        Vec3d base = target.getLerpedPos(tickDelta);

        float time = (System.currentTimeMillis() % 10_000L) / 1000f;
        Color color = getColor();

        for (int i = 0; i < 3; i++) {
            float a = time * 2f + i * 2.1f;
            float r = 0.6f + random(i) * 0.3f;

            Vec3d end = base.add(
                    Math.cos(a) * r,
                    0.9 + Math.sin(time * 3 + i) * 0.4,
                    Math.sin(a) * r
            );

            drawWorm(base.add(0, 0.9, 0), end, color);
        }
    }

    /* ============== GIUN NGHÍ NGOÁY ============== */

    private void drawWorm(Vec3d start, Vec3d end, Color color) {
        int seg = 12;

        for (int i = 0; i < seg; i++) {
            float t1 = i / (float) seg;
            float t2 = (i + 1) / (float) seg;

            Vec3d p1 = lerp(start, end, t1);
            Vec3d p2 = lerp(start, end, t2);

            float alpha = 1f - t1;

            Render3DEngine.drawLine(
                    p1,
                    p2,
                    new Color(
                            color.getRed(),
                            color.getGreen(),
                            color.getBlue(),
                            (int) (alpha * 180)
                    )
            );
        }
    }

    private Vec3d lerp(Vec3d a, Vec3d b, float t) {
        return new Vec3d(
                MathHelper.lerp(t, a.x, b.x),
                MathHelper.lerp(t, a.y, b.y),
                MathHelper.lerp(t, a.z, b.z)
        );
    }

    private float random(int seed) {
        return new Random(seed * 9999L).nextFloat() - 0.5f;
    }
}
