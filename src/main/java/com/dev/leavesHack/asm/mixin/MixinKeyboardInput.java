package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.KeyboardInputEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput redirectKeyPresses(PlayerInput original) {
        KeyboardInputEvent event = new KeyboardInputEvent(original.forward(), original.backward(), original.left(), original.right(), original.jump(), original.sneak(), original.sprint());
        return MeteorClient.EVENT_BUS.post(event).toNewInput();
    }
}
