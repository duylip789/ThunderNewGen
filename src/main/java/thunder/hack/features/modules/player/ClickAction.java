package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.Bind; // Đảm bảo import đúng class Bind
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class ClickAction extends Module {
    public ClickAction() {
        super("ClickAction", Category.PLAYER);
    }

    public final Setting<Boolean> pearl = new Setting<>("Pearl", true);
    // Khởi tạo Bind với phím -1 (None), không giữ, không bỏ qua
    public final Setting<Bind> pearlKey = new Setting<>("Pearl Key", new Bind(-1, false, false), v -> pearl.getValue());

    public final Setting<Boolean> xp = new Setting<>("XP", true);
    public final Setting<Bind> xpKey = new Setting<>("XP Key", new Bind(-1, false, false), v -> xp.getValue());

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;

        // Xử lý XP: Giữ nút là ném liên tục (20 lần/giây)
        if (xp.getValue() && xpKey.getValue().isKeyDown()) {
            performSilentAction(Items.EXPERIENCE_BOTTLE);
        }

        // Xử lý Pearl: Nhấn nút là ném 1 quả ngay lập tức
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
                // Silent Switch: Tráo item qua packet để không đổi tay cầm kiếm
                sendPacket(new UpdateSelectedSlotC2SPacket(itemSlot));
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
                sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            } else {
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
            }
        }
    }
}
