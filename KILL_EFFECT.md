# KillEffect

Find **Render → KillEffect** in Phantom. It is disabled by default and uses the
existing module toggle/keybind, shared color pickers, sliders, dropdowns and
configuration codec. No packets, fake entities, damage, drops, fire or world
changes are produced.

## Effects and settings

Lightning, Shockwave, Soul Rise, Shatter, Confetti, Firework, Flame Burst,
Ice Shatter, Black Hole and Light Pillar are separate animated geometry styles.
Random uses a shuffled ten-style bag, visiting every style before reshuffling.
Shatter is a stylized body-shaped cloud of glowing cube fragments, not the
victim's skin or a modification to its vanilla death model.

Static, Rainbow and Gradient reuse the Trail palette presets: Plasma, Inferno,
Frost, Aurora, Neon, Void and Gold. Color by effect supplies initial style-specific
colors (including multicolored Confetti); choosing a preset or manually changing
a picker disables that override. Size, Intensity and Duration control the effect.

Targets: Players; Players and mobs (default); Everything (also armor stands,
boats, minecarts and hanging decorations). Everything does not turn ordinary item
pickups/despawns into kills. Max distance defaults to 64 blocks. Others' kills
defaults off.

Sound plays the vanilla local `note.pling` sound, with a volume slider and a
bounded upward streak pitch. Screen flash and Camera pulse are optional and
**off by default**; strength sliders allow a subtle edge-only colored flash and
a 150ms multiplicative FOV pulse. The pulse does not write FOV/window settings
or replace AspectRatio's projection hook. Streak scaling is bounded and resets
after ten seconds without a local kill.

Popup posts `+1 Kill` and the victim's name through DynamicIsland's existing
notification API. When the island is disabled, notifications are disabled, or
Lyrics only mode is active, it uses its own fading Unicode HUD popup instead.
Others' kills never increment the local counter or generate local sounds,
flashes, camera pulses or `+1 Kill` messages.

Show total kills is an optional HUD toggle whose normal setting JSON also stores
`totalKills`. The statistic is restored when the profile loads and saved with
the ordinary Phantom config-save lifecycle; no file I/O occurs in the render
loop. Resetting this setting to defaults also resets that statistic.

## Detection and limitations

Local AttackEntityEvent and observed client-side LivingHurtEvent identify the
most recently attacked target. Within three seconds, its health reaching zero,
death animation, death event, or dead-plus-hurt status triggers exactly once.
Dead but still-healthy living entities with no hurt/death evidence are ignored
so simple despawn/removal packets do not normally look like kills. Recently
attacked nonliving destructibles may trigger on their destruction/removal.

Known competing player damage suppresses local credit; known mob attackers also
suppress it. Multiplayer servers do not consistently synchronize reliable killer
metadata, and a client cannot prove every final hit. This is cosmetic best-effort
credit, not an authoritative server kill statistic. Others' kills uses observed
player-attacker data when available, or a recent visible hurt followed by death
when the server omits that data. That optional fallback can include environmental
deaths; it never awards local credit. Previously dead entities are not replayed.

## Rendering and safety

Five fixed slots with sixty particles per slot give a hard 300-particle cap;
new effects replace the oldest slot. Low quality limits this to 120 particles
and reduces geometric detail. Analytic motion is evaluated from monotonic time,
not frame counts. Positions are interpolated at the death location, kept as
doubles in world space and camera-subtracted before rendering. Single-player
pause freezes effect time. World changes, player death/respawn, disconnect and
module disable clear effects and attribution.

Original cached procedural soft/flame/crystal textures and a streaming triangle
VBO are used, never the vanilla particle atlas. Most effects use additive
blending; only Black Hole's dark center uses ordinary alpha blending. World
depth testing stays on and depth writes stay off. Rings, arcs, fragments, sprites
and beams share a bounded batch. Lightning jitter is smoothly interpolated
between 70ms samples. Lightning's follow-up tuning uses a 7.5-block main bolt
(before Size/streak scaling), a wider white-hot core, four arcs, a faster 36ms
attack, and a slightly stronger impact glow. The nine other styles are unchanged.
Firework trails are analytically sampled, not unbounded
position histories.

World and HUD passes use the existing exact AuroraGLState isolation and boundary
restoreDefaultState safety net, restoring textures/lightmap, texture enables,
blend, depth, lighting, fog, cull, alpha, matrices, color, program, buffers and
Minecraft's state cache. Failures log once per enable and disable KillEffect.
No other runtime module is patched.

## Verification

`tools/build-killeffect.ps1 -CheckOpenGL` compiles Java 8, runs attribution,
deduplication, lifetime, streak, pool-cap, random-cycle, settings and saved-counter
tests, then exercises all ten actual render paths in a real LWJGL Pbuffer.
Graphics tests cover distinct effects, depth occlusion, expiry, colors/intensity,
Unicode HUD fallback, edge-only flash, worst-case batching, GPU recreation and
GL state restoration. All remapped Minecraft member references are checked
against Forge/SRG. Packaging adds exactly one registry entry and preserves every
unrelated JAR entry byte-for-byte, including WorldParticles, lyrics, sky, sword,
Trail, AspectRatio and GUI.

The five-effect / 300-cube offscreen stress check took about .45ms per draw on
this machine. This is not a full-game 240 FPS guarantee. Live multiplayer death
packets/kill attribution, audible sound, DynamicIsland delivery and perceived
effect size still need an in-game test on the user's servers.
