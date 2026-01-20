package thunder.hack.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * AutoCrystal DUMMY (ẨN + NHẸ)
 *
 * - Không phải Module
 * - Không đăng ký event
 * - Không hiện GUI
 * - Chỉ để satisfy compile
 */
public final class AutoCrystal {

    /* ========= INSTANCE GIẢ ========= */

    private static final AutoCrystal INSTANCE = new AutoCrystal();

    public static AutoCrystal getInstance() {
        return INSTANCE;
    }

    private AutoCrystal() {
        // KHÔNG LÀM GÌ
    }

    /* ========= DATA GIẢ ========= */

    public static class PlaceData {
        public final BlockPos pos;
        public final float damage;

        public PlaceData(BlockPos pos, float damage) {
            this.pos = pos;
            this.damage = damage;
        }
    }

    /* ========= API GIẢ (CHO MODULE KHÁC) ========= */

    public boolean isEnabled() {
        return false;
    }

    public boolean isActive() {
        return false;
    }

    public float getTargetDamage(PlayerEntity player) {
        return 0f;
    }

    public @Nullable PlaceData getBestData() {
        return null;
    }

    public @Nullable PlaceData getCevData() {
        return null;
    }

    public @Nullable PlaceData getPlaceData(BlockPos pos, PlayerEntity target) {
        return null;
    }
}
