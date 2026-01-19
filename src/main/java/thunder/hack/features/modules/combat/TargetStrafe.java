package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class TargetStrafe extends Module {

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Crimson);
    public final Setting<Float> distance = new Setting<>("Distance", 2.4F, 0.5F, 5.0f);
    public final Setting<Float> speed = new Setting<>("Speed", 0.28F, 0.1F, 1.0f);
    public final Setting<Boolean> jump = new Setting<>("AutoJump", true);
    public final Setting<Boolean> smart = new Setting<>("SmartSwitch", true);

    private boolean direction = true;
    private double currentSpeed;

    public TargetStrafe() {
        super("TargetStrafe", "Xoay quanh mục tiêu né đòn", Category.COMBAT);
    }

    public enum Mode {
        Crimson, // Bypass mạnh nhất cho các server AC gắt
        Prime    // Tối ưu cho SMP, đánh dưới nước và trên đất
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (Aura.target == null || !ModuleManager.aura.isEnabled()) {
            currentSpeed = 0;
            return;
        }

        if (!canStrafe()) return;

        // Tự động nhảy để strafe mượt hơn nếu bật AutoJump
        if (jump.getValue() && mc.player.isOnGround() && MovementUtility.isMoving()) {
            mc.player.jump();
            event.setY(mc.player.getVelocity().y);
        }

        // Tính toán tốc độ dựa trên chế độ
        double baseSpeed = getBaseSpeed();
        
        // Kiểm tra vật cản để đổi hướng (Smart Switch)
        if (smart.getValue()) {
            if (mc.player.horizontalCollision || checkObstacle()) {
                direction = !direction;
            }
        }

        // Tính toán vector di chuyển xoay tròn
        double targetPosDir = Math.atan2(mc.player.getZ() - Aura.target.getZ(), mc.player.getX() - Aura.target.getX());
        
        // Điều chỉnh khoảng cách để không bị flag (Distance Control)
        double curDist = Math.sqrt(mc.player.squaredDistanceTo(Aura.target));
        double diff = curDist - distance.getValue();
        
        // Crimson Mode: Sử dụng công thức mượt hơn để tránh Flag
        // Prime Mode: Giữ khoảng cách cứng hơn để combo
        double cosModifier = (mode.getValue() == Mode.Crimson) ? 0.05 : 0.01;
        
        targetPosDir += (direction ? baseSpeed : -baseSpeed) / Math.max(curDist, 0.2);

        double posX = Aura.target.getX() + Math.cos(targetPosDir) * distance.getValue();
        double posZ = Aura.target.getZ() + Math.sin(targetPosDir) * distance.getValue();
        
        double diffX = posX - mc.player.getX();
        double diffZ = posZ - mc.player.getZ();

        // Áp dụng tốc độ thực tế
        event.setX(diffX * baseSpeed * 2.5);
        event.setZ(diffZ * baseSpeed * 2.5);
        
        // Xử lý khi ở dưới nước (Prime Mode hỗ trợ mạnh)
        if (mc.player.isSubmergedInWater() || mc.player.isInLava()) {
            if (mode.getValue() == Mode.Prime) {
                event.setY(mc.options.jumpKey.isPressed() ? 0.04 : (mc.options.sneakKey.isPressed() ? -0.04 : 0));
            }
        }
        
        event.cancel();
    }

    private double getBaseSpeed() {
        double res = speed.getValue();
        // Giảm tốc độ nhẹ khi dùng Crimson để tránh bị flag speed
        if (mode.getValue() == Mode.Crimson) {
            res *= 0.96;
            if (!mc.player.isOnGround()) res *= 0.98;
        }
        
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amp = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1;
            res *= (1.0 + 0.2 * amp);
        }
        return res;
    }

    private boolean canStrafe() {
        if (mc.player.isSneaking() || ModuleManager.scaffold.isEnabled()) return false;
        if (mc.player.getAbilities().flying) return false;
        return true;
    }

    private boolean checkObstacle() {
        // Kiểm tra xem phía trước có vực thẳm hoặc tường không
        double x = mc.player.getX() + mc.player.getVelocity().x * 5;
        double z = mc.player.getZ() + mc.player.getVelocity().z * 5;
        BlockPos bp = new BlockPos((int)x, (int)mc.player.getY() - 1, (int)z);
        
        if (mc.world.getBlockState(bp).isAir()) return true; // Tránh rơi vực
        if (mc.world.getBlockState(bp.up(1)).isFullCube(mc.world, bp.up(1))) return true; // Tránh đâm tường
        
        return false;
    }

    @EventHandler
    public void onSync(EventSync e) {
        // Giữ xoay nhìn vào mục tiêu để Aura hoạt động tốt hơn
        if (Aura.target != null && ModuleManager.aura.isEnabled() && canStrafe()) {
            float[] rotations = calculateRotations(Aura.target.getX(), Aura.target.getY() + Aura.target.getEyeHeight(Aura.target.getPose()), Aura.target.getZ());
            // Chỉ đồng bộ rotations nếu cần thiết (tùy thuộc vào Aura mode)
        }
    }

    private float[] calculateRotations(double x, double y, double z) {
        double diffX = x - mc.player.getX();
        double diffY = y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = z - mc.player.getZ();
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }
}
