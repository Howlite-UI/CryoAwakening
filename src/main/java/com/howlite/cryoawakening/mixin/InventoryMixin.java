package com.howlite.cryoawakening.mixin;

import com.howlite.cryoawakening.event.GawkerCarryHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Shadow @Final public Player player;

    @Inject(method = "setSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$lockSlotWhenCarryingGawker(int slot, CallbackInfo ci) {
        if (this.player != null && this.player.level() != null) {
            if (GawkerCarryHandler.getCarriedGawker(this.player) != null) {
                ci.cancel();
            }
        }
    }
}
