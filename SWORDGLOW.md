# SwordGlow — layered energy (Java 8, Minecraft Forge 1.8.9 / Lunar)

Open **Phantom → Render → SwordGlow**. Pick **Preset: Plasma** for a starting look,
or select a custom Animation and Color mode. Existing Classic/Fast/Slow/Sharp/Rainbow
config IDs remain valid. Static color, Gradient start/end, speed, intensity, scale,
angle, activation and other-player settings keep their old config keys.

## Current renderer

The normal sword renders untouched first. The proven Lunar held-model hook then
draws the same baked model using three soft procedural layers (one in Low quality).
`GL_EQUAL` and the original atlas alpha restrict energy to actual sword pixels.
A cached 128×128 original DynamicTexture supplies noise, wide bands and speckles.
Brightness includes a texture-color-aware contrast boost and a pale hot core;
position along the blade drives the hilt-to-tip gradient and sweeps. Texture matrices
scroll/rotate each layer independently. No enlarged model or rectangular halo is used.

Bloom enables Forge's supported stencil attachment once, reserves a bit through
MinecraftForgeClient, and masks the real sword silhouette using atlas alpha/depth.
The same alpha-cutout model is offset 0.65 and 1.5 **screen pixels**, with a stencil
test excluding ALL original sword pixels. It never scales model geometry. Foreign
stencil bits are preserved; the reserved bit is cleared/released when finished.
If stencil is unsupported or unavailable, only bloom is skipped. The attachment may
remain allocated in Minecraft's framebuffer; the module does not remove another
renderer's stencil resources. No particle/trail geometry is used as a halo.

## Animation and reactions

Energy Flow, Shimmer, Pulse, Flame, Lightning, Galaxy, Aurora, Rainbow Wave, Charge,
Glitch and Void have distinct procedural patterns. A secondary animation can blend
over them. Primary/secondary changes, equip and enable/disable use 200–300 ms fades.
Void adds a small depth/alpha-clipped attenuation pass before its bright energy.
Glitch offsets sampled RGB/slices, never the sword's solid geometry.

Charge builds while idle and releases on swing or local attack. The local Forge
AttackEntityEvent drives cosmetic hit flashes/sparks; this is immediate client-side
feedback, **not server-confirmed damage**. Swing progress drives reactions/trails;
blocking smoothly adds a cooler pulse; actual slot/item changes trigger an equip
sweep. Durability/NBT synchronization does not constantly restart equipping.

Every style now has a progress-synchronized wind-up, bright hilt-to-tip release and
soft recovery, controlled by **Swing energy (%)** (default 65). This envelope follows
vanilla's interpolated swing, independently of the texture Speed setting. Repeated
attacks detect progress wraps even when vanilla leaves its swing flag on, so Charge
releases and spark accents do not miss consecutive swings. No hand transforms,
attack delays, reach, damage or hit timing change.

Trails contain 12 fixed-60Hz tip samples (~0.2 s at 60 or 240 FPS), plus the live
interpolated tip to remove sampling lag. A bounded cubic curve produces at most 49
points (25 in Low quality). Four feathered ribbon lanes give transparent outer edges
and a slightly pale core, with velocity/envelope-driven strength and age/width taper.
Width never exceeds 3.6% of the actual view-space blade length. Separate strokes and
teleports do not connect into sheets, and a stationary sword emits no visible trail.
**Trail strength (%)** defaults to 75; **Trail lifetime (ms)** defaults to 200 and
supports 80–220. Rainbow trails/sparks follow the shader's moving hue; other color
modes use the selected tip color. A release adds three brief directional sparks,
sharing the existing particle cap and respecting the Particles/Low quality toggles.

Tip/axis data is
cached from the baked model and opaque sprite pixels where available; nonstandard
models fall back to geometric extents. Trails/sparks are drawn in the held render's
view space and projection, not as a world particle entity system. One global pool
caps all visible-player sparks/embers/stars/petals at 25, with delta-time movement
and age fades. Lightning also gets short jittered view-space arcs. Camera-facing
particles and trails use depth testing without writing depth.

## Theme, presets and performance

Theme / Static / Rainbow / Gradient / Palette modes all use selected colors. Theme
colors cross-fade; no hardcoded vanilla purple is used. Phantom currently exposes
Client/Air visual palettes, not nine separate GUI themes with the requested names.
**Theme signature: Auto** follows the active HUD palette label, and explicit
Midnight/Aurora/Sakura/Inferno/Cyberpunk/Ocean/Frost/Vaporwave/Amoled signatures provide
the requested default animations without changing the GUI/theme architecture.
Amoled uses white energy; Sakura gets petal-shaped particles.

One-click Preset selections: Plasma, Inferno, Frostbite, Void, Galaxy, Aurora, Neon,
Toxic, Royal Gold (plus Sakura). Presets apply an 85% intensity and a style-appropriate
speed along with color/band/bloom defaults. Custom preserves manual choices. Inferno/Galaxy/Void
presets select their natural colors; choosing just an animation retains the user's
selected tint. Low quality reduces three layers to one and removes bloom, particles
and extra arcs; the separate trail toggle still works. Built-in custom item renderers are skipped. Nearby actor state is
preallocated (32 slots), remote players are opt-in and culled beyond 16 blocks,
meshes/models are not rebuilt per frame, and model metadata is cached (32 models).

## State safety and verification

Each scope restores texture units/bindings, shader, vertex arrays/VBO, texture/model/
projection matrices, blending, color masks, stencil/depth, lighting/fog/alpha/culling,
shade and polygon offset. GlStateManager caches are reconciled after native model
callbacks. The handler safety net rebinds the block atlas, enables texture 2D,
restores white color/normal alpha blending and MODELVIEW. GUI icons explicitly mask
held-item ownership; the module OFF returns to vanilla item/enchantment rendering.
Risky shader/bloom/motion paths log once and disable their component or fall back to
the older safe glint. No unrelated module or GUI class is patched.

Build: `tools/build-held-glint.ps1 -CheckOpenGL` against the current dist JAR.
The build checks Java 8/dataflow, unchanged unrelated JAR entries, Lunar scope
ownership, 60/240Hz timing, orange alpha-cutout sword visibility, eleven style
outputs, setting responses, exact core silhouette, stencil exclusion and <=2px
halo reach, foreign stencil preservation, particle caps, charge/hit/block/fades,
fixed-duration trails, repeated swing wraps, 60/240Hz swing-envelope agreement,
bounded/subdivided/feathered moving tip trails, stroke/teleport breaks, idle recovery,
actual production motion-pass visibility and state restoration, swing energy control,
zero/low-quality behavior and textured HUD glyph/cross pixels
after drawing. These are offscreen correctness checks, **not** a live Lunar FPS or
appearance guarantee. First/third person, swings, HUD and the resource-pack sword
still need the user's live visual check.
