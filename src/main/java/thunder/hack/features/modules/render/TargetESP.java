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
import thunder.hack.setting.Setting;
import thunder.hack.events.impl.WorldRenderEvent; // Ở 1.21 thường dùng WorldRenderEvent
import java.awt.*;

public class TargetESP extends Module {

    public TargetESP() {
        super("TargetESP", "Hien thi hieu ung quanh muc tieu", Category.RENDER);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Ghost);
    private final Setting<Color> color = new Setting<>("Color", new Color(255, 255, 255, 200));
    private final Setting<Float> size = new Setting<>("Size", 1.0f, 0.1f, 2.0f);

    private enum Mode {
        Circle, Cube, Ghost
    }

    // Ở 1.21, hàm render 3D thường được đặt tên là onWorldRender hoặc onRender3D tùy mixin
    @Override
    public void onWorldRender(WorldRenderEvent event) { 
        // Lấy mục tiêu từ Aura
        Entity target = ModuleManager.targetESP.getTarget(); // Hoặc dùng cách cũ của bạn
        if (target == null) target = ModuleManager.aura.target;
        
        if (target == null) return;

        MatrixStack stack = event.getStack(); // 1.21 dùng getStack() thay vì getMatrixStack()
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
                Render3DEngine.drawTargetEsp(stack, target);
                break;
        }
    }

    // Hàm bổ trợ để lấy target an toàn
    public Entity getTarget() {
        return ModuleManager.aura.isEnabled() ? ModuleManager.aura.target : null;
    }
}
