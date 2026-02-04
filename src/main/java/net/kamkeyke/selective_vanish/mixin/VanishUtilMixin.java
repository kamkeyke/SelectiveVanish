package net.kamkeyke.selective_vanish.mixin;

import net.kamkeyke.selective_vanish.saveddata.VanishVisibilityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import redstonedubstep.mods.vanishmod.VanishUtil;

@Mixin(VanishUtil.class)
public class VanishUtilMixin {

    @Inject(
            method = "playerAllowedToSeeOther",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onPlayerAllowedToSeeOther(Entity subject, Entity otherPlayer, boolean isSubjectVanished, boolean isOtherVanished, CallbackInfoReturnable<Boolean> cir){
        if(subject instanceof ServerPlayer viewer && otherPlayer instanceof ServerPlayer vanishedPlayer){
            VanishVisibilityData visibilityData = VanishVisibilityData.get(vanishedPlayer.server);
            if(visibilityData.canSee(viewer, vanishedPlayer)){
                cir.setReturnValue(true);
            }
        }
    }
}
