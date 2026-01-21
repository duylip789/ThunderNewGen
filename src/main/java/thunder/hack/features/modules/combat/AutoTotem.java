package thunder.hack.features.modules.combat;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import thunder.hack.features.modules.Module;
import thunder.hack.features.setting.Setting;

public class AutoTotem extends Module {

    public Setting<Float> health = new Setting<>("Health", 12.0f, 1f, 36f);
    public Setting<Boolean> invTotem = new Setting<>("InvTotem", true);

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        // Chỉ hoạt động khi mở inventory nếu bật InvTotem
        if (invTotem.getValue()) {
            if (!(mc.currentScreen instanceof InventoryScreen)) return;
        } else {
            if (mc.currentScreen != null) return;
        }

        // Nếu offhand đã có totem thì thôi
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING)
            return;

        // Check máu
        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (hp > health.getValue()) return;

        // Tìm totem trong inventory (KHÔNG hotbar)
        int totemSlot = findTotemSlot();
        if (totemSlot == -1) return;

        // Click chuyển totem sang offhand
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                totemSlot,
                40, // offhand
                SlotActionType.SWAP,
                mc.player
        );
    }

    private int findTotemSlot() {
        for (int i = 9; i < 36; i++) { // chỉ inventory
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }
}
