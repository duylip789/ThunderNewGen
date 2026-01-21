package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import thunder.hack.core.Core;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.injection.accesors.ISPacketEntityVelocity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;

import static thunder.hack.utility.player.MovementUtility.isMoving;

public class TargetStrafe extends Module {
    public Setting<Boolean> jump = new Setting<>("Jump", true);
    public Setting<Boolean> smartCrit = new Setting<>("SmartCrit", true); // Giúp crit mượt không bị giật
    public Setting<Float> distance = new Setting<>("Distance", 1.3F, 0.2F, 7f);
    public Setting<Boolean> avoidEdges = new Setting<>("AvoidEdges", true); // Tránh flag khi sát rìa block

    private final Setting<Boost> boost = new Setting<>("Boost", Boost.None);
    public Setting<Float> setSpeed = new Setting<>("speed", 1.3F, 0.0F, 2f, v -> boost.getValue() == Boost.Elytra);
    private final Setting<Float> velReduction = new Setting<>("Reduction", 6.0f, 0.1f, 10f, v -> boost.getValue() == Boost.Damage);
    private final Setting<Float> maxVelocitySpeed = new Setting<>("MaxVelocity", 0.8f, 0.1f, 2f, v -> boost.getValue() == Boost.Damage);

    public static double oldSpeed, contextFriction, fovval;
    public static boolean needSwap, needSprintState, skip, switchDir, disabled;
    public static int noSlowTicks, jumpTicks, waterTicks;
    static long disableTime;
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
        fovval = mc.options.getFovEffectScale().getValue();
        mc.options.getFovEffectScale().setValue(0d);
        skip = true;
    }

    public boolean canStrafe() {
        if (mc.player.isSneaking() || mc.player.isInLava() || mc.player.isSubmergedInWater()) return false;
        if (ModuleManager.scaffold.isEnabled() || ModuleManager.speed.isEnabled()) return false;
        return Aura.target != null && ModuleManager.aura.isEnabled() && !mc.player.getAbilities().flying;
    }

    // Tối ưu hóa việc né tránh vật cản và hố
    public boolean needToSwitch(double x, double z) {
        if (mc.player.horizontalCollision || (avoidEdges.getValue() && MovementUtility.isPositionPlaceable(new BlockPos((int)x, (int)mc.player.getY() - 1, (int)z), false))) {
            return true;
        }
        BlockPos pos = new BlockPos((int)x, (int)mc.player.getY() - 1, (int)z);
        if (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getBlock() == Blocks.LAVA || mc.world.getBlockState(pos).getBlock() == Blocks.FIRE) {
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        mc.options.getFovEffectScale().setValue(fovval);
    }

    // Tính toán tốc độ mượt mà hơn để tránh bị giật (Lagback)
    public double calculateSpeed(EventMove move) {
        float friction = 0.91f;
        if (mc.player.isOnGround()) {
            friction = mc.world.getBlockState(new BlockPos((int) mc.player.getX(), (int) (mc.player.getY() - 1.0), (int) mc.player.getZ())).getBlock().getSlipperiness() * 0.91f;
        }

        double baseSpeed = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            baseSpeed *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        }

        if (mc.player.isOnGround()) {
            oldSpeed = baseSpeed;
        } else {
            oldSpeed -= 0.013; // Giảm tốc dần khi trên không để mượt hơn
        }
        
        return Math.max(oldSpeed, baseSpeed);
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (canStrafe()) {
            double speed = calculateSpeed(event);
            
            // Tính toán góc xoay quanh mục tiêu
            double targetX = Aura.target.getX();
            double targetZ = Aura.target.getZ();
            
            double currentYaw = Math.atan2(mc.player.getZ() - targetZ, mc.player.getX() - targetX);
            double offset = speed / Math.max(distance.getValue(), mc.player.distanceTo(Aura.target));
            
            currentYaw += switchDir ? offset : -offset;

            double nextX = targetX + Math.cos(currentYaw) * distance.getValue();
            double nextZ = targetZ + Math.sin(currentYaw) * distance.getValue();

            // Check né tránh
            if (needToSwitch(nextX, nextZ)) {
                switchDir = !switchDir;
                currentYaw += (switchDir ? offset : -offset) * 2;
                nextX = targetX + Math.cos(currentYaw) * distance.getValue();
                nextZ = targetZ + Math.sin(currentYaw) * distance.getValue();
            }

            double diffX = nextX - mc.player.getX();
            double diffZ = nextZ - mc.player.getZ();
            double distanceToPoint = Math.hypot(diffX, diffZ);

            event.setX((diffX / distanceToPoint) * speed);
            event.setZ((diffZ / distanceToPoint) * speed);
            event.cancel();
        }
    }

    @EventHandler
    public void updateValues(EventSync e) {
        if (canStrafe()) {
            // Logic Jump/Crit mượt
            if (mc.player.isOnGround()) {
                if (jump.getValue()) {
                    mc.player.jump();
                } else if (smartCrit.getValue() && Aura.target != null) {
                    // Nhảy thấp để crit mà không bị khựng tốc độ
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.41, mc.player.getVelocity().z);
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
            oldSpeed = 0; // Reset tốc độ khi bị teleport (tránh flag)
        }
    }

    private enum Boost {
        None, Elytra, Damage
    }
                }
