package thunder.hack.injection;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import static thunder.hack.ThunderHack.mc;

@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding {

    @Shadow 
    public abstract boolean equals(KeyBinding other);

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void pressHook(CallbackInfoReturnable<Boolean> cir) {
        // Nếu bạn muốn giữ lại tính năng SafeWalk ngầm (tự giữ sneak khi ở mép block)
        // thì dùng code dưới đây. Nếu muốn xóa hẳn thì để trống hàm này.

        if (mc.player != null && mc.world != null && this.equals(mc.options.sneakKey)) {
            // Kiểm tra xem dưới chân có phải là không khí không để tự giữ sneak
            BlockPos underPlayer = new BlockPos(
                (int) Math.floor(mc.player.getX()), 
                (int) Math.floor(mc.player.getY()) - 1, 
                (int) Math.floor(mc.player.getZ())
            );

            if (mc.player.isOnGround() && mc.world.getBlockState(underPlayer).isAir()) {
                // cir.setReturnValue(true); // Bỏ comment dòng này nếu vẫn muốn tự sneak khi ra mép block
            }
        }
    }
}
