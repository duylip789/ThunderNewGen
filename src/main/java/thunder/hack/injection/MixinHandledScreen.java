package thunder.hack.injection;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.Core;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.misc.PeekScreen;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.Timer;

import java.util.Arrays;
import java.util.List;

import static thunder.hack.features.modules.Module.mc;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler>
        extends Screen implements ScreenHandlerProvider<T> {

    @Unique
    private final Timer delayTimer = new Timer();

    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Shadow
    protected abstract boolean isPointOverSlot(Slot slot, double mouseX, double mouseY);

    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int mouseButton, SlotActionType type);

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    private static final ItemStack[] ITEMS = new ItemStack[27];

    /* ================= ITEM SCROLLER ================= */

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;

        for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (isPointOverSlot(slot, mouseX, mouseY) && slot.isEnabled()) {
                if (ModuleManager.itemScroller.isEnabled()
                        && isShiftDown()
                        && Core.hold_mouse0
                        && delayTimer.passedMs(ModuleManager.itemScroller.delay.getValue())) {

                    onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
                    delayTimer.reset();
                }
            }
        }
    }

    private boolean isShiftDown() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /* ================= SERVER HELPER ================= */

    @Inject(
            method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V",
            at = @At("TAIL")
    )
    private void drawSlotHook(DrawContext context, Slot slot, CallbackInfo ci) {
        if (ModuleManager.serverHelper.isEnabled()
                && ModuleManager.serverHelper.aucHelper.getValue()) {
            ModuleManager.serverHelper.onRenderChest(context, slot);
        }
    }

    /* ================= MIDDLE CLICK SHULKER ================= */

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button,
                                CallbackInfoReturnable<Boolean> cir) {
        if (Module.fullNullCheck()) return;

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && focusedSlot != null
                && !focusedSlot.getStack().isEmpty()
                && mc.player.playerScreenHandler.getCursorStack().isEmpty()) {

            ItemStack stack = focusedSlot.getStack();

            if (stack.getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof ShulkerBoxBlock
                    && stack.contains(DataComponentTypes.CONTAINER)) {

                Arrays.fill(ITEMS, ItemStack.EMPTY);

                ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
                if (container != null) {
                    List<ItemStack> list = container.stream().toList();
                    for (int i = 0; i < Math.min(27, list.size()); i++) {
                        ITEMS[i] = list.get(i);
                    }
                }

                mc.setScreen(new PeekScreen(
                        new ShulkerBoxScreenHandler(
                                0,
                                mc.player.getInventory(),
                                new SimpleInventory(ITEMS)
                        ),
                        mc.player.getInventory(),
                        stack.getName(),
                        bi.getBlock()
                ));

                cir.setReturnValue(true);
            }
        }
    }
        }
