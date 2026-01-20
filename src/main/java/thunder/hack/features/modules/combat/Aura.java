package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack; // Thêm lại import này
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public class Aura extends Module {
    public final Setting<Float> range = new Setting<>("Range", 3.1f, 1f, 6f);
    public static Entity target;
    private int hitTicks;

    public Aura() { super("Aura", Category.COMBAT); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        target = mc.world.getEntities().stream()
            .filter(ent -> ent instanceof LivingEntity && ent != mc.player && ent.isAlive() && mc.player.distanceTo(ent) <= range.getValue())
            .filter(ent -> !(ent instanceof PlayerEntity p) || !Managers.FRIEND.isFriend(p))
            .min(Comparator.comparingDouble(ent -> mc.player.squaredDistanceTo(ent))).orElse(null);

        if (target != null && mc.player.getAttackCooldownProgress(0.5f) >= 0.9f && hitTicks-- <= 0) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            hitTicks = 10;
        }
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        // Để trống vì đã chuyển sang TargetESP
    }

    // --- CÁC ENUM PHẢI GIỮ LẠI ĐỂ KHÔNG LỖI BUILD CÁC FILE KHÁC ---
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2 } // Giữ để AutoCrystal không lỗi
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
    public enum WallsBypass { Off, V1, V2 }

    public static class Position {
        public double x, y, z;
        public int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    }
}
