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
    // Để public để Aura và TriggerBot không bị lỗi
    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Collision);
    public final Setting<Float> speed = new Setting<>("Speed", 0.08f, 0.0f, 1.0f);
    public final Setting<Float> distance = new Setting<>("Distance", 2.0f, 0.1f, 5.0f);
    public final Setting<Boolean> jump = new Setting<>("Jump", true); 

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

    public boolean needToSwitch(double x, double z) {
        if (mc.player.horizontalCollision) return true;
        BlockPos checkPos = new BlockPos((int)x, (int)mc.player.getY() - 1, (int)z);
        if (mc.world.isAir(checkPos) || mc.world.getBlockState(checkPos).getBlock() == Blocks.LAVA) {
            return true;
        }
        return false;
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (canStrafe()) {
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
        if (canStrafe() && mc.player.isOnGround() && jump.getValue()) {
            mc.player.jump();
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            // Reset logic if needed
        }
    }

    private enum Mode {
        Collision
    }
}

