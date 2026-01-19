package thunder.hack.features.modules.combat;

import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import net.minecraft.item.Items;

public class MaceSwap extends Module {

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.WindBurst);
    private final Setting<Boolean> maceStun = new Setting<>("Mace Stun", false);
    private final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
    private final Setting<Boolean> silent = new Setting<>("Silent", true);

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

        if (mc.player.fallDistance > 0.5f) {
            // CÁCH SỬA 1: Thử dùng slot() như một hàm Record
            int maceSlot = InventoryUtility.findItemInHotBar(Items.MACE).slot();
            
            if (maceSlot != -1) {
                if (oldSlot == -1) oldSlot = mc.player.getInventory().selectedSlot;
                InventoryUtility.switchTo(maceSlot);
            }
        } else if (switchBack.getValue() && oldSlot != -1) {
            InventoryUtility.switchTo(oldSlot);
            oldSlot = -1;
        }
    }
}
