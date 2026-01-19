package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
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

    public TargetStrafe() {
        super("TargetStrafe", Category.COMBAT);
    }

    public enum Mode {
        Crimson, Prime
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (Aura.target == null || !ModuleManager.aura.isEnabled()) {
            return;
        }

        if (!canStrafe()) return;

        if (jump.getValue() && mc.player.isOnGround() && MovementUtility.isMoving()) {
            mc.player.jump();
            event.setY(mc.player.getVelocity().y);
        }

        double baseSpeed = getBaseSpeed();
        
        if (smart.getValue()) {
            if (mc.player.horizontalCollision || checkObstacle()) {
                direction = !direction;
            }
        }

        double targetPosDir = Math.atan2(mc.player.getZ() - Aura.target.getZ(), mc.player.getX() - Aura.target.getX());
        double curDist = Math.sqrt(mc.player.squaredDistanceTo(Aura.target));
        
        targetPosDir += (direction ? baseSpeed : -baseSpeed) / Math.max(curDist, 0.2);

        double posX = Aura.target.getX() + Math.cos(targetPosDir) * distance.getValue();
        double posZ = Aura.target.getZ() + Math.sin(targetPosDir) * distance.getValue();
        
        double diffX = posX - mc.player.getX();
        double diffZ = posZ - mc.player.getZ();

        event.setX(diffX * baseSpeed * 2.5);
        event.setZ(diffZ * baseSpeed * 2.5);
        
        if (mc.player.isSubmergedInWater() || mc.player.isInLava()) {
            if (mode.getValue() == Mode.Prime) {
                event.setY(mc.options.jumpKey.isPressed() ? 0.04 : (mc.options.sneakKey.isPressed() ? -0.04 : 0));
            }
        }
        
        event.cancel();
    }

    private double getBaseSpeed() {
        double res = speed.getValue();
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
        double x = mc.player.getX() + mc.player.getVelocity().x * 5;
        double z = mc.player.getZ() + mc.player.getVelocity().z * 5;
        BlockPos bp = new BlockPos((int)x, (int)mc.player.getY() - 1, (int)z);
        if (mc.world.getBlockState(bp).isAir()) return true;
        if (mc.world.getBlockState(bp.up(1)).isFullCube(mc.world, bp.up(1))) return true;
        return false;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (Aura.target != null && ModuleManager.aura.isEnabled() && canStrafe()) {
             // Logic xoay mặt nếu cần
        }
    }
}
