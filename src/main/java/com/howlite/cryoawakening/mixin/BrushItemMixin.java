package com.howlite.cryoawakening.mixin;

import com.howlite.cryoawakening.block.entity.PetrifiedLilacLeavesBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrushItem.class)
public abstract class BrushItemMixin {

    @Shadow
    protected abstract HitResult calculateHitResult(Player player);

    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void handlePetrifiedLeavesBrush(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (!(livingEntity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        HitResult hitResult = calculateHitResult(player);
        if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            int elapsed = ((BrushItem) (Object) this).getUseDuration(stack, livingEntity) - remainingUseTicks + 1;
            if (elapsed % 10 == 5) {
                BlockPos pos = blockHitResult.getBlockPos();
                BlockEntity be = level.getBlockEntity(pos);
                // Si le joueur clique sur le dessus du bloc support sous 1 couche fine, cibler les feuilles au-dessus
                if (!(be instanceof PetrifiedLilacLeavesBlockEntity) && blockHitResult.getDirection() == Direction.UP) {
                    BlockEntity aboveBe = level.getBlockEntity(pos.above());
                    if (aboveBe instanceof PetrifiedLilacLeavesBlockEntity) {
                        be = aboveBe;
                    }
                }

                if (be instanceof PetrifiedLilacLeavesBlockEntity leavesBE) {
                    boolean finished = leavesBE.brush(level.getGameTime(), serverLevel, player, blockHitResult.getDirection(), stack);
                    if (finished) {
                        EquipmentSlot slot = player.getItemBySlot(EquipmentSlot.OFFHAND).equals(stack) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                        stack.hurtAndBreak(1, player, slot);
                    }
                }
            }
        }
    }
}
