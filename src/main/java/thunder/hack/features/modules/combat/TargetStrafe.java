package thunder.hack.features.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.MathUtil;

@Mixin // Lưu ý: Bro nhớ đăng ký module này trong ModuleManager
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
        // Tìm mục tiêu từ Aura
        target = ThunderHack.moduleManager.getModuleByClass(Aura.class).getTarget();
        
        if (fullNullCheck() || target == null) return;

        // Tự động nhảy khi ở gần mục tiêu
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }

        // Đổi hướng nếu va chạm tường
        if (mc.player.horizontalCollision) {
            direction *= -1;
        }
    }

    @SubscribeEvent
    public void onMove(EventMove event) {
        if (target == null) return;

        // Thuật toán tính toán Yaw cần thiết để xoay quanh mục tiêu
        float yaw = getRotationToTarget(target);

        if (mode.getValue() == Mode.Collision) {
            // THUẬT TOÁN COLLISION (Bypass SMP/Box)
            // Sử dụng tính toán quỹ đạo lướt để tránh bị phát hiện "Force Motion"
            doStrafe(event, speed.getValue(), yaw);
        } else {
            // THUẬT TOÁN PLUS (Bypass Practice)
            // Ép vector vận tốc trực tiếp dựa trên lực kéo tâm
            double diffX = target.getX() - mc.player.getX();
            double diffZ = target.getZ() - mc.player.getZ();
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            double motionX = (diffX / dist) * (dist - distance.getValue()) + (diffZ / dist) * speed.getValue() * direction;
            double motionZ = (diffZ / dist) * (dist - distance.getValue()) - (diffX / dist) * speed.getValue() * direction;

            event.setX(motionX);
            event.setZ(motionZ);
        }
    }

    private void doStrafe(EventMove event, float moveSpeed, float yaw) {
        // Thuật toán lượng giác để tính toán vị trí di chuyển tối ưu
        double rad = Math.toRadians(yaw + (90 * direction));
        
        // Điều chỉnh dần khoảng cách để tạo đường cong mềm mại (Tránh bị check Heuristic)
        double diffX = target.getX() - mc.player.getX();
        double diffZ = target.getZ() - mc.player.getZ();
        double currentDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        
        double dir = (currentDist > distance.getValue()) ? 0.5 : (currentDist < distance.getValue() - 0.2) ? -0.5 : 0;
        
        double x = Math.cos(rad + dir) * moveSpeed;
        double z = Math.sin(rad + dir) * moveSpeed;

        event.setX(x);
        event.setZ(z);
    }

    private float getRotationToTarget(LivingEntity target) {
        double x = target.getX() - mc.player.getX();
        double z = target.getZ() - mc.player.getZ();
        
        // Prediction logic (Dự đoán vị trí tiếp theo của đối thủ)
        if (predict.getValue()) {
            x += target.getX() - target.prevX;
            z += target.getZ() - target.prevZ;
        }
        
        return (float) (MathHelper.atan2(z, x) * (180 / Math.PI) - 90);
    }
}
