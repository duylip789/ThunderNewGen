package thunder.hack.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.ModuleCategory;
import thunder.hack.features.setting.Setting;

public class AutoCrystal extends Module {

    /* ================= CORE SHARED ================= */

    public static PlayerEntity target = null;
    public float renderDamage = Float.NaN;

    /* ================= SETTINGS (DUMMY) ================= */

    public final Setting<Float> maxSelfDamage =
            new Setting<>("MaxSelfDamage", 6.0f, 0.0f, 20.0f);

    public final Setting<Boolean> ignoreTerrain =
            new Setting<>("IgnoreTerrain", false);

    public final Setting<Boolean> assumeBestArmor =
            new Setting<>("AssumeBestArmor", false);

    public final Setting<Boolean> breakFailsafe =
            new Setting<>("BreakFailsafe", false);

    public final Setting<Boolean> placeFailsafe =
            new Setting<>("PlaceFailsafe", false);

    public final Setting<Integer> attempts =
            new Setting<>("Attempts", 1, 1, 10);

    /* ================= CONSTRUCTOR ================= */

    public AutoCrystal() {
        super("AutoCrystal", ModuleCategory.COMBAT);
        setEnabled(false);   // ❌ không bật
        setDrawn(false);    // ❌ không hiện trong ClickGUI
    }

    /* ================= METHODS USED BY OTHER MODULES ================= */

    public void pause() {
        // no-op
    }

    public boolean isPositionBlockedByCrystal(BlockPos pos) {
        return false;
    }

    @Nullable
    public PlaceData getPlaceData(BlockPos pos, PlayerEntity target, Vec3d from) {
        return null;
    }

    @Nullable
    public Object getInteractResult(BlockPos pos, Vec3d hit) {
        return null;
    }

    public void placeCrystal(BlockPos pos, boolean rotate, boolean swing) {
        // no-op
    }

    /* ================= INNER DATA CLASS ================= */

    public static class PlaceData {
        public BlockPos bhr() {
            return null;
        }
    }
}
