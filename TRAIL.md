# Trail — Phantom, Java 8 / Forge 1.8.9

Find **Phantom → Render → Trail**. It is disabled by default. Enable it and use F5
to see your path clearly. Color defaults to a user-picked violet, not a theme color.
All controls save through the existing module config system.

The existing ribbon is now body-height: **Top height** defaults to 1.5 blocks
(neck/shoulders), **Hem height** defaults to 0 (feet), following the latest requested
neck-to-feet look. Set Hem height to 0.5 for a shorter vanilla-cape-like height.
There is **no Cape mode** and no static attached garment: this is still a movement
trail that fades after you stop. Historical body heading and crouch pose are recorded
with every point, placing the top at the upper back and following the crouched lean.
Sprint/air reactions do not stretch the vertical height. Width's new default is
0.625; existing saved Width choices are preserved. Line/Dual remain slim tracks
within the height span; dots/smoke occupy that span. All six original style IDs,
colors, glow and world-depth/GL safety remain.

### Skinny Solid Band (2026-10-20)

Follow-up: **Match trail length** now defaults on. Solid Band follows the same
Length (s) lifetime/path as the other styles instead of being cut off at 1.6 blocks.
Turn it off to use the explicit Band length cap. Existing Band length values are
preserved, not silently overwritten. **Height offset** now ranges 0–4 blocks so
the center can be raised above the shoulders/head. Width/height/colors/glow and
the other Trail styles are unchanged by this follow-up.

Select **Style → Solid Band** for a filled narrow strip, independent of the older
body-height styles. Defaults: Band height 0.35 blocks, Band length 1.6 blocks,
Height offset 1.05 blocks (center above the feet), Edge width 1 pixel, Tail taper
amount 65%. Height ranges 0.1–1.5, offset 0–1.6; physical length is independent of
the existing Length (s) lifetime. The short rounded nose and smooth closed tail
are calculated along the same bounded cubic path. Both faces render; bloom is a
tight 0.035-block feather, not the older wide bloom. Opacity, Shimmer and Gradient
direction are band-specific controls. Top/Hem/Width still govern the older styles.

Theme color mode now explicitly reads Phantom's live top/bottom accents. Static
(also named Color) reads the actual RGB channels, avoiding the legacy RGBSetting
cached-color issue after bulk channel changes. Editing a picker clears the preset
and exits Rainbow/Theme so a chosen manual color cannot be ignored by those modes;
Gradient retains its two manually selected endpoints. Existing saved modes/presets
and all six older style IDs remain compatible; no user's selected style is changed.

## Project integration found and used

- Phantom's installed `ModuleManager` constructs modules via `addModule(...)`.
  SwordGlow and SkyChanger use this registry; Trail adds exactly one entry there.
  The source checkout is partial, so the builder compiles against the existing
  client JAR and reobfuscates the new classes, as the existing SkyChanger builder does.
- `Module` provides enabled/bind/arraylist state, `registerSettings(...)`, and
  JSON save/load of each named setting. The corresponding legacy source is in the
  sibling Raven-main project. Neither the base module nor config system is replaced.
- ClickGUI reads the registry and existing setting classes. Trail uses
  `ComboSetting`, `SliderSetting`, `TickSetting`, `PickerRGBSetting`, and
  `getHudSuffix()`. No GUI/HUD patch is needed.
- Like SkyChanger's `AuroraController`, `TrailController` owns Forge/FML listeners.
  Render-tick START records once per frame; `RenderWorldLastEvent` draws after
  entities and before HUD, never in an overlay/GUI event. Listeners survive
  toggle-off only for a reversible 200ms smoothstep fade, then unregister.

## Controls and styles

Styles: Ribbon, Line, Dual, Particles, Wings, Aura. Ribbon/Line/Dual/Wings use
feathered triangle strips with a brighter core and optional wider faint bloom.
Dual follows the path's horizontal perpendicular at the feet; jumps/falls remain 3D.
Particles uses spaced soft dots; Aura uses wider, slowly drifting expanding puffs.

Colors: Static uses Color, Rainbow moves hue along the trail in real time, and
Gradient blends Color at the head into Second color at the tail. Presets Plasma,
Inferno, Frost, Aurora, Neon, Void and Gold set the two pickers and select Gradient.
Custom leaves manual settings alone. Loading saved preset configs does not overwrite
subsequent saved manual color adjustments. Theme is an explicit opt-in color mode;
the default still uses the user's own color.

