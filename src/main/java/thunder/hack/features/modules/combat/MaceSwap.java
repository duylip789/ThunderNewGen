package thunder.hack.features.modules.combat;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import thunder.hack.events.impl.AttackEvent; // Đã xóa .player
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import meteordevelopment.orbit.EventHandler;

public class MaceSwap extends Module {

    public MaceSwap() {
        super("MaceSwap", Category.COMBAT);
    }

    public final Setting<Float> minFall = new Setting<>("MinFallDistance", 1.5f, 0.1f, 10.0f);
    public final Setting<Boolean> backSwap = new Setting<>("BackSwap", true);

    private int lastSlot = -1;
    private boolean swapping = false;

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Nếu không rơi đủ độ cao thì không làm gì cả
        if (mc.player.fallDistance < minFall.getValue()) return;

        // Kiểm tra vật phẩm đang cầm
        ItemStack heldItem = mc.player.getMainHandStack();
        boolean isWeapon = heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem;

        if (isWeapon) {
            int maceSlot = findMaceSlot();
            if (maceSlot != -1 && maceSlot != mc.player.getInventory().selectedSlot) {
                lastSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = maceSlot;
                swapping = true;
            }
        }
    }

    // Kiểm tra mỗi tick để trả về slot cũ sau khi đã chạm đất hoặc hết rơi
    @Override
    public void onUpdate() {
        if (swapping && lastSlot != -1 && backSwap.getValue()) {
            if (mc.player.isOnGround() || mc.player.fallDistance == 0) {
                mc.player.getInventory().selectedSlot = lastSlot;
                lastSlot = -1;
                swapping = false;
            }
        }
    }

    // Hàm tự tìm slot Mace trong Hotbar (không cần dùng InventoryUtil)
    private int findMaceSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        lastSlot = -1;
        swapping = false;
    }
}
