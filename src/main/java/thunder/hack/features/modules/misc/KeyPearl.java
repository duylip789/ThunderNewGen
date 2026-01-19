package thunder.hack.features.modules.misc;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;

public class KeyPearl extends Module {

    public KeyPearl() {
        super("KeyPearl", Category.MISC);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Back);

    public enum Mode {
        Back, None
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            this.disable();
            return;
        }

        // Sửa lỗi 1: Sử dụng findItemInHotbar thay vì getItemSlot
        int pearlSlot = InventoryUtility.findItemInHotbar(Items.ENDER_PEARL);

        if (pearlSlot != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;

            // Chuyển sang slot Pearl
            mc.player.getInventory().selectedSlot = pearlSlot;

            // Sửa lỗi 2: Sử dụng đúng constructor cho bản Minecraft mới
            // Chúng ta dùng mc.player.getWorld().getRegistryManager()... hoặc đơn giản hơn là tương tác qua interactionManager
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, 0f, 0f));

            if (mode.getValue() == Mode.Back) {
                mc.player.getInventory().selectedSlot = oldSlot;
            }
        }
        
        this.disable();
    }
}
