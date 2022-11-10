package com.github.federicocarboni.anvilcolor.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    private int repairItemCountCost;
    @Shadow
    private String itemName;
    @Shadow
    private DataSlot cost;

    public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }

    private static MutableComponent createLoreText(ItemStack book) {
        CompoundTag tag = book.getTag();
        ListTag pages = tag.getList("pages", 8);
        return Component.Serializer.fromJsonLenient(pages.getString(0));
    }

    private static void setLoreText(ItemStack result, Component lore) {
        ListTag list = new ListTag();
        list.add(StringTag.valueOf(Component.Serializer.toJson(lore)));
        result.getOrCreateTagElement("display").put("Lore", list);
    }

    private static Component formatItemName(String itemName) {
        String[] split = itemName.split("\\$");
        if (split.length < 1)
            return Component.literal("$");
        ArrayList<MutableComponent> formatted = new ArrayList();
        if (!split[0].isEmpty()) {
            formatted.add(Component.literal(split[0]));
        }
        ChatFormatting lastColor = null;
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
                case 'r' -> isReset = true;
                default -> {
                    text = "$".concat(s);
                }
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

    @Inject(method = "createResult", at = @At("RETURN"), slice = @Slice(
            // we don't want to inject after broadcastChanges()
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V")
    ))
    void createResultBeforeReturn(CallbackInfo ci) {
        if (injectedCreateResult()) {
            broadcastChanges();
        }
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;broadcastChanges()V"))
    void createResultBeforeBroadcastChanges(CallbackInfo ci) {
        injectedCreateResult();
        // changes will be broadcast by createResult() in AnvilMenu.class
    }

    private boolean injectedCreateResult() {
//        System.out.println(itemName);

        ItemStack inputStack = inputSlots.getItem(0);

        if (inputStack.isEmpty())
            return false;

        ItemStack inputStack2 = inputSlots.getItem(1);
        ItemStack resultStack = resultSlots.getItem(0);

        // true if we have dirty state to broadcast
        boolean dirty = false;

        if (resultStack.isEmpty()) {
            if (inputStack2.is(Items.WRITTEN_BOOK) || inputStack2.is(Items.WRITABLE_BOOK)) {
                resultStack = inputStack.copy();
                setLoreText(resultStack, createLoreText(inputStack2));
                resultSlots.setItem(0, resultStack);
                // Just like renaming, adding lore text doesn't increase repair cost
                cost.set(inputStack.getBaseRepairCost() + 1);
                // Consume one book when the player takes the result item
                repairItemCountCost = 1;
                dirty = true;
            }
        }

        if (!StringUtils.isBlank(itemName) && !itemName.equals(inputStack.getHoverName().getString())) {
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
}
