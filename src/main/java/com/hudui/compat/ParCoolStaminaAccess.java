package com.hudui.compat;

import com.alrex.parcool.api.Stamina;
import net.minecraft.world.entity.player.Player;

/**
 * Thin, defensive wrapper around ParCool's public API
 * (com.alrex.parcool.api.Stamina). ParCool is now an OPTIONAL dependency
 * (see mods.toml) — callers must check ModList.get().isLoaded("parcool")
 * before calling into this class (VitalsHudOverlay does this). Every method
 * here still also guards itself with try/catch(Throwable), which — since
 * Throwable covers Errors too — is enough on its own to survive even a
 * missing ParCool jar (NoClassDefFoundError included), so this class is
 * effectively safe to call either way; the ModList check upstream just
 * avoids wasted work and lets the stamina ring be skipped entirely.
 *
 * ParCool's actual API (verified from the jar):
 *   public static Stamina Stamina.get(Player player)  // @Nullable
 *   public int getMaxValue()
 *   public int getValue()
 *   public boolean isExhausted()
 */
public final class ParCoolStaminaAccess {
    private ParCoolStaminaAccess() {}

    /** Returns stamina as a 0..1 ratio. Returns 1 (full) if unavailable. */
    public static float getRatio(Player player) {
        try {
            Stamina stamina = Stamina.get(player);
            if (stamina == null) return 1f;
            int max = stamina.getMaxValue();
            if (max <= 0) return 1f;
            float ratio = stamina.getValue() / (float) max;
            if (ratio < 0f) return 0f;
            if (ratio > 1f) return 1f;
            return ratio;
        } catch (Throwable t) {
            return 1f;
        }
    }

    /** Returns true if ParCool currently reports the player as exhausted. */
    public static boolean isExhausted(Player player) {
        try {
            Stamina stamina = Stamina.get(player);
            return stamina != null && stamina.isExhausted();
        } catch (Throwable t) {
            return false;
        }
    }
}
