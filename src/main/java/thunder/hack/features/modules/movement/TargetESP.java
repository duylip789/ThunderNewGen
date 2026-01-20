package thunder.hack.features.modules.movement;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render3DEngine;
import java.awt.Color;

public class TargetESP extends Module {
    public final Setting<Color> color = new Setting<>("Color", new Color(255, 80, 80, 180));

    public TargetESP() { 
        super("TargetESP", Category.MOVEMENT); 
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        // Chỉ vẽ khi Aura có mục tiêu
        if (Aura.target instanceof LivingEntity target) {
            Vec3d center = target.getPos().add(0, target.getHeight() / 2f, 0);
            long time = System.currentTimeMillis();

            for (int i = 0; i < 3; i++) {
                double rot = Math.toRadians((time / 8 + i * 120) % 360);
                Vec3d end = center.add(
                        Math.cos(rot) * 3.2,
                        Math.sin(rot * 0.5) * 0.6,
                        Math.sin(rot) * 3.2
                );

                // Fix: Đã xóa tham số 2.2f để khớp với định nghĩa hàm drawLine(Vec3d, Vec3d, Color)
                Render3DEngine.drawLine(center, end, new Color(color.getValue().getRGB() & 0xFFFFFF | 0x50000000, true));
            }
        }
    }
}
