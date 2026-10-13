# Phantom Client

Minecraft 1.8.9 client patch sources developed against a locally installed Raven-derived Forge mod on Lunar Client. This repository contains **patch sources and bytecode-remapping utilities only**. It does **not** contain a downloadable client, a Minecraft/Forge distribution, or a patched Raven JAR, and is **not** a standalone, one-command build.

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
