# Ambient world particles

Find **WorldParticles** in the ClickGUI's **Render** category. It is off by default,
uses Phantom's ordinary module toggle/keybind and saved settings, and appears in
the arraylist with its selected shape. No rain, SkyChanger style, server plugin,
or server-side weather change is required.

## Controls

- **Shape:** Stars, Snow, Rain (small raindrops), ETH (faceted diamond symbol).
- **Motion:** Still keeps existing positions fixed in world space; Drift adds
  slow wind/sway. Snow and Rain descend in Drift mode. Positions recycle outside
  the camera-centered volume, rather than moving with the camera.
- **Color mode:** Static, Rainbow, Gradient. Color and Second color are the
  existing shared pickers. These colors do not depend on the HUD theme.
- **Particle count:** 20–300, default 140. Size .04–.4 blocks, default .13 half-size.
- **Radius:** 4–24 blocks, default 10. Speed 0–3, wind 0–2, opacity and glow 0–100%.
- Rotate, Twinkle, Outdoors only, Show in first person, Show in third person.
- **Low quality:** cap 120 and no glow copy. Outdoors only defaults on; turn it
  off to allow particles in indoor air. Solid blocks and unloaded chunks are
  always excluded. The module never loads a chunk to place a particle.

## Implementation

`WorldParticles.java` registers ordinary Combo/Slider/Tick/PickerRGB settings.
`ParticleController` owns Forge render-tick / RenderWorldLast listeners, checks
world, camera, death and teleport changes, and caches roof heights briefly.
The listeners remain for the 200ms toggle-off fade, then unregister and release
GPU objects. The simulation pauses with a paused single-player world, but the
toggle fade still completes.

`ParticleSimulation` has fixed 300-entry arrays and analytic sine-wave velocity
integrals. Time is measured with `System.nanoTime`, not frame counts; doubles
also accumulate rotation to avoid 60/240Hz rounding drift. Shape switches
cross-fade over 180ms. Newly recycled particles ease in and volume edges fade.

`ParticleAssets` generates original antialiased star, snowflake, drop, faceted ETH
and soft glow sprites once per renderer initialization; no external assets or
vanilla particle atlas are sampled. A cached atlas and streaming VBO produce a
single batch (at most 900 quads including glow and shape cross-fade). Steady-state
simulation, snapshots and draw methods allocate no explicit objects or arrays.

The pass uses additive blending, camera-facing billboards, world depth testing,
no depth writes, and a near-camera fade. `AuroraGLState` restores the prior GL
program, VBO/client arrays, texture bindings/enables on both units, blend
functions, depth state, lighting, fog, culling, alpha test, matrices and color,
including Minecraft's cached state. A rendering failure is logged once and
disables only WorldParticles. Existing sky, sword, trail, HUD and lyrics classes
are not changed.

## Build and verification

Run `tools/build-worldparticles.ps1 -CheckOpenGL` in the existing local Forge/MCP
development environment. It compiles Java 8 sources against the current dist JAR,
runs CPU/config tests and a real LWJGL Pbuffer graphics test, remaps MCP references
to SRG, inserts exactly one module registration, and verifies every unrelated
original JAR entry is byte-identical. The scoped registry patcher still defaults
to Trail for its existing build command.

Checks cover all four silhouettes, manual colors/gradient/rainbow, exact 60/240Hz
drift, Still anchoring, pause, fades, recycling, roof/solid-block exclusions,
teleport/world reset, pool caps, settings round trips, GPU resource recreation,
block occlusion, near-camera fade, and GL restoration. The offscreen worst-case
300-particle/glow/cross-fade measurement was about 0.25ms per draw on this machine;
this is not a claim about full-game FPS. Live Lunar/server visuals, first/third
person movement and perceived density still need an in-game check.
