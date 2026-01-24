package thunder.hack.features.modules.combat;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class MaceSwap extends Module {
    public MaceSwap() { super("MaceSwap", Category.COMBAT); }

    public final Setting<Float> minFall = new Setting<>("MinFallDistance", 1.5f, 0.1f, 10.0f);
    public final Setting<Boolean> backSwap = new Setting<>("BackSwap", true);
    
    private int lastSlot = -1;
    private boolean swapping = false;

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        // Nếu đang rơi và nhấn chuột trái (attack)
        if (mc.player.fallDistance >= minFall.getValue() && mc.options.attackKey.isPressed()) {
            ItemStack heldItem = mc.player.getMainHandStack();
            if (heldItem.getItem() instanceof SwordItem || heldItem.getItem() instanceof AxeItem) {
                int maceSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) {
                        maceSlot = i;
                        break;
                    }
                }
                if (maceSlot != -1) {
                    lastSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = maceSlot;
                    swapping = true;
                }
            }
        }

        // Trả lại slot cũ khi chạm đất
        if (swapping && lastSlot != -1 && backSwap.getValue() && mc.player.isOnGround()) {
            mc.player.getInventory().selectedSlot = lastSlot;
            lastSlot = -1;
            swapping = false;
        }
    }
}
