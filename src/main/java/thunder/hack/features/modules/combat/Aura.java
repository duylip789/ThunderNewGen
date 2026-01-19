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

import java.awt.Color;
import java.util.*;
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

    // esp settings mới (không viết hoa, không group)
    public final Setting<ESP> esp = new Setting<>("esp", ESP.ThunderHack);
    public final Setting<Color> nextGenColor = new Setting<>("nextgen color", new Color(255, 255, 255, 200), v -> esp.is(ESP.NextGen));
    public final Setting<Integer> nextGenPoints = new Setting<>("nextgen points", 15, 5, 50, v -> esp.is(ESP.NextGen));
    public final Setting<Float> nextGenSpeed = new Setting<>("nextgen speed", 0.15f, 0.01f, 0.5f, v -> esp.is(ESP.NextGen));

    // sprint mode (theo yêu cầu)
    public final Setting<SprintMode> sprintMode = new Setting<>("sprint mode", SprintMode.Default);

    public final Setting<Sort> sort = new Setting<>("Sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("LockTarget", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("ElytraTarget", true);

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
    public float rotationYaw, rotationPitch, pitchAcceleration = 1f;
    private Vec3d rotationPoint = Vec3d.ZERO, rotationMotion = Vec3d.ZERO;
    private int hitTicks, trackticks;
    private boolean lookingAtHitbox;
    private final Timer delayTimer = new Timer(), pauseTimer = new Timer();
    public Box resolvedBox;
    static boolean wasTargeted = false;

    // danh sách vị trí của tia linh hồn
    private final List<GhostParticle> ghostParticles = new ArrayList<>();

    public Aura() { super("Aura", Category.COMBAT); }

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
        Item handItem = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue()) {
            if (switchMode.getValue() == Switch.None) return handItem instanceof SwordItem || handItem instanceof AxeItem || handItem instanceof TridentItem;
            else return (InventoryUtility.getSwordHotBar().found() || InventoryUtility.getAxeHotBar().found());
        }
        return true;
    }

    private boolean skipRayTraceCheck() {
        return rotationMode.getValue() == Mode.None || rayTrace.getValue() == RayTrace.OFF || rotationMode.is(Mode.Grim) || (rotationMode.is(Mode.Interact) && (interactTicks.getValue() <= 1 || mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext()));
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

    private boolean @NotNull [] preAttack() {
        boolean blocking = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (blocking && unpressShield.getValue()) sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        boolean sprint = Core.serverSprint;
        if (sprint && dropSprint.getValue()) disableSprint();
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
        return new boolean[]{blocking, sprint};
    }

    public void postAttack(boolean block, boolean sprint) {
        if (sprint && returnSprint.getValue() && dropSprint.getValue()) enableSprint();
        if (block && unpressShield.getValue()) sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotationYaw, rotationPitch));
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
    }

    private void disableSprint() {
        if (sprintMode.is(SprintMode.Cancel)) {
            mc.player.setSprinting(false);
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private void enableSprint() {
        if (sprintMode.is(SprintMode.Cancel)) {
            mc.player.setSprinting(true);
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    public void resolvePlayers() {
        if (resolver.not(Resolver.Off)) for (PlayerEntity player : mc.world.getPlayers()) if (player instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) player).resolve(resolver.getValue());
    }

    public void restorePlayers() {
        if (resolver.not(Resolver.Off)) for (PlayerEntity player : mc.world.getPlayers()) if (player instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) player).releaseResolver();
    }

    public void handleKill() { if (target instanceof LivingEntity && (((LivingEntity) target).getHealth() <= 0 || ((LivingEntity) target).isDead())) Managers.NOTIFICATION.publicity("Aura", isRu() ? "Цель успешно нейтрализована!" : "Target successfully neutralized!", 3, Notification.Type.SUCCESS); }

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
        if (!pauseTimer.passedMs(1000) || (mc.player.isUsingItem() && pauseWhileEating.getValue())) return;
        if(pauseBaritone.getValue() && ThunderHack.baritone){
            boolean isTargeted = (target != null);
            if (isTargeted && !wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause"); wasTargeted = true; }
            else if (!isTargeted && wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume"); wasTargeted = false; }
        }
        resolvePlayers();
        auraLogic();
        restorePlayers();
        hitTicks--;
        
        // cập nhật các tia linh hồn đuổi theo
        if (esp.is(ESP.NextGen) && target != null) {
            if (ghostParticles.size() < nextGenPoints.getValue()) {
                ghostParticles.add(new GhostParticle(target.getPos().add(0, target.getEyeHeight(target.getPose()) / 2.0, 0)));
            }
            for (GhostParticle p : ghostParticles) {
                p.update(target, nextGenSpeed.getValue());
            }
        } else {
            ghostParticles.clear();
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (!pauseTimer.passedMs(1000) || (mc.player.isUsingItem() && pauseWhileEating.getValue()) || !haveWeapon()) return;
        if (target != null && rotationMode.getValue() != Mode.None && rotationMode.getValue() != Mode.Grim) { mc.player.setYaw(rotationYaw); mc.player.setPitch(rotationPitch); }
        else { rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); }
        if (oldDelay.getValue().isEnabled() && minCPS.getValue() > maxCPS.getValue()) minCPS.setValue(maxCPS.getValue());
        if (target != null && pullDown.getValue() && (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) || !onlyJumpBoost.getValue())) mc.player.addVelocity(0f, -pullValue.getValue() / 1000f, 0f);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.@NotNull Send e) { if (e.getPacket() instanceof PlayerInteractEntityC2SPacket pie && Criticals.getInteractType(pie) != Criticals.InteractType.ATTACK && target != null) e.cancel(); }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status) if (status.getStatus() == 30 && status.getEntity(mc.world) != null && target != null && status.getEntity(mc.world) == target) Managers.NOTIFICATION.publicity("Aura", isRu() ? ("Успешно сломали щит игроку " + target.getName().getString()) : ("Succesfully destroyed " + target.getName().getString() + "'s shield"), 2, Notification.Type.SUCCESS);
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && tpDisable.getValue()) disable(isRu() ? "Отключаю из-за телепортации!" : "Disabling due to tp!");
        if (e.getPacket() instanceof EntityStatusS2CPacket pac && pac.getStatus() == 3 && pac.getEntity(mc.world) == mc.player && deathDisable.getValue()) disable(isRu() ? "Отключаю из-за смерти!" : "Disabling due to death!");
    }

    @Override public void onEnable() { target = null; lookingAtHitbox = false; rotationPoint = Vec3d.ZERO; rotationMotion = Vec3d.ZERO; rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); delayTimer.reset(); ghostParticles.clear(); }

    private boolean autoCrit() {
        boolean reasonForSkipCrit = !smartCrit.getValue().isEnabled() || mc.player.getAbilities().flying || (mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) || mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || Managers.PLAYER.isInWeb();
        if (hitTicks > 0 || (pauseInInventory.getValue() && Managers.PLAYER.inInventory) || (getAttackCooldown() < attackCooldown.getValue() && !oldDelay.getValue().isEnabled())) return false;
        if (ModuleManager.criticals.isEnabled() && ModuleManager.criticals.mode.is(Criticals.Mode.Grim)) return true;
        boolean mergeWithTargetStrafe = !ModuleManager.targetStrafe.isEnabled() || !ModuleManager.targetStrafe.jump.getValue();
        boolean mergeWithSpeed = !ModuleManager.speed.isEnabled() || mc.player.isOnGround();
        if (!mc.options.jumpKey.isPressed() && mergeWithTargetStrafe && mergeWithSpeed && !onlySpace.getValue() && !autoJump.getValue()) return true;
        if (mc.player.isInLava() || mc.player.isSubmergedInWater() || (!mc.options.jumpKey.isPressed() && isAboveWater())) return true;
        if (mc.player.fallDistance > 1 && mc.player.fallDistance < 1.14) return false;
        if (!reasonForSkipCrit) return !mc.player.isOnGround() && mc.player.fallDistance > (shouldRandomizeFallDistance() ? MathUtility.random(0.15f, 0.7f) : critFallDistance.getValue());
        return true;
    }

    private boolean shieldBreaker(boolean instant) {
        int axeSlot = InventoryUtility.getAxe().slot();
        if (axeSlot == -1 || !shieldBreaker.getValue() || !(target instanceof PlayerEntity)) return false;
        PlayerEntity pTarget = (PlayerEntity) target;
        if (!pTarget.isUsingItem() && !instant) return false;
        if (pTarget.getOffHandStack().getItem() != Items.SHIELD && pTarget.getMainHandStack().getItem() != Items.SHIELD) return false;
        if (axeSlot >= 9) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.interactionManager.attackEntity(mc.player, target);
            swingHand();
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, axeSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        } else {
            sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
            mc.interactionManager.attackEntity(mc.player, target);
            swingHand();
            sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        }
        hitTicks = 10;
        return true;
    }

    private void swingHand() {
        switch (attackHand.getValue()) {
            case OffHand -> mc.player.swingHand(Hand.OFF_HAND);
            case MainHand -> mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public boolean isAboveWater() { return mc.player.isSubmergedInWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER; }
    public float getAttackCooldownProgressPerTick() { return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (20.0 * ThunderHack.TICK_TIMER * (tpsSync.getValue() ? Managers.SERVER.getTPSFactor() : 1f))); }
    public float getAttackCooldown() { return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks() + attackBaseTime.getValue()) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F); }

    private void updateTarget() {
        Entity candidat = findTarget();
        if (target == null) { target = candidat; return; }
        if (sort.getValue() == Sort.FOV || !lockTarget.getValue() || candidat instanceof ProjectileEntity) target = candidat;
        if (skipEntity(target)) target = null;
    }

    private void calcRotations(boolean ready) {
        if (ready) trackticks = (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext() ? 1 : interactTicks.getValue());
        else if (trackticks > 0) trackticks--;
        if (target == null) return;
        Vec3d targetVec = (mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) ? target.getEyePos() : getLegitLook(target);
        if (targetVec == null) return;
        pitchAcceleration = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange() + aimRange.getValue(), getRange() + aimRange.getValue(), rayTrace.getValue()) ? aimedPitchStep.getValue() : pitchAcceleration < maxPitchStep.getValue() ? pitchAcceleration * pitchAccelerate.getValue() : maxPitchStep.getValue();
        float delta_yaw = wrapDegrees((float) wrapDegrees(Math.toDegrees(Math.atan2(targetVec.z - mc.player.getZ(), (targetVec.x - mc.player.getX()))) - 90) - rotationYaw) + (wallsBypass.is(WallsBypass.V2) && !ready && !mc.player.canSee(target) ? 20 : 0);
        float delta_pitch = ((float) (-Math.toDegrees(Math.atan2(targetVec.y - (mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow((targetVec.x - mc.player.getX()), 2) + Math.pow(targetVec.z - mc.player.getZ(), 2))))) - rotationPitch);
        float yawStep = rotationMode.getValue() != Mode.Track ? 360f : random(minYawStep.getValue(), maxYawStep.getValue());
        float pitchStep = rotationMode.getValue() != Mode.Track ? 180f : Managers.PLAYER.ticksElytraFlying > 5 ? 180 : (pitchAcceleration + random(-1f, 1f));
        if (ready) switch (accelerateOnHit.getValue()) { case Yaw -> yawStep = 180f; case Pitch -> pitchStep = 90f; case Both -> { yawStep = 180f; pitchStep = 90f; } }
        if (delta_yaw > 180) delta_yaw = delta_yaw - 180;
        float deltaYaw = MathHelper.clamp(MathHelper.abs(delta_yaw), -yawStep, yawStep);
        float deltaPitch = MathHelper.clamp(delta_pitch, -pitchStep, pitchStep);
        float newYaw = rotationYaw + (delta_yaw > 0 ? deltaYaw : -deltaYaw);
        float newPitch = MathHelper.clamp(rotationPitch + deltaPitch, -90.0F, 90.0F);
        double gcdFix = (Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0)) * 1.2;
        if (trackticks > 0 || rotationMode.getValue() == Mode.Track) { rotationYaw = (float) (newYaw - (newYaw - rotationYaw) % gcdFix); rotationPitch = (float) (newPitch - (newPitch - rotationPitch) % gcdFix); }
        else { rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); }
        if (!rotationMode.is(Mode.Grim)) ModuleManager.rotations.fixRotation = rotationYaw;
        lookingAtHitbox = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue());
    }

    public void onRender3D(MatrixStack stack) {
        if (!haveWeapon() || target == null) return;
        if ((resolver.is(Resolver.BackTrack) || resolverVisualisation.getValue()) && resolvedBox != null) Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(resolvedBox, HudEditor.getColor(0), 1));
        switch (esp.getValue()) {
            case CelkaPasta -> Render3DEngine.drawOldTargetEsp(stack, target);
            case NurikZapen -> CaptureMark.render(target);
            case ThunderHackV2 -> Render3DEngine.renderGhosts(espLength.getValue(), espFactor.getValue(), espShaking.getValue(), espAmplitude.getValue(), target);
            case NextGen -> renderNextGenESP(stack); // render tia nhỏ đuổi theo
            case ThunderHack -> Render3DEngine.drawTargetEsp(stack, target);
        }
        if (clientLook.getValue() && rotationMode.getValue() != Mode.None) {
            mc.player.setYaw((float) Render2DEngine.interpolate(mc.player.prevYaw, rotationYaw, Render3DEngine.getTickDelta()));
            mc.player.setPitch((float) Render2DEngine.interpolate(mc.player.prevPitch, rotationPitch, Render3DEngine.getTickDelta()));
        }
    }

    private void renderNextGenESP(MatrixStack stack) {
        if (ghostParticles.isEmpty()) return;
        for (GhostParticle p : ghostParticles) {
            Render3DEngine.drawFilledSphere(stack, p.pos, 0.035f, nextGenColor.getValue());
            // vẽ tia nối các điểm cũ để tạo hiệu ứng đuôi
            if (p.prevPositions.size() > 1) {
                for (int i = 0; i < p.prevPositions.size() - 1; i++) {
                    Render3DEngine.drawLine(p.prevPositions.get(i), p.prevPositions.get(i+1), nextGenColor.getValue(), 1.5f);
                }
            }
        }
    }

    @Override public void onDisable() { target = null; ghostParticles.clear(); }

    public float getSquaredRotateDistance() {
        float dst = getRange() + aimRange.getValue();
        if ((mc.player.isFallFlying() || ModuleManager.elytraPlus.isEnabled()) && target != null) dst += 4f;
        if (ModuleManager.strafe.isEnabled()) dst += 4f;
        if (rotationMode.getValue() != Mode.Track || rayTrace.getValue() == RayTrace.OFF) dst = getRange();
        return dst * dst;
    }

    public Vec3d getLegitLook(Entity target) {
        float minMotionXZ = 0.003f, maxMotionXZ = 0.03f, minMotionY = 0.001f, maxMotionY = 0.03f;
        double lx = target.getBoundingBox().getLengthX(), ly = target.getBoundingBox().getLengthY(), lz = target.getBoundingBox().getLengthZ();
        if (rotationMotion.equals(Vec3d.ZERO)) rotationMotion = new Vec3d(random(-0.05f, 0.05f), random(-0.05f, 0.05f), random(-0.05f, 0.05f));
        rotationPoint = rotationPoint.add(rotationMotion);
        if (rotationPoint.x >= (lx - 0.05) / 2f) rotationMotion = new Vec3d(-random(minMotionXZ, maxMotionXZ), rotationMotion.getY(), rotationMotion.getZ());
        if (rotationPoint.y >= ly) rotationMotion = new Vec3d(rotationMotion.getX(), -random(minMotionY, maxMotionY), rotationMotion.getZ());
        if (rotationPoint.z >= (lz - 0.05) / 2f) rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), -random(minMotionXZ, maxMotionXZ));
        if (rotationPoint.x <= -(lx - 0.05) / 2f) rotationMotion = new Vec3d(random(minMotionXZ, 0.03f), rotationMotion.getY(), rotationMotion.getZ());
        if (rotationPoint.y <= 0.05) rotationMotion = new Vec3d(rotationMotion.getX(), random(minMotionY, maxMotionY), rotationMotion.getZ());
        if (rotationPoint.z <= -(lz - 0.05) / 2f) rotationMotion = new Vec3d(rotationMotion.getX(), rotationMotion.getY(), random(minMotionXZ, maxMotionXZ));
        rotationPoint.add(random(-0.03f, 0.03f), 0f, random(-0.03f, 0.03f));
        if (!mc.player.canSee(target) && wallsBypass.getValue() == WallsBypass.V1) return target.getPos().add(random(-0.15, 0.15), ly, random(-0.15, 0.15));
        return target.getPos().add(rotationPoint);
    }

    public boolean isInRange(Entity target) {
        if (PlayerUtility.squaredDistanceFromEyes(target.getPos().add(0, target.getEyeHeight(target.getPose()), 0)) > getSquaredRotateDistance() + 4) return false;
        float hb = (float) (target.getBoundingBox().getLengthX() / 2f);
        for (float x1 = -hb; x1 <= hb; x1 += 0.15f) for (float z1 = -hb; z1 <= hb; z1 += 0.15f) for (float y1 = 0.05f; y1 <= target.getBoundingBox().getLengthY(); y1 += 0.25f) {
            Vec3d v = new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1);
            if (PlayerUtility.squaredDistanceFromEyes(v) > getSquaredRotateDistance()) continue;
            float[] rot = Managers.PLAYER.calcAngle(v);
            if (Managers.PLAYER.checkRtx(rot[0], rot[1], (float) Math.sqrt(getSquaredRotateDistance()), getWallRange(), rayTrace.getValue())) return true;
        }
        return false;
    }

    public Entity findTarget() {
        List<LivingEntity> stage = new CopyOnWriteArrayList<>();
        for (Entity ent : mc.world.getEntities()) {
            if ((ent instanceof ShulkerBulletEntity || ent instanceof FireballEntity) && ent.isAlive() && isInRange(ent) && Projectiles.getValue()) return ent;
            if (!skipEntity(ent) && ent instanceof LivingEntity) stage.add((LivingEntity) ent);
        }
        return switch (sort.getValue()) {
            case LowestDistance -> stage.stream().min(Comparator.comparing(e -> (mc.player.squaredDistanceTo(e.getPos())))).orElse(null);
            case HighestDistance -> stage.stream().max(Comparator.comparing(e -> (mc.player.squaredDistanceTo(e.getPos())))).orElse(null);
            case FOV -> stage.stream().min(Comparator.comparing(this::getFOVAngle)).orElse(null);
            case LowestHealth -> stage.stream().min(Comparator.comparing(e -> (e.getHealth() + e.getAbsorptionAmount()))).orElse(null);
            case HighestHealth -> stage.stream().max(Comparator.comparing(e -> (e.getHealth() + e.getAbsorptionAmount()))).orElse(null);
            case LowestDurability, HighestDurability -> stage.stream().min(Comparator.comparing(e -> {
                float v = 0; for (ItemStack armor : e.getArmorItems()) if (armor != null && !armor.getItem().equals(Items.AIR)) v += ((armor.getMaxDamage() - armor.getDamage()) / (float) armor.getMaxDamage()); return v;
            })).orElse(null);
        };
    }

    private boolean skipEntity(Entity entity) {
        if (isBullet(entity)) return false;
        if (!(entity instanceof LivingEntity ent) || ent.isDead() || !entity.isAlive() || entity instanceof ArmorStandEntity || entity instanceof CatEntity || skipNotSelected(entity) || !InteractionUtility.isVecInFOV(ent.getPos(), fov.getValue())) return true;
        if (entity instanceof PlayerEntity player) {
            if (ModuleManager.antiBot.isEnabled() && AntiBot.bots.contains(entity)) return true;
            if (player == mc.player || Managers.FRIEND.isFriend(player) || (player.isCreative() && ignoreCreative.getValue()) || (player.getArmor() == 0 && ignoreNaked.getValue()) || (player.isInvisible() && ignoreInvisible.getValue()) || (player.getTeamColorValue() == mc.player.getTeamColorValue() && ignoreTeam.getValue() && mc.player.getTeamColorValue() != 16777215)) return true;
        }
        return !isInRange(entity) || (entity.hasCustomName() && ignoreNamed.getValue());
    }

    private boolean isBullet(Entity entity) { return (entity instanceof ShulkerBulletEntity || entity instanceof FireballEntity) && entity.isAlive() && PlayerUtility.squaredDistanceFromEyes(entity.getPos()) < getSquaredRotateDistance() && Projectiles.getValue(); }
    private boolean skipNotSelected(Entity entity) { if (entity instanceof SlimeEntity && !Slimes.getValue()) return true; if (entity instanceof HostileEntity he) { if (!hostiles.getValue()) return true; if (onlyAngry.getValue()) return !he.isAngryAt(mc.player); } if (entity instanceof PlayerEntity && !Players.getValue()) return true; if (entity instanceof VillagerEntity && !Villagers.getValue()) return true; if (entity instanceof MobEntity && !Mobs.getValue()) return true; return entity instanceof AnimalEntity && !Animals.getValue(); }
    private float getFOVAngle(@NotNull LivingEntity e) { double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ(); float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0); return Math.abs(yaw - MathHelper.wrapDegrees(mc.player.getYaw())); }
    public void pause() { pauseTimer.reset(); }
    private boolean shouldRandomizeDelay() { return randomHitDelay.getValue() && (mc.player.isOnGround() || mc.player.fallDistance < 0.12f || mc.player.isSwimming() || mc.player.isFallFlying()); }
    private boolean shouldRandomizeFallDistance() { return randomHitDelay.getValue() && !shouldRandomizeDelay(); }

    // logic đuổi theo đối thủ
    private static class GhostParticle {
        public Vec3d pos;
        public final List<Vec3d> prevPositions = new ArrayList<>();
        private final Random random = new Random();

        public GhostParticle(Vec3d startPos) { this.pos = startPos; }

        public void update(Entity target, float speed) {
            prevPositions.add(pos);
            if (prevPositions.size() > 5) prevPositions.remove(0);
            
            // tính toán vector hướng về mục tiêu (giữa thân)
            Vec3d targetCenter = target.getPos().add(0, target.getEyeHeight(target.getPose()) / 2.0, 0);
            Vec3d dir = targetCenter.subtract(pos).normalize();
            
            // thêm chút hỗn loạn (jitter) để giống tia linh hồn
            Vec3d jitter = new Vec3d((random.nextFloat() - 0.5) * 0.1, (random.nextFloat() - 0.5) * 0.1, (random.nextFloat() - 0.5) * 0.1);
            pos = pos.add(dir.multiply(speed)).add(jitter);
        }
    }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, NextGen }
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
    public enum WallsBypass { Off, V1, V2 }
    public enum SprintMode { Default, Cancel }
}
