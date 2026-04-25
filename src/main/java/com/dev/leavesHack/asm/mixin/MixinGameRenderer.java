package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.RenderLeaves3DEvent;
import com.dev.leavesHack.utils.render.Render3DUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0), method = "renderWorld")
    void render3dHook(RenderTickCounter tickCounter, CallbackInfo ci) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        Render3DUtil.lastProjMat.set(RenderSystem.getProjectionMatrix());
        Render3DUtil.lastModMat.set(RenderSystem.getModelViewMatrix());
        Render3DUtil.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        MeteorClient.EVENT_BUS.post(RenderLeaves3DEvent.get(matrixStack, mc.getRenderTickCounter().getTickDelta(true)));
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
}
