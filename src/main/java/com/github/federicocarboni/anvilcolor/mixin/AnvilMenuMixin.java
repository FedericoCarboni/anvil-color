package com.github.federicocarboni.anvilcolor.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.tools.obfuscation.mirror.FieldHandle;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    private int repairItemCountCost;
    @Shadow
    private String itemName;
    @Shadow
    private DataSlot cost;
//    @Shadow
//    protected Container inputSlots;

    public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(menuType, i, inventory, containerLevelAccess);
    }

    // Inject this method before the vanilla createResult() returns
    @Inject(method = "createResult", at = @At("HEAD"))
    void createResultInject(CallbackInfo info) {
        System.out.println("createResult())");
        ItemStack itemStack = this.inputSlots.getItem(0);
        ItemStack itemStack2 = this.resultSlots.getItem(0);
        System.out.println(itemStack2.toString());
//        if (StringUtils.isBlank(this.itemName)) {
//            if (itemStack.hasCustomHoverName()) {
//                itemStack2.resetHoverName();
//            }
//        }
        itemStack2.setHoverName(Component.literal(this.itemName).withStyle(ChatFormatting.RESET).withStyle(ChatFormatting.DARK_RED));
        this.resultSlots.setItem(0, itemStack2);
        this.broadcastChanges();
    }
}
