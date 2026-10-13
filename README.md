# Phantom Client

Minecraft 1.8.9 client patches developed against a locally installed Raven-derived Forge mod on Lunar Client. The repository includes the **exact patched JAR copied from the installed mod** and the separately developed patch sources. It is **not** a standalone, one-command build; the source archive does not include all upstream sources needed to reproduce the binary.

## Downloads

[![Download latest JAR](https://img.shields.io/badge/Download-latest%20JAR-385A45?style=for-the-badge)](https://github.com/yxzroot/Phantom-Client/releases/latest/download/Phantom-Client-1.8.9.jar)
[![Download latest patch sources ZIP](https://img.shields.io/badge/Download-latest%20patch%20sources%20ZIP-344B73?style=for-the-badge)](https://github.com/yxzroot/Phantom-Client/archive/refs/heads/main.zip)

The repository's copy is at [`dist/Phantom-Client-1.8.9.jar`](dist/Phantom-Client-1.8.9.jar); its SHA-256 is `c019d0e9bfd426818730ee8aa32cfdc23e308cbb3b78b449849f0e9d0d89d8a3`. The source ZIP button downloads the **latest `main` branch**, including the JAR and Phantom patch sources. It does **not** include the complete original Raven B+/B++ source tree.

For the tested setup, close Minecraft, place the JAR in Lunar Client's Minecraft **1.8.9 Forge mods** folder, and avoid loading a second copy of the same Raven-derived mod at the same time. Compatibility with other clients, installations, or servers has not been established.

## Credits and origins

- **Raven B+** was created by [Kopamed](https://github.com/Kopamed) ([original Raven B+ project](https://github.com/Kopamed/Raven-bPLUS)). Credit for the original client belongs to its creator and contributors.
- The local codebase used here descends from **Raven B++**, the continuation by [K-ov and contributors](https://github.com/K-ov/Raven). Credit for Raven B++ and its existing features belongs to that project.
- Phantom Client is an independent set of local additions and modifications; it is **not** an official release from either upstream project and is not affiliated with Lunar Client or Hypixel.

## Patch sources

The files under `src/` cover the locally developed bridge/scaffold helpers, Freelook integration, player-only hitbox outline, CPS counter and draggable editor, color-picker UI, HUD animation, click-sound changes, and related mixins. `tools/` contains utilities for MCP-to-SRG remapping and validating selected class files when modifying a compatible JAR.

These sources describe changes to a specific **already patched** Raven-compatible mod. They reference classes and hooks not all present in a clean upstream Raven B++ checkout, notably its Apple GUI and local mixin registration. Additional porting and integration are required to build them against another version. Do not use the tools to redistribute upstream binaries without checking upstream permissions and licenses.

For local development, use your own lawful Minecraft 1.8.9/Forge environment, the corresponding MCP-to-SRG mappings, ASM, and a compatible Raven-derived base. Compile Java 8-compatible sources against that environment, integrate module/mixin registration and required GUI hooks, then verify bytecode and test in-game. `tools/BuildLunarModuleJar.java` only demonstrates remapping and registering **one** module; it does not build this entire project by itself.

No claim is made that movement automation or other behavior is undetectable or allowed on any particular server. Follow the rules of the servers you play on.

## License

The patch source here is distributed under GPL-3.0; see [LICENSE](LICENSE). Credits above do not replace the applicable licenses and notices for any upstream project used separately.
