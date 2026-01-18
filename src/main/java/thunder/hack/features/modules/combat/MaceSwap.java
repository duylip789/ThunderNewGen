package thunder.hack.features.modules.combat;

import thunder.hack.features.modules.Module;
import thunder.hack.features.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;

public class MaceSwap extends Module {

    // Đây là các dòng tạo ra Setting trong Menu
    // Khi bạn chuột phải vào module, các dòng này sẽ hiện ra
    private final Setting<Boolean> sword = new Setting<>("Sword", true); // Dung kiem khi dung dat
    private final Setting<Boolean> axe = new Setting<>("Axe", false);   // Dung riu khi dung dat
    private final Setting<Boolean> windBurst = new Setting<>("WindBurst", true); // Uu tien Mace co dong nay
    private final Setting<Boolean> breach = new Setting<>("Breach", false);      // Uu tien Mace co dong nay

    public MaceSwap() {
        super("MaceSwap", "Tu dong doi sang Mace khi roi xuong", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        // Kiem tra xem nguoi choi co dang roi tu do khong (De dap Crit Mace)
        boolean isFalling = mc.player.fallDistance > 0.5;

        if (isFalling) {
            // Neu dang roi -> Tim Mace de swap
            int maceSlot = findMace();
            if (maceSlot != -1) {
                InventoryUtility.switchTo(maceSlot);
            }
        } else {
            // Neu dang dung duoi dat -> Tim Kiem hoac Riu
            if (sword.getValue()) {
                int swordSlot = findSword();
                if (swordSlot != -1) InventoryUtility.switchTo(swordSlot);
            } else if (axe.getValue()) {
                int axeSlot = findAxe();
                if (axeSlot != -1) InventoryUtility.switchTo(axeSlot);
            }
        }
    }

    // Ham tim Mace (Co xu ly logic WindBurst/Breach don gian)
    private int findMace() {
        // Trong ThunderHack thuc te ban can dung EnchantmentHelper de check WindBurst
        // Day la code tim Mace co ban nhat:
        return InventoryUtility.findItemInHotBar(Items.MACE);
    }

    private int findSword() {
        return InventoryUtility.findClass(SwordItem.class);
    }

    private int findAxe() {
        return InventoryUtility.findClass(AxeItem.class);
    }
}
