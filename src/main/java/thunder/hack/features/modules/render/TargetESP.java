package thunder.hack.features.modules.render;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;

public class TargetESP extends Module {

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    @Override
    public void onRender() {
        if (!ModuleManager.aura.isEnabled()) return;

        LivingEntity target = Aura.target;
        if (target == null) return;

        Vec3d base = target.getPos().add(0, target.getHeight() / 2f, 0);

        double time = System.currentTimeMillis() / 250.0;

        // màu giống ảnh (trắng + hơi glow)
        Color color = new Color(255, 255, 255, 200);

        // === 3 TIA ===
        renderWorm(base, time, 0, color);
        renderWorm(base, time, Math.PI * 2 / 3, color);
        renderWorm(base, time, Math.PI * 4 / 3, color);
    }

    private void renderWorm(Vec3d base, double time, double offset, Color color) {
        Vec3d prev = null;

        for (int i = 0; i < 25; i++) {
            double progress = i / 25.0;

            double angle = time + progress * 6 + offset;
            double radius = 0.6 * (1 - progress);
            double height = progress * 1.8;

            Vec3d pos = base.add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
            );

            if (prev != null) {
                Render3DEngine.drawLine(prev, pos, color);
            }

            prev = pos;
        }
    }
}
