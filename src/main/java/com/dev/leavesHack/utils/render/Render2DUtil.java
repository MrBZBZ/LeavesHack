package com.dev.leavesHack.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

import static com.dev.leavesHack.utils.render.Render3DUtil.worldSpaceToScreenSpace;
import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Render2DUtil {
    public static void rect(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
        rectFilled(stack, x1, y1, x2, y2, color);
    }

    public static void arrow(MatrixStack matrixStack, float x, float y, Color color) {
        drawRectWithOutline(matrixStack, x - 1f, y - 1f, 2, 2, color, Color.BLACK);
    }

    public static void rectFilled(MatrixStack matrix, float x1, float y1, float x2, float y2, int color) {
        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float j = (float) (color & 255) / 255.0F;
        if (f <= 0.01) return;

        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y2, 0.0F).color(g, h, j, f);
        bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y2, 0.0F).color(g, h, j, f);
        bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x2, y1, 0.0F).color(g, h, j, f);
        bufferBuilder.vertex(matrix.peek().getPositionMatrix(), x1, y1, 0.0F).color(g, h, j, f);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, Color startColor, Color endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor.getRGB());
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor.getRGB());
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor.getRGB());
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void horizontalGradient(MatrixStack matrices, float x1, float y1, float x2, float y2, int startColor, int endColor) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(endColor);
        bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(endColor);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }
    

    public static void drawRectWithOutline(MatrixStack matrices, float x, float y, float width, float height, Color c, Color c2) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(c.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, int c) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y + height, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x + width, y + height, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x + width, y, 0.0F).color(c);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(c);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }
    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, int c, double radius) {
        drawRect(matrices, x, y, width, height, new Color(c, true), radius);
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color color, double radius) {
        renderRoundedQuad(matrices, color, (double)x, (double)y, (double)(x + width), (double)(y + height), radius, 4.0);
    }

    public static void renderRoundedQuad(MatrixStack matrices, Color c, double fromX, double fromY, double toX, double toY, double radius, double samples) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        renderRoundedRect(matrices.peek().getPositionMatrix(),
            (float)c.getRed() / 255.0F, 
            (float)c.getGreen() / 255.0F, 
            (float)c.getBlue() / 255.0F, 
            (float)c.getAlpha() / 255.0F,
                (float) fromX, (float) fromY, (float) toX, (float) toY, (float) radius, (int) samples);
        RenderSystem.disableBlend();
    }

    public static void renderRoundedRect(
            Matrix4f m,
            float r, float g, float b, float a,
            float x1, float y1, float x2, float y2,
            float radius, int samples
    ) {
        radius = Math.min(radius, Math.min(x2 - x1, y2 - y1) * 0.5f);

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // 中心
        quad(buf, m,
                x1 + radius, y1 + radius,
                x2 - radius, y2 - radius,
                r, g, b, a);

        // 上下边
        quad(buf, m, x1 + radius, y1, x2 - radius, y1 + radius, r, g, b, a);
        quad(buf, m, x1 + radius, y2 - radius, x2 - radius, y2, r, g, b, a);

        // 左右边
        quad(buf, m, x1, y1 + radius, x1 + radius, y2 - radius, r, g, b, a);
        quad(buf, m, x2 - radius, y1 + radius, x2, y2 - radius, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        // 四个圆角（每个单独 fan）
        drawCorner(m, x2 - radius, y1 + radius, radius, 270, 360, samples, r, g, b, a);
        drawCorner(m, x2 - radius, y2 - radius, radius,   0,  90, samples, r, g, b, a);
        drawCorner(m, x1 + radius, y2 - radius, radius,  90, 180, samples, r, g, b, a);
        drawCorner(m, x1 + radius, y1 + radius, radius, 180, 270, samples, r, g, b, a);
    }
    private static void drawCorner(
            Matrix4f m,
            float cx, float cy,
            float rds,
            int startDeg, int endDeg,
            int samples,
            float r, float g, float b, float a
    ) {
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        buf.vertex(m, cx, cy, 0).color(r, g, b, a);

        for (int i = 0; i <= samples; i++) {
            double ang = Math.toRadians(startDeg + (double) ((endDeg - startDeg) * i) / samples);
            buf.vertex(
                    m,
                    (float)(cx + Math.cos(ang) * rds),
                    (float)(cy + Math.sin(ang) * rds),
                    0
            ).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
    private static void quad(
            BufferBuilder b, Matrix4f m,
            float x1, float y1, float x2, float y2,
            float r, float g, float bl, float a
    ) {
        b.vertex(m, x1, y1, 0).color(r, g, bl, a);
        b.vertex(m, x2, y1, 0).color(r, g, bl, a);
        b.vertex(m, x2, y2, 0).color(r, g, bl, a);
        b.vertex(m, x1, y2, 0).color(r, g, bl, a);
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color c) {
        drawRect(matrices, x, y, width, height, c.getRGB());
    }

    public static void drawRect(DrawContext drawContext, float x, float y, float width, float height, Color c) {
        drawRect(drawContext.getMatrices(), x, y, width, height, c);
        //drawContext.fill((int) x, (int) y, (int) (x + width), (int) (y + height), c.getRGB());
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
    }

    public static void drawGlow(MatrixStack matrices, float x, float y, float width, float height, int color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int startColor = injectAlpha(color, 20);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float halfWidth = width / 2.0F;
        float halfHeight = height / 2.0F;
        float centerX = x + halfWidth;
        float centerY = y + halfHeight;
        float x2 = x + width;
        float y2 = y + height;

        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);

        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, centerX, y, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);

        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, centerY, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x, y2, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);

        bufferBuilder.vertex(matrix, centerX, centerY, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x2, centerY, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(startColor);
        bufferBuilder.vertex(matrix, centerX, y2, 0.0F).color(startColor);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
    public static int injectAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
    public static void drawCircleOutline(MatrixStack matrices, float radius, int color, int segments, float lineWidth) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(lineWidth);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = cx + (float) (Math.cos(angle) * radius);
            float y = cy + (float) (Math.sin(angle) * radius);
            buffer.vertex(matrix, x, y, 0).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
    }
    public static void drawLineToTop(MatrixStack matrices, Vec3d worldPos, int color) {
        Vec3d screen = worldSpaceToScreenSpace(worldPos);

        // 判断是否在屏幕内（避免乱飞）
        if (screen.z < 0 || screen.z > 1) return;
        float endX = (float) screen.x;
        float endY = (float) screen.y;

        float startX = mc.getWindow().getScaledWidth() / 2f;
        float startY = 0f;

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        buffer.vertex(matrix, startX, startY, 0).color(r, g, b, a);
        buffer.vertex(matrix, endX, endY, 0).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
