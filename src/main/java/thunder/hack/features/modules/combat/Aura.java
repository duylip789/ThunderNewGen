package thunder.hack.features.modules.combat;

import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.OtherClientPlayerEntity;
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
import thunder.hack.features.modules.client.HudEditor;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.util.UseAction.BLOCK;
import static net.minecraft.util.math.MathHelper.wrapDegrees;
import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.math.MathUtility.random;

public class Aura extends Module {
    public final Setting<Float> attackRange = new Setting<>("Range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("ThroughWallsRange", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride",false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f,v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<Integer> fov = new Setting<>("FOV", 180, 1, 180);
    public final Setting<Mode> rotationMode = new Setting<>("RotationMode", Mode.Track);
    public final Setting<Integer> interactTicks = new Setting<>("InteractTicks", 3, 1, 10, v -> rotationMode.getValue() == Mode.Interact);
    public final Setting<Switch> switchMode = new Setting<>("AutoWeapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("OnlyWeapon", false, v -> switchMode.getValue() != Switch.Silent);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("SmartCrit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("OnlyCrit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("AutoJump", false).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("ShieldBreaker", true);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("PauseWhileEating", false);
    public final Setting<Boolean> tpsSync = new Setting<>("TPSSync", false);
    public final Setting<Boolean> clientLook = new Setting<>("ClientLook", false);
    public final Setting<Boolean> pauseBaritone = new Setting<>("PauseBaritone", false);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("OldDelay", new BooleanSettingGroup(false));
    public final Setting<Integer> minCPS = new Setting<>("MinCPS", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("MaxCPS", 12, 1, 20).addToGroup(oldDelay);

    // ESP SETTINGS
    public final Setting<ESP> esp = new Setting<>("ESP", ESP.ThunderHack);
    public final Setting<SettingGroup> espGroup = new Setting<>("ESPSettings", new SettingGroup(false, 0), v -> esp.is(ESP.ThunderHackV2));
    public final Setting<Integer> espLength = new Setting<>("ESPLength", 14, 1, 40, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Integer> espFactor = new Setting<>("ESPFactor", 8, 1, 20, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> espShaking = new Setting<>("ESPShaking", 1.8f, 1.5f, 10f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);
    public final Setting<Float> espAmplitude = new Setting<>("ESPAmplitude", 3f, 0.1f, 8f, v -> esp.is(ESP.ThunderHackV2)).addToGroup(espGroup);

    public final Setting<SettingGroup> ghostGroup = new Setting<>("GhostV2Settings", new SettingGroup(false, 0), v -> esp.is(ESP.GhostV2));
    public final Setting<Float> ghostSpeed = new Setting<>("GhostSpeed", 3.0f, 1.0f, 10.0f, v -> esp.is(ESP.GhostV2)).addToGroup(ghostGroup);
    public final Setting<Float> ghostSize = new Setting<>("GhostSize", 0.15f, 0.05f, 0.5f, v -> esp.is(ESP.GhostV2)).addToGroup(ghostGroup);
    public final Setting<Float> ghostOrbit = new Setting<>("GhostOrbit", 0.8f, 0.3f, 2.0f, v -> esp.is(ESP.GhostV2)).addToGroup(ghostGroup);

    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true);

    /* ADVANCED   */
    public final Setting<SettingGroup> advanced = new Setting<>("Advanced", new SettingGroup(false, 0));
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f).addToGroup(advanced);
    public final Setting<Boolean> randomHitDelay = new Setting<>("RandomHitDelay", false).addToGroup(advanced);
    public final Setting<Boolean> pauseInInventory = new Setting<>("PauseInInventory", true).addToGroup(advanced);
    public final Setting<Boolean> dropSprint = new Setting<>("DropSprint", true).addToGroup(advanced);
    public final Setting<Boolean> returnSprint = new Setting<>("ReturnSprint", true, v -> dropSprint.getValue()).addToGroup(advanced);
    public final Setting<RayTrace> rayTrace = new Setting<>("RayTrace", RayTrace.OnlyTarget).addToGroup(advanced);
    public final Setting<Boolean> grimRayTrace = new Setting<>("GrimRayTrace", true).addToGroup(advanced);
    public final Setting<Boolean> unpressShield = new Setting<>("UnpressShield", true).addToGroup(advanced);
    public final Setting<Boolean> deathDisable = new Setting<>("DisableOnDeath", true).addToGroup(advanced);
    public final Setting<Boolean> tpDisable = new Setting<>("TPDisable", false).addToGroup(advanced);
    public final Setting<Boolean> pullDown = new Setting<>("FastFall", false).addToGroup(advanced);
    public final Setting<Boolean> onlyJumpBoost = new Setting<>("OnlyJumpBoost", false, v -> pullDown.getValue()).addToGroup(advanced);
    public final Setting<Float> pullValue = new Setting<>("PullValue", 3f, 0f, 20f, v -> pullDown.getValue()).addToGroup(advanced);
    public final Setting<AttackHand> attackHand = new Setting<>("AttackHand", AttackHand.MainHand).addToGroup(advanced);
    public final Setting<Resolver> resolver = new Setting<>("Resolver", Resolver.Advantage).addToGroup(advanced);
    public final Setting<Integer> backTicks = new Setting<>("BackTicks", 4, 1, 20, v -> resolver.is(Resolver.BackTrack)).addToGroup(advanced);
    public final Setting<Boolean> resolverVisualisation = new Setting<>("ResolverVisualisation", false, v -> !resolver.is(Resolver.Off)).addToGroup(advanced);
    public final Setting<AccelerateOnHit> accelerateOnHit = new Setting<>("AccelerateOnHit", AccelerateOnHit.Off).addToGroup(advanced);
    public final Setting<Integer> minYawStep = new Setting<>("MinYawStep", 65, 1, 180).addToGroup(advanced);
    public final Setting<Integer> maxYawStep = new Setting<>("MaxYawStep", 75, 1, 180).addToGroup(advanced);
    public final Setting<Float> aimedPitchStep = new Setting<>("AimedPitchStep", 1f, 0f, 90f).addToGroup(advanced);
    public final Setting<Float> maxPitchStep = new Setting<>("MaxPitchStep", 8f, 1f, 90f).addToGroup(advanced);
    public final Setting<Float> pitchAccelerate = new Setting<>("PitchAccelerate", 1.65f, 1f, 10f).addToGroup(advanced);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(advanced);
    public final Setting<Float> attackBaseTime = new Setting<>("AttackBaseTime", 0.5f, 0f, 2f).addToGroup(advanced);
    public final Setting<Integer> attackTickLimit = new Setting<>("AttackTickLimit", 11, 0, 20).addToGroup(advanced);
    public final Setting<Float> critFallDistance = new Setting<>("CritFallDistance", 0f, 0f, 1f).addToGroup(advanced);

    public final Setting<SettingGroup> targets = new Setting<>("Targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("Players", true).addToGroup(targets);
    public final Setting<Boolean> Mobs = new Setting<>("Mobs", true).addToGroup(targets);
    public final Setting<Boolean> Animals = new Setting<>("Animals", true).addToGroup(targets);
    public final Setting<Boolean> Villagers = new Setting<>("Villagers", true).addToGroup(targets);
    public final Setting<Boolean> Slimes = new Setting<>("Slimes", true).addToGroup(targets);
    public final Setting<Boolean> hostiles = new Setting<>("Hostiles", true).addToGroup(targets);
    public final Setting<Boolean> onlyAngry = new Setting<>("OnlyAngryHostiles", true, v -> hostiles.getValue()).addToGroup(targets);
    public final Setting<Boolean> Projectiles = new Setting<>("Projectiles", true).addToGroup(targets);
    public final Setting<Boolean> ignoreInvisible = new Setting<>("IgnoreInvisibleEntities", false).addToGroup(targets);
    public final Setting<Boolean> ignoreNamed = new Setting<>("IgnoreNamed", false).addToGroup(targets);
    public final Setting<Boolean> ignoreTeam = new Setting<>("IgnoreTeam", false).addToGroup(targets);
    public final Setting<Boolean> ignoreCreative = new Setting<>("IgnoreCreative", true).addToGroup(targets);
    public final Setting<Boolean> ignoreNaked = new Setting<>("IgnoreNaked", false).addToGroup(targets);
    public final Setting<Boolean> ignoreShield = new Setting<>("AttackShieldingEntities", true).addToGroup(targets);

    public static Entity target;
    public float rotationYaw, rotationPitch;
    public float pitchAcceleration = 1f;
    private Vec3d rotationPoint = Vec3d.ZERO, rotationMotion = Vec3d.ZERO;
    private int hitTicks, trackticks;
    private boolean lookingAtHitbox;
    private final Timer delayTimer = new Timer(), pauseTimer = new Timer();
    public Box resolvedBox;
    static boolean wasTargeted = false;

    public Aura() { super("Aura", Category.COMBAT); }

    // CLASS POSITION (GIỮ NGUYÊN ĐỂ KHÔNG BỊ LỖI MIXIN)
    public static class Position {
        private double x, y, z;
        private int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { return ticks++ > ModuleManager.aura.backTicks.getValue(); }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    private float getRange(){ return elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue(); }
    private float getWallRange(){ return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue(); }

    public void auraLogic() {
        if (!haveWeapon() || mc.player == null) { target = null; return; }
        handleKill(); updateTarget();
        if (target == null) return;
        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && autoJump.getValue()) mc.player.jump();
        boolean ready;
        if (grimRayTrace.getValue()) { ready = autoCrit() && (lookingAtHitbox || skipRayTraceCheck()); calcRotations(autoCrit()); }
        else { calcRotations(autoCrit()); ready = autoCrit() && (lookingAtHitbox || skipRayTraceCheck()); }
        if (ready) { if (shieldBreaker(false)) return; boolean[] st = preAttack(); if (!(target instanceof PlayerEntity pl) || !(pl.isUsingItem() && pl.getOffHandStack().getItem() == Items.SHIELD) || ignoreShield.getValue()) attack(); postAttack(st[0], st[1]); }
    }

    private boolean haveWeapon() {
        Item i = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue()) {
            if (switchMode.getValue() == Switch.None) return i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem;
            else return (InventoryUtility.getSwordHotBar().found() || InventoryUtility.getAxeHotBar().found());
        }
        return true;
    }

    private boolean skipRayTraceCheck() {
        return rotationMode.is(Mode.None) || rayTrace.is(RayTrace.OFF) || rotationMode.is(Mode.Grim) || (rotationMode.is(Mode.Interact) && (interactTicks.getValue() <= 1 || mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0, -0.25).offset(0, 1, 0)).iterator().hasNext()));
    }

    public void attack() {
        Criticals.cancelCrit = true; ModuleManager.criticals.doCrit();
        int ps = switchMethod(); mc.interactionManager.attackEntity(mc.player, target);
        Criticals.cancelCrit = false; swingHand(); hitTicks = getHitTicks();
        if (ps != -1) InventoryUtility.switchTo(ps);
    }

    private boolean[] preAttack() {
        boolean b = mc.player.isUsingItem() && mc.player.getActiveItem().getUseAction() == BLOCK;
        if (b && unpressShield.getValue()) sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        boolean s = Core.serverSprint; if (s && dropSprint.getValue()) disableSprint();
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
        return new boolean[]{b, s};
    }

    public void postAttack(boolean b, boolean s) {
        if (s && returnSprint.getValue() && dropSprint.getValue()) enableSprint();
        if (b && unpressShield.getValue()) sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotationYaw, rotationPitch));
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
    }

