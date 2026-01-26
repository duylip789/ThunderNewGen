package thunder.hack.features.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import meteordevelopment.orbit.EventHandler; // Đây là đường dẫn chuẩn cho hầu hết bản Recode

public class TargetStrafe extends Module {

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Collision);
    private final Setting<Float> speed = new Setting<>("Speed", 0.23f, 0.1f, 1.0f);
    private final Setting<Float> distance = new Setting<>("Distance", 3.0f, 0.1f, 4.0f);
    private final Setting<Boolean> predict = new Setting<>("Predict", true);

    private LivingEntity target = null;
    private int direction = 1;

    public TargetStrafe() {
        super("TargetStrafe", "Xoay quanh mục tiêu để bypass", Category.COMBAT);
    }

    public enum Mode {
        Collision, Plus
    }

    @Override
    public void onUpdate() {
        // Tìm target từ module Aura
        Aura aura = ThunderHack.moduleManager.getModuleByClass(Aura.class);
        target = aura.getTarget();
        
        if (fullNullCheck() || target == null) return;

        // Tự động nhảy để duy trì vận tốc (AirStrafe logic)
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }

        // Đổi hướng xoay khi gặp vật cản
        if (mc.player.horizontalCollision) {
            direction *= -1;
        }
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (target == null || fullNullCheck()) return;

        // Tính toán góc xoay dựa trên vị trí mục tiêu
        float yaw = getRotationToTarget(target);

        if (mode.getValue() == Mode.Collision) {
            // THUẬT TOÁN COLLISION - Bypass các server như SMP, Box
            // Sử dụng chuyển động xoáy vòng cung mượt
            double rad = Math.toRadians(yaw + (90 * direction));
            
            double diffX = target.getX() - mc.player.getX();
            double diffZ = target.getZ() - mc.player.getZ();
            double currentDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
            
            // Điều chỉnh vector hướng để duy trì khoảng cách Distance đã cài đặt
            double adjust = (currentDist > distance.getValue()) ? 0.45 : (currentDist < distance.getValue() - 0.2) ? -0.45 : 0;
            
            event.setX(Math.cos(rad + adjust) * speed.getValue());
            event.setZ(Math.sin(rad + adjust) * speed.getValue());
        } else {
            // THUẬT TOÁN PLUS - Bypass Practice (Sử dụng Magnet Vector)
            double diffX = target.getX() - mc.player.getX();
            double diffZ = target.getZ() - mc.player.getZ();
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            // Vector toán học ép quỹ đạo tròn hoàn hảo (Exosware logic)
            double motionX = (diffX / dist) * (dist - distance.getValue()) + (diffZ / dist) * speed.getValue() * direction;
            double motionZ = (diffZ / dist) * (dist - distance.getValue()) - (diffX / dist) * speed.getValue() * direction;

            event.setX(motionX);
            event.setZ(motionZ);
        }
    }

    private float getRotationToTarget(LivingEntity target) {
        double x = target.getX() - mc.player.getX();
        double z = target.getZ() - mc.player.getZ();
        
        if (predict.getValue()) {
            // Predict vị trí dựa trên DeltaMovement để tránh bị trễ packet
            x += (target.getX() - target.prevX) * 2.5;
            z += (target.getZ() - target.prevZ) * 2.5;
        }
        
        return (float) (MathHelper.atan2(z, x) * (180 / Math.PI) - 90);
    }
}
