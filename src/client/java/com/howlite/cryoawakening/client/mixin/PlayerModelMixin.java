package com.howlite.cryoawakening.client.mixin;

import com.howlite.cryoawakening.entity.GawkerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void cryoawakening$raiseArmsWhenCarryingGawker(AvatarRenderState state, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            boolean isCarrying = mc.level.getEntitiesOfClass(
                GawkerEntity.class,
                new AABB(state.x - 2.0, state.y - 1.0, state.z - 2.0, state.x + 2.0, state.y + 3.0, state.z + 2.0),
                g -> g.isCarried() && g.getCarrierId() == state.id
            ).stream().findFirst().isPresent();

            if (isCarrying) {
                PlayerModel self = (PlayerModel) (Object) this;

                // Lever les deux bras vers le haut pour tenir le Gawker au-dessus de la tête
                self.rightArm.setRotation((float) (-Math.PI + 0.35), -0.15f, 0.25f);
                self.leftArm.setRotation((float) (-Math.PI + 0.35), 0.15f, -0.25f);

                self.rightSleeve.setRotation(self.rightArm.xRot, self.rightArm.yRot, self.rightArm.zRot);
                self.leftSleeve.setRotation(self.leftArm.xRot, self.leftArm.yRot, self.leftArm.zRot);
            }
        }
    }
}
