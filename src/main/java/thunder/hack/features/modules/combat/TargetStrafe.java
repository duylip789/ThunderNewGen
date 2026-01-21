package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class TargetStrafe extends Module {
    // Các Setting đúng theo hình ảnh bạn gửi
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Collision);
    private final Setting<Float> speed = new Setting<>("Speed", 0.08f, 0.0f, 1.0f); // Thanh Speed như ảnh
    private final Setting<Float> distance = new Setting<>("Distance", 2.0f, 0.1f, 5.0f); // Max là 5.0

    public static boolean switchDir = true;
    private static TargetStrafe instance;

    public TargetStrafe() {
        super("TargetStrafe", Category.COMBAT);
        instance = this;
    }

    public static TargetStrafe getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        switchDir = true;
    }

    public boolean canStrafe() {
        if (mc.player.isSneaking() || mc.player.isInLava() || mc.player.isSubmergedInWater()) return false;
        if (ModuleManager.scaffold.isEnabled() || ModuleManager.speed.isEnabled()) return false;
        return Aura.target != null && ModuleManager.aura.isEnabled();
    }

    // Logic xử lý Collision và tránh hố để mượt như ảnh 2
    public boolean needToSwitch(double x, double z) {
        if (mc.player.horizontalCollision) return true;
        
        // Kiểm tra an toàn bề mặt (Avoid Edges)
        BlockPos checkPos = new BlockPos((int)x, (int)mc.player.getY() - 1, (int)z);
        if (mc.world.isAir(checkPos) || mc.world.getBlockState(checkPos).getBlock() == Blocks.LAVA) {
            return true;
        }
        return false;
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (canStrafe()) {
            // Tốc độ di chuyển kết hợp giữa giá trị Speed bạn chỉnh và hiệu ứng thuốc
            double currentSpeed = speed.getValue();
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                currentSpeed *= 1.3; 
            }

            double targetX = Aura.target.getX();
            double targetZ = Aura.target.getZ();
            
            double currentYaw = Math.atan2(mc.player.getZ() - targetZ, mc.player.getX() - targetX);
            double offset = currentSpeed / Math.max(distance.getValue(), mc.player.distanceTo(Aura.target));
            
            double checkYaw = currentYaw + (switchDir ? offset : -offset);
            double nextX = targetX + Math.cos(checkYaw) * distance.getValue();
            double nextZ = targetZ + Math.sin(checkYaw) * distance.getValue();

            // Nếu va chạm hoặc gặp hố (Mode Collision hoạt động)
            if (needToSwitch(nextX, nextZ)) {
                switchDir = !switchDir;
                checkYaw = currentYaw + (switchDir ? offset : -offset);
            }

            double destX = targetX + Math.cos(checkYaw) * distance.getValue();
            double destZ = targetZ + Math.sin(checkYaw) * distance.getValue();

            double diffX = destX - mc.player.getX();
            double diffZ = destZ - mc.player.getZ();
            double dist = Math.hypot(diffX, diffZ);

            if (dist > 0) {
                event.setX((diffX / dist) * currentSpeed);
                event.setZ((diffZ / dist) * currentSpeed);
            }
            event.cancel();
        }
    }

    @EventHandler
    public void updateValues(EventSync e) {
        // Tự động nhảy để Crit mượt khi đang Strafe giống ảnh 2
        if (canStrafe() && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            // Tránh flag khi bị teleport
        }
    }

    private enum Mode {
        Collision
    }
}
