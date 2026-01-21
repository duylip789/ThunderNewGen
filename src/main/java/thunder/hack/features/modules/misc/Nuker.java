package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.impl.ItemSelectSetting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.player.PlayerUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.ArrayList;

import static net.minecraft.block.Blocks.*;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class Nuker extends Module {

    public Nuker() {
        super("Nuker", Category.MISC);
    }

    public final Setting<ItemSelectSetting> selectedBlocks =
            new Setting<>("SelectedBlocks", new ItemSelectSetting(new ArrayList<>()));

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Default);
    private final Setting<Integer> delay = new Setting<>("Delay", 25, 0, 1000);
    private final Setting<BlockSelection> blocks = new Setting<>("Blocks", BlockSelection.Select);
    private final Setting<Boolean> ignoreWalls = new Setting<>("IgnoreWalls", false);
    private final Setting<Boolean> flatten = new Setting<>("Flatten", false);
    private final Setting<Boolean> creative = new Setting<>("Creative", false);
    private final Setting<Boolean> avoidLava = new Setting<>("AvoidLava", false);
    private final Setting<Float> range = new Setting<>("Range", 4.2f, 1.5f, 25f);

    private final Setting<ColorMode> colorMode = new Setting<>("ColorMode", ColorMode.Sync);
    public final Setting<ColorSetting> color =
            new Setting<>("Color", new ColorSetting(0x2250b4b4),
                    v -> colorMode.getValue() == ColorMode.Custom);

    private Block targetBlockType;
    private BlockData blockData;
    private final Timer breakTimer = new Timer();

    private NukerThread nukerThread = new NukerThread();
    private float rotationYaw = -999, rotationPitch;

    @Override
    public void onEnable() {
        nukerThread = new NukerThread();
        nukerThread.setDaemon(true);
        nukerThread.start();
    }

    @Override
    public void onDisable() {
        nukerThread.interrupt();
        blockData = null;
    }

    @EventHandler
    public void onBlockInteract(EventAttackBlock e) {
        if (mc.world.isAir(e.getBlockPos())) return;

        if (blocks.getValue() == BlockSelection.Select) {
            targetBlockType = mc.world.getBlockState(e.getBlockPos()).getBlock();
            sendMessage(isRu()
                    ? "Выбран блок: " + Formatting.AQUA + targetBlockType.getName().getString()
                    : "Selected block: " + Formatting.AQUA + targetBlockType.getName().getString());
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (rotationYaw != -999) {
            mc.player.setYaw(rotationYaw);
            mc.player.setPitch(rotationPitch);
            rotationYaw = -999;
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (blockData == null || mc.options.attackKey.isPressed()) return;

        if (PlayerUtility.squaredDistanceFromEyes(blockData.bp.toCenterPos()) > range.getPow2Value()
                || mc.world.isAir(blockData.bp)) {
            blockData = null;
            return;
        }

        float[] angle = InteractionUtility.calculateAngle(blockData.vec3d);
        rotationYaw = angle[0];
        rotationPitch = angle[1];

        if (mode.getValue() == Mode.Default) {
            breakBlock();
        }
    }

    private void breakBlock() {
        mc.interactionManager.updateBlockBreakingProgress(blockData.bp, blockData.dir);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (creative.getValue()) {
            mc.interactionManager.breakBlock(blockData.bp);
        }
    }

    public BlockData getNukerBlockPos() {
        int r = (int) Math.ceil(range.getValue());

        for (BlockPos b : BlockPos.iterateOutwards(mc.player.getBlockPos(), r, r, r)) {
            if (flatten.getValue() && b.getY() < mc.player.getY()) continue;
            if (avoidLava.getValue() && checkLava(b)) continue;

            BlockState state = mc.world.getBlockState(b);
            if (!isAllowed(state.getBlock())) continue;

            if (PlayerUtility.squaredDistanceFromEyes(b.toCenterPos()) > range.getPow2Value())
                continue;

            BlockHitResult hit = mc.world.raycast(new RaycastContext(
                    InteractionUtility.getEyesPos(mc.player),
                    b.toCenterPos(),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(b)) {
                return new BlockData(b, hit.getPos(), hit.getSide());
            }
        }
        return null;
    }

    private boolean checkLava(BlockPos base) {
        for (Direction dir : Direction.values()) {
            if (mc.world.getBlockState(base.offset(dir)).getBlock() == LAVA)
                return true;
        }
        return false;
    }

    private boolean isAllowed(Block block) {
        return switch (blocks.getValue()) {
            case All -> block != BEDROCK && !(block instanceof FluidBlock);
            case Select -> block == targetBlockType;
            default -> true;
        };
    }

    public class NukerThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    if (!Module.fullNullCheck() && blockData == null) {
                        blockData = getNukerBlockPos();
                    }
                    Thread.sleep(10);
                } catch (Exception ignored) {}
            }
        }
    }

    private enum Mode { Default, Fast, FastAF }
    private enum ColorMode { Custom, Sync }
    private enum BlockSelection { Select, All, BlackList, WhiteList }

    public record BlockData(BlockPos bp, Vec3d vec3d, Direction dir) {}
}
