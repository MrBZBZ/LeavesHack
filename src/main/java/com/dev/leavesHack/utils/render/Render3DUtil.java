package com.dev.leavesHack.utils.render;

import com.dev.leavesHack.asm.accessors.ITextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Render3DUtil {
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();
    private static void drawWithShadow(MatrixStack matrices, String info, float x, float y, int color) {
        var immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        ((ITextRenderer)mc.textRenderer).invokeDrawLayer(info, x, y, color, true, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xf000f0);
        immediate.draw();

        mc.textRenderer.draw(info, x, y, color, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xf000f0);
        immediate.draw();
    }
    public static void renderText3D(String info, Vec3d targetPos, int color) {
        Camera camera = mc.gameRenderer.getCamera();
        RenderSystem.enableBlend();
        GL11.glDepthFunc(GL11.GL_ALWAYS);
        MatrixStack matrixStack = new MatrixStack();
        double x = targetPos.getX();
        double y = targetPos.getY();
        double z = targetPos.getZ();
        int width = mc.textRenderer.getWidth(info);
        float hwidth = width / 2.0f;
        Render3DUtil.renderInfo(info, hwidth, x, y, z, camera, matrixStack, color);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
    }
    public static void renderInfo(String info, float width, double x, double y, double z, Camera camera, MatrixStack matrices, int color) {
        final Vec3d pos = camera.getPos();
        float scale = (float) (-0.025f + (pos.squaredDistanceTo(x, y, z) > (6 * 6) ? (Math.sqrt(pos.squaredDistanceTo(x, y, z)) - 6) * -0.0025f : 0));
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(x - pos.getX(),
                y - pos.getY() + (scale / -0.025f - 1) / 4,
                z - pos.getZ());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        matrices.scale(scale, scale, -1.0f);

        drawWithShadow(matrices, info, -width, 0.0f, color);

        matrices.pop();
    }
    public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getPos().x;
        double deltaY = pos.y - camera.getPos().y;
        double deltaZ = pos.z - camera.getPos().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.f).mul(lastWorldSpaceMatrix);
        Matrix4f matrixProj = new Matrix4f(lastProjMat);
        Matrix4f matrixModel = new Matrix4f(lastModMat);
        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);
        return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (displayHeight - target.y) / mc.getWindow().getScaleFactor(), target.z);
    }
    public static Vec3d worldToScreen(Vec3d pos) {
        Camera camera = mc.gameRenderer.getCamera();

        Vec3d camPos = camera.getPos();
        Vec3d rel = pos.subtract(camPos);

        Vector4f vec = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1.0f);

        Matrix4f matrix = new Matrix4f(lastProjMat);
        matrix.mul(lastModMat);
        vec.mul(matrix);

        if (vec.w() <= 0) return new Vec3d(0,0,-1);

        float ndcX = vec.x() / vec.w();
        float ndcY = vec.y() / vec.w();
        float ndcZ = vec.z() / vec.w();

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        double screenX = (ndcX + 1) / 2 * width;
        double screenY = (1 - ndcY) / 2 * height;

        return new Vec3d(screenX, screenY, ndcZ);
    }
}
