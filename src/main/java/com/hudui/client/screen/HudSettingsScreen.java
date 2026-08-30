package com.hudui.client.screen;

import com.hudui.Config;
import com.hudui.CrosshairFadeMode;
import com.hudui.CrosshairStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Simple in-game settings screen for hudui, opened via a keybinding
 * (Controls -> Key Binds -> HUD UI -> "Open HUD Settings", unbound by
 * default — bind it to whatever key you like).
 *
 * Every change here writes straight to Config; ForgeConfigSpec saves to
 * config/hudui-client.toml on every .set() call, so there's no separate
 * "Save" step — "Done" just closes the screen.
 */
public class HudSettingsScreen extends Screen {

    private static final int ROW_H = 24;
    private static final int WIDE_W = 200;
    private static final int STEP_W = 20;
    private static final int LABEL_W = 120;

    // 15 rows total (see init()) + the gaps below each group + the Done button.
    private static final int TOTAL_CONTENT_HEIGHT = 15 * ROW_H + 8 + 12 + 12 + 20;

    private int centerX;
    private int contentTop;

    public HudSettingsScreen() {
        super(Component.literal("HUD UI Settings"));
    }

    @Override
    protected void init() {
        centerX = this.width / 2;
        contentTop = this.height / 2 - TOTAL_CONTENT_HEIGHT / 2;
        int y = contentTop;

        // --- crosshair ---
        addRenderableWidget(Button.builder(showCrosshairLabel(), b -> {
            Config.SHOW_CROSSHAIR.set(!Config.SHOW_CROSSHAIR.get());
            b.setMessage(showCrosshairLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(styleLabel(), b -> {
            Config.CROSSHAIR_STYLE.set(nextStyle(Config.CROSSHAIR_STYLE.get()));
            b.setMessage(styleLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        Button sizeLabelBtn = Button.builder(sizeLabel(), b -> {})
                .bounds(centerX - LABEL_W / 2, y, LABEL_W, 20).build();
        sizeLabelBtn.active = false;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            setCrosshairSize(-0.25);
            sizeLabelBtn.setMessage(sizeLabel());
        }).bounds(centerX - LABEL_W / 2 - STEP_W - 4, y, STEP_W, 20).build());
        addRenderableWidget(sizeLabelBtn);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            setCrosshairSize(0.25);
            sizeLabelBtn.setMessage(sizeLabel());
        }).bounds(centerX + LABEL_W / 2 + 4, y, STEP_W, 20).build());
        y += ROW_H;

        // --- crosshair auto-fade ---
        addRenderableWidget(Button.builder(fadeModeLabel(), b -> {
            Config.CROSSHAIR_FADE_MODE.set(nextFadeMode(Config.CROSSHAIR_FADE_MODE.get()));
            b.setMessage(fadeModeLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(targetPlayersOnlyLabel(), b -> {
            Config.CROSSHAIR_TARGET_PLAYERS_ONLY.set(!Config.CROSSHAIR_TARGET_PLAYERS_ONLY.get());
            b.setMessage(targetPlayersOnlyLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(targetIncludeBlocksLabel(), b -> {
            Config.CROSSHAIR_TARGET_INCLUDE_BLOCKS.set(!Config.CROSSHAIR_TARGET_INCLUDE_BLOCKS.get());
            b.setMessage(targetIncludeBlocksLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        Button idleLabelBtn = Button.builder(idleSecondsLabel(), b -> {})
                .bounds(centerX - LABEL_W / 2, y, LABEL_W, 20).build();
        idleLabelBtn.active = false;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            setIdleSeconds(-1);
            idleLabelBtn.setMessage(idleSecondsLabel());
        }).bounds(centerX - LABEL_W / 2 - STEP_W - 4, y, STEP_W, 20).build());
        addRenderableWidget(idleLabelBtn);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            setIdleSeconds(1);
            idleLabelBtn.setMessage(idleSecondsLabel());
        }).bounds(centerX + LABEL_W / 2 + 4, y, STEP_W, 20).build());
        y += ROW_H + 8;

        // --- hotbar ---
        addRenderableWidget(Button.builder(showHotbarLabel(), b -> {
            Config.SHOW_HOTBAR.set(!Config.SHOW_HOTBAR.get());
            b.setMessage(showHotbarLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(autoHideLabel(), b -> {
            Config.HOTBAR_AUTO_HIDE.set(!Config.HOTBAR_AUTO_HIDE.get());
            b.setMessage(autoHideLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        Button delayLabelBtn = Button.builder(delayLabel(), b -> {})
                .bounds(centerX - LABEL_W / 2, y, LABEL_W, 20).build();
        delayLabelBtn.active = false;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            setAutoHideSeconds(-1);
            delayLabelBtn.setMessage(delayLabel());
        }).bounds(centerX - LABEL_W / 2 - STEP_W - 4, y, STEP_W, 20).build());
        addRenderableWidget(delayLabelBtn);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            setAutoHideSeconds(1);
            delayLabelBtn.setMessage(delayLabel());
        }).bounds(centerX + LABEL_W / 2 + 4, y, STEP_W, 20).build());
        y += ROW_H + 12;

        // --- vitals rings ---
        addRenderableWidget(Button.builder(showHpRingLabel(), b -> {
            Config.SHOW_HP_RING.set(!Config.SHOW_HP_RING.get());
            b.setMessage(showHpRingLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(showArmorRingLabel(), b -> {
            Config.SHOW_ARMOR_RING.set(!Config.SHOW_ARMOR_RING.get());
            b.setMessage(showArmorRingLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(showFoodRingLabel(), b -> {
            Config.SHOW_FOOD_RING.set(!Config.SHOW_FOOD_RING.get());
            b.setMessage(showFoodRingLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(showStaminaRingLabel(), b -> {
            Config.SHOW_STAMINA_RING.set(!Config.SHOW_STAMINA_RING.get());
            b.setMessage(showStaminaRingLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H;

        addRenderableWidget(Button.builder(showAirRingLabel(), b -> {
            Config.SHOW_AIR_RING.set(!Config.SHOW_AIR_RING.get());
            b.setMessage(showAirRingLabel());
        }).bounds(centerX - WIDE_W / 2, y, WIDE_W, 20).build());
        y += ROW_H + 12;

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(centerX - 60, y, 120, 20).build());
    }

    private void setCrosshairSize(double delta) {
        double v = Config.CROSSHAIR_SIZE.get() + delta;
        v = Mth.clamp(v, 0.25, 5.0);
        v = Math.round(v * 100.0) / 100.0;
        Config.CROSSHAIR_SIZE.set(v);
    }

    private void setAutoHideSeconds(int delta) {
        int v = Mth.clamp(Config.HOTBAR_AUTO_HIDE_SECONDS.get() + delta, 1, 60);
        Config.HOTBAR_AUTO_HIDE_SECONDS.set(v);
    }

    private void setIdleSeconds(int delta) {
        int v = Mth.clamp(Config.CROSSHAIR_IDLE_SECONDS.get() + delta, 1, 30);
        Config.CROSSHAIR_IDLE_SECONDS.set(v);
    }

    private static CrosshairStyle nextStyle(CrosshairStyle current) {
        CrosshairStyle[] values = CrosshairStyle.values();
        int idx = (current.ordinal() + 1) % values.length;
        return values[idx];
    }

    private static CrosshairFadeMode nextFadeMode(CrosshairFadeMode current) {
        CrosshairFadeMode[] values = CrosshairFadeMode.values();
        int idx = (current.ordinal() + 1) % values.length;
        return values[idx];
    }

    private Component showCrosshairLabel() {
        return Component.literal("Show Crosshair: " + (Config.SHOW_CROSSHAIR.get() ? "ON" : "OFF"));
    }

    private Component styleLabel() {
        return Component.literal("Crosshair Style: " + Config.CROSSHAIR_STYLE.get().name());
    }

    private Component sizeLabel() {
        return Component.literal("Size: " + String.format("%.2f", Config.CROSSHAIR_SIZE.get()));
    }

    private Component showHotbarLabel() {
        return Component.literal("Show Hotbar: " + (Config.SHOW_HOTBAR.get() ? "ON" : "OFF"));
    }

    private Component autoHideLabel() {
        return Component.literal("Hotbar Auto-Hide: " + (Config.HOTBAR_AUTO_HIDE.get() ? "ON" : "OFF"));
    }

    private Component delayLabel() {
        return Component.literal("Auto-hide after: " + Config.HOTBAR_AUTO_HIDE_SECONDS.get() + "s");
    }

    private Component fadeModeLabel() {
        return Component.literal("Crosshair Fade: " + Config.CROSSHAIR_FADE_MODE.get().name());
    }

    private Component targetPlayersOnlyLabel() {
        return Component.literal("Target: " + (Config.CROSSHAIR_TARGET_PLAYERS_ONLY.get() ? "Players Only" : "Any Living Entity"));
    }

    private Component targetIncludeBlocksLabel() {
        return Component.literal("Target Blocks Too: " + (Config.CROSSHAIR_TARGET_INCLUDE_BLOCKS.get() ? "ON" : "OFF"));
    }

    private Component idleSecondsLabel() {
        return Component.literal("Idle after: " + Config.CROSSHAIR_IDLE_SECONDS.get() + "s");
    }

    private Component showHpRingLabel() {
        return Component.literal("HP Ring: " + (Config.SHOW_HP_RING.get() ? "ON" : "OFF"));
    }

    private Component showArmorRingLabel() {
        return Component.literal("Armor Ring: " + (Config.SHOW_ARMOR_RING.get() ? "ON" : "OFF"));
    }

    private Component showFoodRingLabel() {
        return Component.literal("Food Ring: " + (Config.SHOW_FOOD_RING.get() ? "ON" : "OFF"));
    }

    private Component showStaminaRingLabel() {
        return Component.literal("Stamina Ring: " + (Config.SHOW_STAMINA_RING.get() ? "ON" : "OFF"));
    }

    private Component showAirRingLabel() {
        return Component.literal("Air Ring: " + (Config.SHOW_AIR_RING.get() ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, centerX, contentTop - 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
