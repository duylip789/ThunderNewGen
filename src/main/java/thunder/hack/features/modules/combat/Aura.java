package thunder.hack.features.modules.combat;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import thunder.hack.ThunderHack;
import thunder.hack.core.Core;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.gui.notification.Notification;
import thunder.hack.injection.accesors.ILivingEntity;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.interfaces.IOtherClientPlayerEntity;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.player.SearchInvResult;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import thunder.hack.utility.render.animation.CaptureMark;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.util.UseAction.BLOCK;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.math.MathUtility.random;

public class Aura extends Module {

    // --- SETTINGS ---
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f, v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<SprintMode> sprint = new Setting<>("Sprint", SprintMode.Legit);
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track);
    public final Setting<Integer> interactTicks = new Setting<>("InteractTicks", 3, 1, 10, v -> rotationMode.getValue() == Mode.Interact);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);

    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false, v -> switchMode.getValue() != Switch.Silent);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Float> critFallDistance = new Setting<>("CritFallDistance", 0.15f, 0f, 1.0f).addToGroup(smartCrit);

    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true);
    public final Setting<Boolean> ignoreShield = new Setting<>("IgnoreShield", true);
    public final Setting<Boolean> unpressShield = new Setting<>("UnpressShield", true);

    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);
    public final Setting<Boolean> pauseInInventory = new Setting<>("PauseInInventory", true);
    public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", false);
    public final Setting<Boolean> tpsSync = new Setting<>("TPSSync", false);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("OldDelay", new BooleanSettingGroup(false));
    public final Setting<Integer> minCPS = new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f);
    public final Setting<Float> attackBaseTime = new Setting<>("AttackBaseTime", 0.5f, 0f, 2f);
    public final Setting<Integer> attackTickLimit = new Setting<>("AttackTickLimit", 11, 0, 20);

    public final Setting<ESP> esp = new Setting<>("ESP", ESP.ThunderHack);
    public final Setting<SettingGroup> espGroup = new Setting<>("VisualSettings", new SettingGroup(false, 0));
    public final Setting<Integer> espLength = new Setting<>("ESPLength", 14, 1, 40).addToGroup(espGroup);
    public final Setting<Integer> espFactor = new Setting<>("ESPFactor", 8, 1, 20).addToGroup(espGroup);
    public final Setting<Float> espShaking = new Setting<>("ESPShaking", 1.8f, 1.5f, 10f).addToGroup(espGroup);
    public final Setting<Float> espAmplitude = new Setting<>("ESPAmplitude", 3f, 0.1f, 8f).addToGroup(espGroup);
    public final Setting<Float> ghostAlpha = new Setting<>("GhostAlpha", 0.3f, 0.1f, 1f).addToGroup(espGroup);

    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true);
    public final Setting<Resolver> resolver = new Setting<>("Resolver", Resolver.Advantage);
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20);

    public final Setting<SettingGroup> targets = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("Players", true).addToGroup(targets);
    public final Setting<Boolean> Mobs = new Setting<>("Mobs", true).addToGroup(targets);
    public final Setting<Boolean> Animals = new Setting<>("Animals", true).addToGroup(targets);
    public final Setting<Boolean> Villagers = new Setting<>("Villagers", true).addToGroup(targets);
    public final Setting<Boolean> Slimes = new Setting<>("Slimes", true).addToGroup(targets);
    public final Setting<Boolean> hostiles = new Setting<>("Hostiles", true).addToGroup(targets);
    public final Setting<Boolean> Projectiles = new Setting<>("Projectiles", true).addToGroup(targets);
    public final Setting<Boolean> ignoreInvisible = new Setting<>("IgnoreInvisible", false).addToGroup(targets);
    public final Setting<Boolean> ignoreCreative = new Setting<>("IgnoreCreative", true).addToGroup(targets);

    public final Setting<SettingGroup> advanced = new Setting<>("Advanced", new SettingGroup(false, 0));
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f).addToGroup(advanced);
    public final Setting<Boolean> randomHitDelay = new Setting<>("RandomHitDelay", false).addToGroup(advanced);
    public final Setting<RayTrace> rayTrace = new Setting<>("RayTrace", RayTrace.OnlyTarget).addToGroup(advanced);
    public final Setting<Boolean> grimRayTrace = new Setting<>("GrimRayTrace", true).addToGroup(advanced);
    public final Setting<Boolean> tpDisable = new Setting<>("TPDisable", false).addToGroup(advanced);
    public final Setting<Boolean> pullDown = new Setting<>("FastFall", false).addToGroup(advanced);
    public final Setting<Float> pullValue = new Setting<>("PullValue", 3f, 0f, 20f, v -> pullDown.getValue()).addToGroup(advanced);
    public final Setting<AttackHand> attackHand = new Setting<>("AttackHand", AttackHand.MainHand).addToGroup(advanced);
    public final Setting<AccelerateOnHit> accelerateOnHit = new Setting<>("AccelerateOnHit", AccelerateOnHit.Off).addToGroup(advanced);
    public final Setting<Integer> minYawStep = new Setting<>("MinYawStep", 65, 1, 180).addToGroup(advanced);
    public final Setting<Integer> maxYawStep = new Setting<>("MaxYawStep", 75, 1, 180).addToGroup(advanced);
    public final Setting<Float> maxPitchStep = new Setting<>("MaxPitchStep", 90f, 1f, 180f).addToGroup(advanced);

    // --- LOGIC ---
    public static Entity target;
    public float rotationYaw, rotationPitch;
    private int hitTicks, trackticks;
    private boolean lookingAtHitbox;
    private final Timer pauseTimer = new Timer();
    private final List<GhostData> ghostList = new ArrayList<>();
    public Box resolvedBox;
    static boolean wasTargeted = false;

    public Aura() { super("Aura", Category.COMBAT); }

    public void pause() { pauseTimer.reset(); }

    public boolean isAboveWater() {
        return mc.player.isSubmergedInWater() || (mc.world != null && mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER);
    }

    public float getAttackCooldownProgressPerTick() {
        return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (20.0 * (tpsSync.getValue() ? Managers.SERVER.getTPSFactor() : 1f)));
    }

    public float getAttackCooldown() {
        if (mc.player == null) return 0f;
        return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks() + attackBaseTime.getValue()) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F);
    }

    private float getRange() { return elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue(); }
    private float getWallRange() { return elytra.getValue() && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue(); }

    public void auraLogic() {
        if (!haveWeapon()) { target = null; return; }
        handleKill();
        updateTarget();
        if (target == null) return;

        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && autoJump.getValue()) mc.player.jump();

        boolean ready = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
        calcRotations(autoCrit());

        if (ready) {
            if (shieldBreaker(false)) return;
            boolean[] state = preAttack();
            if (!(target instanceof PlayerEntity pl) || !(pl.isUsingItem() && pl.getOffHandStack().getItem() == Items.SHIELD) || ignoreShield.getValue()) attack();
            postAttack(state[0], state[1]);
        }
    }

    private boolean haveWeapon() {
        if (mc.player == null) return false;
        Item item = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue()) return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem;
        return true;
    }

    private boolean skipRayTraceCheck() { return rotationMode.getValue() == Mode.None || rayTrace.getValue() == RayTrace.OFF || rotationMode.is(Mode.Grim); }

    public void attack() {
        Criticals.cancelCrit = true;
        ModuleManager.criticals.doCrit();
        int prev = switchMethod();
        mc.interactionManager.attackEntity(mc.player, target);
        Criticals.cancelCrit = false;
        swingHand();
        hitTicks = getHitTicks();
        if (prev != -1) InventoryUtility.switchTo(prev);
    }

    private boolean[] preAttack() {
        boolean blocking = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (blocking && unpressShield.getValue()) mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        if (rotationMode.is(Mode.Grim)) mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
        return new boolean[]{blocking, Core.serverSprint};
    }

    public void postAttack(boolean block, boolean sprint) {
        if (block && unpressShield.getValue()) mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, rotationYaw, rotationPitch));
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(1000) || mc.player == null) return;
        if (mc.player.isUsingItem() && pauseWhileEating.getValue()) return;
        resolvePlayers();
        auraLogic();
        restorePlayers();
        hitTicks--;
        if (target != null && esp.is(ESP.GhostV2)) ghostList.add(new GhostData(target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch(), (LivingEntity) target));
        if (ghostList.size() > espLength.getValue()) ghostList.remove(0);
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || !pauseTimer.passedMs(1000) || !haveWeapon()) return;
        if (target != null && rotationMode.getValue() != Mode.None && !rotationMode.is(Mode.Grim)) {
            mc.player.setYaw(rotationYaw); mc.player.setPitch(rotationPitch);
        }
    }

    private void updateTarget() {
        Entity cand = findTarget();
        if (target == null || !lockTarget.getValue()) target = cand;
        if (target != null && skipEntity(target)) target = null;
    }

    private void calcRotations(boolean ready) {
        if (target == null) return;
        Vec3d v = target.getPos().add(0, target.getEyeHeight(target.getPose()) * 0.8, 0);
        float[] ang = Managers.PLAYER.calcAngle(v);
        float yStep = random(minYawStep.getValue(), maxYawStep.getValue());
        rotationYaw += MathHelper.clamp(wrapDegrees(ang[0] - rotationYaw), -yStep, yStep);
        rotationPitch += MathHelper.clamp(ang[1] - rotationPitch, -maxPitchStep.getValue(), maxPitchStep.getValue());
        lookingAtHitbox = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue());
    }

    private boolean autoCrit() {
        if (mc.player == null || hitTicks > 0) return false;
        if (mc.player.isOnGround()) return !onlySpace.getValue() && !autoJump.getValue();
        return mc.player.fallDistance > critFallDistance.getValue();
    }

    private int getHitTicks() { return oldDelay.getValue().isEnabled() ? (int)(20f/random(minCPS.getValue(), maxCPS.getValue())) : attackTickLimit.getValue(); }
    private void swingHand() { mc.player.swingHand(attackHand.getValue() == AttackHand.OffHand ? Hand.OFF_HAND : Hand.MAIN_HAND); }
    private int switchMethod() {
        SearchInvResult s = InventoryUtility.getSwordHotBar();
        if (s.found() && switchMode.getValue() != Switch.None) {
            int old = mc.player.getInventory().selectedSlot; s.switchTo();
            return switchMode.getValue() == Switch.Silent ? old : -1;
        }
        return -1;
    }

    private boolean shieldBreaker(boolean instant) {
        if (!shieldBreaker.getValue() || !(target instanceof PlayerEntity p)) return false;
        if (!p.isUsingItem() || p.getActiveItem().getItem() != Items.SHIELD) return false;
        SearchInvResult axe = InventoryUtility.getAxeHotBar();
        if (axe.found()) { axe.switchTo(); mc.interactionManager.attackEntity(mc.player, target); swingHand(); return true; }
        return false;
    }

    public void onRender3D(MatrixStack stack) {
        if (target == null) return;
        if (esp.is(ESP.GhostV2)) {
            for (GhostData gd : ghostList) {
                stack.push(); Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
                stack.translate(gd.x - cam.x, gd.y - cam.y, gd.z - cam.z);
                Render3DEngine.drawFilledBox(stack, new Box(-0.3, 0, -0.3, 0.3, 1.8, 0.3), Render2DEngine.injectAlpha(Color.WHITE, (int)(ghostAlpha.getValue()*255)));
                stack.pop();
            }
        }
        if (esp.is(ESP.ThunderHack)) Render3DEngine.drawTargetEsp(stack, target);
    }

    public Entity findTarget() {
        List<LivingEntity> list = new CopyOnWriteArrayList<>();
        for (Entity e : mc.world.getEntities()) if (e instanceof LivingEntity && !skipEntity(e)) list.add((LivingEntity) e);
        return list.stream().min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e))).orElse(null);
    }

    private boolean skipEntity(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity == mc.player || !entity.isAlive()) return true;
        if (Managers.FRIEND.isFriend(entity.getName().getString())) return true;
        if (entity instanceof PlayerEntity && !Players.getValue()) return true;
        if (entity.distanceTo(mc.player) > getRange() + aimRange.getValue()) return true;
        return false;
    }

    public void handleKill() { if (target instanceof LivingEntity l && !l.isAlive()) target = null; }
    public void resolvePlayers() { if (resolver.getValue() != Resolver.Off) for (PlayerEntity p : mc.world.getPlayers()) if (p instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) p).resolve(resolver.getValue()); }
    public void restorePlayers() { for (PlayerEntity p : mc.world.getPlayers()) if (p instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) p).releaseResolver(); }

    @Override public void onEnable() { target = null; ghostList.clear(); }
    @Override public void onDisable() { target = null; }

    public static class Position {
        public double x, y, z; public int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; this.ticks = 0; }
        public double getX() { return x; } public double getY() { return y; } public double getZ() { return z; }
        public boolean shouldRemove(Position p) { return p.ticks++ > ModuleManager.aura.backTicks.getValue(); }
    }

    private static class GhostData {
        double x, y, z; float yaw, pitch; LivingEntity entity;
        GhostData(double x, double y, double z, float yaw, float pitch, LivingEntity entity) { this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch; this.entity = entity; }
    }

    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, GhostV2 }
    public enum Mode { Interact, Track, Grim, None }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, FOV }
    public enum Switch { Normal, None, Silent }
    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum WallsBypass { Off, V1, V2 }
    public enum SprintMode { Legit, HvH, SMP }
    public enum AttackHand { MainHand, OffHand, None }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
}
