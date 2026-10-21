# Trailing AimAssist

Java 8 / Forge 1.8.9 replacement of the existing Combat → AimAssist, not a new
registry entry. Name, module config key, keybind, toggle/HUD state, public legacy
fields and friends methods remain compatible. No attack/click automation or
packet rotation/silent aiming is added.

## Controller

- Up to 32 candidates, each with a fixed 132-sample, roughly 500 ms ring buffer.
  Partial-tick interpolated positions are sampled per render frame. History
  coalesces above 256 FPS, preserving the full time window without resizing.
- Default aim point: chest, delayed 120 ms, with a 0.035-block motion-aligned
  trailing offset. Trail strength scales both. Offset disappears when motion
  history catches up at rest; angular displacement is capped at 4 degrees.
  Head/Chest/Body are supported, including vertical jump/fall history.
- Analytically integrated critically damped **velocity** rather than a spring
  that snaps the camera onto a target. Speed 4, yaw 80%, pitch 15% by default;
  bounded smooth hash/value noise supplies low-frequency humanization.
- The assist stops at a 0.7-degree dead zone around both the real body aim point
  and delayed point. Quantized movement is checked against crossing either
  zone, so accumulation cannot perform the final correction automatically.
- Real Entity.setAngles input is observed without consuming it. Opposing input
  clears that axis's velocity and fractional sensitivity accumulator immediately,
  with a short 100 ms yield. Both current and previous angles receive the same
  additive delta, matching vanilla interpolation/picking semantics.
- Fades in over 150 ms, with a tapered activation band inside the FOV/range edge.
  Lost-target strength fades internally. Invalid gates/out-of-range/out-of-FOV
  targets stop actual rotation immediately for safety; no steering toward stale
  enemies during fade-out. Switching has a 2-degree stickiness bonus and 180 ms
  challenger delay, then fades the new target in with clean histories.

Render hook: existing MixinFreelookRenderer injects at HEAD of
EntityRenderer.renderWorld (`func_78471_a(FJ)V`), after vanilla mouse input and
before picking/camera setup. Existing input mixin observes Entity.setAngles.
Freelook's active camera is excluded. No world/HUD GL calls on the normal path.

## Settings and filters

Speed, yaw/pitch strength, FOV (60-degree full cone), Range (4.5 blocks), Trail
strength, Trail delay, Trail offset, Max offset, Hitbox target, Humanization,
Fade time, Switch delay; optional click/mouse movement/sword-or-axe/no-GUI gates;
Ignore teammates, invisible, creative/spectator; Players or Players + mobs;
Pause while mining; Debug off by default.

Dead entities, armor stands, self, friends and hidden targets are excluded.
Teams use scoreboard membership, not rank-color/name-prefix guesses. The
existing AntiBot filter is respected when enabled. Visibility checks use a
100 ms per-candidate cache; vanilla ray tracing can allocate internally, but
the assist's history/controller/pull create no per-frame objects or arrays.
AssistSlider uses the unchanged slider/config widget but caches rounded values
on changes, avoiding legacy BigDecimal allocations during frame reads.

World unload/change, respawn/death, teleport, long frame gaps, new selections,
config application and disabling clear the appropriate state. Rendering errors
log once and disable the module. Debug draws a small depth-tested aim marker
and throttled angle label with existing exact GL isolation, including exceptions.

## Old configs

Legacy `Speed 1 (yaw)` becomes bounded `Speed` at 0.1 scale; `Speed 2 (yaw)`
maps to yaw strength at 3x scale. Distance, Click aim and Break blocks map to
their new counterparts. Old disabled pitch stays disabled. FOV, Weapon only,
keybind and toggle/HUD flags load normally. The old blatant/snap path is not
preserved. Migration copies JSON in memory and never rewrites the source file.
Fresh configs use the new controls; no separate profile/config system is added.

## Verification

`tools/build-aimassist.ps1 -CheckOpenGL -AuditDirectory <configs-folder>` compiles
only this module/helpers and the two relevant existing mixins against Java 8.
It tests the real controller with isolated Minecraft/entity fixtures and the
actual installed Module/setting classes (test-runtime MCP remapping only).

Tests cover 60/240 FPS, delayed history/high-FPS capacity, moving/stationary
targets, offset cap, 30-second no-centering behavior, mouse override, sensitivity
steps, fades, filters/gates, actual sword acceptance, sticky switching,
teleports/death/world reset, OFF inert, legacy/new config round trips, and
read-only migration of real saved profiles. Offscreen GL tests cover 200 debug
frames plus a forced exception with state restoration. Packaging validates
Java 8/dataflow, exact SRG/client member resolution, legacy ABI, one existing
registry entry, unchanged Freelook/Scaffold method bodies apart from the added
observer, and byte-identical unrelated JAR entries (including the Config-tab fix).

These are automated/isolated checks, not a live Lunar gameplay or full-game
240-FPS benchmark. In-game feel and mod-hook compatibility still need a restart
and local practice-world test. No GitHub publication is performed.

## Autoclicker compatibility / callback follow-up

The installed legacy LeftClicker (different from the upstream source copy)
calls Utils.Client.setMouseButtonState(0, false/true) between clicks. That helper
writes LWJGL's own Mouse.buttons buffer and posts Forge MouseEvents. The old
AimAssist gate read this transient buffer as a physical release and cleared its
target, restarting the 150 ms fade and velocity filter on each click cycle.

AttackGate now accepts the vanilla attack-key state and observed press events,
and bridges at most 180 ms between presses **only while LeftClicker is enabled**.
An idle enabled clicker does not activate anything; after releases stop arriving,
the grace expires. Ordinary manual releases stop the gate immediately when the
clicker is off. No LeftClicker code or unrelated settings are changed.

AimAssist also registers directly on the same Forge/FML event buses used by the
working WorldParticles controller. Render START opens one update slot; the normal
world hook, existing camera hook fallback, and world-last fallback consume that
same slot, so forwarded events and multiple camera passes cannot double-step.
Fallback rotations refresh picking. The original renderWorld hook's execution
was not proven by the previous live logs; it is no longer a single failure point.

Debug now displays a HUD status even without a target: callback route, missing
attack input, GUI/focus/weapon/mining/mouse gates, filtered/out-of-range target,
tracking, or the no-centering zone. It remains off by default.

New regression tests simulate sustained autoclicking with short DOWN pulses and
UP gaps, verify target/fade continuity, and verify idle/released clickers stop.
They invoke the actual Forge listener entry points while deliberately skipping
both native renderer hooks, then mix primary/camera/forwarded events and assert
rotation is applied once per frame. Debug HUD state restoration is included.
Real configs are still audited read-only and are not changed by installation.

## Higher maximum speed

Speed now ranges from 0.5 to 60 instead of 0.5 to 20: three times the former
maximum pull. The default remains 4, and every existing value keeps precisely
the same response. No smoothing, fade, mouse override, dead-zone, click gate or
other module behavior changes. High-speed tests cover config round trips,
60/240 FPS agreement, the no-centering zone, and immediate mouse opposition.
