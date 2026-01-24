package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class ClickAction extends Module {
    public ClickAction() {
        super("ClickAction", Category.PLAYER);
    }

    // Cài đặt bật tắt
    public final Setting<Boolean> pearl = new Setting<>("Pearl", true);
    public final Setting<Boolean> xp = new Setting<>("XP", true);

    // Dùng kiểu Integer cho phím bấm để tránh lỗi class Bind không tồn tại
    // Mặc định: Pearl là Chuột giữa (GLFW_MOUSE_BUTTON_MIDDLE), XP là phím X
    public final Setting<Integer> pearlKey = new Setting<>("Pearl Key", GLFW.GLFW_MOUSE_BUTTON_MIDDLE, 0, 1000, v -> pearl.getValue());
    public final Setting<Integer> xpKey = new Setting<>("XP Key", GLFW.GLFW_KEY_X, 0, 1000, v -> xp.getValue());

    private boolean pearlPressed = false;

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;

        // Xử lý XP: Giữ phím là đập liên tục (Bypass Grim/Matrix)
        if (xp.getValue() && isKeyDown(xpKey.getValue())) {
            performSilentAction(Items.EXPERIENCE_BOTTLE);
        }

        // Xử lý Pearl: Nhấn 1 lần ném 1 quả
        boolean isPearlDown = isKeyDown(pearlKey.getValue());
        if (pearl.getValue() && isPearlDown && !pearlPressed) {
            performSilentAction(Items.ENDER_PEARL);
            pearlPressed = true;
        } else if (!isPearlDown) {
            pearlPressed = false;
        }
    }

    private void performSilentAction(net.minecraft.item.Item item) {
        SearchInvResult result = InventoryUtility.findItemInHotBar(item);
        
        if (result.found()) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            int itemSlot = result.slot();

            if (itemSlot != oldSlot) {
                // Silent Switch thần thánh: Server thấy đổi, client vẫn cầm kiếm
                sendPacket(new UpdateSelectedSlotC2SPacket(itemSlot));
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
                sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
            } else {
                sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0));
            }
        }
    }

    // Hàm kiểm tra phím bấm thủ công để không phụ thuộc vào class Bind
    private boolean isKeyDown(int key) {
        if (key < 0) return false;
        if (key < 8) return org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        return org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }
}
