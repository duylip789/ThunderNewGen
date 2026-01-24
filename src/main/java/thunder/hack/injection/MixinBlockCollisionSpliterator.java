package thunder.hack.injection;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventCollision;

@Mixin(value = BlockCollisionSpliterator.class, priority = 800)
public abstract class MixinBlockCollisionSpliterator {

    @Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BlockView;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState computeNextHook(BlockView instance, BlockPos blockPos) {
        // 1. Lấy state gốc của block
        BlockState originalState = instance.getBlockState(blockPos);
        
        // 2. Tạo sự kiện Collision và gửi đi (để các module như Phase, Spider... xử lý nếu cần)
        EventCollision event = new EventCollision(originalState, blockPos);
        ThunderHack.EVENT_BUS.post(event);
        
        // 3. Trả về state đã được xử lý (Nếu không có module nào can thiệp thì nó vẫn là originalState)
        return event.getState();
    }
}
