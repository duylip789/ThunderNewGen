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
        Back, // Ném xong quay về item cũ
        None  // Ném xong giữ nguyên ngọc trên tay
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            this.disable();
            return;
        }

        // Tìm vị trí của Ender Pearl trong Hotbar
        int pearlSlot = InventoryUtility.getItemSlot(Items.ENDER_PEARL);

        if (pearlSlot != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;

            // Chuyển sang slot có Ngọc
            InventoryUtility.switchTo(pearlSlot);

            // Gửi packet ném ngọc ngay lập tức
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));

            // Nếu mode là Back, chuyển về slot cũ
            if (mode.getValue() == Mode.Back) {
                InventoryUtility.switchTo(oldSlot);
            }
        } else {
            // Thông báo nếu không có ngọc (tùy chọn)
            // Command.sendMessage("No Ender Pearls found in hotbar!");
        }

        // Tự động tắt module sau khi thực thi xong (vì đây là dạng instant action)
        this.disable();
    }
}
