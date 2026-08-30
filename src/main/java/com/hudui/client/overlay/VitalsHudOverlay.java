package com.hudui.client.overlay;

import com.hudui.Config;
import com.hudui.HudUiMod;
import com.hudui.client.render.HudShapes;
import com.hudui.compat.ParCoolStaminaAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.ModList;

/**
 * Bottom-left FiveM-style HUD: ring gauges for HP (with a thin outer armor
 * ring), Food, Stamina (read directly from ParCool, greyed out while
 * exhausted — only drawn at all if ParCool is installed, otherwise the mod
 * still works fine and just skips this ring), and Air (only shown while
 * underwater / air isn't full, same as vanilla's bubble icons).
 *
 * Each ring can be turned off individually (Config.SHOW_HP_RING /
 * SHOW_ARMOR_RING / SHOW_FOOD_RING / SHOW_STAMINA_RING / SHOW_AIR_RING).
 * Whichever rings end up shown this frame are laid out left-to-right with
 * no gap left behind for a hidden one — e.g. turning off Food slides
 * Stamina/Air left to sit right after HP.
 *
 * Tweak layout here: RADIUS, MARGIN_LEFT, MARGIN_BOTTOM, GAUGE_GAP.
 */
public class VitalsHudOverlay implements IGuiOverlay {

    // ---- layout (easy to tweak) ----
    public static final float RADIUS = 9f;
    public static final float THICKNESS = 2.5f;
    public static final float MARGIN_LEFT = 14f;
    public static final float MARGIN_BOTTOM = 14f;
    public static final float GAUGE_GAP = 6f;

    private static final float ARMOR_RING_GAP = 1.5f;
    private static final float ARMOR_RING_THICKNESS = 1.5f;
    private static final int RING_SEGMENTS = 48;
    private static final float ICON_SIZE = 12f;
    private static final int ICON_TEX_SIZE = 64;

    private static final ResourceLocation ICON_HP =
            new ResourceLocation(HudUiMod.MOD_ID, "textures/gui/hp.png");
    private static final ResourceLocation ICON_FOOD =
            new ResourceLocation(HudUiMod.MOD_ID, "textures/gui/food.png");
    private static final ResourceLocation ICON_STAMINA =
            new ResourceLocation(HudUiMod.MOD_ID, "textures/gui/stamina.png");
    private static final ResourceLocation ICON_AIR =
            new ResourceLocation(HudUiMod.MOD_ID, "textures/gui/air.png");

    // ---- colors (ARGB) ----
    private static final int RING_BG = 0x66000000;
    private static final int ICON_COLOR = 0xFFFFFFFF;

    private static final int HP_COLOR = 0xFFE84C4C;
    private static final int ARMOR_COLOR = 0xFFC8C8D8;
    private static final int FOOD_COLOR = 0xFFFF9838;
    private static final int AIR_COLOR = 0xFF6FD6FF;
    private static final int STAMINA_COLOR = 0xFF57E878;
    private static final int STAMINA_EXHAUSTED_COLOR = 0xFF808080;

    private static boolean loggedError = false;

    // Computed once — ParCool doesn't get installed/removed mid-session.
    private static Boolean parCoolLoaded = null;

    private static boolean isParCoolLoaded() {
        if (parCoolLoaded == null) {
            try {
                parCoolLoaded = ModList.get().isLoaded("parcool");
            } catch (Throwable t) {
                parCoolLoaded = false;
            }
        }
        return parCoolLoaded;
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        try {
            renderInternal(gui, graphics, screenWidth, screenHeight);
        } catch (Throwable t) {
            if (!loggedError) {
                HudUiMod.LOGGER.error("hudui: VitalsHudOverlay failed to render", t);
                loggedError = true;
            }
        }
    }

