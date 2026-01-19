package thunder.hack.features.modules.client;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import java.util.HashMap;
import java.util.Map;

public class FixHP extends Module {
    public FixHP() {
        super("FixHP", Category.CLIENT);
    }

    public final Setting<Boolean> packetSync = new Setting<>("PacketSync", true);
    public final Setting<Boolean> rapidUpdate = new Setting<>("RapidUpdate", true);

    private final Map<Integer, Float> realHealthMap = new HashMap<>();

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.world == null) return;

        if (event.getPacket() instanceof EntityTrackerUpdateS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.id());
            if (entity instanceof LivingEntity living) {
                packet.trackedValues().forEach(entry -> {
                    if (entry.id() == 9 && packetSync.getValue()) {
                        float serverHealth = (float) entry.value();
                        living.setHealth(serverHealth);
                    }
                });
            }
        }

        if (event.getPacket() instanceof HealthUpdateS2CPacket packet) {
            if (rapidUpdate.getValue()) {
                mc.player.setHealth(packet.getHealth());
            }
        }

        if (event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 2 || packet.getStatus() == 3) {
                Entity ent = packet.getEntity(mc.world);
                if (ent instanceof LivingEntity living) {
                    realHealthMap.put(living.getId(), living.getHealth());
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        if (mc.world != null && rapidUpdate.getValue()) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof LivingEntity living && realHealthMap.containsKey(e.getId())) {
                    if (Math.abs(living.getHealth() - realHealthMap.get(e.getId())) > 0.5f) {
                        living.setHealth(realHealthMap.get(e.getId()));
                    }
                }
            }
        }
    }
}
