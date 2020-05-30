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
public class MixinPlayerManager {

    @Inject(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcastChatMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"))
    public void handlePlayerJoin(ClientConnection connection, ServerPlayerEntity entity, CallbackInfo ci) {
        final Text joinMessage = ConfabricateTester.instance().getConfiguration().getMessage();
        if (joinMessage != null) { // our own MOTD
            entity.sendSystemMessage(joinMessage, Util.field_25140);
        }
    }

}
