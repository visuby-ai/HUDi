package com.hudui.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Small immediate-mode drawing helpers for shapes GuiGraphics doesn't
 * provide out of the box: filled circles, progress rings/arcs, filled
 * polygons and chamfered (cut-corner) rectangles. All colors are ARGB ints
 * (0xAARRGGBB), matching GuiGraphics' own fill()/drawString() convention.
 */
public final class HudShapes {
    private HudShapes() {}

    private static float alpha(int argb) { return ((argb >>> 24) & 0xFF) / 255f; }
    private static float red(int argb)   { return ((argb >>> 16) & 0xFF) / 255f; }
    private static float green(int argb) { return ((argb >>> 8) & 0xFF) / 255f; }
    private static float blue(int argb)  { return (argb & 0xFF) / 255f; }

    private static void prepare() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // Some other overlay drawn earlier this frame may have left the shader
        // color tinted (e.g. red hurt flash, potion tint). Reset it so our
        // per-vertex colors aren't multiplied by a leftover tint and come out
        // washed out / wrong.
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        // World rendering runs right before GUI overlays and leaves backface
        // culling ON. GuiGraphics' own fill()/blit() quietly disable it every
        // time, but our raw Tesselator draws bypass that — so any polygon
        // whose vertex winding comes out "back-facing" gets silently culled
        // (no error, it's just invisible). This is why the bolt icon (one
        // winding) showed up while the heart/drumstick/droplet circles and
        // the hotbar's chamfered rects (different winding) did not.
        RenderSystem.disableCull();
    }

    /**
     * Draws a square icon texture (expected to be a white/transparent PNG,
     * e.g. exported from an icon set) centered at (cx, cy), tinted with
     * colorArgb. The source PNG is assumed to be a texSize x texSize square
     * (our icons are pre-processed to 64x64).
     */
    public static void blitIcon(GuiGraphics graphics, ResourceLocation texture, float cx, float cy,
                                 float size, int colorArgb, int texSize) {
        int w = Math.round(size);
        int h = w;
        int x = Math.round(cx - size / 2f);
        int y = Math.round(cy - size / 2f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(red(colorArgb), green(colorArgb), blue(colorArgb), alpha(colorArgb));
        graphics.blit(texture, x, y, w, h, 0f, 0f, texSize, texSize, texSize, texSize);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    /** Fills an arbitrary convex(-ish) polygon given parallel x/y arrays, via a triangle fan from the centroid. */
    public static void fillPolygon(GuiGraphics graphics, float[] xs, float[] ys, int colorArgb) {
        if (xs.length < 3 || xs.length != ys.length) return;
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        prepare();

        float cx = 0f, cy = 0f;
        for (int i = 0; i < xs.length; i++) { cx += xs[i]; cy += ys[i]; }
        cx /= xs.length;
        cy /= ys.length;

        float r = red(colorArgb), g = green(colorArgb), b = blue(colorArgb), a = alpha(colorArgb);

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a).endVertex();
        for (int i = 0; i <= xs.length; i++) {
            int idx = i % xs.length;
            buffer.vertex(matrix, xs[idx], ys[idx], 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    /** Filled circle (solid disc). */
    public static void fillCircle(GuiGraphics graphics, float cx, float cy, float radius, int segments, int colorArgb) {
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        prepare();
        float r = red(colorArgb), g = green(colorArgb), b = blue(colorArgb), a = alpha(colorArgb);

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, cx, cy, 0).color(r, g, b, a).endVertex();
        for (int i = 0; i <= segments; i++) {
            double ang = 2.0 * Math.PI * i / segments;
            float x = cx + (float) (Math.sin(ang) * radius);
            float y = cy - (float) (Math.cos(ang) * radius);
            buffer.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    /**
     * Draws a ring/donut arc between innerRadius (= outerRadius - thickness) and outerRadius,
     * starting at startDeg (0 = 12 o'clock) and sweeping sweepDeg clockwise.
     * Pass sweepDeg = 360 for a full ring (used as the background track).
     */
    public static void ringArc(GuiGraphics graphics, float cx, float cy, float outerRadius, float thickness,
                                float startDeg, float sweepDeg, int segments, int colorArgb) {
        if (sweepDeg <= 0f) return;
        float innerRadius = Math.max(0f, outerRadius - thickness);

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        prepare();
        float r = red(colorArgb), g = green(colorArgb), b = blue(colorArgb), a = alpha(colorArgb);

        int steps = Math.max(1, Math.round(segments * (Math.min(sweepDeg, 360f) / 360f)));
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= steps; i++) {
            double deg = startDeg + sweepDeg * i / (double) steps;
            double rad = Math.toRadians(deg);
            float sx = (float) Math.sin(rad);
            float cyv = (float) Math.cos(rad);
            float ox = cx + sx * outerRadius, oy = cy - cyv * outerRadius;
            float ix = cx + sx * innerRadius, iy = cy - cyv * innerRadius;
            buffer.vertex(matrix, ox, oy, 0).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, ix, iy, 0).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    /** Filled rectangle with cut (chamfered) corners. chamfer is clamped to half the shorter side. */
    public static void chamferedRect(GuiGraphics graphics, float x, float y, float w, float h, float chamfer, int colorArgb) {
        float c = Math.max(0f, Math.min(chamfer, Math.min(w, h) / 2f));
        float[] xs = { x + c, x + w - c, x + w, x + w, x + w - c, x + c, x, x };
        float[] ys = { y, y, y + c, y + h - c, y + h, y + h, y + h - c, y + c };
        fillPolygon(graphics, xs, ys, colorArgb);
    }

    /**
     * Filled rectangle with true circular-arc rounded corners (unlike
     * chamferedRect, which cuts a straight bevel — that looks like an
     * octagon at larger radii; this looks like an actual rounded square).
     * radius is clamped to half the shorter side. 4 segments per 90-degree
     * corner is plenty smooth at HUD sizes.
     */
    public static void roundedRect(GuiGraphics graphics, float x, float y, float w, float h, float radius, int colorArgb) {
        float r = Math.max(0f, Math.min(radius, Math.min(w, h) / 2f));
        if (r < 0.5f) {
            chamferedRect(graphics, x, y, w, h, 0f, colorArgb);
            return;
        }

        final int segsPerCorner = 4;
        final int pointCount = (segsPerCorner + 1) * 4;
        float[] xs = new float[pointCount];
        float[] ys = new float[pointCount];

        // corner centers + the angle range (degrees) each corner's arc sweeps,
        // going clockwise in screen space (x right, y down) starting top-left.
        float[] centerX = { x + r, x + w - r, x + w - r, x + r };
        float[] centerY = { y + r, y + r, y + h - r, y + h - r };
        float[] startAngle = { 180f, 270f, 0f, 90f };

        int i = 0;
        for (int corner = 0; corner < 4; corner++) {
            for (int s = 0; s <= segsPerCorner; s++) {
                double angle = Math.toRadians(startAngle[corner] + 90.0 * s / segsPerCorner);
                xs[i] = centerX[corner] + r * (float) Math.cos(angle);
                ys[i] = centerY[corner] + r * (float) Math.sin(angle);
                i++;
            }
        }

        fillPolygon(graphics, xs, ys, colorArgb);
    }

    /** Small filled diamond, used as a simple heart-substitute icon. */
    public static void fillDiamond(GuiGraphics graphics, float cx, float cy, float radius, int colorArgb) {
        float[] xs = { cx, cx + radius, cx, cx - radius };
        float[] ys = { cy - radius, cy, cy + radius, cy };
        fillPolygon(graphics, xs, ys, colorArgb);
    }

    /** Small filled lightning-bolt icon, used for the stamina gauge. */
    public static void fillBolt(GuiGraphics graphics, float cx, float cy, float size, int colorArgb) {
        float[] xs = {
                cx + size * 0.15f, cx - size * 0.35f, cx + size * 0.05f,
                cx - size * 0.15f, cx + size * 0.35f, cx - size * 0.05f
        };
        float[] ys = {
                cy - size, cy + size * 0.1f, cy + size * 0.15f,
                cy + size, cy - size * 0.1f, cy - size * 0.15f
        };
        fillPolygon(graphics, xs, ys, colorArgb);
    }

    /** Small filled heart icon (two round lobes + a pointed bottom), used for the HP gauge. */
    public static void fillHeart(GuiGraphics graphics, float cx, float cy, float size, int colorArgb) {
        float lobeR = size * 0.34f;
        fillCircle(graphics, cx - lobeR * 0.85f, cy - lobeR * 0.35f, lobeR, 10, colorArgb);
        fillCircle(graphics, cx + lobeR * 0.85f, cy - lobeR * 0.35f, lobeR, 10, colorArgb);
        float[] xs = { cx - size * 0.82f, cx + size * 0.82f, cx };
        float[] ys = { cy - size * 0.18f, cy - size * 0.18f, cy + size * 0.82f };
        fillPolygon(graphics, xs, ys, colorArgb);
    }

    /** Small filled drumstick icon (round meat + angled bone), used for the Food gauge. */
    public static void fillDrumstick(GuiGraphics graphics, float cx, float cy, float size, int colorArgb) {
        fillCircle(graphics, cx - size * 0.18f, cy - size * 0.18f, size * 0.5f, 10, colorArgb);
        float[] xs = { cx + size * 0.05f, cx + size * 0.7f, cx + size * 0.5f, cx - size * 0.15f };
        float[] ys = { cy + size * 0.05f, cy + size * 0.7f, cy + size * 0.9f, cy + size * 0.25f };
        fillPolygon(graphics, xs, ys, colorArgb);
    }

    /** Small filled droplet/bubble icon (round base + pointed top), used for the Air gauge. */
    public static void fillDroplet(GuiGraphics graphics, float cx, float cy, float size, int colorArgb) {
        float r = size * 0.52f;
        fillCircle(graphics, cx, cy + size * 0.18f, r, 12, colorArgb);
        float[] xs = { cx - r * 0.65f, cx + r * 0.65f, cx };
        float[] ys = { cy - size * 0.05f, cy - size * 0.05f, cy - size * 0.9f };
        fillPolygon(graphics, xs, ys, colorArgb);
    }
}
