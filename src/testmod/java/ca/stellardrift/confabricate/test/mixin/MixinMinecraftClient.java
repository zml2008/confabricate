package ca.stellardrift.confabricate.test.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Temporary fix from modmuss50's #live-update-updates post
// Fixes https://bugs.mojang.com/browse/MC-186109
@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @ModifyArg(method = "method_29337", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/class_5350;method_29466(Ljava/util/List;ZILjava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)"
                    + "Ljava/util/concurrent/CompletableFuture;"))
    private boolean setIntegrated(boolean in) {
        return false;
    }
}
