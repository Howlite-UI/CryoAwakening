package com.howlite.cryoawakening.mixin;

import com.howlite.cryoawakening.event.InfiltrationServerHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Protège le joueur contre les faux positifs anti-fly et "moved wrongly!" pendant
 * toute la traversée autoritaire du plafond (Infiltration Pearl), tout en conservant
 * la synchronisation de la rotation de la tête (pour regarder à 360° en phase PEEKING).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    private boolean clientIsFloating;

    @Shadow
    private int aboveGroundTickCount;

    @Shadow
    public abstract void resetPosition();

    @Inject(method = "tick", at = @At("HEAD"))
    private void cryoawakening$resetFloatingDuringInfiltration(CallbackInfo ci) {
        if (this.player != null && InfiltrationServerHandler.isPlayerInfiltrating(this.player)) {
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void cryoawakening$keepAuthoritativePositionDuringInfiltration(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (this.player != null && InfiltrationServerHandler.isPlayerInfiltrating(this.player)) {
            if (packet.hasRotation()) {
                float yRot = packet.getYRot(this.player.getYRot());
                float xRot = packet.getXRot(this.player.getXRot());
                this.player.setYRot(yRot);
                this.player.setXRot(xRot);
                this.player.setYHeadRot(yRot);
            }
            this.clientIsFloating = false;
            this.aboveGroundTickCount = 0;
            this.resetPosition();
            ci.cancel();
        }
    }
}
