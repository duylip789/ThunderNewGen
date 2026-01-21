package thunder.hack.features.modules.combat;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AutoTotem extends Module {

    // ===== SETTINGS =====
    public final Setting<Float> health =
            new Setting<>("Health", 12.0f, 1f, 36f);

    public final Setting<Boolean> invTotem =
            new Setting<>("InvTotem", true);

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        // Nếu máu > setting thì bỏ
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.getValue())
            return;

        // Nếu đã cầm totem
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING)
            return;

        // Tìm totem
        int slot = findTotemSlot();
        if (slot == -1) return;

        // Swap sang offhand
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                40,
                net.minecraft.screen.slot.SlotActionType.SWAP,
                mc.player
        );
    }

    private int findTotemSlot() {
        // Hotbar + inventory
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                if (!invTotem.getValue() && i >= 9) continue;
                return i;
            }
        }
        return -1;
    }
}
