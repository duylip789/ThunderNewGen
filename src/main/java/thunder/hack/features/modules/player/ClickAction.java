package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
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

    // ===== SETTINGS =====
    public final Setting<Boolean> pearl =
            new Setting<>("Pearl", true);

    public final Setting<Boolean> xp =
            new Setting<>("XP", true);

    // Mouse Middle = M2, XP = X (đúng như ảnh)
    public final Setting<Integer> pearlKey =
            new Setting<>("Pearl Key", GLFW.GLFW_MOUSE_BUTTON_MIDDLE, 0, 1000, v -> pearl.getValue());

    public final Setting<Integer> xpKey =
            new Setting<>("XP Key", GLFW.GLFW_KEY_X, 0, 1000, v -> xp.getValue());

    private boolean pearlPressed = false;

    // ===== MAIN LOGIC =====
    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.world == null) return;

        // ===== XP: giữ phím spam =====
        if (xp.getValue() && isKeyDown(xpKey.getValue())) {
            silentUse(Items.EXPERIENCE_BOTTLE);
        }

        // ===== PEARL: bấm 1 lần 1 quả =====
        boolean down = isKeyDown(pearlKey.getValue());
        if (pearl.getValue() && down && !pearlPressed) {
            silentUse(Items.ENDER_PEARL);
            pearlPressed = true;
        }

        if (!down) {
            pearlPressed = false;
        }
    }

    // ===== SILENT USE (1.21 SAFE) =====
    private void silentUse(Item item) {
        SearchInvResult result = InventoryUtility.findItemInHotBar(item);
        if (!result.found()) return;

        int oldSlot = mc.player.getInventory().selectedSlot;
        int slot = result.slot();

        if (slot != oldSlot) {
            sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            sendUsePacket();
            sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
        } else {
            sendUsePacket();
        }
    }

    // ===== USE PACKET (1.21 FIX) =====
    private void sendUsePacket() {
        sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                0,
                mc.player.getYaw(),
                mc.player.getPitch()
        ));
    }

    // ===== KEY CHECK (NO BIND CLASS) =====
    private boolean isKeyDown(int key) {
        long handle = mc.getWindow().getHandle();

        // Mouse
        if (key >= 0 && key <= 7) {
            return GLFW.glfwGetMouseButton(handle, key) == GLFW.GLFW_PRESS;
        }

        // Keyboard
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }
}
