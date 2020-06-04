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

import ca.stellardrift.confabricate.test.ConfabricateTester;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public final class MixinPlayerManager {

    @Inject(method = "onPlayerConnect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;broadcastChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;"
                    + "Ljava/util/UUID;)V"))
    private void handlePlayerJoin(final ClientConnection connection, final ServerPlayerEntity entity, final CallbackInfo ci) {
        final Text joinMessage = ConfabricateTester.instance().getConfiguration().getMessage();
        if (joinMessage != null) { // our own MOTD
            entity.sendSystemMessage(joinMessage, Util.NIL_UUID);
        }
    }

}
