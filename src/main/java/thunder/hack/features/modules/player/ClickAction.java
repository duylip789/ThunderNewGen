package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Bind;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class ClickAction extends Module {
    public ClickAction() {
        super("ClickAction", Category.PLAYER);
    }

    public final Setting<Boolean> pearl = new Setting<>("Pearl", true);
    public final Setting<Bind> pearlKey = new Setting<>("Pearl Key", new Bind(-1, false, false), v -> pearl.getValue());

    public final Setting<Boolean> xp = new Setting<>("XP", true);
    public final Setting<Bind> xpKey = new Setting<>("XP Key", new Bind(-1, false, false), v -> xp.getValue());

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Legit);
    public final Setting<Boolean> rotation = new Setting<>("Rotation", true);

    public enum Mode { Legit, Fast }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;

        // Xử lý XP (Giữ nút để đập liên tục)
        if (xp.getValue() && xpKey.getValue().isKeyDown()) {
            throwItem(Items.EXPERIENCE_BOTTLE);
        }

        // Xử lý Pearl (Bấm nút là ném 1 quả)
        // Lưu ý: pearlKey.getValue().isKeyPressed() dùng cho nhấn 1 lần
        if (pearl.getValue() && pearlKey.getValue().isKeyPressed()) {
            throwItem(Items.ENDER_PEARL);
        }
    }

    private void throwItem(net.minecraft.item.Item item) {
        SearchInvResult result = InventoryUtility.findItemInHotBar(item);
        
        if (result.found()) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            int newSlot = result.slot();

            if (newSlot != oldSlot) {
                // Gửi packet đổi slot (Grim/Matrix sẽ thấy bạn cầm item đó rất nhanh)
                sendPacket(new UpdateSelectedSlotC2SPacket(newSlot));
                
                // Gửi packet sử dụng item
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0)); // 0 là sequence
                
                // Trả về slot cũ ngay lập tức (vẫn cầm kiếm trên màn hình)
                sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                
                // Đồng bộ lại slot cho client tránh bị ghost item
                mc.player.getInventory().selectedSlot = oldSlot;
            } else {
                // Nếu đang cầm sẵn trên tay thì chỉ việc ném
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
            }
        }
    }
}

