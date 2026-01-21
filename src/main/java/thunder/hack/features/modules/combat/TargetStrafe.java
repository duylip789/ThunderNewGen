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
    public Setting<Boolean> jump = new Setting<>("Jump", true);
    public Setting<Boolean> smartCrit = new Setting<>("SmartCrit", true);
    public Setting<Float> distance = new Setting<>("Distance", 1.8F, 0.2F, 7f);
    public Setting<Boolean> avoidEdges = new Setting<>("AvoidEdges", true);

    public static double oldSpeed;
    public static boolean switchDir;
    public static int waterTicks;

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
        oldSpeed = 0;
        switchDir = true;
    }

    public boolean canStrafe() {
        if (mc.player.isSneaking() || mc.player.isInLava() || mc.player.isSubmergedInWater()) return false;
        if (ModuleManager.scaffold.isEnabled() || ModuleManager.speed.isEnabled()) return false;
        if (mc.player.getAbilities().flying) return false;
        return Aura.target != null && ModuleManager.aura.isEnabled();
    }

    // Hàm kiểm tra an toàn thay thế cho MovementUtility bị lỗi
    private boolean isSafeToWalk(BlockPos pos) {
        return !mc.world.isAir(pos) && 
               mc.world.getBlockState(pos).getBlock() != Blocks.LAVA && 
               mc.world.getBlockState(pos).getBlock() != Blocks.FIRE;
    }

    public boolean needToSwitch(double x, double z) {
        // Nếu va chạm tường -> Đổi hướng
        if (mc.player.horizontalCollision) return true;
        
        // Nếu bật AvoidEdges -> Check xem phía trước có phải hố không
        if (avoidEdges.getValue()) {
            BlockPos checkPos = new BlockPos((int)x, (int)mc.player.getY() - 1, (int)z);
            if (!isSafeToWalk(checkPos)) {
                return true; // Phía trước là hố/lava -> Đổi hướng
            }
        }
        return false;
    }

    public double calculateSpeed() {
        double baseSpeed = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        // Giảm tốc nhẹ khi trên không để mượt hơn (giống các client xịn)
        if (!mc.player.isOnGround()) {
             baseSpeed *= 0.98; 
        }
        return baseSpeed;
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (canStrafe()) {
            double speed = calculateSpeed();
            
            double targetX = Aura.target.getX();
            double targetZ = Aura.target.getZ();
            
            // Tính góc hiện tại so với Target
            double currentYaw = Math.atan2(mc.player.getZ() - targetZ, mc.player.getX() - targetX);
            
            // Tốc độ xoay vòng
            double offset = speed / Math.max(distance.getValue(), mc.player.distanceTo(Aura.target));
            
            // Tính toán vị trí tiếp theo
            double checkYaw = currentYaw + (switchDir ? offset : -offset);
            double nextX = targetX + Math.cos(checkYaw) * distance.getValue();
            double nextZ = targetZ + Math.sin(checkYaw) * distance.getValue();

            // Kiểm tra va chạm hoặc hố sâu
            if (needToSwitch(nextX, nextZ)) {
                switchDir = !switchDir; // Đảo chiều chạy
                checkYaw = currentYaw + (switchDir ? offset : -offset); // Tính lại hướng mới
            }

            // Gán vector di chuyển mới (chạy vòng tròn)
            double finalYaw = checkYaw;
            double destX = targetX + Math.cos(finalYaw) * distance.getValue();
            double destZ = targetZ + Math.sin(finalYaw) * distance.getValue();

            double diffX = destX - mc.player.getX();
            double diffZ = destZ - mc.player.getZ();
            
            // Chuẩn hóa vector
            double dist = Math.hypot(diffX, diffZ);
            if (dist > 0) {
                event.setX((diffX / dist) * speed);
                event.setZ((diffZ / dist) * speed);
            }
            event.cancel();
        } else {
            oldSpeed = 0;
        }
    }

    @EventHandler
    public void updateValues(EventSync e) {
        if (canStrafe()) {
            if (mc.player.isOnGround()) {
                if (jump.getValue()) {
                    mc.player.jump();
                } else if (smartCrit.getValue() && Aura.target != null) {
                    // Nhảy thấp 0.41 để Crit nhanh mà không bị giật (Motion Y)
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.41, mc.player.getVelocity().z);
                    e.cancel(); // Ngăn packet thừa
                }
            }
        }

        if (mc.player.isSubmergedInWater()) {
            waterTicks = 10;
        } else {
            waterTicks--;
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            oldSpeed = 0; // Reset khi bị lagback
        }
    }
}
