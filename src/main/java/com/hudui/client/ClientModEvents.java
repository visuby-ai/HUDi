package com.hudui.client;

import com.hudui.HudUiMod;
import com.hudui.client.overlay.CrosshairOverlay;
import com.hudui.client.overlay.MinimalHotbarOverlay;
import com.hudui.client.overlay.VitalsHudOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers our custom overlays on the mod event bus. This is the piece
 * that was missing before: VitalsHudOverlay/MinimalHotbarOverlay existed as
 * classes but nothing ever told Forge to render them.
 *
 * vitals_hud is registered *below* the chat panel (instead of above
 * everything) so that when chat is open/visible, the chat text draws on
 * top of our rings instead of our rings covering the chat.
 */
@Mod.EventBusSubscriber(modid = HudUiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "vitals_hud", new VitalsHudOverlay());
        event.registerAboveAll("minimal_hotbar", new MinimalHotbarOverlay());
        event.registerAboveAll("crosshair", new CrosshairOverlay());
    }
}
