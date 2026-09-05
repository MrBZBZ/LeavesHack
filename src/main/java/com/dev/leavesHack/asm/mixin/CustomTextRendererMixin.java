package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.utils.skid.FontFix;
import java.io.IOException;
import java.nio.ByteBuffer;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.renderer.text.CustomTextRenderer.SHADOW_COLOR;

@Mixin(value = CustomTextRenderer.class, remap = false)
public abstract class CustomTextRendererMixin implements TextRenderer {

    @Shadow
    @Final
    private MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);


    private FontFix[] fonts_fix;
    private FontFix font_fix;

    @Shadow
    private boolean building;
    @Shadow
    private boolean scaleOnly;
    @Shadow
    private double fontScale = 1;
    @Shadow
    private double scale = 1;


    @Inject(method = "<init>",at = @At("RETURN"))
    public void onInit(FontFace fontFace, CallbackInfo ci) throws IOException {

        ByteBuffer buffer = fontFace.readToDirectByteBuffer();

        fonts_fix = new FontFix[5];
        for (int i = 0; i < fonts_fix.length; i++) {
            fonts_fix[i] = new FontFix(buffer, (int) Math.round(27 * ((i * 0.5) + 1)));
        }
    }



    /**
     * @author Nippaku_Zanmu
     * @reason  我只能用这种方法修复他 之前尝试过Mixin Font类 但是字体会乱码
     */
    @Overwrite
    public void begin(double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");

        if (!scaleOnly) mesh.begin();

        if (big) {
            this.font_fix = fonts_fix[fonts_fix.length - 1];
        }
        else {
            double scaleA = Math.floor(scale * 10) / 10;

            int scaleI;
            if (scaleA >= 3) scaleI = 5;
            else if (scaleA >= 2.5) scaleI = 4;
            else if (scaleA >= 2) scaleI = 3;
            else if (scaleA >= 1.5) scaleI = 2;
            else scaleI = 1;

            font_fix = fonts_fix[scaleI - 1];
        }

        this.building = true;
        this.scaleOnly = scaleOnly;

        this.fontScale = font_fix.getHeight() / 27.0;
        this.scale = 1 + (scale - fontScale) / fontScale;
    }
    /**
     * @author Nippaku_Zanmu
     * @reason  我只能用这种方法修复他 之前尝试过Mixin Font类 但是字体会乱码
     */
    @Overwrite
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) return 0;

        FontFix font = building ? this.font_fix : fonts_fix[0];
        return (font.getWidth(text, length) + (shadow ? 1 : 0)) * scale / 1.5;
    }
    /**
     * @author Nippaku_Zanmu
     * @reason  我只能用这种方法修复他 之前尝试过Mixin Font类 但是字体会乱码
     */
    @Overwrite
    public double getHeight(boolean shadow) {
        FontFix font = building ? this.font_fix : fonts_fix[0];
        return (font.getHeight() + 1 + (shadow ? 1 : 0)) * scale / 1.5;
    }
    /**
     * @author Nippaku_Zanmu
     * @reason  我只能用这种方法修复他 之前尝试过Mixin Font类 但是字体会乱码
     */
    @Overwrite
    public double render(String text, double x, double y, Color color, boolean shadow) {
        boolean wasBuilding = building;
        if (!wasBuilding) begin();

        double width;
        if (shadow) {
            int preShadowA = SHADOW_COLOR.a;
            SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            width = font_fix.render(mesh, text, x + fontScale * scale / 1.5, y + fontScale * scale / 1.5, SHADOW_COLOR, scale / 1.5);
            font_fix.render(mesh, text, x, y, color, scale / 1.5);

            SHADOW_COLOR.a = preShadowA;
        }
        else {
            width = font_fix.render(mesh, text, x, y, color, scale / 1.5);
        }

        if (!wasBuilding) end();
        return width;
    }

    /**
     * @author Nippaku_Zanmu
     * @reason  我只能用这种方法修复他 之前尝试过Mixin Font类 但是字体会乱码
     */
    @Overwrite
    public void end() {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        if (!scaleOnly) {
            mesh.end();

            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().getMainRenderTarget())
                .pipeline(MeteorRenderPipelines.UI_TEXT)
                .mesh(mesh)
                .sampler("u_Texture", font_fix.texture.getTextureView(), font_fix.texture.getSampler())
                .end();
        }

        building = false;
        scale = 1;
    }

    public void destroy() {}
}
