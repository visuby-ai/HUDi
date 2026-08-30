package com.hudui;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side config, generated at config/hudui-client.toml on first launch.
 * Edit that file (or use a mod-config-menu style mod in-game) to change
 * these; the values are re-read live, no restart needed.
 */
public class Config {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_HOTBAR;
    public static final ForgeConfigSpec.BooleanValue HOTBAR_AUTO_HIDE;
    public static final ForgeConfigSpec.IntValue HOTBAR_AUTO_HIDE_SECONDS;
    public static final ForgeConfigSpec.BooleanValue SHOW_CROSSHAIR;
    public static final ForgeConfigSpec.EnumValue<CrosshairStyle> CROSSHAIR_STYLE;
    public static final ForgeConfigSpec.DoubleValue CROSSHAIR_SIZE;

    public static final ForgeConfigSpec.EnumValue<CrosshairFadeMode> CROSSHAIR_FADE_MODE;
    public static final ForgeConfigSpec.BooleanValue CROSSHAIR_TARGET_PLAYERS_ONLY;
    public static final ForgeConfigSpec.BooleanValue CROSSHAIR_TARGET_INCLUDE_BLOCKS;
    public static final ForgeConfigSpec.IntValue CROSSHAIR_IDLE_SECONDS;

    public static final ForgeConfigSpec.BooleanValue SHOW_HP_RING;
    public static final ForgeConfigSpec.BooleanValue SHOW_ARMOR_RING;
    public static final ForgeConfigSpec.BooleanValue SHOW_FOOD_RING;
    public static final ForgeConfigSpec.BooleanValue SHOW_STAMINA_RING;
    public static final ForgeConfigSpec.BooleanValue SHOW_AIR_RING;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("hud");

        SHOW_HOTBAR = BUILDER
                .comment("If true, shows the custom hotbar. If false, hides it entirely (no hotbar at all, vanilla's is also replaced).")
                .define("showHotbar", true);

        HOTBAR_AUTO_HIDE = BUILDER
                .comment(
                        "If true, the hotbar smoothly fades out after a few seconds of not switching slots,",
                        "and slides back in as soon as you switch slots again.",
                        "If false, the hotbar always stays visible.")
                .define("hotbarAutoHide", true);

        HOTBAR_AUTO_HIDE_SECONDS = BUILDER
                .comment("Seconds of not switching hotbar slots before it starts fading out. Only used if hotbarAutoHide is true.")
                .defineInRange("hotbarAutoHideSeconds", 5, 1, 60);

        SHOW_CROSSHAIR = BUILDER
                .comment("If true, shows a crosshair in the center of the screen. If false, hides it entirely.")
                .define("showCrosshair", true);

        CROSSHAIR_STYLE = BUILDER
                .comment(
                        "Crosshair style:",
                        "  CROSS  - small plus sign (default)",
                        "  DOT    - single small dot",
                        "  CIRCLE - thin ring",
                        "  CUSTOM - your own image. Put a PNG at config/hudui/custom_crosshair.png",
                        "           (create the 'hudui' folder yourself if it isn't there). Any size/aspect",
                        "           ratio works — it's scaled by crosshairSize below. Swapping the file",
                        "           while the game is running is picked up automatically, no restart needed.",
                        "           If the file is missing or fails to load, this falls back to CROSS.")
                .defineEnum("crosshairStyle", CrosshairStyle.CROSS);

        CROSSHAIR_SIZE = BUILDER
                .comment("Crosshair size multiplier. 1.0 = default size, 2.0 = double size, 0.5 = half size, etc.")
                .defineInRange("crosshairSize", 1.0, 0.25, 5.0);

        CROSSHAIR_FADE_MODE = BUILDER
                .comment(
                        "When (if ever) the crosshair should smoothly fade out on its own:",
                        "  OFF           - always fully visible (default)",
                        "  AIM_AT_TARGET - only visible while aiming at a target; fades out otherwise",
                        "  IDLE          - fades out after crosshairIdleSeconds of not moving the camera,",
                        "                  fades back in the instant you look around again",
                        "  AIM_OR_IDLE   - visible if EITHER aiming at a target OR you moved the camera",
                        "                  recently; fades out only when neither is true")
                .defineEnum("crosshairFadeMode", CrosshairFadeMode.OFF);

        CROSSHAIR_TARGET_PLAYERS_ONLY = BUILDER
                .comment(
                        "Only used by AIM_AT_TARGET / AIM_OR_IDLE fade modes.",
                        "If true, only aiming at another PLAYER counts as \"aiming at a target\" (for entities).",
                        "If false, aiming at any living entity (mobs, animals, etc.) also counts.",
                        "Has no effect on block targeting below.")
                .define("crosshairTargetPlayersOnly", true);

        CROSSHAIR_TARGET_INCLUDE_BLOCKS = BUILDER
                .comment(
                        "Only used by AIM_AT_TARGET / AIM_OR_IDLE fade modes.",
                        "If true, aiming at a solid block ALSO counts as \"aiming at a target\", in addition",
                        "to entities/players above — so the crosshair appears whether you're aiming at a",
                        "person or at a block. If false, only entities count (per crosshairTargetPlayersOnly).")
                .define("crosshairTargetIncludeBlocks", false);

        CROSSHAIR_IDLE_SECONDS = BUILDER
                .comment("Only used by IDLE / AIM_OR_IDLE fade modes. Seconds of a completely still camera before it's considered idle.")
                .defineInRange("crosshairIdleSeconds", 3, 1, 30);

        BUILDER.pop();

        BUILDER.push("vitals");

        SHOW_HP_RING = BUILDER
                .comment("If true, shows the HP ring (bottom-left). If false, hides it entirely (and its armor ring, if any).")
                .define("showHpRing", true);

        SHOW_ARMOR_RING = BUILDER
                .comment("If true, shows the thin armor ring around the HP ring. Only has an effect while showHpRing is also true.")
                .define("showArmorRing", true);

        SHOW_FOOD_RING = BUILDER
                .comment("If true, shows the Food ring. If false, hides it entirely.")
                .define("showFoodRing", true);

        SHOW_STAMINA_RING = BUILDER
                .comment("If true, shows the Stamina ring (only ever drawn if ParCool is installed, regardless of this setting).")
                .define("showStaminaRing", true);

        SHOW_AIR_RING = BUILDER
                .comment("If true, allows the Air ring to show while underwater (same as vanilla's bubbles). If false, it never shows.")
                .define("showAirRing", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {}
}
