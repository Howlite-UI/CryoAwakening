package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.client.event.InfiltrationClientHandler;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verrouille les touches de déplacement (ZQSD / Espace / Shift) pendant la traversée du plafond
 * et la phase PEEKING afin que le joueur ne dérive pas hors de la colonne.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void cryoawakening$lockMovementDuringInfiltration(CallbackInfo ci) {
        if (InfiltrationClientHandler.isActiveSequence()) {
            this.keyPresses = Input.EMPTY;
            this.moveVector = Vec2.ZERO;
        }
    }
}
