package thunder.hack.features.modules.render;

import net.minecraft.entity.Entity;
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

    // ❌ KHÔNG @Override
    public void onRender() {
        if (!ModuleManager.aura.isEnabled()) return;
        if (Aura.target == null) return;

        // ✅ FIX ENTITY -> LIVINGENTITY
        if (!(Aura.target instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) Aura.target;

        Vec3d base = target.getPos().add(0, target.getHeight() / 2.0, 0);
        double time = System.currentTimeMillis() / 250.0;

        Color color = new Color(255, 255, 255, 200);

        // === 3 TIA GIUN ===
        drawWorm(base, time, 0, color);
        drawWorm(base, time, Math.PI * 2 / 3, color);
        drawWorm(base, time, Math.PI * 4 / 3, color);
    }

    private void drawWorm(Vec3d base, double time, double offset, Color color) {
        Vec3d prev = null;

        for (int i = 0; i < 24; i++) {
            double p = i / 24.0;

            double angle = time + p * 6 + offset;
            double radius = 0.6 * (1 - p);
            double height = p * 1.7;

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
