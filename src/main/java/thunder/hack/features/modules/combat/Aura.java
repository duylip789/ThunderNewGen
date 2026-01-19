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
    public final Setting<Float> attackRange = new Setting<>("range", 3.1f, 1f, 6.0f);
    public final Setting<Float> wallRange = new Setting<>("through walls range", 3.1f, 0f, 6.0f);
    public final Setting<Boolean> elytra = new Setting<>("elytra override", false);
    public final Setting<Float> elytraAttackRange = new Setting<>("elytra range", 3.1f, 1f, 6.0f, v -> elytra.getValue());
    public final Setting<Float> elytraWallRange = new Setting<>("elytra through walls range", 3.1f, 0f, 6.0f, v -> elytra.getValue());
    public final Setting<WallsBypass> wallsBypass = new Setting<>("walls bypass", WallsBypass.Off, v -> getWallRange() > 0);
    public final Setting<Integer> fov = new Setting<>("fov", 180, 1, 180);
    public final Setting<Mode> rotationMode = new Setting<>("rotation mode", Mode.Track);
    public final Setting<Integer> interactTicks = new Setting<>("interact ticks", 3, 1, 10, v -> rotationMode.getValue() == Mode.Interact);
    public final Setting<Switch> switchMode = new Setting<>("auto weapon", Switch.None);
    public final Setting<Boolean> onlyWeapon = new Setting<>("only weapon", false, v -> switchMode.getValue() != Switch.Silent);
    public final Setting<BooleanSettingGroup> smartCrit = new Setting<>("smart crit", new BooleanSettingGroup(true));
    public final Setting<Boolean> onlySpace = new Setting<>("only crit", false).addToGroup(smartCrit);
    public final Setting<Boolean> autoJump = new Setting<>("auto jump", false).addToGroup(smartCrit);
    public final Setting<Boolean> shieldBreaker = new Setting<>("shield breaker", true);
    public final Setting<Boolean> pauseWhileEating = new Setting<>("pause while eating", false);
    public final Setting<Boolean> tpsSync = new Setting<>("tps sync", false);
    public final Setting<Boolean> clientLook = new Setting<>("client look", false);
    public final Setting<Boolean> pauseBaritone = new Setting<>("pause baritone", false);
    public final Setting<BooleanSettingGroup> oldDelay = new Setting<>("old delay", new BooleanSettingGroup(false));
    public final Setting<Integer> minCPS = new Setting<>("min cps", 7, 1, 20).addToGroup(oldDelay);
    public final Setting<Integer> maxCPS = new Setting<>("max cps", 12, 1, 20).addToGroup(oldDelay);

    // newgen esp settings
    public final Setting<ESP> esp = new Setting<>("esp", ESP.ThunderHack);
    public final Setting<Color> newGenColor = new Setting<>("newgen color", new Color(255, 255, 255, 200), v -> esp.is(ESP.NewGen));
    public final Setting<Integer> newGenPoints = new Setting<>("newgen points", 15, 1, 50, v -> esp.is(ESP.NewGen));
    public final Setting<Float> newGenSpeed = new Setting<>("newgen speed", 0.15f, 0.01f, 1.0f, v -> esp.is(ESP.NewGen));
    public final Setting<SprintMode> sprintMode = new Setting<>("sprint mode", SprintMode.Default);

    public final Setting<Sort> sort = new Setting<>("sort", Sort.LowestDistance);
    public final Setting<Boolean> lockTarget = new Setting<>("lock target", true);
    public final Setting<Boolean> elytraTarget = new Setting<>("elytra target", true);

    public final Setting<SettingGroup> advanced = new Setting<>("advanced", new SettingGroup(false, 0));
    public final Setting<Float> aimRange = new Setting<>("aim range", 3.1f, 0f, 6.0f).addToGroup(advanced);
    public final Setting<Boolean> randomHitDelay = new Setting<>("random hit delay", false).addToGroup(advanced);
    public final Setting<Boolean> pauseInInventory = new Setting<>("pause in inventory", true).addToGroup(advanced);
    public final Setting<Boolean> dropSprint = new Setting<>("drop sprint", true).addToGroup(advanced);
    public final Setting<Boolean> returnSprint = new Setting<>("return sprint", true, v -> dropSprint.getValue()).addToGroup(advanced);
    public final Setting<RayTrace> rayTrace = new Setting<>("ray trace", RayTrace.OnlyTarget).addToGroup(advanced);
    public final Setting<Boolean> grimRayTrace = new Setting<>("grim ray trace", true).addToGroup(advanced);
    public final Setting<Boolean> unpressShield = new Setting<>("unpress shield", true).addToGroup(advanced);
    public final Setting<Boolean> deathDisable = new Setting<>("disable on death", true).addToGroup(advanced);
    public final Setting<Boolean> tpDisable = new Setting<>("tp disable", false).addToGroup(advanced);
    public final Setting<Boolean> pullDown = new Setting<>("fast fall", false).addToGroup(advanced);
    public final Setting<Boolean> onlyJumpBoost = new Setting<>("only jump boost", false, v -> pullDown.getValue()).addToGroup(advanced);
    public final Setting<Float> pullValue = new Setting<>("pull value", 3f, 0f, 20f, v -> pullDown.getValue()).addToGroup(advanced);
    public final Setting<AttackHand> attackHand = new Setting<>("attack hand", AttackHand.MainHand).addToGroup(advanced);
    public final Setting<Resolver> resolver = new Setting<>("resolver", Resolver.Advantage).addToGroup(advanced);
    public final Setting<Integer> backTicks = new Setting<>("back ticks", 4, 1, 20).addToGroup(advanced);
    public final Setting<Boolean> resolverVisualisation = new Setting<>("resolver visualisation", false, v -> !resolver.is(Resolver.Off)).addToGroup(advanced);
    public final Setting<AccelerateOnHit> accelerateOnHit = new Setting<>("accelerate on hit", AccelerateOnHit.Off).addToGroup(advanced);
    public final Setting<Integer> minYawStep = new Setting<>("min yaw step", 65, 1, 180).addToGroup(advanced);
    public final Setting<Integer> maxYawStep = new Setting<>("max yaw step", 75, 1, 180).addToGroup(advanced);
    public final Setting<Float> aimedPitchStep = new Setting<>("aimed pitch step", 1f, 0f, 90f).addToGroup(advanced);
    public final Setting<Float> maxPitchStep = new Setting<>("max pitch step", 8f, 1f, 90f).addToGroup(advanced);
    public final Setting<Float> pitchAccelerate = new Setting<>("pitch accelerate", 1.65f, 1f, 10f).addToGroup(advanced);
    public final Setting<Float> attackCooldown = new Setting<>("attack cooldown", 0.9f, 0.5f, 1f).addToGroup(advanced);
    public final Setting<Float> attackBaseTime = new Setting<>("attack base time", 0.5f, 0f, 2f).addToGroup(advanced);
    public final Setting<Integer> attackTickLimit = new Setting<>("attack tick limit", 11, 0, 20).addToGroup(advanced);
    public final Setting<Float> critFallDistance = new Setting<>("crit fall distance", 0f, 0f, 1f).addToGroup(advanced);

    public final Setting<SettingGroup> targets = new Setting<>("targets", new SettingGroup(false, 0));
    public final Setting<Boolean> Players = new Setting<>("players", true).addToGroup(targets);
    public final Setting<Boolean> Mobs = new Setting<>("mobs", true).addToGroup(targets);
    public final Setting<Boolean> Animals = new Setting<>("animals", true).addToGroup(targets);
    public final Setting<Boolean> Villagers = new Setting<>("villagers", true).addToGroup(targets);
    public final Setting<Boolean> Slimes = new Setting<>("slimes", true).addToGroup(targets);
    public final Setting<Boolean> hostiles = new Setting<>("hostiles", true).addToGroup(targets);
    public final Setting<Boolean> onlyAngry = new Setting<>("only angry hostiles", true, v -> hostiles.getValue()).addToGroup(targets);
    public final Setting<Boolean> Projectiles = new Setting<>("projectiles", true).addToGroup(targets);
    public final Setting<Boolean> ignoreInvisible = new Setting<>("ignore invisible entities", false).addToGroup(targets);
    public final Setting<Boolean> ignoreNamed = new Setting<>("ignore named", false).addToGroup(targets);
    public final Setting<Boolean> ignoreTeam = new Setting<>("ignore team", false).addToGroup(targets);
    public final Setting<Boolean> ignoreCreative = new Setting<>("ignore creative", true).addToGroup(targets);
    public final Setting<Boolean> ignoreNaked = new Setting<>("ignore naked", false).addToGroup(targets);
    public final Setting<Boolean> ignoreShield = new Setting<>("attack shielding entities", true).addToGroup(targets);

    public static Entity target;
    public float rotationYaw, rotationPitch, pitchAcceleration = 1f;
    private Vec3d rotationPoint = Vec3d.ZERO, rotationMotion = Vec3d.ZERO;
    private int hitTicks, trackticks;
    private boolean lookingAtHitbox;
    private final Timer delayTimer = new Timer(), pauseTimer = new Timer();
    public Box resolvedBox;
    static boolean wasTargeted = false;

    private final List<SoulParticle> soulParticles = new ArrayList<>();

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
        
        if (sprintMode.is(SprintMode.Cancel)) mc.player.setSprinting(false);
        mc.interactionManager.attackEntity(mc.player, target);
        if (sprintMode.is(SprintMode.Cancel)) mc.player.setSprinting(true);

        Criticals.cancelCrit = false;
        swingHand();
        hitTicks = getHitTicks();
        if (prevSlot != -1) InventoryUtility.switchTo(prevSlot);
    }

    private boolean @NotNull [] preAttack() {
        boolean blocking = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == BLOCK;
        if (blocking && unpressShield.getValue()) sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        boolean sprint = Core.serverSprint;
        if (sprint && dropSprint.getValue()) {
            mc.player.setSprinting(false);
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
        return new boolean[]{blocking, sprint};
    }

    public void postAttack(boolean block, boolean sprint) {
        if (sprint && returnSprint.getValue() && dropSprint.getValue()) {
            mc.player.setSprinting(true);
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
        if (block && unpressShield.getValue()) sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, rotationYaw, rotationPitch));
        if (rotationMode.is(Mode.Grim)) sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround()));
    }

    @EventHandler
    public void onUpdate(PlayerUpdateEvent e) {
        if (!pauseTimer.passedMs(1000) || (mc.player.isUsingItem() && pauseWhileEating.getValue())) return;
        if(pauseBaritone.getValue() && ThunderHack.baritone){
            boolean isTargeted = (target != null);
            if (isTargeted && !wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause"); wasTargeted = true; }
            else if (!isTargeted && wasTargeted) { BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume"); wasTargeted = false; }
        }
        
        // Cập nhật tia linh hồn đuổi theo
        if (esp.is(ESP.NewGen) && target != null) {
            if (soulParticles.size() < newGenPoints.getValue()) {
                soulParticles.add(new SoulParticle(target.getPos().add(0, 1, 0)));
            }
            soulParticles.forEach(p -> p.tick(target, newGenSpeed.getValue()));
        } else {
            soulParticles.clear();
        }

        resolvePlayers();
        auraLogic();
        restorePlayers();
        hitTicks--;
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (!pauseTimer.passedMs(1000) || (mc.player.isUsingItem() && pauseWhileEating.getValue()) || !haveWeapon()) return;
        if (target != null && rotationMode.getValue() != Mode.None && rotationMode.getValue() != Mode.Grim) { mc.player.setYaw(rotationYaw); mc.player.setPitch(rotationPitch); }
        else { rotationYaw = mc.player.getYaw(); rotationPitch = mc.player.getPitch(); }
        if (oldDelay.getValue().isEnabled() && minCPS.getValue() > maxCPS.getValue()) minCPS.setValue(maxCPS.getValue());
        if (target != null && pullDown.getValue() && (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) || !onlyJumpBoost.getValue())) mc.player.addVelocity(0f, -pullValue.getValue() / 1000f, 0f);
    }

    public void onRender3D(MatrixStack stack) {
        if (target == null) return;
        
        if (esp.is(ESP.NewGen)) {
            for (SoulParticle p : soulParticles) {
                Render3DEngine.drawFilledSphere(stack, p.pos, 0.03f, newGenColor.getValue());
                if (p.trail.size() > 1) {
                    for (int i = 0; i < p.trail.size() - 1; i++) {
                        Render3DEngine.drawLine(p.trail.get(i), p.trail.get(i + 1), newGenColor.getValue(), 1.5f);
                    }
                }
            }
        } else if (esp.is(ESP.ThunderHack)) {
             Render3DEngine.drawTargetEsp(stack, target);
        } else if (esp.is(ESP.ThunderHackV2)) {
             Render3DEngine.renderGhosts(0.5f, 1f, true, 0.1f, target);
        }
        
        if (clientLook.getValue() && rotationMode.getValue() != Mode.None) {
            mc.player.setYaw((float) Render2DEngine.interpolate(mc.player.prevYaw, rotationYaw, Render3DEngine.getTickDelta()));
            mc.player.setPitch((float) Render2DEngine.interpolate(mc.player.prevPitch, rotationPitch, Render3DEngine.getTickDelta()));
        }
    }

    // Các hàm phụ trợ giữ nguyên từ gốc để không lỗi
    public void resolvePlayers() { if (resolver.not(Resolver.Off)) for (PlayerEntity player : mc.world.getPlayers()) if (player instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) player).resolve(resolver.getValue()); }
    public void restorePlayers() { if (resolver.not(Resolver.Off)) for (PlayerEntity player : mc.world.getPlayers()) if (player instanceof OtherClientPlayerEntity) ((IOtherClientPlayerEntity) player).releaseResolver(); }
    public void handleKill() { if (target instanceof LivingEntity && (((LivingEntity) target).getHealth() <= 0 || ((LivingEntity) target).isDead())) Managers.NOTIFICATION.publicity("Aura", isRu() ? "Цель успешно нейтрализована!" : "Target successfully neutralized!", 3, Notification.Type.SUCCESS); }
    private int switchMethod() { int prevSlot = -1; SearchInvResult swordResult = InventoryUtility.getSwordHotBar(); if (swordResult.found() && switchMode.getValue() != Switch.None) { if (switchMode.getValue() == Switch.Silent) prevSlot = mc.player.getInventory().selectedSlot; swordResult.switchTo(); } return prevSlot; }
    private int getHitTicks() { return oldDelay.getValue().isEnabled() ? 1 + (int) (20f / random(minCPS.getValue(), maxCPS.getValue())) : (randomHitDelay.getValue() ? (int) MathUtility.random(11, 13) : attackTickLimit.getValue()); }
    private boolean autoCrit() { if (hitTicks > 0 || (pauseInInventory.getValue() && Managers.PLAYER.inInventory) || (getAttackCooldown() < attackCooldown.getValue() && !oldDelay.getValue().isEnabled())) return false; return true; }
    public float getAttackCooldown() { return MathHelper.clamp(((float) ((ILivingEntity) mc.player).getLastAttackedTicks() + attackBaseTime.getValue()) / (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * (20.0 * ThunderHack.TICK_TIMER * (tpsSync.getValue() ? Managers.SERVER.getTPSFactor() : 1f))), 0.0F, 1.0F); }

    private void updateTarget() { Entity candidat = findTarget(); if (target == null || sort.getValue() == Sort.FOV || !lockTarget.getValue()) target = candidat; if (target != null && skipEntity(target)) target = null; }

    private void calcRotations(boolean ready) {
        if (target == null) return;
        Vec3d targetVec = target.getEyePos();
        float[] rots = Managers.PLAYER.calcAngle(targetVec);
        rotationYaw = rots[0];
        rotationPitch = rots[1];
        lookingAtHitbox = Managers.PLAYER.checkRtx(rotationYaw, rotationPitch, getRange(), getWallRange(), rayTrace.getValue());
    }

    public Entity findTarget() {
        List<LivingEntity> stage = new ArrayList<>();
        for (Entity ent : mc.world.getEntities()) if (!skipEntity(ent) && ent instanceof LivingEntity) stage.add((LivingEntity) ent);
        return stage.stream().min(Comparator.comparing(e -> (mc.player.squaredDistanceTo(e.getPos())))).orElse(null);
    }

    private boolean skipEntity(Entity entity) {
        if (!(entity instanceof LivingEntity ent) || ent == mc.player || ent.isDead() || !ent.isAlive()) return true;
        if (Managers.FRIEND.isFriend(ent.getName().getString())) return true;
        return mc.player.distanceTo(ent) > getRange() + aimRange.getValue();
    }

    @Override public void onEnable() { soulParticles.clear(); }
    @Override public void onDisable() { target = null; soulParticles.clear(); }

    // --- CLASS QUAN TRỌNG ĐỂ MIXIN KHÔNG LỖI ---
    public static class Position {
        private double x, y, z;
        private int ticks;
        public Position(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public boolean shouldRemove() { return ticks++ > ModuleManager.aura.backTicks.getValue(); }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }

    // Logic tia linh hồn
    private static class SoulParticle {
        public Vec3d pos;
        public List<Vec3d> trail = new ArrayList<>();
        private final Random rand = new Random();
        public SoulParticle(Vec3d start) { this.pos = start; }
        public void tick(Entity target, float speed) {
            trail.add(pos);
            if (trail.size() > 12) trail.remove(0);
            Vec3d targetVec = target.getPos().add(0, target.getHeight() / 2f, 0);
            Vec3d moveDir = targetVec.subtract(pos).normalize().multiply(speed);
            pos = pos.add(moveDir).add((rand.nextFloat() - 0.5) * 0.15, (rand.nextFloat() - 0.5) * 0.15, (rand.nextFloat() - 0.5) * 0.15);
        }
    }

    public enum RayTrace { OFF, OnlyTarget, AllEntities }
    public enum Sort { LowestDistance, HighestDistance, LowestHealth, HighestHealth, LowestDurability, HighestDurability, FOV }
    public enum Switch { Normal, None, Silent }
    public enum Resolver { Off, Advantage, Predictive, BackTrack }
    public enum Mode { Interact, Track, Grim, None }
    public enum AttackHand { MainHand, OffHand, None }
    public enum ESP { Off, ThunderHack, NurikZapen, CelkaPasta, ThunderHackV2, NewGen }
    public enum AccelerateOnHit { Off, Yaw, Pitch, Both }
    public enum WallsBypass { Off, V1, V2 }
    public enum SprintMode { Default, Cancel }
}
