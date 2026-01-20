package thunder.hack.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class AutoCrystalBase {

    // Dummy class – giữ để tránh lỗi build
    public static class PlaceData {
        public BlockPos pos;
        public float damage;

        public PlaceData(BlockPos pos, float damage) {
            this.pos = pos;
            this.damage = damage;
        }
    }

    @Nullable
    public PlaceData getPlaceData(BlockPos bp, PlayerEntity target) {
        return null;
    }
}
