package com.github.federicocarboni.anvilcolor.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ItemRenameMixin {
	// Must use a redirect here or the server will default to italic standard names
//    @Redirect(method = "handleRenameItem",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;setItemName(Ljava/lang/String;)V"))
//    public void setItemName(AnvilMenu self, String string) {
//        //
//    }
}
