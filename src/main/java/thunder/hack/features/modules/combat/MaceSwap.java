package thunder.hack.features.modules.combat;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import thunder.hack.events.impl.player.AttackEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.InventoryUtil;
import meteordevelopment.orbit.EventHandler;

public class MaceSwap extends Module {

    public MaceSwap() {
        super("MaceSwap", Category.COMBAT);
    }

    // --- SETTINGS ---
    public final Setting<Float> minFall = new Setting<>("MinFallDistance", 1.5f, 0.1f, 10.0f);
    public final Setting<Boolean> backSwap = new Setting<>("BackSwap", true); // Tự động quay về kiếm sau khi đập

    private int lastSlot = -1;
    private boolean swapping = false;

    @EventHandler
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null) return;

        // 1. Kiểm tra xem có đang rơi không
        if (mc.player.fallDistance < minFall.getValue()) return;

        // 2. Kiểm tra item đang cầm trên tay có phải Kiếm hoặc Rìu không
        ItemStack heldItem = mc.player.getMainHandStack();
        boolean isWeapon = heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem;

        if (isWeapon) {
            // 3. Tìm slot có Chùy (Mace) trong hotbar
            int maceSlot = findMaceSlot();

            if (maceSlot != -1 && maceSlot != mc.player.getInventory().selectedSlot) {
                // Lưu lại slot cũ (Kiếm/Rìu)
                lastSlot = mc.player.getInventory().selectedSlot;
                
                // Thực hiện Swap sang Mace ngay lập tức
                mc.player.getInventory().selectedSlot = maceSlot;
                swapping = true;
                
                // Lưu ý: Hệ thống sẽ tự gửi packet đánh bằng vật phẩm ở slot mới (Mace)
            }
        }
    }

    // Sau khi đánh xong (Tick tiếp theo), nếu bật BackSwap thì quay lại kiếm/rìu
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

    private int findMaceSlot() {
        // Tìm trong 9 ô Hotbar
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MACE) {
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
