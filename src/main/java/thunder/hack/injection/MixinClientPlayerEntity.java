package thunder.hack.injection;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;

import static thunder.hack.features.modules.Module.fullNullCheck;
import static thunder.hack.features.modules.Module.mc;

@Mixin(value = ClientPlayerEntity.class, priority = 800)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    @Unique
    boolean pre_sprint_state = false;
    @Unique
    private boolean updateLock = false;
    @Unique
    private Runnable postAction;

    @Shadow
    public abstract float getPitch(float tickDelta);
    @Shadow
    protected abstract void sendMovementPackets();

    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHook(CallbackInfo info) {
        if(Module.fullNullCheck()) return;
        ThunderHack.EVENT_BUS.post(new PlayerUpdateEvent());
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
    private boolean tickMovementHook(ClientPlayerEntity player) {
        if (ModuleManager.noSlow.isEnabled() && ModuleManager.noSlow.canNoSlow())
            return false;
        return player.isUsingItem();
    }

    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    public void shouldSlowDownHook(CallbackInfoReturnable<Boolean> cir) {
        if(ModuleManager.noSlow.isEnabled()) {
            if (isCrawling()) {
                if (ModuleManager.noSlow.crawl.getValue())
                    cir.setReturnValue(false);
            } else {
                if (ModuleManager.noSlow.sneak.getValue())
                    cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if(Module.fullNullCheck()) return;
        EventMove event = new EventMove(movement.x, movement.y, movement.z);
        ThunderHack.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sendMovementPacketsHook(CallbackInfo info) {
        if (fullNullCheck()) return;

        // FIX: Đảm bảo lấy giá trị chuẩn từ Player để tránh bị khóa 0,0
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        EventSync event = new EventSync(currentYaw, currentPitch);
        ThunderHack.EVENT_BUS.post(event);
        
        // Cập nhật lại Yaw/Pitch sau khi Event xử lý (nếu có rotation từ Aura/Scaffold)
        // Nếu không có module nào can thiệp, nó sẽ giữ nguyên yaw/pitch của chuột
        postAction = event.getPostAction();

        EventSprint e = new EventSprint(isSprinting());
        ThunderHack.EVENT_BUS.post(e);
        ThunderHack.EVENT_BUS.post(new EventAfterRotate());

        if (e.getSprintState() != mc.player.lastSprinting) {
            if (e.getSprintState())
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this, ClientCommandC2SPacket.Mode.START_SPRINTING));
            else
                mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(this, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

            mc.player.lastSprinting = e.getSprintState();
        }
        pre_sprint_state = mc.player.lastSprinting;
        Core.lockSprint = true;

        // Cẩn thận: Nếu Event bị cancel mà không rõ lý do, nó sẽ khóa xoay. 
        // Mình thêm check để tránh cancel bậy.
        if (event.isCancelled() && (event.getYaw() != currentYaw || event.getPitch() != currentPitch)) {
            info.cancel();
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"), cancellable = true)
    private void sendMovementPacketsPostHook(CallbackInfo info) {
        if (fullNullCheck()) return;
        mc.player.lastSprinting = pre_sprint_state;
        Core.lockSprint = false;
        EventPostSync event = new EventPostSync();
        ThunderHack.EVENT_BUS.post(event);
        if(postAction != null) {
            postAction.run();
            postAction = null;
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true)
    private void PostUpdateHook(CallbackInfo info) {
        if(Module.fullNullCheck()) return;
        if (updateLock) return;

        PostPlayerUpdateEvent playerUpdateEvent = new PostPlayerUpdateEvent();
        ThunderHack.EVENT_BUS.post(playerUpdateEvent);
        
        if (playerUpdateEvent.isCancelled()) {
            // Giới hạn Iterations để tránh treo máy/khóa chuột
            int iterations = Math.min(playerUpdateEvent.getIterations(), 10);
            if (iterations > 0) {
                for (int i = 0; i < iterations; i++) {
                    updateLock = true;
                    tick();
                    updateLock = false;
                    sendMovementPackets();
                }
            }
            info.cancel();
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
        if (ModuleManager.noPush.isEnabled() && ModuleManager.noPush.blocks.getValue()) {
            info.cancel();
        }
    }
}
