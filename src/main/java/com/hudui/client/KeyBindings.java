package com.hudui.client;

import com.hudui.HudUiMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Unbound by default (Controls -> Key Binds -> HUD UI) so we don't steal a
 * key the player already relies on. ForgeClientEvents listens for it being
 * pressed and opens HudSettingsScreen.
 */
@Mod.EventBusSubscriber(modid = HudUiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {

    public static final KeyMapping OPEN_HUD_SETTINGS = new KeyMapping(
            "key.hudui.open_settings",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.hudui");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_HUD_SETTINGS);
    }
}
