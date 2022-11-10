package net.fabricmc.example.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ItemRenameMixin {
//	@Inject(at = @At("HEAD"), method = "createResult()V")
//	public void createResult(CallbackInfo info) {
//		ExampleMod.LOGGER.info("This line is printed by an example mod mixin!");
//	}

	@Redirect(method = "handleRenameItem",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;setItemName(Ljava/lang/String;)V"))
	public void setItemName(AnvilMenu self, String string) {
		System.out.println(string);
		self.setItemName(string);
	}
}
