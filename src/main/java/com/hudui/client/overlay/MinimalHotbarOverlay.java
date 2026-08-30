package com.hudui.client.overlay;

import com.hudui.Config;
import com.hudui.HudUiMod;
import com.hudui.client.render.HudShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Minimal dark hotbar: small-gap slots with rounded ("glassy") corners
 * (matches the reference screenshot the user sent — separated squares, not
 * flush). Each slot is one flat, semi-transparent rounded rect (a layered
 * multi-ring version was tried first but read as a visible double/triple
 * border instead of a soft blur, so this is intentionally simple). The
 * selected slot gets a soft rounded highlight instead of a hard square
 * border, to match.
 *
 * Config.SHOW_HOTBAR turns the whole thing off/on.
 *
 * Auto-hide: if Config.HOTBAR_AUTO_HIDE is on, the bar stays fully visible
 * for Config.HOTBAR_AUTO_HIDE_SECONDS after the last time the selected slot
 * changed, then smoothly slides down + fades out over FADE_MS. Switching
 * slots again (scroll / number keys) instantly brings it back. If
 * HOTBAR_AUTO_HIDE is off, it just always stays fully visible.
 *
 * Tweak layout here: SLOT_SIZE, SLOT_GAP, SLOT_RADIUS; colors: SLOT_BG,
 * SELECTED_HIGHLIGHT; fade timing: FADE_MS.
 */
public class MinimalHotbarOverlay implements IGuiOverlay {

    public static final int SLOT_SIZE = 20;
    private static final int SLOT_COUNT = 9;
    private static final int SLOT_GAP = 3;
    private static final int MARGIN_BOTTOM = 4;
    private static final float SLOT_RADIUS = 4f;
    private static final long FADE_MS = 400L;

    // One flat, semi-transparent fill per slot — no layering.
    private static final int SLOT_BG = 0x55282424;
    private static final int SELECTED_HIGHLIGHT = 0x4DFFFFFF;

    // If render() ever throws, we log it once (not every frame) so a crash-free
    // failure still shows up clearly in latest.log instead of silently leaving
    // the vanilla hotbar visible with no explanation.
    private static boolean loggedError = false;

    // Activity tracking for auto-hide (real time, not tick count, so the fade
    // is smooth regardless of tick rate / lag).
    private static Player trackedPlayer = null;
    private static int lastSelectedSlot = -1;
    private static long lastChangeTimeMs = 0L;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        try {
            renderInternal(gui, graphics, screenWidth, screenHeight);
        } catch (Throwable t) {
            if (!loggedError) {
                HudUiMod.LOGGER.error("hudui: MinimalHotbarOverlay failed to render", t);
                loggedError = true;
            }
        }
    }

    private void renderInternal(ForgeGui gui, GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!Config.SHOW_HOTBAR.get()) return;

        Minecraft mc = gui.getMinecraft();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        // Spectators get the vanilla spectator action bar in this same spot
        // (see ForgeClientEvents) instead of an item hotbar — don't draw our
        // slots on top of / instead of it.
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        int selected = player.getInventory().selected;
        long now = System.currentTimeMillis();

        // A new player instance (world (re)join / respawn) always starts
        // fully visible instead of possibly picking up a stale timer.
        if (player != trackedPlayer) {
            trackedPlayer = player;
            lastSelectedSlot = selected;
            lastChangeTimeMs = now;
        } else if (selected != lastSelectedSlot) {
            lastSelectedSlot = selected;
            lastChangeTimeMs = now;
        }

        float fade = 1f;
        if (Config.HOTBAR_AUTO_HIDE.get()) {
            long holdMs = Config.HOTBAR_AUTO_HIDE_SECONDS.get() * 1000L;
            long elapsed = now - lastChangeTimeMs;
            if (elapsed > holdMs) {
                fade = 1f - Mth.clamp((elapsed - holdMs) / (float) FADE_MS, 0f, 1f);
            }
        }
        if (fade <= 0.02f) return; // fully hidden — skip rendering entirely

        int totalWidth = SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * SLOT_GAP;
        float x0 = (screenWidth - totalWidth) / 2f;
        float y0 = screenHeight - MARGIN_BOTTOM - SLOT_SIZE;

        int slotBg = scaleAlpha(SLOT_BG, fade);
        int selectedHighlight = scaleAlpha(SELECTED_HIGHLIGHT, fade);
        // slides down by up to one slot height as it fades out, so the
        // close reads as a smooth motion rather than a sudden alpha pop
        float slideOffset = (1f - fade) * SLOT_SIZE;

        graphics.pose().pushPose();
        graphics.pose().translate(0, slideOffset, 0);

        for (int i = 0; i < SLOT_COUNT; i++) {
            float sx = x0 + i * (SLOT_SIZE + SLOT_GAP);

            HudShapes.roundedRect(graphics, sx, y0, SLOT_SIZE, SLOT_SIZE, SLOT_RADIUS, slotBg);

            if (i == selected) {
                HudShapes.roundedRect(graphics, sx + 1f, y0 + 1f, SLOT_SIZE - 2f, SLOT_SIZE - 2f, SLOT_RADIUS - 0.5f, selectedHighlight);
            }

            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                int ix = (int) sx + 2;
                int iy = (int) y0 + 2;
                graphics.renderItem(stack, ix, iy);
                graphics.renderItemDecorations(mc.font, stack, ix, iy);
            }
        }

        graphics.pose().popPose();
    }

    private static int scaleAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int newA = Mth.clamp(Math.round(a * factor), 0, 255);
        return (newA << 24) | (argb & 0x00FFFFFF);
    }
}
