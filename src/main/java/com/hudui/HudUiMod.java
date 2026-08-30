package com.hudui;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main mod entry point.
 *
 * This is a client-only HUD mod: all real work happens in
 * {@link com.hudui.client.ClientModEvents} (registers our overlays) and
 * {@link com.hudui.client.ForgeClientEvents} (hides the vanilla heart/armor/
 * food/exp bar/hotbar/crosshair and ParCool's own stamina overlay). Both of
 * those classes are wired up automatically via @Mod.EventBusSubscriber.
 *
 * The constructor registers {@link Config} (Forge then generates
 * config/hudui-client.toml itself the first time the game boots with this
 * mod installed — no manual step needed) and also makes sure the
 * config/hudui/ folder exists, so the CUSTOM crosshair option
 * (see {@link com.hudui.client.overlay.CrosshairOverlay}) works the moment
 * someone drops custom_crosshair.png in, without having to create the
 * folder by hand first.
 */
@Mod(HudUiMod.MOD_ID)
public class HudUiMod {
    public static final String MOD_ID = "hudui";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HudUiMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        createHuduiConfigFolder();
    }

    private static void createHuduiConfigFolder() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("hudui: failed to create config folder at " + dir, e);
        }
    }
}
