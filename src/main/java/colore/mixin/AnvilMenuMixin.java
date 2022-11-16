package colore.mixin;

import colore.ColoreMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    private int repairItemCountCost;
    @Shadow
    private String itemName;
    @Shadow
    private DataSlot cost;

    private boolean repairItemCountCostIsZero = false;

    public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }

    @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void onTakeNoConsume(Container slots, int slot, ItemStack item) {
        if (slots == inputSlots && slot == 1 && item == ItemStack.EMPTY) {
            if (!repairItemCountCostIsZero) {
                slots.setItem(slot, item);
            }
            repairItemCountCostIsZero = false;
        } else {
            slots.setItem(slot, item);
        }
    }

    @Inject(method = "createResult", at = @At("RETURN"), slice = @Slice(
            // we don't want to inject on the return after broadcastChanges()
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V")
    ))
    private void createResultBeforeReturn(CallbackInfo ci) {
        if (injectedCreateResult()) {
            broadcastChanges();
        }
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V"))
    private void createResultBeforeBroadcastChanges(CallbackInfo ci) {
        injectedCreateResult();
        // changes will be broadcast by createResult() in AnvilMenu.class
    }

    private boolean injectedCreateResult() {
        ItemStack inputStack = inputSlots.getItem(0);

        if (inputStack.isEmpty())
            return false;

        ItemStack inputStack2 = inputSlots.getItem(1);
        ItemStack resultStack = resultSlots.getItem(0);

        // true if we have dirty state to broadcast
        boolean dirty = false;

        if (ColoreMod.booksAddLore && resultStack.isEmpty()) {
            // Custom lore text by combining any item with a writable or written book. The text of the first pages of
            // the book will be written to the item's lore NBT tag. Supports formatted text (using § in writable books).
            // item + Written Book or Book and Quill => item with book contents as lore text
            if (inputStack2.is(Items.WRITTEN_BOOK) || inputStack2.is(Items.WRITABLE_BOOK)) {
                ArrayList<Component> loreText = createLoreText(inputStack2, inputStack2.is(Items.WRITABLE_BOOK));
                resultStack = inputStack.copy();
                if (loreText != null) {
                    setLoreText(resultStack, loreText);
                    dirty = true;
                } else if (hasLoreText(inputStack)) {
                    // empty books reset lore text
                    resetLoreText(resultStack);
                    dirty = true;
                }
                if (dirty) {
                    // Consume only 1 book when the player takes the result item
                    if (loreText == null && ColoreMod.consumeBookOnLoreReset) {
                        repairItemCountCost = 1;
                    } else if (loreText != null && ColoreMod.consumeBookOnLoreAdded) {
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

        // Custom renames using $ (instead of §) for color codes, players can just write the format code for each style
        // to add to the text, and it will be converted to a JSON component and be written to the item's display Name
        // NBT tag
        if (ColoreMod.styledRenames && !StringUtils.isBlank(itemName) && !itemName.equals(inputStack.getHoverName().getString())) {
            Component name = formatItemName(itemName);
            if (name == null) {
                cost.set(0);
                repairItemCountCost = 0;
                resultSlots.setItem(0, ItemStack.EMPTY);
                return true;
            }
            resultStack.getOrCreateTagElement("display")
                    .putString("Name", Component.Serializer.toJson(name));
            dirty = true;
        }

        return dirty;
    }

    @Nullable
    private static MutableComponent componentFromJson(String s) {
        try {
            return Component.Serializer.fromJson(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static MutableComponent getLoreLineComponent(String line, Style style) {
        MutableComponent component = Component.literal(line).withStyle(style);
        // reset italic and purple color back to white text, purple doesn't really make sense as the default so use
        // white text as base
        if (!style.isItalic()) component = component.withStyle(Style.EMPTY.withItalic(false));
        if (style.getColor() == null) component = component.withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
        return component;
    }

    private static ArrayList<Component> createLoreText(ItemStack writtenBook, boolean writable) {
        CompoundTag tag = writtenBook.getTag();
        if (tag == null || !tag.contains("pages", Tag.TAG_LIST)) {
            return null;
        }
        ListTag pages = tag.getList("pages", ListTag.TAG_STRING);
        int numPages = pages.size();
        if (numPages == 0 || StringUtils.isBlank(pages.getString(0)) && numPages == 1) {
            return null;
        }
        ArrayList<Component> components = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            MutableComponent c = writable ? Component.literal(pages.getString(i)) : componentFromJson(pages.getString(i));
            if (c != null) {
                // this is not sound
                AtomicReference<MutableComponent> lastComponent = new AtomicReference<>();
                c.visit((Style style, String s) -> {
                    String[] lines = StringUtils.split(s, "\r\n");
                    if (lines.length == 0) {
                        components.add(Component.empty());
                        return Optional.empty();
                    }
                    String first = lines[0];
                    MutableComponent lc = lastComponent.get();
                    int j = 0;
                    if (lc != null) {
                        lc.append(getLoreLineComponent(first, style));
                        components.add(lc);
                        lastComponent.set(null);
                        j = 1;
                    }
                    for (; j < lines.length - 1; j++) {
                        String line = lines[j];
                        components.add(getLoreLineComponent(line, style));
                    }
                    if (j >= lines.length) {
                        return Optional.empty();
                    }
                    lastComponent.set(getLoreLineComponent(lines[j], style));
                    return Optional.empty();
                }, Style.EMPTY);
                MutableComponent lc = lastComponent.get();
                if (lc != null)
                    components.add(lc);
            }
        }
        if (components.isEmpty())
            return null;
        return components;
    }

    private static void setLoreText(ItemStack result, ArrayList<Component> lore) {
        ListTag list = new ListTag();
        for (Component loreLine : lore) {
            list.add(StringTag.valueOf(Component.Serializer.toJson(loreLine)));
        }
        result.getOrCreateTagElement("display").put("Lore", list);
    }

    private static boolean hasLoreText(ItemStack input) {
        CompoundTag compoundTag = input.getTagElement("display");
        return compoundTag != null && compoundTag.contains("Lore", Tag.TAG_LIST);
    }

    private static void resetLoreText(ItemStack result) {
        CompoundTag compoundTag = result.getTagElement("display");
        if (compoundTag != null) {
            compoundTag.remove("Lore");
            if (compoundTag.isEmpty()) {
                result.removeTagKey("display");
            }
        }

        CompoundTag tag = result.getTag();
        if (tag != null && tag.isEmpty()) {
            result.setTag(null);
        }
    }

    /**
     * Transforms a formatted item name string into a chat component, it is a bit more lenient than the default § format.
     * @param itemName the formatted string to parse into a Component
     * @return component representing the string's contents
     */
    private static Component formatItemName(String itemName) {
        String[] split = itemName.split("\\$");
        if (split.length < 1)
            return Component.literal("$");
        ArrayList<MutableComponent> formatted = new ArrayList<>();
        if (!split[0].isEmpty()) {
            formatted.add(Component.literal(split[0]));
        }
        ChatFormatting lastColor = ChatFormatting.WHITE;
        boolean lastIsObfuscated = false;
        boolean lastIsBold = false;
        boolean lastIsStrikethrough = false;
        boolean lastIsUnderlined = false;
        boolean lastIsItalic = true;
        ChatFormatting color = null;
        boolean isObfuscated = false;
        boolean isBold = false;
        boolean isStrikethrough = false;
        boolean isUnderlined = false;
        boolean isItalic = false;
        boolean isReset = false;
        for (int i = 1; i < split.length; i++) {
            String text = null;
            String s = split[i];
            if (s.isEmpty()) {
                formatted.add(Component.literal("$"));
                continue;
            }
            char c = s.charAt(0);
            if (s.length() != 1)
                text = s.substring(1);
            switch (c) {
                case '0' -> color = ChatFormatting.BLACK;
                case '1' -> color = ChatFormatting.DARK_BLUE;
                case '2' -> color = ChatFormatting.DARK_GREEN;
                case '3' -> color = ChatFormatting.DARK_AQUA;
                case '4' -> color = ChatFormatting.DARK_RED;
                case '5' -> color = ChatFormatting.DARK_PURPLE;
                case '6' -> color = ChatFormatting.GOLD;
                case '7' -> color = ChatFormatting.GRAY;
                case '8' -> color = ChatFormatting.DARK_GRAY;
                case '9' -> color = ChatFormatting.BLUE;
                case 'a' -> color = ChatFormatting.GREEN;
                case 'b' -> color = ChatFormatting.AQUA;
                case 'c' -> color = ChatFormatting.RED;
                case 'd' -> color = ChatFormatting.LIGHT_PURPLE;
                case 'e' -> color = ChatFormatting.YELLOW;
                case 'f' -> color = ChatFormatting.WHITE;
                case 'k' -> isObfuscated = true;
                case 'l' -> isBold = true;
                case 'm' -> isStrikethrough = true;
                case 'n' -> isUnderlined = true;
                case 'o' -> isItalic = true;
                case 'r' -> {
                    isObfuscated = false;
                    isBold = false;
                    isStrikethrough = false;
                    isUnderlined = false;
                    isItalic = false;
                    color = ChatFormatting.WHITE;
                    isReset = true;
                }
                default -> text = "$".concat(s);
            }
            if (text != null) {
                Style style = Style.EMPTY;
                if (isReset) {
                    if (lastIsObfuscated) style = style.withObfuscated(false);
                    if (lastIsBold) style = style.withBold(false);
                    if (lastIsStrikethrough) style = style.withStrikethrough(false);
                    if (lastIsUnderlined) style = style.withUnderlined(false);
                    if (lastIsItalic) style = style.withItalic(false);
                    if (lastColor != null) style = style.withColor(ChatFormatting.WHITE);
                    lastIsObfuscated = false;
                    lastIsBold = false;
                    lastIsStrikethrough = false;
                    lastIsUnderlined = false;
                    lastIsItalic = false;
                    lastColor = null;
                    isReset = false;
                }
                if (color != null) {
                    style = style.withColor(color);
                    lastColor = color;
                }
                if (isObfuscated) {
                    style = style.withObfuscated(true);
                    lastIsObfuscated = true;
                }
                if (isBold) {
                    style = style.withBold(true);
                    lastIsBold = true;
                }
                if (isStrikethrough) {
                    style = style.withStrikethrough(true);
                    lastIsStrikethrough = true;
                }
                if (isUnderlined) {
                    style = style.withUnderlined(true);
                    lastIsUnderlined = true;
                }
                if (isItalic) {
                    style = style.withItalic(true);
                    lastIsItalic = true;
                }
                isObfuscated = false;
                isBold = false;
                isStrikethrough = false;
                isUnderlined = false;
                isItalic = false;
                color = null;
                formatted.add(Component.literal(text).withStyle(style));
            }
        }
        if (formatted.size() == 0)
            return Component.literal(itemName);
        MutableComponent first = formatted.get(0);
        for (int i = 1; i < formatted.size(); i++) {
            first.append(formatted.get(i));
        }
        return first;
    }
}
