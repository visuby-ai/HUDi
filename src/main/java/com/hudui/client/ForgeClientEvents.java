package com.hudui.client;

import com.hudui.HudUiMod;
import com.hudui.client.screen.HudSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Hides the vanilla heart/armor/food/air/experience-bar/hotbar/crosshair
 * overlays (we draw our own instead), and hides every overlay registered
 * under the "parcool" namespace so ParCool's own stamina bar doesn't double
 * up with our stamina ring. Also opens HudSettingsScreen when
 * KeyBindings.OPEN_HUD_SETTINGS is pressed.
 *
 * Special case: vanilla's HOTBAR overlay is also what draws the spectator
 * action bar — Gui#renderHotbar internally delegates to SpectatorGui when
 * the local player is in spectator mode instead of drawing item slots.
 * Blanket-cancelling HOTBAR therefore also killed the spectator action bar
 * (camera-target list / "press a key to teleport" hints). We only cancel
 * HOTBAR when the player is NOT a spectator, so our MinimalHotbarOverlay
 * replaces the normal hotbar but the vanilla spectator bar still works.
 */
@Mod.EventBusSubscriber(modid = HudUiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEvents {

    // Compared by id() (ResourceLocation equality) rather than only by
    // reference (== .type()) — belt-and-braces in case some overlay wrapper
    // isn't a cached singleton for a given vanilla entry.
    private static final Set<ResourceLocation> HIDDEN_VANILLA_IDS = Set.of(
            VanillaGuiOverlay.PLAYER_HEALTH.id(),
            VanillaGuiOverlay.ARMOR_LEVEL.id(),
            VanillaGuiOverlay.FOOD_LEVEL.id(),
            VanillaGuiOverlay.AIR_LEVEL.id(),
            VanillaGuiOverlay.EXPERIENCE_BAR.id(),
            VanillaGuiOverlay.HOTBAR.id(),
            VanillaGuiOverlay.CROSSHAIR.id()
    );

    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        NamedGuiOverlay overlay = event.getOverlay();
        ResourceLocation id = overlay.id();

        if ("parcool".equals(id.getNamespace())) {
            event.setCanceled(true);
            return;
        }

        if (HIDDEN_VANILLA_IDS.contains(id)) {
            if (id.equals(VanillaGuiOverlay.HOTBAR.id()) && isLocalPlayerSpectator()) {
                // Let vanilla draw the spectator action bar instead of our hotbar.
                return;
            }
            event.setCanceled(true);
        }
    }

    private static boolean isLocalPlayerSpectator() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (KeyBindings.OPEN_HUD_SETTINGS.consumeClick() && mc.screen == null) {
            mc.setScreen(new HudSettingsScreen());
        }
    }
}
