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
import java.awt.*;

public class TargetESP extends Module {

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Ghost);
    private final Setting<Color> color = new Setting<>("Color", new Color(255, 255, 255, 200));
    private final Setting<Float> size = new Setting<>("Size", 1.0f, 0.1f, 2.0f);

    public TargetESP() {
        // FIX LỖI 1: Constructor chỉ nhận 2 tham số (String, Category)
        super("TargetESP", Category.RENDER);
    }

    private enum Mode {
        Circle, Cube, Ghost
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        Entity target = ModuleManager.aura.target;
        if (target == null) return;

        Color c = color.getValue();

        switch (mode.getValue()) {
            case Circle:
                // FIX LỖI 2: drawCircle3D yêu cầu: MatrixStack, Entity, float radius, int color, int segments, boolean filled, int fade
                Render3DEngine.drawCircle3D(stack, target, size.getValue(), c.getRGB(), 30, false, 1);
                break;
            case Cube:
                Box box = target.getBoundingBox();
                Render3DEngine.drawFilledBox(stack, box, Render2DEngine.injectAlpha(c, 100));
                Render3DEngine.drawBoxOutline(box, c, 2.0f);
                break;
            case Ghost:
                // Chế độ Ghost gọi thẳng hàm bạn đã có trong Render3DEngine
                Render3DEngine.drawTargetEsp(stack, target);
                break;
        }
    }
}
