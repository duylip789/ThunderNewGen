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
    public final Setting<Boolean> elytra = new Setting<>("ElytraOverride", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("ElytraRange", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("ElytraThroughWallsRange", 3.1f, 0f, 6.0f, v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("WallsBypass", WallsBypass.Off, v -> getWallRange() > 0);
    
    // Sprint đặt dưới WallsBypass như yêu cầu
    public final Setting<SprintMode> sprintMode = new Setting<>("Sprint", SprintMode.Legit);

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
    public final Setting<SettingGroup> ghostGroup = new Setting<>("GhostSettings", new SettingGroup(false, 0), v -> esp.is(ESP.Ghost));
    public final Setting<Float> ghostSpeed = new Setting<>("GhostSpeed", 3.0f, 1.0f, 10.0f, v -> esp.is(ESP.Ghost)).addToGroup(ghostGroup);
    public final Setting<Float> ghostOrbit = new Setting<>("GhostOrbit", 0.8f, 0.3f, 2.0f, v -> esp.is(ESP.Ghost)).addToGroup(ghostGroup);

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
    public float rotationYaw;
    public float rotationPitch;
    public float pitchAcceleration = 1f;

    private int hitTicks;
    private int trackticks;
    private boolean lookingAtHitbox;

    private final Timer delayTimer = new Timer();
    private final Timer pauseTimer = new Timer();

    public Box resolvedBox;
    static boolean wasTargeted = false;

    public Aura() {
        super("Aura", Category.COMBAT);
    }

    // FIX: Thêm lại hàm pause() để sửa lỗi PearlChaser, AutoBuff, Phase
    public void pause() {
        pauseTimer.reset();
    }

    // FIX: Thêm lại hàm getRange() để sửa lỗi build
    private float getRange() {
        return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraAttackRange.getValue() : attackRange.getValue();
    }

    // FIX: Thêm lại hàm getWallRange() để sửa lỗi build
    private float getWallRange() {
        return elytra.getValue() && mc.player != null && mc.player.isFallFlying() ? elytraWallRange.getValue() : wallRange.getValue();
    }

    public void auraLogic() {
        if (!haveWeapon() || mc.player == null) { target = null; return; }
        handleKill(); updateTarget();
        if (target == null) return;
        if (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() && autoJump.getValue()) mc.player.jump();

        boolean ready;
        if (grimRayTrace.getValue()) {
            ready = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
            calcRotations(autoCrit());
        } else {
            calcRotations(autoCrit());
            ready = autoCrit() && (lookingAtHitbox || skipRayTraceCheck());
        }

        if (ready) {
            if (shieldBreaker(false)) return;
            boolean[] state = preAttack();
            if (!(target instanceof PlayerEntity pl) || !(pl.isUsingItem() && pl.getOffHandStack().getItem() == Items.SHIELD) || ignoreShield.getValue()) {
                attack();
            }
            postAttack(state[0], state[1]);
        }
    }

    private boolean haveWeapon() {
        Item item = mc.player.getMainHandStack().getItem();
        if (onlyWeapon.getValue()) {
            if (switchMode.getValue() == Switch.None) return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem;
            else return (InventoryUtility.getSwordHotBar().found() || InventoryUtility.getAxeHotBar().found());
        }
        return true;
    }

    private boolean skipRayTraceCheck() { return rotationMode.getValue() == Mode.None || rayTrace.getValue() == RayTrace.OFF || rotationMode.is(Mode.Grim); }

    public void attack() {
        Criticals.cancelCrit = true;
        ModuleManager.criticals.doCrit();
        int prev = switchMethod();
        
        // SPRINT LOGIC THEO CHẾ ĐỘ
        applySprintLogic();

        mc.interactionManager.attackEntity(mc.player, target);
        Criticals.cancelCrit = false;
        swingHand();
        hitTicks = getHitTicks();
        if (prev != -1) InventoryUtility.switchTo(prev);
    }

    private void applySprintLogic() {
        if (sprintMode.is(SprintMode.HvH)) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
                sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        } else if (sprintMode.is(SprintMode.SMP)) {
            if (!mc.player.isSprinting() && mc.player.forwardSpeed > 0) {
                mc.player.setSprinting(true);
                sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
        }
    }

    private boolean[] preAttack() {
        boolean block = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (block && unpressShield.getValue()) sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        boolean sprint = Core.serverSprint;
        if (sprint && dropSprint.getValue()) disableSprint();
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
        return new boolean[]{block, sprint};
    }

    public void postAttack(boolean block, boolean sprint) {
        if (sprint && returnSprint.getValue() && dropSprint.getValue()) enableSprint();
        if (block && unpressShield.getValue()) sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotationYaw, rotationPitch));
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
    }

    private void disableSprint() { mc.player.setSprinting(false); sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING)); }
    private void enableSprint() { mc.player.setSprinting(true); sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING)); }

    private int switchMethod() {
        int prev = -1;
        SearchInvResult res = InventoryUtility.getSwordHotBar();
        if (res.found() && switchMode.getValue() != Switch.None) {
            if (switchMode.getValue() == Switch.Silent) prev = mc.player.getInventory().selectedSlot;
            res.switchTo();
        }
        return prev;
    }

    private int getHitTicks() { return oldDelay.getValue().isEnabled() ? 1 + (int) (20f / random(minCPS.getValue(), maxCPS.getValue())) : (shouldRandomizeDelay() ? (int)MathUtility.random(11, 13) : attackTickLimit.getValue()); }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(1000) || mc.player == null) return;
        if (mc.player.isUsingItem() && pauseWhileEating.getValue()) return;
        if(pauseBaritone.getValue() && ThunderHack.baritone){
            boolean isTargeted = (target != null);
            if (isTargeted && !wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause"); wasTargeted = true; }
            else if (!isTargeted && wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume"); wasTargeted = false; }
        }
        resolvePlayers(); auraLogic(); restorePlayers(); hitTicks--;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (!pauseTimer.passedMs(1000) || mc.player == null) return;
        if (target != null && rotationMode.getValue() != Mode.None && rotationMode.getValue() != Mode.Grim) { mc.player.setYaw(rotationYaw); mc.player.setPitch(rotationPitch); }
        else { rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); }
        if (target != null && pullDown.getValue() && (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) || !onlyJumpBoost.getValue())) mc.player.addVelocity(0f, -pullValue.getValue() / 1000f, 0f);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.@NotNull Receive e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket && tpDisable.getValue()) disable("Disabling due to teleport!");
        if (e.getPacket() instanceof EntityStatusS2CPacket pac && pac.getStatus() == 3 && pac.getEntity(mc.world) == mc.player && deathDisable.getValue()) disable("Disabling due to death!");
    }

    @Override public void onEnable() { target = null; rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); delayTimer.reset(); }
    @Override public void onDisable() { target = null; }

    public void resolvePlayers() { if (resolver.not(Resolver.Off)) for (PlayerEntity p : mc.world.getPlayers()) if (p instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) p).resolve(resolver.getValue()); }
    public void restorePlayers() { if (resolver.not(Resolver.Off)) for (PlayerEntity p : mc.world.getPlayers()) if (p instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) p).releaseResolver(); }
    public void handleKill() { if (target instanceof LivingEntity le && (le.getHealth() <= 0 || le.isDead())) Managers.NOTIFICATION.publicity("Aura", "Target neutralized!", 3, Notification.Type.SUCCESS); }

    private void calcRotations(boolean ready) {
        if (ready) trackticks = (mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.25, 0.0, -0.25).offset(0.0, 1, 0.0)).iterator().hasNext() ? 1 : interactTicks.getValue());
        else if (trackticks > 0) trackticks--;
        if (target == null) return;
        Vec3d targetVec = getLegitLook(target);
        if (targetVec == null) return;
        float delta_yaw = wrapDegrees((float) wrapDegrees(Math.toDegrees(Math.atan2(targetVec.z - mc.player.getZ(), (targetVec.x - mc.player.getX()))) - 90) - rotationYaw) + (wallsBypass.is(WallsBypass.V2) && !ready && !mc.player.canSee(target) ? 20 : 0);
        float delta_pitch = ((float) (-Math.toDegrees(Math.atan2(targetVec.y - (mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow((targetVec.x - mc.player.getX()), 2) + Math.pow(targetVec.z - mc.player.getZ(), 2))))) - rotationPitch);
        float yawStep = rotationMode.getValue() != Mode.Track ? 360f : random(minYawStep.getValue(), maxYawStep.getValue());
        float deltaYaw = MathHelper.clamp(MathHelper.abs(delta_yaw), -yawStep, yawStep);
        rotationYaw = rotationYaw + (delta_yaw > 0 ? deltaYaw : -deltaYaw);
        rotationPitch = MathHelper.clamp(rotationPitch + delta_pitch, -90.0F, 90.0F);
        lookingAtHitbox = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue());
    }

    public void onRender3D(MatrixStack stack) {
        if (target == null) return;
        if (resolvedBox != null) Render3DEngine.OUTLINE_QUEUE.add(new Render3DEngine.OutlineAction(resolvedBox, HudEditor.getColor(0), 1));

        switch (esp.getValue()) {
            case ThunderHack -> Render3DEngine.drawTargetEsp(stack, target);
            case NurikZapen -> CaptureMark.render(target);
            case CelkaPasta -> Render3DEngine.drawOldTargetEsp(stack, target);
            case Ghost -> {
                // FIX: Vẽ vòng tròn quay quanh mục tiêu thay vì dùng hàm drawSphere bị lỗi
                double speed = System.currentTimeMillis() / 1000.0 * ghostSpeed.getValue();
                double radius = ghostOrbit.getValue();
                Vec3d targetPos = target.getLerpedPos(mc.getTickDelta());
                double x = targetPos.x + Math.sin(speed) * radius;
                double z = targetPos.z + Math.cos(speed) * radius;
                double y = targetPos.y + (Math.sin(speed * 0.5) + 1.0) * (target.getHeight() / 2.0);
                Render3DEngine.drawFilledBox(stack, new Box(x - 0.1, y - 0.1, z - 0.1, x + 0.1, y + 0.1, z + 0.1), HudEditor.getColor(0));
            }
            case ThunderHackV2 -> { /* Deleted logic as requested */ }
        }
    }

    public float getAttackCooldown() { return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks() + attackBaseTime.getValue()) / getAttackCooldownProgressPerTick(), 0.0F, 1.0F); }
    public float getAttackCooldownProgressPerTick() { return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (20.0 * ThunderHack.TICK_TIMER * (tpsSync.getValue() ? Managers.SERVER.getTPSFactor() : 1f))); }
    public boolean isAboveWater() { return mc.player.isSubmergedInWater() || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos().add(0, -0.4, 0))).getBlock() == Blocks.WATER; }

    private boolean autoCrit() {
        if (hitTicks > 0) return false;
        if (getAttackCooldown() < attackCooldown.getValue() && !oldDelay.getValue().isEnabled()) return false;
        if (mc.player.isInLava() || mc.player.isSubmergedInWater()) return true;
        return !mc.player.isOnGround() && mc.player.fallDistance > critFallDistance.getValue();
    }

    private boolean shieldBreaker(boolean instant) {
        int axeSlot = InventoryUtility.getAxe().slot();
        if (axeSlot == -1 || !shieldBreaker.getValue() || !(target instanceof PlayerEntity)) return false;
        if (!((PlayerEntity) target).isUsingItem() && !instant) return false;
        InventoryUtility.switchTo(axeSlot);
        mc.interactionManager.attackEntity(mc.player, target);
        swingHand();
        InventoryUtility.switchTo(mc.player.getInventory().selectedSlot);
        return true;
    }

    private void swingHand() {
        if (attackHand.getValue() == AttackHand.MainHand) mc.player.swingHand(Hand.MAIN_HAND);
        else if (attackHand.getValue() == AttackHand.OffHand) mc.player.swingHand(Hand.OFF_HAND);
    }

    public Vec3d getLegitLook(Entity target) { return target.getBoundingBox().getCenter(); }

    private void updateTarget() {
        target = mc.world.getEntities().stream()
                .filter(e -> !skipEntity(e))
                .min(Comparator.comparing(e -> mc.player.distanceTo(e))).orElse(null);
    }

    private boolean skipEntity(Entity entity) {
        if (!(entity instanceof LivingEntity ent) || ent == mc.player || !ent.isAlive()) return true;
        if (entity instanceof PlayerEntity && Managers.FRIEND.isFriend((PlayerEntity) entity)) return true;
        return mc.player.distanceTo(entity) > getRange() + aimRange.getValue();
    }

    private boolean shouldRandomizeDelay() { return randomHitDelay.getValue() && (mc.player.isOnGround() || mc.player.fallDistance < 0.12f); }

    public static class Position {
        private double x, y, z; private int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { return ticks++ > ModuleManager.aura.backTicks.getValue(); }
        public double getX() { return x; } public double getY() { return y; } public double getZ() { return z; }
    }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    // Giữ ThunderHackV2 để AutoCrystal không bị lỗi build
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, Ghost, ThunderHackV2 }
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
    public enum WallsBypass { Off, V1, V2 }
    public enum SprintMode { Legit, HvH, SMP }
}
