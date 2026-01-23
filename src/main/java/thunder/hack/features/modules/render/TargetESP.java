package thunder.hack.features.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import thunder.hack.events.impl.EventRender3D;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.core.manager.client.ModuleManager;
import java.awt.*;

public class TargetESP extends Module {

    // Sử dụng Category.RENDER để nó nằm cùng nhóm với các module vẽ
    public TargetESP() {
        super("TargetESP", "Hiển thị hiệu ứng quanh mục tiêu", Category.RENDER);
    }

    // --- CÀI ĐẶT ---
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Ghost);
    private final Setting<Color> color = new Setting<>("Color", new Color(255, 255, 255, 200));
    private final Setting<Float> size = new Setting<>("Size", 1.0f, 0.1f, 2.0f);

    private enum Mode {
        Circle, // Trước đây là CelkaPasta
        Cube,   // Trước đây là NurikZapen
        Ghost   // Bản 3D bay lung tung cầu vồng
    }

    @Override
    public void onRender3D(EventRender3D event) {
        // Lấy mục tiêu đang bị tấn công từ module Aura
        Entity target = ModuleManager.get(Aura.class).target;

        if (target == null) return;

        MatrixStack stack = event.getMatrixStack();
        Color c = color.getValue();

        switch (mode.getValue()) {
            case Circle:
                // Vẽ vòng tròn Celka (có sẵn trong Render3DEngine của bạn)
                Render3DEngine.drawCircle3D(stack, target, size.getValue(), c);
                break;

            case Cube:
                // Vẽ hình hộp Nurik (Kết hợp khối đặc và khung)
                Box box = target.getBoundingBox();
                Render3DEngine.drawFilledBox(stack, box, Render2DEngine.injectAlpha(c, 100));
                Render3DEngine.drawBoxOutline(box, c, 2.0f);
                break;

            case Ghost:
                // Gọi hàm "Bay lung tung" chúng ta vừa thêm vào Render3DEngine
                Render3DEngine.drawTargetEsp(stack, target);
                break;
        }
    }
}

