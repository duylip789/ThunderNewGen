package thunder.hack.gui.misc;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

public class PeekScreen extends ShulkerBoxScreen {

    private static final ItemStack[] ITEMS = new ItemStack[27];

    public PeekScreen(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title, Block block) {
        super(handler, inventory, title);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && focusedSlot != null
                && !focusedSlot.getStack().isEmpty()
                && client.player.playerScreenHandler.getCursorStack().isEmpty()) {

            ItemStack stack = focusedSlot.getStack();

            if (stack.contains(DataComponentTypes.CONTAINER)) {

                Arrays.fill(ITEMS, ItemStack.EMPTY);
                ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);

                if (container != null) {
                    List<ItemStack> list = container.stream().toList();
                    for (int i = 0; i < Math.min(list.size(), 27); i++) {
                        ITEMS[i] = list.get(i);
                    }
                }

                client.setScreen(new PeekScreen(
                        new ShulkerBoxScreenHandler(
                                0,
                                client.player.getInventory(),
                                new SimpleInventory(ITEMS)
                        ),
                        client.player.getInventory(),
                        stack.getName(),
                        ((BlockItem) stack.getItem()).getBlock()
                ));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
}
