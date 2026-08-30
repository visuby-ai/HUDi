package com.hudui.client.overlay;

import com.hudui.Config;
import com.hudui.CrosshairFadeMode;
import com.hudui.CrosshairStyle;
import com.hudui.HudUiMod;
import com.hudui.client.render.HudShapes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Crosshair, replacing the vanilla one so it can be resized/restyled. Style,
 * size and on/off are all controlled from Config (config/hudui-client.toml):
 *   showCrosshair, crosshairStyle (CROSS/DOT/CIRCLE/CUSTOM), crosshairSize.
 *
 * CROSS blits vanilla Minecraft's own crosshair sprite (textures/gui/icons.png,
 * the same 15x15 region vanilla itself draws), just scaled by crosshairSize —
 * so it's pixel-for-pixel the real "default Minecraft" crosshair, not a
 * hand-drawn approximation. DOT/CIRCLE are custom shapes (vanilla doesn't
 * have those), drawn in plain white with no outline to match vanilla's
 * coloring (a hand-drawn version with a black outline was tried first, but
 * that isn't how vanilla actually looks).
 *
 * For CrosshairStyle.CUSTOM, drop a PNG at config/hudui/custom_crosshair.png
 * — it's loaded as a plain dynamic texture (not a resource pack asset), so
 * it works without a restart and the file is re-read automatically whenever
 * its last-modified time changes. Falls back to CROSS if that file is
 * missing or fails to load.
 *
 * Auto-fade (Config.CROSSHAIR_FADE_MODE): same idea as the hotbar's
 * auto-hide (real time, not tick count, so it's smooth regardless of tick
 * rate/lag). Every render() call we check two independent conditions —
 * "currently aiming at a target" (via Minecraft#hitResult) and "camera
 * hasn't rotated in a while" (tracked by comparing yaw/pitch against the
 * previous frame) — and combine them per CROSSHAIR_FADE_MODE into a
 * target alpha of 0 or 1. A single float smoothly chases that target alpha
 * at a fixed rate (FADE_MS to go fully 0->1), instead of snapping, so
 * switching targets or resuming/stopping movement never causes a hard pop.
 *
 * "Aiming at a target" (isAimingAtTarget) covers entities (players, or any
 * living entity — see CROSSHAIR_TARGET_PLAYERS_ONLY) and, if
 * CROSSHAIR_TARGET_INCLUDE_BLOCKS is enabled, solid blocks too — so the
 * crosshair can be set to appear whether you're aiming at a person or at
 * a block, independently of each other.
 */
public class CrosshairOverlay implements IGuiOverlay {

    private static final ResourceLocation VANILLA_ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int VANILLA_CROSSHAIR_SIZE = 15; // vanilla's crosshair sprite is 15x15
    private static final int VANILLA_ATLAS_SIZE = 256;    // icons.png is a 256x256 atlas

    private static final int BASE_DOT_SIZE = 2;
    private static final int BASE_CIRCLE_RADIUS = 4;
    private static final int BASE_CUSTOM_SIZE = 16;

    private static final int LINE_COLOR = 0xFFFFFFFF;

    private static final ResourceLocation CUSTOM_TEXTURE_ID =
            new ResourceLocation(HudUiMod.MOD_ID, "dynamic/custom_crosshair");

    private static boolean loggedError = false;
    private static boolean loggedCustomLoadError = false;

    // Custom-image cache: reloaded automatically if the file's mtime changes.
    private static DynamicTexture customTexture = null;
    private static long customTextureMtime = -1L;
    private static int customTexWidth = 0;
    private static int customTexHeight = 0;

    // --- auto-fade state ---
    private static final long FADE_MS = 250L;          // time for alpha to go fully 0 <-> 1
    private static final float IDLE_EPSILON_DEG = 0.15f; // camera rotation below this per-frame doesn't count as "moved"

    private static Player trackedPlayer = null;
    private static float lastYaw = 0f;
    private static float lastPitch = 0f;
    private static long lastMoveTimeMs = 0L;
    private static long lastFrameTimeMs = 0L;
    private static float currentAlpha = 1f;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        try {
            renderInternal(gui, graphics, screenWidth, screenHeight);
        } catch (Throwable t) {
            if (!loggedError) {
                HudUiMod.LOGGER.error("hudui: CrosshairOverlay failed to render", t);
                loggedError = true;
            }
        }
    }

    private void renderInternal(ForgeGui gui, GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!Config.SHOW_CROSSHAIR.get()) return;

        Minecraft mc = gui.getMinecraft();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;
        if (mc.screen != null) return;

        float alpha = updateFade(mc, player);
        if (alpha <= 0.02f) return; // fully hidden — skip drawing entirely

        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        float scale = (float) (double) Config.CROSSHAIR_SIZE.get();
        CrosshairStyle style = Config.CROSSHAIR_STYLE.get();

        if (style == CrosshairStyle.CUSTOM) {
            ResourceLocation tex = getOrLoadCustomTexture(mc);
            if (tex != null) {
                drawCustom(graphics, tex, cx, cy, scale, alpha);
                return;
            }
            style = CrosshairStyle.CROSS; // fall back if the file is missing/broken
        }

        switch (style) {
            case DOT -> drawDot(graphics, cx, cy, scale, alpha);
            case CIRCLE -> drawCircle(graphics, cx, cy, scale, alpha);
            default -> drawVanillaCross(graphics, cx, cy, scale, alpha);
        }
    }

    /**
     * Advances the smooth fade state by one frame and returns the alpha
     * (0..1) to draw the crosshair with this frame.
     */
    private static float updateFade(Minecraft mc, Player player) {
        long now = System.currentTimeMillis();

        // New player instance (world (re)join / respawn) always starts fully
        // visible and with a fresh idle timer, instead of picking up stale state.
        if (player != trackedPlayer) {
            trackedPlayer = player;
            lastYaw = player.getYRot();
            lastPitch = player.getXRot();
            lastMoveTimeMs = now;
            lastFrameTimeMs = now;
            currentAlpha = 1f;
        }

        CrosshairFadeMode mode = Config.CROSSHAIR_FADE_MODE.get();
        if (mode == CrosshairFadeMode.OFF) {
            currentAlpha = 1f;
            lastFrameTimeMs = now;
            return 1f;
        }

        // --- "has the camera moved recently" tracking ---
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float yawDelta = Math.abs(Mth.wrapDegrees(yaw - lastYaw));
        float pitchDelta = Math.abs(pitch - lastPitch);
        if (yawDelta > IDLE_EPSILON_DEG || pitchDelta > IDLE_EPSILON_DEG) {
            lastMoveTimeMs = now;
        }
        lastYaw = yaw;
        lastPitch = pitch;

        boolean idle = (now - lastMoveTimeMs) > Config.CROSSHAIR_IDLE_SECONDS.get() * 1000L;

        // --- "is the crosshair currently over a target" ---
        boolean aimingAtTarget = isAimingAtTarget(mc);

        boolean wantVisible = switch (mode) {
            case AIM_AT_TARGET -> aimingAtTarget;
            case IDLE -> !idle;
            case AIM_OR_IDLE -> aimingAtTarget || !idle;
            case OFF -> true; // unreachable, handled above
        };

        // --- chase the target alpha at a fixed rate, so it's always a smooth ramp ---
        long frameMs = Math.max(0L, now - lastFrameTimeMs);
        lastFrameTimeMs = now;
        float step = FADE_MS <= 0 ? 1f : frameMs / (float) FADE_MS;
        float target = wantVisible ? 1f : 0f;
        currentAlpha = Mth.clamp(currentAlpha + Math.signum(target - currentAlpha) * step, 0f, 1f);
        // snap once close enough so it doesn't asymptotically creep forever
        if (Math.abs(currentAlpha - target) < 0.01f) currentAlpha = target;

        return currentAlpha;
    }

    private static boolean isAimingAtTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;

        if (hit instanceof EntityHitResult entityHit) {
            if (Config.CROSSHAIR_TARGET_PLAYERS_ONLY.get()) {
                return entityHit.getEntity() instanceof Player;
            }
            return entityHit.getEntity() instanceof LivingEntity;
        }

        if (Config.CROSSHAIR_TARGET_INCLUDE_BLOCKS.get()
                && hit instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            return true;
        }

        return false;
    }

    private void drawVanillaCross(GuiGraphics graphics, int cx, int cy, float scale, float alpha) {
        int size = Math.max(1, Math.round(VANILLA_CROSSHAIR_SIZE * scale));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        graphics.blit(VANILLA_ICONS, cx - size / 2, cy - size / 2, size, size,
                0f, 0f, VANILLA_CROSSHAIR_SIZE, VANILLA_CROSSHAIR_SIZE, VANILLA_ATLAS_SIZE, VANILLA_ATLAS_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawDot(GuiGraphics graphics, int cx, int cy, float scale, float alpha) {
        int size = Math.max(1, Math.round(BASE_DOT_SIZE * scale));
        graphics.fill(cx - size, cy - size, cx + size, cy + size, scaleAlpha(LINE_COLOR, alpha));
    }

    private void drawCircle(GuiGraphics graphics, int cx, int cy, float scale, float alpha) {
        float radius = BASE_CIRCLE_RADIUS * scale;
        float thickness = Math.max(1f, scale);
        HudShapes.ringArc(graphics, cx, cy, radius, thickness, 0f, 360f, 24, scaleAlpha(LINE_COLOR, alpha));
    }

    private void drawCustom(GuiGraphics graphics, ResourceLocation tex, int cx, int cy, float scale, float alpha) {
        if (customTexWidth <= 0 || customTexHeight <= 0) return;
        float aspect = customTexWidth / (float) customTexHeight;
        int renderH = Math.max(1, Math.round(BASE_CUSTOM_SIZE * scale));
        int renderW = Math.max(1, Math.round(renderH * aspect));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        graphics.blit(tex, cx - renderW / 2, cy - renderH / 2, renderW, renderH,
                0f, 0f, customTexWidth, customTexHeight, customTexWidth, customTexHeight);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int newA = Mth.clamp(Math.round(a * factor), 0, 255);
        return (newA << 24) | (argb & 0x00FFFFFF);
    }

    private static Path customCrosshairPath() {
        return FMLPaths.CONFIGDIR.get().resolve("hudui").resolve("custom_crosshair.png");
    }

    /** Returns the ResourceLocation to draw, or null if no usable custom image is available. */
    private static ResourceLocation getOrLoadCustomTexture(Minecraft mc) {
        try {
            Path path = customCrosshairPath();
            if (!Files.isRegularFile(path)) return null;

            long mtime = Files.getLastModifiedTime(path).toMillis();
            if (customTexture == null || mtime != customTextureMtime) {
                try (InputStream in = Files.newInputStream(path)) {
                    NativeImage image = NativeImage.read(in);
                    DynamicTexture newTexture = new DynamicTexture(image);

                    DynamicTexture old = customTexture;
                    customTexture = newTexture;
                    customTexWidth = image.getWidth();
                    customTexHeight = image.getHeight();
                    customTextureMtime = mtime;
                    mc.getTextureManager().register(CUSTOM_TEXTURE_ID, customTexture);
                    if (old != null) old.close();
                }
            }
            return CUSTOM_TEXTURE_ID;
        } catch (Throwable t) {
            if (!loggedCustomLoadError) {
                HudUiMod.LOGGER.error("hudui: failed to load custom crosshair from " + customCrosshairPath(), t);
                loggedCustomLoadError = true;
            }
            return null;
        }
    }
}
