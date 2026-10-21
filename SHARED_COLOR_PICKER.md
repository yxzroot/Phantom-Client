# Shared Phantom color popup

All 21 installed module RGB fields use one `clickgui/color/ColorPicker` through
the rebuilt ClickGUI, legacy Apple, Raven and KV swatch adapters. This includes
SwordGlow, SkyChanger, Trail, DynamicIsland, PhantomHUD gradients, panels, player
ESP, radar and tracers. Theme presets are not RGB settings; their editable RGB
endpoints use the same picker. Future RGBSetting subclasses get it automatically.

The popup is a modal Forge GUI overlay, not a replacement screen. Parent drawer,
scroll and layout remain open. Press Select/Enter to keep live changes; Cancel,
Escape, outside click or a screen change restores the original RGB and opacity.
HSV retains hue through black/gray; RGB sliders and numeric fields, HEX clipboard
input, optional opacity, Shift precision, keyboard Tab and invalid-input outlines
share the same transactional model. RGB-only colors cannot silently accept a
non-opaque HEX alpha. Existing separate opacity SliderSettings remain unchanged.

The legacy `rgbsetting` red/green/blue JSON schema is unchanged. RGBSetting now
computes packed RGB from channels, fixing stale `setColors`/direct array writes,
and fixes its old out-of-bounds reset. The original class API and NSColor enum IDs
are preserved. The old standalone ColorPickerScreen and obsolete channel-dragging
implementations are removed from the new JAR; legacy adapter names stay compatible.

Poppins, SDF rounded panels/shadows and existing render helpers are reused. The
SV field is two vertex-colored quads; hue uses six gradient segments. A cached
GLSL coverage mask rounds gradient/checkerboard edges. No palette texture rebuilds
or per-frame gradient object allocation. Render state is scoped with AuroraGLState
and reconciled with Minecraft's cache; popup failure closes safely and logs once.
Recent colors/preset row is omitted (optional) to keep the panel uncluttered.

Build: `tools/build-shared-picker.ps1 -CheckOpenGL`. Tests cover actual setting
codecs, stale-color/reset regression, live preview/commit/cancel, hue stability,
alpha, HEX validation/paste, outside dragging, keyboard, bounded scaled placement,
three rendered tabs/fonts/checkerboards and 240 offscreen passes with GL restore.
Packaging lists every actual module color field and verifies unrelated JAR bytes
are unchanged. Live Lunar interaction/FPS still needs user verification.
