package colore.mixin;

import colore.string.Formatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Objects;

import static colore.ColoreMod.getConfig;
import static colore.string.Formatting.resetLoreText;
import static colore.string.Formatting.setLoreText;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    private int repairItemCountCost;
    @Shadow
    private String itemName;
    @Shadow
    private DataSlot cost;

    // minecraft doesn't allow for repair costs to be 0, so we have to monkey patch it, see onTakeNoConsume
    private boolean repairItemCountCostIsZero = false;

    private AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }

    private boolean injectedCreateResult() {
        ItemStack inputStack = inputSlots.getItem(0);

        if (inputStack.isEmpty()) {
            return false;
        }

        ItemStack inputStack2 = inputSlots.getItem(1);
        ItemStack resultStack = resultSlots.getItem(0);

        // true if we have dirty state to broadcast
        boolean dirty = false;

        // Custom lore text by combining any item with a writable or written book. The text of the first pages of
        // the book will be written to the item's lore NBT tag. Supports formatted text (using § in writable books).
        // item + Written Book or Book and Quill => item with book contents as lore text
        if (getConfig().booksSetLore && resultStack.isEmpty()) {
            if (inputStack2.is(Items.WRITTEN_BOOK) || inputStack2.is(Items.WRITABLE_BOOK)) {
                ArrayList<Component> loreText = Formatting.createLoreText(inputStack2, inputStack2.is(Items.WRITABLE_BOOK), getConfig().ampersandFormattingInWritableBooks);
                resultStack = inputStack.copy();
                if (loreText != null) {
                    setLoreText(resultStack, loreText);
                    dirty = true;
                } else if (Formatting.hasLoreText(inputStack)) {
                    // empty books reset lore text
                    resetLoreText(resultStack);
                    dirty = true;
                }
                if (dirty) {
                    // Consume only 1 book when the player takes the result item
                    if (loreText == null && getConfig().consumeBookOnLoreReset || loreText != null && getConfig().consumeBookOnLoreAdded) {
                        repairItemCountCost = 1;
                    } else {
                        repairItemCountCost = 0;
                        repairItemCountCostIsZero = true;
                    }
                    // Just like renaming, adding lore text doesn't increase repair cost, but it still uses the base
                    // repair cost of the input item
                    cost.set(inputStack.getBaseRepairCost() + 1);
                    resultSlots.setItem(0, resultStack);
                }
            }
        }

        Component hoverName = inputStack.getHoverName();

        // Custom renames using & (instead of §) for color codes, players can just write the format code for each style
        // to add to the text, and it will be converted to a JSON component and be written to the item's display Name
        // NBT tag
        if (getConfig().ampersandFormattingRename && !StringUtils.isBlank(itemName) && !itemName.equals(hoverName.getString())) {
            Component name = Formatting.formatString(itemName, Formatting.ITALIC, Formatting.RESET_STYLE);
            if (name == null) {
                resultStack.resetHoverName();
            } else {
                resultStack.getOrCreateTagElement("display")
                        .putString("Name", Component.Serializer.toJson(name));
            }
            // When NBT data is the same there was no change to the items
            if (Objects.equals(resultStack.getTag(), inputStack.getTag())) {
                cost.set(0);
                repairItemCountCost = 0;
                resultSlots.setItem(0, ItemStack.EMPTY);
            }
            dirty = true;
        }

        return dirty;
    }

    // this is needed to properly handle the early returns in createResult(), we need to catch those for the lore books
    // "recipe"
    @Inject(method = "createResult", at = @At("RETURN"), slice = @Slice(
            // we don't want to inject on the return after broadcastChanges()
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V")
    ))
    private void createResultBeforeReturn(CallbackInfo ignored) {
        if (injectedCreateResult()) {
            broadcastChanges();
        }
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V"))
    private void createResultBeforeBroadcastChanges(CallbackInfo ignored) {
        injectedCreateResult();
        // changes will be broadcast by createResult() in AnvilMenu.class
    }

    // prevent minecraft from setting the ItemStack to count 0
    @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void onTakeNoConsume(Container slots, int slot, ItemStack item) {
        if (repairItemCountCostIsZero && slots == inputSlots && slot == 1 && item == ItemStack.EMPTY) {
            repairItemCountCostIsZero = false;
        } else {
            slots.setItem(slot, item);
        }
    }
}
