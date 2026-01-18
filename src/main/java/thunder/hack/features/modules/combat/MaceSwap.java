package thunder.hack.features.modules.combat;

import thunder.hack.features.modules.Module;
// Sửa dòng import này (thêm chữ s vào settings hoặc kiểm tra đúng đường dẫn của NextGen)
import thunder.hack.setting.Setting; 
import thunder.hack.utility.player.InventoryUtility;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;

public class MaceSwap extends Module {

    // Khai báo lại Setting cho đúng cú pháp NextGen
    private final Setting<Boolean> sword = new Setting<>("Sword", true);
    private final Setting<Boolean> axe = new Setting<>("Axe", false);
    private final Setting<Boolean> windBurst = new Setting<>("WindBurst", true);
    private final Setting<Boolean> breach = new Setting<>("Breach", false);

    public MaceSwap() {
        super("MaceSwap", "Tu dong doi sang Mace khi roi xuong", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        boolean isFalling = mc.player.fallDistance > 0.5f; // Thêm chữ f sau 0.5

        if (isFalling) {
            int maceSlot = InventoryUtility.findItemInHotBar(Items.MACE);
            if (maceSlot != -1) {
                InventoryUtility.switchTo(maceSlot);
            }
        } else {
            if (sword.getValue()) {
                int swordSlot = InventoryUtility.findClass(SwordItem.class);
                if (swordSlot != -1) InventoryUtility.switchTo(swordSlot);
            } else if (axe.getValue()) {
                int axeSlot = InventoryUtility.findClass(AxeItem.class);
                if (axeSlot != -1) InventoryUtility.switchTo(axeSlot);
            }
        }
    }
}
