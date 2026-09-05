package com.dev.leavesHack.events;

import com.mojang.blaze3d.vertex.PoseStack;

//由于彗星Render3D有神秘问题，所以涉及3d矩阵变换需要使用这个
public class RenderLeaves3DEvent {
    private static final RenderLeaves3DEvent INSTANCE = new RenderLeaves3DEvent();
    public PoseStack matrixStack;
    public float tickDelta;

    public static RenderLeaves3DEvent get(PoseStack matrixStack, float tickDelta) {
        INSTANCE.matrixStack = matrixStack;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }
}