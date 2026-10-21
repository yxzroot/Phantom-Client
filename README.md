# Phantom Client

Minecraft 1.8.9 client patches developed against a locally installed Raven-derived Forge mod on Lunar Client. The repository includes the **exact patched JAR copied from the installed mod** and the separately developed patch sources. It is **not** a standalone, one-command build; the source archive does not include all upstream sources needed to reproduce the binary.

## Downloads

[![Download latest JAR](https://img.shields.io/badge/Download-latest%20JAR-385A45?style=for-the-badge)](https://github.com/yxzroot/Phantom-Client/releases/latest/download/Phantom-Client-1.8.9.jar)
[![Download latest patch sources ZIP](https://img.shields.io/badge/Download-latest%20patch%20sources%20ZIP-344B73?style=for-the-badge)](https://github.com/yxzroot/Phantom-Client/archive/refs/heads/main.zip)

The repository's copy is at [`dist/Phantom-Client-1.8.9.jar`](dist/Phantom-Client-1.8.9.jar); its SHA-256 is `b2fa7f51a5589a3122336ddae9503513da0811a765040a1b65575244223f14c7`. The source ZIP button downloads the **latest `main` branch**, including the JAR and Phantom patch sources. It does **not** include the complete original Raven B+/B++ source tree.

## Current build

- Dynamic Island with compact and expanded lyrics, Unicode text rendering, playback synchronization and a local Riff bridge.
- SkyChanger with animated skies, aurora, Overcast Dusk, additional sky styles, stars, cloud controls and local world-border visibility.
- SwordGlow, Trail, WorldParticles and KillEffect with configurable colors and styles.
- AspectRatio with a world-only projection override; AimAssist updates and smoother Airstrafe direction changes.
- Shared color picker, GUI preferences, config creation/listing/scrolling fixes and the existing HUD integration.

Feature notes and targeted build/test scripts are included in the root Markdown files and `tools/`. See [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES) and `licenses/` for the adapted AirClient visual code's attribution and license details.

## Install with Wraith

Wraith recognizes this repository through its root [`launcher.json`](launcher.json). Choose **Phantom Client** in Wraith, click **Install**, and select a local Minecraft Forge 1.8.9 profile. Wraith downloads the latest published GitHub Release and copies it into that profile’s `mods` folder; it never starts Minecraft automatically.

For the tested setup, close Minecraft, place the JAR in Lunar Client's Minecraft **1.8.9 Forge mods** folder, and avoid loading a second copy of the same Raven-derived mod at the same time. Compatibility with other clients, installations, or servers has not been established.

## Credits and origins

- **Phantom Client started from a build shared by [Raindots on YouTube](https://www.youtube.com/@raindots)**. I further developed that build into this project. Raindots's build was based on Raven B+; credit for sharing that starting build belongs to Raindots.
- **Raven B+** was created by [Kopamed](https://github.com/Kopamed) ([original Raven B+ project](https://github.com/Kopamed/Raven-bPLUS)). Credit for the original client belongs to its creator and contributors.
- Some local development sources also derive from **Raven B++**, the continuation by [K-ov and contributors](https://github.com/K-ov/Raven). Credit for Raven B++ and its existing features belongs to that project.
- Phantom Client is an independent set of local additions and modifications; it is **not** an official release from Raindots or either upstream project and is not affiliated with Lunar Client or Hypixel.

## Patch sources

The files under `src/` cover the locally developed Phantom HUD and Poppins TTF renderer, bridge/scaffold helpers (including the Legit Scaffold Auto Aim controller), the retained-state GRAVE ClickGUI rebuild, Freelook integration, player-only hitbox outline, compact animated CPS pill and draggable editor, color-picker UI, HUD animation, click-sound changes, and related mixins. `tools/` contains utilities for MCP-to-SRG remapping, patching mapped classes into an installed JAR, Phantom HUD patching, selected class-file validation, and the Auto Clicker’s sword-only weapon filter.

These sources describe changes to a specific **already patched** Raven-compatible mod. They reference classes and hooks not all present in a clean upstream Raven B++ checkout, notably its Apple GUI and local mixin registration. Additional porting and integration are required to build them against another version. Do not use the tools to redistribute upstream binaries without checking upstream permissions and licenses.

For local development, use your own lawful Minecraft 1.8.9/Forge environment, the corresponding MCP-to-SRG mappings, ASM, and a compatible Raven-derived base. Compile Java 8-compatible sources against that environment, integrate module/mixin registration and required GUI hooks, then verify bytecode and test in-game. `tools/BuildLunarModuleJar.java` only demonstrates remapping and registering **one** module; it does not build this entire project by itself.

No claim is made that movement automation or other behavior is undetectable or allowed on any particular server. Follow the rules of the servers you play on.

## License

The patch source here is distributed under GPL-3.0; see [LICENSE](LICENSE). Credits above do not replace the applicable licenses and notices for any upstream project used separately.
