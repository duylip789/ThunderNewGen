package thunder.hack.features.modules.misc;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

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

        // Tự tìm slot Pearl trong 9 slot Hotbar
        int pearlSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                pearlSlot = i;
                break;
            }
        }

        if (pearlSlot != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;

            // Chuyển sang slot Pearl
            mc.player.getInventory().selectedSlot = pearlSlot;

            // Gửi packet ném Pearl (tương thích 1.19.4 - 1.20.x)
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, 0f, 0f));

            // Nếu mode là Back thì quay về slot cũ
            if (mode.getValue() == Mode.Back) {
                mc.player.getInventory().selectedSlot = oldSlot;
            }
        }
        
        // Tắt module ngay sau khi ném
        this.disable();
    }
}
