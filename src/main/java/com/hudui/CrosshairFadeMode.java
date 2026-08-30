package com.hudui;

/**
 * Controls when the crosshair smoothly fades out (see
 * Config.CROSSHAIR_FADE_MODE / CrosshairOverlay).
 */
public enum CrosshairFadeMode {
    /** Always fully visible — the old/default behavior. */
    OFF,
    /** Only visible while aiming at a target (see CROSSHAIR_TARGET_PLAYERS_ONLY); fades out otherwise. */
    AIM_AT_TARGET,
    /** Fades out after CROSSHAIR_IDLE_SECONDS of not moving the camera; fades back in as soon as you look around. */
    IDLE,
    /** Visible if EITHER aiming at a target OR the camera moved recently; fades out only when neither is true. */
    AIM_OR_IDLE
}
