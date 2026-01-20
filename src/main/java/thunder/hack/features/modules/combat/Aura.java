package thunder.hack.features.modules.combat;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import thunder.hack.ThunderHack;
import thunder.hack.core.*;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.*;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.*;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import java.util.*;

public class Aura extends Module {
    public final Setting<Float> range = new Setting<>("Range", 3.1f, 1f, 6f);
    public final Setting<Mode> rotMode = new Setting<>("Rotation", Mode.Track);
    public final Setting<Switch> autoWep = new Setting<>("Switch", Switch.None);
    public final Setting<BooleanSettingGroup> crit = new Setting<>("Crit", new BooleanSettingGroup(true));
    public final Setting<Boolean> shieldBk = new Setting<>("ShieldBreaker", true);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);
    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);

    public static Entity target;
    private float yaw, pitch;
    private int hitTicks;
    private final Timer pTimer = new Timer();

    public Aura() { super("Aura", Category.COMBAT); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (mc.player.isUsingItem() || !pTimer.passedMs(1000)) return;
        target = findTarget();
        if (target == null) return;

        if (canAttack()) {
            shieldBreaker();
            boolean sprint = Core.serverSprint;
            if (sprint) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            
            attack();
            
            if (sprint) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
        hitTicks--;
    }

    private void attack() {
        int slot = autoWep.getValue() == Switch.Silent ? mc.player.getInventory().selectedSlot : -1;
        if (autoWep.getValue() != Switch.None) InventoryUtility.getSwordHotBar().switchTo();
        
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        hitTicks = 10;
        
        if (slot != -1) InventoryUtility.switchTo(slot);
    }

    private boolean canAttack() {
        return hitTicks <= 0 && mc.player.getAttackCooldownProgress(0.5f) >= 0.9f && (mc.player.fallDistance > 0.1 || !crit.getValue().isEnabled());
    }

    private Entity findTarget() {
        return mc.world.getEntities().stream()
            .filter(e -> e instanceof LivingEntity && e != mc.player && e.isAlive() && mc.player.distanceTo(e) <= range.getValue())
            .filter(e -> !(e instanceof PlayerEntity p) || !Managers.FRIEND.isFriend(p))
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e))).orElse(null);
    }

    private void shieldBreaker() {
        if (shieldBk.getValue() && target instanceof PlayerEntity p && p.isBlocking()) {
            int axe = InventoryUtility.getAxe().slot();
            if (axe != -1) {
                InventoryUtility.switchTo(axe);
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack stack) {
        if (clientLook.getValue() && target != null) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    public enum Mode { Track, Grim, None }
    public enum Sort { LowestDistance, LowestHealth, FOV }
    public enum Switch { Normal, Silent, None }
}
