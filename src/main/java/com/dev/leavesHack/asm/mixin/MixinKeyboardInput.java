package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.KeyboardInputEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input redirectKeyPresses(Input original) {
        KeyboardInputEvent event = new KeyboardInputEvent(original.forward(), original.backward(), original.left(), original.right(), original.jump(), original.shift(), original.sprint());
        return MeteorClient.EVENT_BUS.post(event).toNewInput();
    }
}