- **Length (s)** is lifetime, adjustable 0.25–3 seconds (default 1.2).
- **Width** is the ribbon's full base width in blocks, 0.04–1.2 (default 0.22).
  Line/Dual are intentionally thinner; Wings flares wider; Aura expands with age.
- **Intensity (%)** and **Bloom strength (%)** independently control brightness/bloom.
- **Show in first person**, **Show in third person** default on. First-person local
  head/near-eye samples fade out to keep the effect away from the view. The effect
  is behind you; looking ahead does not put a trail overlay in front of the camera.
- **Show on other players** defaults off. When on, only living, visible players
  within 16 blocks are eligible; up to seven nearby players plus the local player
  are tracked at once.
- **Sparkles** uses one shared maximum-20 pool, with distance-based emission,
  analytical age/motion and short fades. **Landing burst** adds a small fading ring.
- Airborne samples are 30% wider/brighter. Sprinting is brighter and extends sample
  lifetime by 35%. Neither changes movement, packets or world data.
- **Low quality** uses fewer recorded points, coarser tessellation, and no bloom or
  sparkles. Landing rings use fewer segments.

## Implementation and safety

Eight preallocated 1,024-point world-space rings record partial-tick-interpolated
positions only while moving (with a tiny noise threshold). Standing still does not
refresh timestamps; age expiry removes the trail. Cubic bounded tangents avoid
overshoot, followed by approximately 0.035-block spatial resampling, independent of
render FPS. Geometry is capped at 768 samples (256 in Low quality). At unusually high
speed/FPS or maximum lifetime the oldest samples may be capped rather than allocating.

The renderer subtracts RenderManager viewer coordinates in double precision before
casting vertices to floats. Billboard normals use path-direction × camera-view,
with continuous sign and a vertical/end-on fallback. The actual eye/right/up frame
is recovered from MODELVIEW so third-person camera offsets are respected.

World changes/unload, player identity changes/respawn, death, large position jumps
(over eight blocks), invalid coordinates and long observation gaps clear history.
Raw tick-position jumps also clear before interpolation can stretch a teleport into
a line. Paused single-player worlds freeze animation time.

One streaming VBO, one reusable direct vertex buffer and one reusable curve buffer
serve every actor. Draws are additive (`SRC_ALPHA, ONE`), depth-tested, with depth
writes off. Main/bloom geometry has soft transverse and age/width falloff, not flat
opaque quads. Effects stay behind opaque world depth; translucent surfaces retain
Minecraft's normal depth-buffer limitations.

GL scopes save matrices, both texture units/bindings, shader, VBO/client arrays,
lighting, fog, alpha, cull, depth and blend state. Minecraft's state cache is
reconciled afterward. The final safety net restores the previous texture, texture2D,
white color, normal alpha blending and MODELVIEW. Exceptions log once, stop listeners,
clear the effect and disable the module; a later enable can retry. One cached VBO is
reused across toggles/worlds rather than recreated every frame. No new mixins are needed.

## Verification

Run `tools/build-trail.ps1 -CheckOpenGL`. Automated checks cover:

- 60/240Hz frame recording, cubic/spatial agreement, stationary expiry, bounded
  buffers, 200ms fade/reversal, landing/air/sprint and teleport/world/death clearing;
- double-precision camera subtraction near world-coordinate limits, vertical
  paths, eight camera angles, first-person head fade and visibility gates;
- real Phantom settings codecs, all six rendered styles, three color modes,
  seven distinct preset palettes, bloom/width/intensity and low quality;
- production depth occlusion, 240 simulated draws, ring/spark paths, GL state and
  a transparent textured HUD/cross glyph immediately afterward;
- Java 8 bytecode/dataflow, exact SRG/Forge member resolution, no explicit object/
  array allocations in frame paths, one registry entry, all unrelated JAR entries
  byte-identical (including SkyChanger, SwordGlow, GUI and config code).

The contact-sheet preview uses the production renderer on a synthetic walking/jump
path, not a game screenshot. Offscreen 240-frame checks are **not** a measured
240-FPS live Lunar guarantee. Walking/sprinting/jumping, F5 camera, real servers and
the combined HUD/sky/sword appearance still need an in-game visual check.
