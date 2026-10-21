# Airstrafe

Location: **Movement → Airstrafe**. Disabled and unbound by default. Uses the existing
module registration, settings widgets, keybind, config and arraylist systems.

Defaults: 40% strength, 0.50 blocks/tick speed cap, 60% smoothing. Sprint-only and
hold-jump-only are off. Water/lava/web/ladder exclusion is on. Debug is off.
Debug shows horizontal blocks/second, blocks/tick and activation status.

The existing local-player `MoveInputEvent` from `MixinEntity.moveFlying` supplies
input direction/yaw and vanilla horizontal acceleration. Airstrafe consumes that
acceleration exactly once and adds a small smoothed contribution. A 100ms ramp
runs on the 20Hz simulation clock, independently of rendering FPS. Vertical
motion, gravity, jump height, movement timers and automatic jumping are untouched.
No new mixin or projection/render hook is needed.

Quick-turn response: steering authority increases smoothly with the angle between
current velocity and input (2x sideways, up to 3x directly opposing). The strength
ramp responds faster during turns, without restarting the takeoff fade or replacing
velocity. Straight-line acceleration, the speed cap and vertical motion are unchanged.
At default strength a full-speed reversal takes six simulation ticks rather than
twelve in the isolated no-drag comparison. Momentum prevents truly instantaneous
reversal; this is stronger bounded countersteering, not a direction snap.

The cap bounds newly generated speed, including base acceleration while steering.
Existing momentum above the cap (for example knockback or another module's boost)
is not abruptly cut; Airstrafe can steer it without increasing its magnitude.
Landing clears steering state, not the player's ordinary horizontal momentum.
It also resets on world/player/dimension changes, tick rewind, death, unload and
position jumps above four blocks. Special-medium exclusion can be explicitly
turned off; flight, spectator, riding and ground exclusions cannot.

Implementation: `movement/Airstrafe.java` and `movement/airstrafe/`.
Sliders retain normal serialization and widgets with allocation-free cached reads.
Normal physics paths contain no explicit object/array allocation. Debug reuses the
existing GL-state guard, caches its text for 100ms and is entirely skipped when off.
Unexpected movement/render exceptions log once and disable this module.

Build and automated checks: `tools/build-airstrafe.ps1 -CheckOpenGL`.
Checks include the actual production listener via Guava, 20,000 bounded steering
cases, momentum and fade behavior, all gates, duplicate-event suppression, reset
cases, identical simulation with 60/240 render calls per second, unchanged vertical
motion/gravity, settings/config/keybind/HUD round-trips, safe exception disabling,
and 200 offscreen debug draws with GL-state restoration after a forced exception.
Packaging validates Java 8 bytecode, mapped Minecraft references, one registration,
only horizontal Minecraft field writes and byte-identical unrelated JAR entries.

These are isolated automated tests, not a claim of live Lunar gameplay testing or
a full-client 240 FPS benchmark. Test steering/landing in a local world after
restart. Server movement rules/anti-cheat may reject modified air acceleration;
client-side implementation does not make altered movement cosmetic or server-authorized.
Installation preserves existing configuration files; old profiles simply have no
Airstrafe entry until the client next saves normally. AimAssist is unchanged.
