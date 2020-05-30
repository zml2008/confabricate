/*
 * Copyright 2020 zml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private boolean setIntegrated(final boolean in) {
        return false;
    }

}