    private void disableSprint() { mc.player.setSprinting(false); sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING)); }
    private void enableSprint() { mc.player.setSprinting(true); sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING)); }

    public void resolvePlayers() { if (resolver.not(Resolver.Off)) for (PlayerEntity p : mc.world.getPlayers()) if (p instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) p).resolve(resolver.getValue()); }
    public void restorePlayers() { if (resolver.not(Resolver.Off)) for (PlayerEntity p : mc.world.getPlayers()) if (p instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) p).releaseResolver(); }

    public void handleKill() { if (target instanceof LivingEntity le && (le.getHealth() <= 0 || le.isDead())) Managers.NOTIFICATION.publicity("Aura", isRu() ? "Цель нейтрализована!" : "Target neutralized!", 3, Notification.Type.SUCCESS); }

    private int switchMethod() {
        int ps = -1; SearchInvResult sw = InventoryUtility.getSwordHotBar();
        if (sw.found() && switchMode.not(Switch.None)) { if (switchMode.is(Switch.Silent)) ps = mc.player.getInventory().selectedSlot; sw.switchTo(); }
        return ps;
    }

    private int getHitTicks() { return oldDelay.getValue().isEnabled() ? 1 + (int)(20f/random(minCPS.getValue(), maxCPS.getValue())) : (shouldRandomizeDelay() ? (int)MathUtility.random(11, 13) : attackTickLimit.getValue()); }

    @EventHandler public void onUpdate(PlayerUpdateEvent e) { if (!pauseTimer.passedMs(1000) || mc.player == null) return; resolvePlayers(); auraLogic(); restorePlayers(); hitTicks--; }
    @EventHandler public void onSync(EventSync e) {
        if (!pauseTimer.passedMs(1000) || mc.player == null) return;
        if (target != null && rotationMode.not(Mode.None) && rotationMode.not(Mode.Grim)) { mc.player.setYaw(rotationYaw); mc.player.setPitch(rotationPitch); }
        else { rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); }
    }

    @EventHandler public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && tpDisable.getValue()) disable("TP Disable");
        if (e.getPacket() instanceof EntityStatusS2CPacket pac && pac.getStatus() == 3 && pac.getEntity(mc.world) == mc.player && deathDisable.getValue()) disable("Death Disable");
    }

    @Override public void onEnable() { target = null; rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); delayTimer.reset(); }
    @Override public void onDisable() { target = null; }

    private void calcRotations(boolean ready) {
        if (ready) { trackticks = (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0, -0.25).offset(0, 1, 0)).iterator().hasNext() ? 1 : interactTicks.getValue()); }
        else if (trackticks > 0) trackticks--;
        if (target == null) return;

        Vec3d tv;
        if (mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) tv = target.getEyePos();
        else tv = getLegitLook(target);
        if (tv == null) return;

        pitchAcceleration = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange() + aimRange.getValue(), getRange() + aimRange.getValue(), rayTrace.getValue()) ? aimedPitchStep.getValue() : pitchAcceleration < maxPitchStep.getValue() ? pitchAcceleration * pitchAccelerate.getValue() : maxPitchStep.getValue();
        float dy = wrapDegrees((float) wrapDegrees(Math.toDegrees(Math.atan2(tv.z - mc.player.getZ(), (tv.x - mc.player.getX()))) - 90) - rotationYaw) + (wallsBypass.is(WallsBypass.V2) && !ready && !mc.player.canSee(target) ? 20 : 0);
        float dp = ((float) (-Math.toDegrees(Math.atan2(tv.y - (mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow((tv.x - mc.player.getX()), 2) + Math.pow(tv.z - mc.player.getZ(), 2))))) - rotationPitch);
        float ys = rotationMode.not(Mode.Track) ? 360f : random(minYawStep.getValue(), maxYawStep.getValue());
        float ps = rotationMode.not(Mode.Track) ? 180f : Managers.PLAYER.ticksElytraFlying > 5 ? 180 : (pitchAcceleration + random(-1f, 1f));
        if (ready) switch (accelerateOnHit.getValue()) { case Yaw -> ys = 180f; case Pitch -> ps = 90f; case Both -> { ys = 180f; ps = 90f; } }
        if (dy > 180) dy -= 180;
        float dY = MathHelper.clamp(MathHelper.abs(dy), -ys, ys);
        float dP = MathHelper.clamp(dp, -ps, ps);
        float nY = rotationYaw + (dy > 0 ? dY : -dY);
        float nP = MathHelper.clamp(rotationPitch + dP, -90.0F, 90.0F);
        double gcd = (Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0)) * 1.2;
        if (trackticks > 0 || rotationMode.is(Mode.Track)) { rotationYaw = (float) (nY - (nY - rotationYaw) % gcd); rotationPitch = (float) (nP - (nP - rotationPitch) % gcd); }
        else { rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); }
        if (rotationMode.not(Mode.Grim)) ModuleManager.rotations.fixRotation = rotationYaw;
        lookingAtHitbox = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue());
    }

    public void onRender3D(MatrixStack stack) {
        if (!haveWeapon() || target == null || mc.player == null) return;

        // Dòng này hoạt động tốt, chứng tỏ OUTLINE_QUEUE tồn tại
        if ((resolver.is(Resolver.BackTrack) || resolverVisualisation.getValue()) && resolvedBox != null)
            Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(resolvedBox, HudEditor.getColor(0), 1));

        switch (esp.getValue()) {
            case CelkaPasta -> Render3DEngine.drawOldTargetEsp(stack, target);
            case NurikZapen -> CaptureMark.render(target);
            case ThunderHackV2 -> Render3DEngine.renderGhosts(espLength.getValue(), espFactor.getValue(), espShaking.getValue(), espAmplitude.getValue(), target);
            case ThunderHack -> Render3DEngine.drawTargetEsp(stack, target);
            case GhostV2 -> {
                double time = System.currentTimeMillis() / 1000.0 * ghostSpeed.getValue();
                double x = Math.sin(time) * ghostOrbit.getValue();
                double z = Math.cos(time) * ghostOrbit.getValue();
                double y = (Math.sin(time * 0.5) + 1.0) * (target.getHeight() / 2.0);
                Vec3d pos = target.getPos().add(x, y, z);
                
                Box ghostBox = new Box(pos.x - ghostSize.getValue(), pos.y - ghostSize.getValue(), pos.z - ghostSize.getValue(),
                                      pos.x + ghostSize.getValue(), pos.y + ghostSize.getValue(), pos.z + ghostSize.getValue());
                                      
                // FIX LỖI 100%: Dùng Queue thay vì drawBox để tránh lỗi Symbol
                Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(ghostBox, HudEditor.getColor(0), 2));
            }
        }

        if (clientLook.getValue() && rotationMode.not(Mode.None)) {
            mc.player.setYaw((float) Render2DEngine.interpolate(mc.player.prevYaw, rotationYaw, Render3DEngine.getTickDelta()));
            mc.player.setPitch((float) Render2DEngine.interpolate(mc.player.prevPitch, rotationPitch, Render3DEngine.getTickDelta()));
        }
    }

    public float getSquaredRotateDistance() {
        float dst = getRange() + aimRange.getValue();
        if ((mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) && target != null) dst += 4f;
        if (ModuleManager.strafe.isEnabled()) dst += 4f;
        return dst * dst;
    }

    public Vec3d getLegitLook(Entity target) {
        if (rotationMotion.equals(Vec3d.ZERO)) rotationMotion = new Vec3d(random(-0.05f, 0.05f), random(-0.05f, 0.05f), random(-0.05f, 0.05f));
        rotationPoint = rotationPoint.add(rotationMotion);
        if (rotationPoint.x >= (target.getBoundingBox().getLengthX() - 0.05) / 2f) rotationMotion = new Vec3d(-random(0.003f, 0.03f), rotationMotion.getY(), rotationMotion.getZ());
        if (rotationPoint.y >= target.getBoundingBox().getLengthY()) rotationMotion = new Vec3d(rotationMotion.getX(), -random(0.001f, 0.03f), rotationMotion.getZ());
        if (rotationPoint.z >= (target.getBoundingBox().getLengthZ() - 0.05) / 2f) rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), -random(0.003f, 0.03f));
        if (rotationPoint.x <= -(target.getBoundingBox().getLengthX() - 0.05) / 2f) rotationMotion = new Vec3d(random(0.003f, 0.03f), rotationMotion.getY(), rotationMotion.getZ());
        if (rotationPoint.y <= 0.05) rotationMotion = new Vec3d(rotationMotion.getX(), random(0.001f, 0.03f), rotationMotion.getZ());
        if (rotationPoint.z <= -(target.getBoundingBox().getLengthZ() - 0.05) / 2f) rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), random(0.003f, 0.03f));
        rotationPoint.add(random(-0.03f, 0.03f), 0f, random(-0.03f, 0.03f));
        return target.getPos().add(rotationPoint);
    }

    public boolean isInRange(Entity target) {
        if (PlayerUtility.squaredDistanceFromEyes(target.getPos().add(0, target.getEyeHeight(target.getPose()), 0)) > getSquaredRotateDistance() + 4) return false;
        return PlayerUtility.squaredDistanceFromEyes(target.getPos()) <= getSquaredRotateDistance();
    }

    public Entity findTarget() {
        List<LivingEntity> targetsList = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (skipEntity(ent)) continue;
            if (ent instanceof LivingEntity) targetsList.add((LivingEntity) ent);
        }
        return targetsList.stream().min(Comparator.comparing(e -> mc.player.squaredDistanceTo(e.getPos()))).orElse(null);
    }

    private boolean skipEntity(Entity entity) {
        if (!(entity instanceof LivingEntity ent)) return true;
        if (ent == mc.player || !ent.isAlive() || ent.isDead()) return true;
        if (ent instanceof PlayerEntity && (Managers.FRIEND.isFriend((PlayerEntity) ent) || ((PlayerEntity) ent).isCreative())) return true;
        return !isInRange(entity);
    }

    public void pause() { pauseTimer.reset(); }
    private boolean shouldRandomizeDelay() { return randomHitDelay.getValue() && mc.player.isOnGround(); }
    private boolean shouldRandomizeFallDistance() { return randomHitDelay.getValue() && !shouldRandomizeDelay(); }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, GhostV2 }
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
    public enum WallsBypass { Off, V1, V2 }
}