    private void renderInternal(ForgeGui gui, GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;
        // Spectators have no HP/food/air/armor to show, and the ring gauges
        // would just sit there showing stale/irrelevant values.
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        boolean hasStamina = isParCoolLoaded();

        float diameter = RADIUS * 2f;
        float baseY = screenHeight - MARGIN_BOTTOM - RADIUS;

        int maxAir = player.getMaxAirSupply();
        boolean showAir = Config.SHOW_AIR_RING.get()
                && (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < maxAir);

        boolean showHp = Config.SHOW_HP_RING.get();
        boolean showFood = Config.SHOW_FOOD_RING.get();
        boolean showStamina = hasStamina && Config.SHOW_STAMINA_RING.get();

        // Lay out only the rings that are actually shown this frame, left to
        // right with no gap left behind for a hidden one.
        float nextCx = MARGIN_LEFT + RADIUS;

        if (showHp) {
            drawHpRing(graphics, player, nextCx, baseY);
            nextCx += diameter + GAUGE_GAP;
        }
        if (showFood) {
            drawFoodRing(graphics, player, nextCx, baseY);
            nextCx += diameter + GAUGE_GAP;
        }
        if (showStamina) {
            drawStaminaRing(graphics, player, nextCx, baseY);
            nextCx += diameter + GAUGE_GAP;
        }
        if (showAir) {
            drawAirRing(graphics, player, maxAir, nextCx, baseY);
        }
    }

    private void drawHpRing(GuiGraphics graphics, Player player, float cx, float cy) {
        float hp = player.getMaxHealth() > 0f ? Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f) : 0f;
        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f, RING_SEGMENTS, RING_BG);
        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f * hp, RING_SEGMENTS, HP_COLOR);

        if (Config.SHOW_ARMOR_RING.get()) {
            float armor = Mth.clamp(player.getArmorValue() / 20f, 0f, 1f);
            if (armor > 0f) {
                float armorOuter = RADIUS + ARMOR_RING_GAP + ARMOR_RING_THICKNESS;
                HudShapes.ringArc(graphics, cx, cy, armorOuter, ARMOR_RING_THICKNESS, 0f, 360f, RING_SEGMENTS, RING_BG);
                HudShapes.ringArc(graphics, cx, cy, armorOuter, ARMOR_RING_THICKNESS, 0f, 360f * armor, RING_SEGMENTS, ARMOR_COLOR);
            }
        }
        HudShapes.blitIcon(graphics, ICON_HP, cx, cy, ICON_SIZE, ICON_COLOR, ICON_TEX_SIZE);
    }

    private void drawFoodRing(GuiGraphics graphics, Player player, float cx, float cy) {
        float food = Mth.clamp(player.getFoodData().getFoodLevel() / 20f, 0f, 1f);
        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f, RING_SEGMENTS, RING_BG);
        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f * food, RING_SEGMENTS, FOOD_COLOR);
        HudShapes.blitIcon(graphics, ICON_FOOD, cx, cy, ICON_SIZE, ICON_COLOR, ICON_TEX_SIZE);
    }

    private void drawStaminaRing(GuiGraphics graphics, Player player, float cx, float cy) {
        float staminaRatio = ParCoolStaminaAccess.getRatio(player);
        boolean exhausted = ParCoolStaminaAccess.isExhausted(player);
        int staminaColor = exhausted ? STAMINA_EXHAUSTED_COLOR : STAMINA_COLOR;

        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f, RING_SEGMENTS, RING_BG);
        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f * staminaRatio, RING_SEGMENTS, staminaColor);
        HudShapes.blitIcon(graphics, ICON_STAMINA, cx, cy, ICON_SIZE, staminaColor, ICON_TEX_SIZE);
    }

    private void drawAirRing(GuiGraphics graphics, Player player, int maxAir, float cx, float cy) {
        float air = maxAir > 0 ? Mth.clamp(player.getAirSupply() / (float) maxAir, 0f, 1f) : 1f;
        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f, RING_SEGMENTS, RING_BG);
        HudShapes.ringArc(graphics, cx, cy, RADIUS, THICKNESS, 0f, 360f * air, RING_SEGMENTS, AIR_COLOR);
        HudShapes.blitIcon(graphics, ICON_AIR, cx, cy, ICON_SIZE, ICON_COLOR, ICON_TEX_SIZE);
    }
}
