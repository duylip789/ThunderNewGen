package thunder.hack.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.core.manager.client.ModuleManager;
import java.awt.*;

// --- ĐỊA CHỈ ĐÃ SỬA LẠI THEO Ý BẠN ---
import thunder.hack.setting.Setting; 
import thunder.hack.events.impl.EventRender3D; 
// ------------------------------------

public class TargetESP extends Module {

    public TargetESP() {
        // Nếu Category.RENDER lỗi thì đổi thành Category.VISUALS
        super("TargetESP", "Hien thi Target", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Ghost);
    private final Setting<Color> color = new Setting<>("Color", new Color(255, 255, 255, 200));
    private final Setting<Float> size = new Setting<>("Size", 1.0f, 0.1f, 2.0f);

    private enum Mode {
        Circle, Cube, Ghost
    }

    @Override
    public void onRender3D(EventRender3D event) { 
        // Lay muc tieu tu Aura
        Entity target = ModuleManager.get(Aura.class).target;
        if (target == null) return;

        MatrixStack stack = event.getMatrixStack();
        Color c = color.getValue();

        switch (mode.getValue()) {
            case Circle:
                Render3DEngine.drawCircle3D(stack, target, size.getValue(), c);
                break;
            case Cube:
                Box box = target.getBoundingBox();
                Render3DEngine.drawFilledBox(stack, box, Render2DEngine.injectAlpha(c, 100));
                Render3DEngine.drawBoxOutline(box, c, 2.0f);
                break;
            case Ghost:
                // Su dung ham 3D bay lung tung trong Render3DEngine
                Render3DEngine.drawTargetEsp(stack, target);
                break;
        }
    }
}
