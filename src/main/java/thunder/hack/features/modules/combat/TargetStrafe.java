package thunder.hack.features.modules.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import thunder.hack.ThunderHack;
import thunder.hack.events.impl.EventMove;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.setting.Setting;
import thunder.hack.events.EventHandler; // FIX: Bỏ '.impl' đi là chuẩn bài

public class TargetStrafe extends Module {

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Collision);
    public final Setting<Float> speed = new Setting<>("Speed", 0.23f, 0.1f, 1.0f);
    public final Setting<Float> distance = new Setting<>("Distance", 3.0f, 0.1f, 4.0f);
    public final Setting<Boolean> predict = new Setting<>("Predict", true);
    
    // Setting này để fix lỗi bên TriggerBot và Aura (đang gọi ModuleManager.targetStrafe.jump)
    public final Setting<Boolean> jump = new Setting<>("Jump", true);

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
        // Kiểm tra an toàn trước khi lấy Aura
        if (fullNullCheck()) return;

        Aura aura = ThunderHack.MODULE_MANAGER.getModuleByClass(Aura.class);
        if (aura == null) return;

        // Truy cập trực tiếp vào biến target của Aura
        target = aura.target;
        
        if (target == null) return;

        // Tự động nhảy khi ở dưới đất
        if (jump.getValue() && mc.player.isOnGround()) {
            mc.player.jump();
        }

        // Đổi hướng xoay khi gặp vật cản (vách núi, tường...)
        if (mc.player.horizontalCollision) {
            direction *= -1;
        }
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (target == null || fullNullCheck()) return;

        float yaw = getRotationToTarget(target);

        if (mode.getValue() == Mode.Collision) {
            // THUẬT TOÁN COLLISION (Bypass SMP/BOX) - Quỹ đạo tròn mềm mại
            double rad = Math.toRadians(yaw + (90 * direction));
            double diffX = target.getX() - mc.player.getX();
            double diffZ = target.getZ() - mc.player.getZ();
            double currentDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
            
            // Điều chỉnh góc để bám sát khoảng cách 'Distance'
            double adjust = (currentDist > distance.getValue()) ? 0.45 : (currentDist < distance.getValue() - 0.2) ? -0.45 : 0;
            
            event.setX(Math.cos(rad + adjust) * speed.getValue());
            event.setZ(Math.sin(rad + adjust) * speed.getValue());
        } else {
            // THUẬT TOÁN PLUS (Bypass Practice) - Vector kéo nam châm cực mạnh
            double diffX = target.getX() - mc.player.getX();
            double diffZ = target.getZ() - mc.player.getZ();
            double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

            if (dist == 0) return; // Tránh lỗi chia cho 0

            // Công thức vector từ Exosware giúp bypass chuyển động
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
            // Predict vị trí dựa trên vận tốc hiện tại của đối thủ
            x += (target.getX() - target.prevX) * 2.5;
            z += (target.getZ() - target.prevZ) * 2.5;
        }
        
        return (float) (MathHelper.atan2(z, x) * (180 / Math.PI) - 90);
    }
}
