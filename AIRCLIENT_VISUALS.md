# AirClient in-game visuals for Phantom

Scope: in-game HUD/palette/drawing only, as clarified by the user on 2026-10-19.
All ClickGUI panels, widgets, iOS switches, GUI themes, HUD editor and gameplay
modules are preserved. No LiquidBounce runtime classes or third-party font/image
assets are imported. See AIRCLIENT_VISUAL_MAP.md for the pre-change inventory and
THIRD_PARTY_NOTICES for exact origins, modifications and licensing.

## Controls

Use the existing Phantom HUD module settings:

- Visual style: AirClient (new default) or Classic (previous Phantom appearance).
- Visual theme: Client (existing colors) or 284 named upstream palettes.
- Theme fade (ms): 50-1000, default 200; switching mid-fade starts at the current color.
- Show Header / Watermark info: PHANTOM nameplate; optional player, FPS, ping, clock.
- Game info: optional XYZ position and horizontal blocks-per-second card.
- Existing Show Values, Scale, Position, X/Y Offset, Sort Mode and Color Mode still
  apply. Normal Phantom config serialization saves every new setting.
- Selected named visual theme is shared with SwordGlow's Theme color mode. Static,
  Rainbow and Gradient SwordGlow modes retain their independent behavior. No GUI
  accent methods are replaced. Client preserves the prior HUD/SwordGlow colors.

If Phantom Panels is enabled it keeps its previous priority over Phantom HUD;
disable Panels to see the new Phantom HUD style. Existing user configuration is
not rewritten just to activate the new style.

## Safety and performance

The palette is an immutable Java enum built from upstream's cached themeColorMap.
Each HUD row retains its animation state, strings and width. Module roster/suffix
sampling and sorting happen at most 10 times per second; name/ping/time/position
metrics refresh at most four times per second. Animations use measured elapsed time,
not frame counts or reported FPS. Resizing/GUI scale changes invalidate measurements.

Rounded drawing uses precomputed trigonometry and three bounded shadow rings.
Every helper call restores GL attributes and the active texture unit; the complete
HUD also restores the prior shader, matrix mode/model matrix, lighting, blend,
texture bindings, depth/alpha/cull flags and current color. Fonts are prepared
outside the raw GL scope, and the fast existing Phantom font draw path stays within
it. No shader compilation, framebuffer allocation, stencil clear or dynamic blur
is performed by this HUD.

The player/world/network info are null-checked. A failed element is logged once
and suppressed until the HUD is toggled, while other elements continue. A broken
module suffix disables only that module's HUD row. Font failure disables this
style for that activation rather than crashing or repeatedly drawing errors.

Live blur, framebuffer bloom/shadows, music, target HUD combat dependencies,
alternate ClickGUIs and HUD designer are intentionally excluded. The old HUD and
GUI remain available. Actual in-game FPS depends on world/client settings; a 240Hz
simulation/GL state regression is not a promise of 240 FPS.

The prior SwordGlow clock redirect did not exist in Lunar's transformed RenderItem
and caused a startup InjectionError. The combined build replaces it with the two
texture-translation hooks and scopes tinting to the managed sword, including zero
intensity/idle swing mode, without coloring an unrelated enchanted item.

## Build

Cached Java 8 / Forge 1.8.9 / MCP and ASM tools from the adjacent Raven-main project
are required, as for the other Phantom patch builds. This remains a patch project
against the existing Phantom binary, not a complete independent client source tree.

```
./tools/build-air-visuals.ps1 -Area Foundation -CheckOpenGL
./tools/build-air-visuals.ps1 -Area Hud -CheckOpenGL
```

The first stage compiles palette/drawing alone. The second compiles the complete
in-game adaptation and writes a remapped, verified JAR under build-air-visuals/.
Only PhantomHUD, the visual helpers, SwordGlow and its item mixin may change.
The patch verifier checks every other original ZIP entry, specifically including
the GUI and gameplay modules, for byte-for-byte preservation. License texts and
THIRD_PARTY_NOTICES are embedded in the JAR.

Tests cover 284 unique upstream palettes, 60/240Hz transition timing, interrupted
cross-fades, row timing and offscreen GL restoration with both texture units bound.
Source provenance and license text are preserved; distributing this combined JAR
also requires complete corresponding source for the pre-existing GPL-covered base.
