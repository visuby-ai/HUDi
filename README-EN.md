# HUD UI
### Forge 1.20.1 + ParCool

This mod was put together with AI assistance. I handled the idea, direction, and testing, while AI helped build the code and supporting systems.

It replaces the default Minecraft HUD with three circular gauges in the bottom-left corner:

- **HP** — A red health gauge with a heart icon and `HP` label. A thin outer ring shows your current **armor** value.
- **Food** — A blue food gauge that uses Minecraft's normal hunger value.
- **Stamina** — A green stamina gauge with a lightning icon. It reads directly from ParCool through the `com.alrex.parcool.api.Stamina` API. The gauge turns gray when stamina is exhausted.

The mod also:

- Hides Minecraft's default health, armor, hunger, and XP bars.
- Hides ParCool's built-in stamina HUD so it does not overlap with this one.
- Replaces the vanilla hotbar with a minimal, translucent black version with clipped corners. The selected slot has a thin highlighted border.

## Why the previous build did not work

The earlier ZIP was missing several files required for Forge to load and render the mod. This was more than a small bug:

- `HudUiMod.java` was missing. This is the main `@Mod` class, so Forge could not detect the mod at all.
- `ClientModEvents.java` was missing. This file registers the overlays.
- `ForgeClientEvents.java` was missing. This is responsible for hiding the vanilla and ParCool HUD elements.
- `HudShapes.java` was missing. Both overlays need it to draw their shapes.
- `ParCoolStaminaAccess.java` was missing.
- `src/main/resources/META-INF/mods.toml` was missing. Forge uses this manifest file to identify and load the mod.
- The two remaining files, `VitalsHudOverlay.java` and `MinimalHotbarOverlay.java`, were incomplete placeholders. They calculated values but did not actually render anything.

Those files have now been recreated and implemented in this project. The circular gauges and hotbar are drawn with a vertex buffer instead of image textures, which keeps the mod lightweight and avoids missing-asset issues.

The ParCool API was checked directly against the included JAR. The mod uses `Stamina.get()`, `getValue()`, `getMaxValue()`, and `isExhausted()` to stay compatible with the installed version.

## Project structure

```text
mod/
├── build.gradle
├── gradle.properties
├── libs/
│   └── ParCool-1.20.1-3.4.3.3.jar
├── src/main/resources/
│   ├── META-INF/
│   │   └── mods.toml
│   └── pack.mcmeta
└── src/main/java/com/hudui/
    ├── HudUiMod.java
    ├── client/
    │   ├── ClientModEvents.java
    │   ├── ForgeClientEvents.java
    │   ├── overlay/
    │   │   ├── VitalsHudOverlay.java
    │   │   └── MinimalHotbarOverlay.java
    │   └── render/
    │       └── HudShapes.java
    └── compat/
        └── ParCoolStaminaAccess.java
```

## Building the mod

You will need an internet connection the first time you build the project, since Gradle has to download the required Minecraft and Forge dependencies.

```bash
cd mod
./gradlew build
```

The finished JAR will be created here:

```text
build/libs/hudui-1.0.0.jar
```

Place it in your Minecraft `mods` folder together with `ParCool-1.20.1-3.4.3.3.jar`.

ParCool is required. The dependency is declared in `mods.toml`, so Minecraft will not load the mod unless ParCool is installed.

> Note: This project has not been compiled in the original build environment because network access was restricted there. The code was written against the Forge 1.20.1 API and the included ParCool JAR, but you should still build it locally and check the log for any errors.

The first Gradle build may take a while because it needs to download and process Minecraft files.

## Customization

If you want to adjust the layout or colors:

- `VitalsHudOverlay.java` contains settings for `RADIUS`, `MARGIN_LEFT`, `MARGIN_BOTTOM`, `GAUGE_GAP`, and the gauge colors.
- `MinimalHotbarOverlay.java` contains `SLOT_SIZE`, `SLOT_GAP`, `SLOT_BG`, `SELECTED_BG`, and `SELECTED_BORDER`.

## Notes

- The HP, food, and stamina icons are drawn as simple vector shapes in code. No PNG textures are used.
- If you want to replace them with custom icons later, texture support can be added.
- If the mod crashes or the HUD does not appear after building, check `run/logs/latest.log`. If Minecraft creates a crash report, include the files from `crash-reports/` as well. The logs will make it much easier to find the actual issue.