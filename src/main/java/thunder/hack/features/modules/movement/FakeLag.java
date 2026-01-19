package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;

import java.util.concurrent.ConcurrentLinkedQueue;

public class FakeLag extends Module {
    public FakeLag() {
        super("FakeLag", Category.MOVEMENT);
    }

    public final Setting<Integer> minDelay = new Setting<>("MinDelay", 150, 0, 400);
    public final Setting<Integer> maxDelay = new Setting<>("MaxDelay", 200, 0, 400);
    public final Setting<Mode> mode = new Setting<>("Bypass", Mode.Grim);
    public final Setting<Boolean> onlyMoving = new Setting<>("OnlyMoving", true);
    public final Setting<Boolean> randomFactor = new Setting<>("Randomize", true);

    private final Timer timer = new Timer();
    private final ConcurrentLinkedQueue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private int currentDelay = 200;

    public enum Mode {
        Normal, Grim, Vulcan, Matrix
    }

    @Override
    public void onEnable() {
        packets.clear();
        timer.reset();
        updateDelay();
    }

    @Override
    public void onDisable() {
        clearPackets();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;
        Packet<?> packet = event.getPacket();

        if (onlyMoving.getValue() && (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0)) {
             // Continue
        } else if (onlyMoving.getValue()) {
            clearPackets();
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket || packet instanceof PlayerActionC2SPacket || packet instanceof ClientCommandC2SPacket) {
            packets.add(packet);
            event.cancel();
        }
    }

    @Override
    public void onUpdate() {
        if (timer.passedMs(currentDelay)) {
            clearPackets();
            updateDelay();
            timer.reset();
        }
    }

    private void clearPackets() {
        while (!packets.isEmpty()) {
            Packet<?> p = packets.poll();
            if (p != null) mc.getNetworkHandler().sendPacket(p);
        }
    }

    private void updateDelay() {
        currentDelay = randomFactor.getValue() ? (int) MathUtility.random(minDelay.getValue(), maxDelay.getValue()) : maxDelay.getValue();
    }
}
