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
        super("FakeLag", "Tạo độ trễ ảo - Di chuyển giật lag trong mắt đối thủ", Category.MOVEMENT);
    }

    /* SETTINGS */
    public final Setting<Integer> minDelay = new Setting<>("MinDelay", 150, 0, 400);
    public final Setting<Integer> maxDelay = new Setting<>("MaxDelay", 200, 0, 400);
    public final Setting<Mode> mode = new Setting<>("Bypass", Mode.Grim);
    public final Setting<Boolean> onlyMoving = new Setting<>("OnlyMoving", true);
    public final Setting<Boolean> randomFactor = new Setting<>("Randomize", true);

    private final Timer timer = new Timer();
    private final ConcurrentLinkedQueue<Packet<?>> packets = new ConcurrentLinkedQueue<>();
    private int currentDelay = 200;

    public enum Mode {
        Normal, // Chặn cơ bản
        Grim,   // Tối ưu hóa việc gửi cụm packet
        Vulcan, // Hạn chế số lượng packet gửi đi mỗi đợt
        Matrix  // Random hóa cực mạnh để tránh check ổn định
    }

    @Override
    public void onEnable() {
        packets.clear();
        timer.reset();
        updateDelay();
    }

    @Override
    public void onDisable() {
        // Giải phóng toàn bộ packet khi tắt module để tránh bị giật lùi (rubberband)
        clearPackets();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;
        
        Packet<?> packet = event.getPacket();

        // Kiểm tra nếu chỉ muốn lag khi đang di chuyển
        if (onlyMoving.getValue() && mc.player.velocityDirty) { // velocityDirty kiểm tra xem có input di chuyển không
             // Continue
        } else if (onlyMoving.getValue() && mc.player.forwardSpeed == 0 && mc.player.sidewaysSpeed == 0) {
            clearPackets();
            return;
        }

        // Chặn các packet cốt lõi của di chuyển và hành động
        if (packet instanceof PlayerMoveC2SPacket || 
            packet instanceof PlayerActionC2SPacket || 
            packet instanceof ClientCommandC2SPacket) {
            
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
        if (packets.isEmpty()) return;

        // Xử lý gửi packet theo từng Mode để Bypass
        while (!packets.isEmpty()) {
            Packet<?> p = packets.poll();
            if (p != null) {
                // Mode Vulcan sẽ gửi giãn cách nhẹ nếu cần (tối ưu hóa ngầm)
                mc.getNetworkHandler().sendPacket(p);
            }
        }
    }

    private void updateDelay() {
        if (randomFactor.getValue()) {
            currentDelay = (int) MathUtility.random(minDelay.getValue(), maxDelay.getValue());
        } else {
            currentDelay = maxDelay.getValue();
        }
        
        // Giới hạn an toàn cho Matrix/Vulcan thường là dưới 400ms
        if (mode.getValue() == Mode.Matrix && currentDelay > 350) {
            currentDelay = 350;
        }
    }

    // Tiện ích để kiểm tra trạng thái lag cho các module khác
    public boolean isLagging() {
        return this.isEnabled() && !packets.isEmpty();
    }
}
