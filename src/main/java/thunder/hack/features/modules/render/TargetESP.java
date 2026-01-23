package thunder.hack.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.setting.Setting;
import java.awt.Color;

public class TargetESP extends Module {

    // Các Setting đầy đủ như bản cũ
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Ghost);
    private final Setting<Color> color = new Setting<>("Color", new Color(255, 255, 255, 200));
    private final Setting<Float> size = new Setting<>("Size", 1.0f, 0.1f, 2.0f);

    public TargetESP() {
        // Fix đúng constructor 2 tham số cho 1.21
        super("TargetESP", Category.RENDER);
    }

    private enum Mode {
        Circle, Cube, Ghost
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        // Lấy target chuẩn từ Aura của ThunderHack
        if (ModuleManager.aura == null || ModuleManager.aura.target == null) return;
        
        Entity target = ModuleManager.aura.target;
        Color c = color.getValue();

        switch (mode.getValue()) {
            case Circle:
                // FIX LỖI #284: Thêm đủ 7 tham số mà Render3DEngine 1.21 yêu cầu
                // stack, entity, radius, color(int), segments, filled, fade
                Render3DEngine.drawCircle3D(stack, target, size.getValue(), c.getRGB(), 30, false, 1);
                break;

            case Cube:
                // Render hộp theo style cũ bạn thích
                Box box = target.getBoundingBox();
                Render3DEngine.drawFilledBox(stack, box, Render2DEngine.injectAlpha(c, 100));
                Render3DEngine.drawBoxOutline(box, c, 2.0f);
                break;

            case Ghost:
                // Hiệu ứng Ghost bay lượn sóng cực dài và mượt
                Render3DEngine.drawTargetEsp(stack, target);
                break;
        }
    }
}
