package thunder.hack.features.modules.movement;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.animation.CaptureMark;
import java.awt.Color;

public class TargetESP extends Module {
    public final Setting<Mode> mode = new Setting<>("Mode", Mode.NewGen);
    public final Setting<Color> color = new Setting<>("Color", new Color(255, 80, 80, 180));

    public TargetESP() { super("TargetESP", Category.MOVEMENT); }

    public enum Mode { NewGen, Thunder, Nurik, Celka, Ghosts }

    @Override
    public void onRender3D(MatrixStack stack) {
        if (Aura.target instanceof LivingEntity target) {
            switch (mode.getValue()) {
                case NewGen -> renderNewGen(target);
                case Thunder -> Render3DEngine.drawTargetEsp(stack, target);
                case Nurik -> CaptureMark.render(target);
                case Celka -> Render3DEngine.drawOldTargetEsp(stack, target);
                case Ghosts -> Render3DEngine.renderGhosts(14, 8, 1.8f, 3f, target);
            }
        }
    }

    private void renderNewGen(LivingEntity ent) {
        Vec3d c = ent.getPos().add(0, ent.getHeight() / 2f, 0);
        for (int i = 0; i < 3; i++) {
            double r = Math.toRadians((System.currentTimeMillis() / 8 + i * 120) % 360);
            Vec3d e = c.add(Math.cos(r) * 3.2, Math.sin(r * 0.5) * 0.6, Math.sin(r) * 3.2);
            Render3DEngine.drawLine(c, e, new Color(color.getValue().getRGB() & 0xFFFFFF | 0x50000000, true), 2.2f);
        }
    }
}

