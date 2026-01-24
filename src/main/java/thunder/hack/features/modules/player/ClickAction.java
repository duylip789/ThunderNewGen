package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class ClickAction extends Module {
    public ClickAction() {
        super("ClickAction", Category.PLAYER);
    }

    public final Setting<Boolean> pearl = new Setting<>("Pearl", true);
    // Thay đổi Bind thành KeyBind để khớp với core của bạn
    public final Setting<thunder.hack.setting.KeyBind> pearlKey = new Setting<>("Pearl Key", new thunder.hack.setting.KeyBind(-1), v -> pearl.getValue());

    public final Setting<Boolean> xp = new Setting<>("XP", true);
    public final Setting<thunder.hack.setting.KeyBind> xpKey = new Setting<>("XP Key", new thunder.hack.setting.KeyBind(-1), v -> xp.getValue());

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;

        // Xử lý XP: Giữ nút là ném liên tục
        if (xp.getValue() && xpKey.getValue().isKeyDown()) {
            performSilentAction(Items.EXPERIENCE_BOTTLE);
        }

        // Xử lý Pearl: Nhấn nút là ném 1 quả (dùng chế độ "Just Pressed")
        if (pearl.getValue() && pearlKey.getValue().isKeyPressed()) {
            performSilentAction(Items.ENDER_PEARL);
        }
    }

    private void performSilentAction(net.minecraft.item.Item item) {
        SearchInvResult result = InventoryUtility.findItemInHotBar(item);
        
        if (result.found()) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            int itemSlot = result.slot();

            if (itemSlot != oldSlot) {
                // Packet-based Silent Switch: Server thấy đổi nhưng tay vẫn cầm kiếm
                sendPacket(new UpdateSelectedSlotC2SPacket(itemSlot));
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
                sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            } else {
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
            }
        }
    }
}
