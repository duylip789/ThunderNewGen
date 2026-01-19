package thunder.hack.features.modules.combat;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;

public class MaceSwap extends Module {

    // Tạo menu thả xuống (Mode)
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.WindBurst);
    
    // Các nút gạt tích (Checkboxes)
    private final Setting<Boolean> maceStun = new Setting<>("Mace Stun", false);
    private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
    private final Setting<Boolean> silent = new Setting<>("Silent", true);

    // Khai báo các chế độ trong Mode
    private enum Mode {
        Normal, Breach, WindBurst
    }

    public MaceSwap() {
        super("MaceSwap", Category.COMBAT);
    }

    private int oldSlot = -1;

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        // Logic cơ bản: Nếu đang rơi thì swap sang Mace
        if (mc.player.fallDistance > 0.5f) {
            int maceSlot = InventoryUtility.findItemInHotBar(Items.MACE).getSlot();
            if (maceSlot != -1) {
                if (oldSlot == -1) oldSlot = mc.player.getInventory().selectedSlot;
                InventoryUtility.switchTo(maceSlot);
            }
        } else if (switchBack.getValue() && oldSlot != -1) {
            // Nếu tiếp đất và có bật Switch Back thì đổi về đồ cũ
            InventoryUtility.switchTo(oldSlot);
            oldSlot = -1;
        }
    }
}
