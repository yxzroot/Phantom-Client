# Antarctic Aurora

Open Phantom's ClickGUI, choose **Render**, and search for **SkyChanger**.
Enable it, open its settings, and select **Style: Antarctic Aurora**.
The module is disabled by default and saves through Phantom's existing config system.
**Style** includes **Vanilla**, **Antarctic Aurora**, **Constellation**, and **Overcast Dusk**.

## Overcast Dusk

**Style: Overcast Dusk** adds a near-black blue-violet dome and three slowly
drifting cloud layers. Its 512×256 periodic five-octave, directionally stretched,
domain-warped noise texture is generated once and cached. Spherical cloud-plane
mapping is continuous across the azimuth seam and pole, with a horizon fade.
The dome mesh and texture are never rebuilt per frame.

The upgraded Dusk uses brighter blue/lavender-gray highlights over dark gaps,
large warped bands with soft contrast, and three independently scrolling cloud
planes converging toward the horizon. Cached trilinear mipmaps keep distant
streaks soft instead of aliased. Horizon mist and fog share the same brighter haze
color, with a smooth zero-slope blend rather than a hard horizon edge. All existing
controls/config keys remain; the other sky styles are unchanged.

Controls: Dusk color tint, Dusk second color, Cloud speed, Cloud contrast,
Cloud scale, Horizon glow, Darkness, Dusk lightning (off), Dusk stars (off),
Fog match (on), World tint strength (%). The dusk prefixes distinguish these
from the existing aurora/constellation star controls. Vanilla clouds are hidden.
The fog match uses the exact same horizon formula as the sky shader. World tint
is a reversible world-pass-only cool/desaturated lightmap grade, restored before the hand and
after the world, never a block texture or world-data edit. Snow/aurora vignette,
reflections, curtains and Milky Way are excluded in this style.

Three-way convex style fades prevent an unrelated style flashing between
Constellation and Overcast Dusk. The shared Fade time defaults to 300ms. Existing
styles' final rendering and saved labels remain unchanged. Tests cover seamless
noise, three-way 60/240Hz fades, cached production shader output/settings/motion,
world tint restoration, 240 simulated draws and HUD/GL state recovery.

## Constellation

Choose **Style: Constellation** for a fully black sky with white glowing points and
thin connected lines. A deterministic, cached 300-node graph joins every point to
its closest earlier neighbors, so the web stays connected as density changes.
Edges follow spherical arcs, and endpoints share the same shader deformation, so
the points and lines gently ripple/drift together instead of separating.

**Constellation speed**, **Constellation density (%)**, **Constellation brightness
(%)**, and **Constellation lines** use the existing sliders/toggle. Existing **Stars**
and **Star density (%)** control the white point markers. **Bloom** adds a faint
wider line pass; **Low quality** skips it. Shooting stars/chance also work here and
use white streaks. Density or brightness zero leaves a completely black sky.

Changing Aurora ↔ Constellation cross-fades using **Fade time**, including reversed
transitions. Snow, blue vignette, cold lightmap, aurora reflections, Milky Way, moon,
aurora wisps and wind audio are excluded/faded out in Constellation, preserving the
black-and-white look. Black fog blends its horizon. Time lock, cloud hiding, border
hiding, normal toggle-off/provider restoration and config saving remain available.
No vanilla world-lighting or texture data is overwritten by this style.

Tests include a connected graph at every density, legacy/new style config labels,
60/240Hz style fades, real shader output on a black background, monochrome pixels,
point/line/brightness/density controls, motion, 240 simulated renders, and GL/HUD safety.

## Antarctic Aurora and shared controls

**Hide clouds** removes vanilla clouds. Antarctic Aurora always hides blocky vanilla
clouds; its **Cloud mode** adds optional dark-tinted or wispy high clouds instead.
It uses a no-draw Forge cloud renderer; it does not change
Minecraft's global cloud settings. Turning the setting/module off, changing worlds,
or leaving the world restores the previous cloud renderer. A renderer installed
after ours by another mod is left alone.

**Hide world border** (on by default) hides only Minecraft's blue/red striped wall
while SkyChanger is enabled, including on servers. It cancels the mapped
`RenderGlobal.renderWorldBorder(Entity,float)` at HEAD, before any GL changes.
Turning the setting or module off immediately restores vanilla rendering. It never
changes border size/position, packets, collisions or the server's movement limits;
actual glass/block walls are not hidden. The toggle is saved in the normal config.

**Stars** toggles the cached, colored, twinkling star field; **Star density (%)**
controls its density (0–100%). **Shooting stars** is independently toggleable.
**Shooting star chance (%)** is one chance roll per **Shooting star interval (s)**,
not per frame: defaults are 35% per 8 seconds, adjustable 0–100% and 3–60 seconds.
100% means one meteor in each interval, not a constantly visible meteor; 0% means
none. Position, direction and start time vary, and the streak fades over 0.65–0.95 s.
Meteors can appear in any sky direction, so an event may be behind the camera.
These effects use the Antarctic Aurora style; regular stars remain visible regardless
of time lock. No per-frame RNG, mesh allocations or world entities are added.

The redesigned module includes 4–6 separate folded curtain ribbons (not a cylinder),
irregular drifting rays, 29-second rolling substorms, faint bloom copies, a cached
diagonal Milky Way texture, three star size/brightness layers, an optional moon,
and optional soft high wisps. Low quality halves curtain count and omits bloom.
It includes shader-animated cached aurora/star VBOs, bounded drifting
snow, camera-column roof checks, fog reflection, world-pass-only lightmap tint,
optional vignette and optional low-volume vanilla rain hiss as wind ambience.
It does not replace block textures or write world time/data. The optional sound
is an approximation, not a recorded Antarctic wind track. Grass whitening is omitted.
Nether/End atmosphere is skipped; the optional other-dimensions setting affects sky only.

GL-state repair was compiled and checked independently before the redesign at
`build-aurora/20261020-014607-850/`. Raw drawing scopes restore shader, VBO/client
arrays, matrices, both texture units/bindings and fixed-function state, then
reconcile GlStateManager's cache. Handler boundaries restore the incoming texture,
white color, ordinary alpha blend function, texture 2D and MODELVIEW. Lightmap
uploads preserve pixel-store state and sampler/bindings without TextureUtil's
upload path. Vignette now runs at overlay Pre/ALL, before HUD textures are drawn.
Fog grading changes event values only, never GL state. Cold lightmap pixels are
restored before the held hand and at world-pass exit.

Build: `tools/build-aurora.ps1 -CheckOpenGL` against the current dist JAR or an
explicit `-BaseJar` backup. Tests cover real renderer shader/VBO output, settings,
state restoration, simulation timing, particle limits, roofs, lightmap recovery,
cloud override ownership, exact border-hook mappings/render-only cancellation,
Stars/density controls, 0/35/100% shooting chance and 60/240Hz event agreement,
production meteor pixels/state restoration, every new visual setting, and a textured transparent
glyph/cross drawn after the production lightmap upload. These are not a live-game
FPS benchmark; the user is doing the final Lunar HUD/appearance check.
