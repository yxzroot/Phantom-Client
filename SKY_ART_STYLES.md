# Original procedural SkyChanger styles

## Cherry tree projection repair

The previous 512×512 square profile spread across 360° made canopies look like flattened discs. `CherryTreeArt` replaces only that tree art with branching, multi-lobed original silhouettes drawn in angular degrees, rooted below the horizon with a mist fade. The regular profile is now 8192×256; its texels-per-degree are balanced between axes so ordinary mip selection does not smear the canopy horizontally. The cached low-quality/hardware fallback is 2048×64. Horizontal wrap is seamless; vertical UV is clamped and never repeats. Mipmaps and both quality variants are cached, with no per-frame art generation or texture-size queries.

`CherryTreeTest` renders native 16:9 and stretched 4:3 previews at 1280×720, checks tree elevation bounds, UV seam, dimensions, filters, transparency and quality changes. Other sky art/settings and petals are unchanged by this follow-up.

Render → SkyChanger → Style adds **Cosmic Nebula**, **Synthwave Sunset**, and **Cherry Blossom Dusk**. Existing enum names, settings keys, shaders and render paths are retained. No new mixins or world modifications are introduced.

## Controls

`Sky palette` defaults to Automatic (the selected style's own palette). It also offers the nine named presets and live Theme colors. `Sky brightness`, `Style cloud speed`, `Horizon haze`, `Style fog match`, and `Style world tint (%)` control these three styles only. The prefixed names intentionally preserve the existing Overcast cloud/fog/tint settings and saved values.

Shared existing controls: Stars, Star density (%), Shooting stars/chance/interval, Low quality, Time lock, Other dimensions (sky only), Hide clouds. New-style transitions use 300 ms even when the legacy Fade time setting differs.

- Nebula: Planet controls the original ringed planet and accompanying moon. Original billowing warped noise counter-scrolls behind a diagonal dusty galaxy and up to 2,000 individually twinkling stars. Rare comets are independent of the shooting-star chance.
- Synthwave: Sun size, Stripe speed, Skyline, Grid (off by default). The gradient disc's scrolling cutouts are shader-generated from cached quad geometry; the surrounding rim-lit mountain profile is cached procedural art.
- Cherry: Cherry sun or moon, Petal amount (Off/Light/Heavy), Petal size, Petal wind strength, Blossom trees (on), Lanterns (off). Original notched white/pink petals are world-space billboards, not the Minecraft particle atlas.

## Lifetime and rendering

`ArtSkyAssets` prewarms CPU art on a daemon at module registration. Noise, star field, sphere, sprites, mountain profile and blossom treeline are cached for the session. GPU textures/VBOs are created once per renderer lifetime on the render thread, disposed when the controller stops, and reused throughout animation. No meshes or textures are rebuilt per frame. The noise has periodic domain warping; 512px/256px noise variants support the quality toggle. Low quality halves stars/petals and removes celestial bloom.

`SkyStyleMix` retains the old indices and adds three reversible convex weights. World fog blends to the corresponding palette horizon; world-only lightmap tint is restored from exact original pixels before rendering the hand. Sky art is camera-centered, with depth writes off. The existing provider/cloud ownership rules restore previous handlers without clobbering another mod.

`PetalSimulation` has fixed 250-element storage (Light 90; Heavy 250; low quality 45/125). It recycles around the camera, fades under roofs using the existing 33×33 height cache, clears on world change/unload, and reseeds after large teleports. `PetalRenderer` streams into one persistent buffer after entities, depth test on/depth writes off, using its own generated texture. No block texture, biome, server state, or world data is edited.

All passes use `AuroraGLState` and the controller's `restoreDefaultState` safety net. Sky errors log once, detach the custom provider handler and clear tint/fades; petal errors disable that element independently. Non-surface dimensions are skipped by default, with sky-only opt-in and no petals/fog/lightmap changes there.

## Verification

Run `tools/build-aurora.ps1 -CheckOpenGL -ArtStages 3` against the desired base JAR. Each style was compiled and tested before implementing the next. Tests cover periodic cached textures, the star/particle caps, all 36 reversible style pairs at 60/240 Hz, palette/brightness and per-style switches, petal indoor fade/teleports, actual GL depth occlusion, GL-state restoration, and the previous Aurora/Constellation/Overcast regressions. Packaging validates Java 8, exact existing Forge hook mappings and unchanged unrelated JAR entries.

Off-screen 240-draw tests are correctness/stability checks, **not** a live 240 FPS performance guarantee. In-game visual review, shader-pack compatibility and actual frame time remain user testing items.
