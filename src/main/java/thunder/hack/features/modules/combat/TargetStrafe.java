package thunder.hack.features.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.events.impl.EventHandler; // Dùng cái này thay cho SubscribeEvent

public class TargetStrafe extends Module {

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Collision);
    private final Setting<Float> speed = new Setting<>("Speed", 0.23f, 0.1f, 1.0f);
    private final Setting<Float> distance = new Setting<>("Distance", 3.0f, 0.1f, 4.0f);
    private final Setting<Boolean> predict = new Setting<>("Predict", true);

    private LivingEntity target = null;
    private int direction = 1;

    public TargetStrafe() {
        super("TargetStrafe", Category.COMBAT);
    }

    public enum Mode {
        Collision, Plus
    }

    @Override
    public void onUpdate() {
        // Lấy target từ Aura của ThunderHack
        target = ThunderHack.moduleManager.getModuleByClass(Aura.class).getTarget();
        
        if (fullNullCheck() || target == null) return;

        // Tự động nhảy (Tăng tốc độ cho strafe)
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }

        // Đổi hướng khi đập mặt vào tường
        if (mc.player.horizontalCollision) {
            direction *= -1;
        }
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (target == null || fullNullCheck()) return;

        float yaw = getRotationToTarget(target);

        if (mode.getValue() == Mode.Collision) {
            // THUẬT TOÁN COLLISION (SMP/BOX BYPASS)
            // Tính toán hướng lượn dựa trên vòng cung lượng giác
            double rad = Math.toRadians(yaw + (90 * direction));
            
            double diffX = target.getX() - mc.player.getX();
            double diffZ = target.getZ() - mc.player.getZ();
            double currentDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
            
            // Điều chỉnh góc để luôn giữ khoảng cách Distance
            double adjust = (currentDist > distance.getValue()) ? 0.4 : (currentDist < distance.getValue() - 0.3) ? -0.4 : 0;
            
            event.setX(Math.cos(rad + adjust) * speed.getValue());
            event.setZ(Math.sin(rad + adjust) * speed.getValue());
        } else {
            // THUẬT TOÁN PLUS (PRACTICE BYPASS)
            // Sử dụng Vector kéo tâm nam châm (Magnet Pull)
            double diffX = target.getX() - mc.player.getX();
            double diffZ = target.getZ() - mc.player.getZ();
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            // Công thức Vector lướt của Exosware
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
            // Dự đoán vị trí dựa trên chuyển động thực tế
            x += (target.getX() - target.prevX) * 2.0;
            z += (target.getZ() - target.prevZ) * 2.0;
        }
        
        return (float) (MathHelper.atan2(z, x) * (180 / Math.PI) - 90);
    }
}
