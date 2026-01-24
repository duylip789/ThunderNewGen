package thunder.hack.features.modules.render;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.Render3DEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;

import java.awt.*;
import java.util.Random;

public class TargetESP extends Module {

    public TargetESP() {
        super("TargetESP", Category.RENDER);
    }

    public void onRender3D(Render3DEvent e) {
        if (!ModuleManager.aura.isEnabled()) return;

        LivingEntity target = Aura.target;
        if (target == null) return;

        Vec3d base = target.getPos().add(0, target.getHeight() * 0.6, 0);
        Color color = ModuleManager.hud.getColor(1);

        renderNewGenGhost(base, color);
    }

    private void renderNewGenGhost(Vec3d base, Color color) {
        Random r = new Random();

        for (int i = 0; i < 3; i++) {
            Vec3d start = base.add(
                    r.nextGaussian() * 0.15,
                    r.nextGaussian() * 0.15,
                    r.nextGaussian() * 0.15
            );

            Vec3d end = start.add(
                    r.nextGaussian() * 0.8,
                    r.nextGaussian() * 0.8,
                    r.nextGaussian() * 0.8
            );

            Render3DEngine.drawLine(start, end, color);
        }
    }
}
