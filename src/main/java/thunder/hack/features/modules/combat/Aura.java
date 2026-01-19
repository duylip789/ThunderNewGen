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

import java.awt.*;
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
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f, v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<SprintMode> sprint = new Setting<>("Sprint", SprintMode.Legit);
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

    public final Setting<SettingGroup> advanced = new Setting<>("Advanced", new SettingGroup(false, 0));
    public final Setting<Float> aimRange = new Setting<>("AimRange", 3.1f, 0f, 6.0f).addToGroup(advanced);
    public final Setting<Boolean> randomHitDelay = new Setting<>("RandomHitDelay", false).addToGroup(advanced);
    public final Setting<Boolean> pauseInInventory = new Setting<>("PauseInInventory", true).addToGroup(advanced);
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
    public final Setting<Float> maxPitchStep = new Setting<>("MaxPitchStep", 90f, 1f, 180f).addToGroup(advanced);
    public final Setting<Float> pitchAccelerate = new Setting<>("PitchAccelerate", 1.65f, 1f, 10f).addToGroup(advanced);
    public final Setting<Float> attackCooldown = new Setting<>("AttackCooldown", 0.9f, 0.5f, 1f).addToGroup(advanced);
    public final Setting<Float> attackBaseTime = new Setting<>("AttackBaseTime", 0.5f, 0f, 2f).addToGroup(advanced);
    public final Setting<Integer> attackTickLimit = new Setting<>("AttackTickLimit", 11, 0, 20).addToGroup(advanced);
    public final Setting<Float> critFallDistance = new Setting<>("CritFallDistance", 0.15f, 0f, 1f).addToGroup(advanced);

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
    public final Setting<Boolean> ignoreShield = new Setting<>("IgnoreShield", true).addToGroup(targets);

    public static Entity target;
    public float rotationYaw;
    public float rotationPitch;
    public float pitchAcceleration = 1f;
    private Vec3d rotationPoint = Vec3d.ZERO;
    private Vec3d rotationMotion = Vec3d.ZERO;
    private int hitTicks;
    private int trackticks;
    private boolean lookingAtHitbox;
    private final Timer delayTimer = new Timer();
    private final Timer pauseTimer = new Timer();
    private final List<GhostData> ghostList = new ArrayList<>();
    public Box resolvedBox;
    static boolean wasTargeted = false;

    public Aura() { super("Aura", Category.COMBAT); }

    public void pause() {
        pauseTimer.reset();
    }

    private float getRange() { return elytra.getValue() && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue(); }
    private float getWallRange() { return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue(); }

    public void auraLogic() {
        if (!haveWeapon()) { target = null; return; }
        handleKill();
        updateTarget();
        if (target == null) return;
        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && autoJump.getValue()) mc.player.jump();

        boolean readyForAttack;
        if (grimRayTrace.getValue()) {
            readyForAttack = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
            calcRotations(autoCrit());
        } else {
            calcRotations(autoCrit());
            readyForAttack = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
        }

        if (readyForAttack) {
            if (shieldBreaker(false)) return;
            boolean[] playerState = preAttack();
            if (!(target instanceof PlayerEntity pl) || !(pl.isUsingItem() && pl.getOffHandStack().getItem() == Items.SHIELD) || ignoreShield.getValue())
                attack();
            postAttack(playerState[0], playerState[1]);
        }
    }

    private boolean haveWeapon() {
        if (mc.player == null) return false;
        Item handItem = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue()) {
            if (switchMode.getValue() == Switch.None) return handItem instanceof SwordItem || handItem instanceof AxeItem || handItem instanceof TridentItem;
            else return (InventoryUtility.getSwordHotBar().found() || InventoryUtility.getAxeHotBar().found());
        }
        return true;
    }

    private boolean skipRayTraceCheck() {
        return rotationMode.getValue() == Mode.None || rayTrace.getValue() == RayTrace.OFF || rotationMode.is(Mode.Grim) || (rotationMode.is(Mode.Interact) && (interactTicks.getValue() <= 1 || (mc.world != null && mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext())));
    }

    public void attack() {
        Criticals.cancelCrit = true;
        ModuleManager.criticals.doCrit();
        int prevSlot = switchMethod();
        mc.interactionManager.attackEntity(mc.player, target);
        Criticals.cancelCrit = false;
        swingHand();
        hitTicks = getHitTicks();
        if (prevSlot != -1) InventoryUtility.switchTo(prevSlot);
    }

    private boolean[] preAttack() {
        boolean blocking = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (blocking && unpressShield.getValue()) sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        boolean sprintActive = Core.serverSprint;
        if (sprintActive && (sprint.is(SprintMode.HvH) || sprint.is(SprintMode.SMP))) disableSprint();
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
        return new boolean[]{blocking, sprintActive};
    }

    public void postAttack(boolean block, boolean sprintActive) {
        if (sprintActive && (sprint.is(SprintMode.HvH) || sprint.is(SprintMode.SMP))) enableSprint();
        if (block && unpressShield.getValue()) sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotationYaw, rotationPitch));
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
    }

    private void disableSprint() { mc.player.setSprinting(false); sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING)); }
    private void enableSprint() { mc.player.setSprinting(true); sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING)); }

    public void resolvePlayers() { if (resolver.not(Resolver.Off) && mc.world != null) for (PlayerEntity player : mc.world.getPlayers()) if (player instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) player).resolve(resolver.getValue()); }
    public void restorePlayers() { if (resolver.not(Resolver.Off) && mc.world != null) for (PlayerEntity player : mc.world.getPlayers()) if (player instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) player).releaseResolver(); }

    public void handleKill() { if (target instanceof LivingEntity && (((LivingEntity) target).getHealth() <= 0 || !((LivingEntity) target).isAlive())) Managers.NOTIFICATION.publicity("Aura", isRu() ? "Цель нейтрализована!" : "Target neutralized!", 3, Notification.Type.SUCCESS); }

    private int switchMethod() {
        int prevSlot = -1;
        SearchInvResult swordResult = InventoryUtility.getSwordHotBar();
        if (swordResult.found() && switchMode.getValue() != Switch.None) {
            if (switchMode.getValue() == Switch.Silent) prevSlot = mc.player.getInventory().selectedSlot;
            swordResult.switchTo();
        }
        return prevSlot;
    }

    private int getHitTicks() { return oldDelay.getValue().isEnabled() ? 1 + (int) (20f / random(minCPS.getValue(), maxCPS.getValue())) : (shouldRandomizeDelay() ? (int) MathUtility.random(11, 13) : attackTickLimit.getValue()); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(1000) || mc.player == null) return;
        if (mc.player.isUsingItem() && pauseWhileEating.getValue()) return;
        if (pauseBaritone.getValue() && ThunderHack.baritone) {
            if (target != null && !wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause"); wasTargeted = true; }
            else if (target == null && wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume"); wasTargeted = false; }
        }
        resolvePlayers();
        auraLogic();
        restorePlayers();
        hitTicks--;
        if (target != null && esp.is(ESP.GhostV2)) ghostList.add(new GhostData(target.getX(), target.getY(), target.getZ(), target.getYaw(), target.getPitch(), (LivingEntity) target));
        if (ghostList.size() > espLength.getValue()) ghostList.remove(0);
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (!pauseTimer.passedMs(1000) || mc.player == null || (mc.player.isUsingItem() && pauseWhileEating.getValue()) || !haveWeapon()) return;
        if (target != null && rotationMode.getValue() != Mode.None && rotationMode.getValue() != Mode.Grim) {
            mc.player.setYaw(rotationYaw); mc.player.setPitch(rotationPitch);
        } else {
            rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch();
        }
        if (target != null && pullDown.getValue() && (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) || !onlyJumpBoost.getValue())) mc.player.addVelocity(0f, -pullValue.getValue() / 1000f, 0f);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) { if (e.getPacket() instanceof PlayerInteractEntityC2SPacket pie && Criticals.getInteractType(pie) != Criticals.InteractType.ATTACK && target != null) e.cancel(); }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30 && status.getEntity(mc.world) == target) Managers.NOTIFICATION.publicity("Aura", isRu() ? "Щит сломан!" : "Shield broken!", 2, Notification.Type.SUCCESS);
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && tpDisable.getValue()) disable();
        if (e.getPacket() instanceof EntityStatusS2CPacket pac && pac.getStatus() == 3 && pac.getEntity(mc.world) == mc.player && deathDisable.getValue()) disable();
    }

    @Override
    public void onEnable() { target = null; lookingAtHitbox = false; rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); ghostList.clear(); }

    private boolean autoCrit() {
        if (mc.player == null || hitTicks > 0 || (pauseInInventory.getValue() && Managers.PLAYER.inInventory) || (getAttackCooldown() < attackCooldown.getValue() && !oldDelay.getValue().isEnabled())) return false;
        if (ModuleManager.criticals.isEnabled() && ModuleManager.criticals.mode.is(Criticals.Mode.Grim)) return true;
        if (mc.player.isInLava() || mc.player.isSubmergedInWater() || isAboveWater()) return true;
        if (mc.player.fallDistance > 1 && mc.player.fallDistance < 1.14) return false;
        if (smartCrit.getValue().isEnabled()) return !mc.player.isOnGround() && mc.player.fallDistance > (shouldRandomizeFallDistance() ? MathUtility.random(0.15f, 0.7f) : critFallDistance.getValue());
        return true;
    }

    private boolean shieldBreaker(boolean instant) {
        int axeSlot = InventoryUtility.getAxe().slot();
        if (axeSlot == -1 || !shieldBreaker.getValue() || !(target instanceof PlayerEntity)) return false;
        PlayerEntity ent = (PlayerEntity) target;
        if (!ent.isUsingItem() && !instant) return false;
        if (ent.getOffHandStack().getItem() != Items.SHIELD && ent.getMainHandStack().getItem() != Items.SHIELD) return false;
        int prev = mc.player.getInventory().selectedSlot;
        InventoryUtility.switchTo(axeSlot);
        mc.interactionManager.attackEntity(mc.player, target);
        swingHand();
        InventoryUtility.switchTo(prev);
        hitTicks = 10;
        return true;
    }

    private void swingHand() { if (attackHand.is(AttackHand.MainHand)) mc.player.swingHand(Hand.MAIN_HAND); else if (attackHand.is(AttackHand.OffHand)) mc.player.swingHand(Hand.OFF_HAND); }

    public boolean isAboveWater() { return mc.player.isSubmergedInWater() || (mc.world != null && mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER); }
    public float getAttackCooldownProgressPerTick() { return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (20.0 * (tpsSync.getValue() ? Managers.SERVER.getTPSFactor() : 1f))); }
    public float getAttackCooldown() { if (mc.player == null) return 0f; return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks() + attackBaseTime.getValue()) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F); }

    private void updateTarget() { Entity candidat = findTarget(); if (target == null || !lockTarget.getValue()) target = candidat; if (target != null && skipEntity(target)) target = null; }

    private void calcRotations(boolean ready) {
        if (target == null) return;
        if (ready) trackticks = (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext() ? 1 : interactTicks.getValue());
        else if (trackticks > 0) trackticks--;
        Vec3d targetVec = getLegitLook(target); if (targetVec == null) return;
        float[] angles = Managers.PLAYER.calcAngle(targetVec);
        float yawStep = random(minYawStep.getValue(), maxYawStep.getValue());
        rotationYaw += MathHelper.clamp(wrapDegrees(angles[0] - rotationYaw), -yawStep, yawStep);
        rotationPitch += MathHelper.clamp(angles[1] - rotationPitch, -maxPitchStep.getValue(), maxPitchStep.getValue());
        lookingAtHitbox = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue());
    }

    public void onRender3D(MatrixStack stack) {
        if (target == null) return;
        if (esp.is(ESP.GhostV2)) {
            for (GhostData gd : ghostList) {
                stack.push(); Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
                stack.translate(gd.x - cam.x, gd.y - cam.y, gd.z - cam.z);
                Render3DEngine.drawFilledBox(stack, new Box(-0.3, 0, -0.3, 0.3, 1.8, 0.3), Render2DEngine.injectAlpha(Color.WHITE, (int) (ghostAlpha.getValue() * 255)));
                stack.pop();
            }
        }
        if (esp.is(ESP.ThunderHack)) Render3DEngine.drawTargetEsp(stack, target);
    }

    public Entity findTarget() {
        List<LivingEntity> first_stage = new ArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if (ent instanceof LivingEntity && !skipEntity(ent)) first_stage.add((LivingEntity) ent);
            if (Projectiles.getValue() && (ent instanceof ShulkerBulletEntity || ent instanceof FireballEntity) && ent.isAlive() && isInRange(ent)) return ent;
        }
        return first_stage.stream().min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e))).orElse(null);
    }

    private boolean skipEntity(Entity entity) {
        if (!(entity instanceof LivingEntity ent) || ent == mc.player || !ent.isAlive() || entity instanceof ArmorStandEntity || Managers.FRIEND.isFriend(ent.getName().getString())) return true;
        if (entity instanceof PlayerEntity p && ((p.isCreative() && ignoreCreative.getValue()) || (p.isInvisible() && ignoreInvisible.getValue()))) return true;
        return !isInRange(entity);
    }

    private boolean isInRange(Entity entity) { return mc.player.distanceTo(entity) <= getRange() + aimRange.getValue(); }

    public Vec3d getLegitLook(Entity target) { return target.getEyePos(); }

    private boolean shouldRandomizeDelay() { return randomHitDelay.getValue() && mc.player.isOnGround(); }
    private boolean shouldRandomizeFallDistance() { return randomHitDelay.getValue() && !shouldRandomizeDelay(); }

    public static class Position {
        public double x, y, z; public int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; this.ticks = 0; }
        public boolean shouldRemove() { return ticks++ > ModuleManager.aura.backTicks.getValue(); }
        public double getX() { return x; } public double getY() { return y; } public double getZ() { return z; }
    }

    private static class GhostData {
        double x, y, z; float yaw, pitch; LivingEntity entity;
        GhostData(double x, double y, double z, float yaw, float pitch, LivingEntity entity) { this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch; this.entity = entity; }
    }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, GhostV2 }
    public enum SprintMode { Legit, HvH, SMP }
    public enum WallsBypass { Off, V1, V2 }
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
}
