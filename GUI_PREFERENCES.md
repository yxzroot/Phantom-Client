# GUI settings follow-up

The header actions are **Notifications** (GUI notification history), **Refresh**
(refresh module/HUD cards and discover config files), and **Settings** (interface
preferences). They are now labeled buttons on normal-sized windows; very narrow
windows keep compact notification/refresh icons and a labeled Settings button.

The old 228×180 text list has been replaced by a bounded settings popover with
a title/close action, inline descriptions, shared iOS switches, actual speed/scale/
tooltip sliders and a scrollable theme dropdown. Scale applies on release to keep
the dragged control stationary. Outside click and Escape close the menu without
clicking a module beneath it. Popup input is modal and header presses reset on release.

The previous animation toggle/speed row could do nothing when the legacy Gui
module did not define matching settings. The preferences now have working fallback
values, saved as gui.menuAnimation and gui.animationSpeed in the existing config.
Existing module-backed settings still take priority where present. Other existing
GUI preference keys and all gameplay/render modules are preserved.

Build/test: `tools/build-preferences.ps1 -CheckOpenGL`. CPU checks exercise real
toggle, speed, deferred scale, tooltip delay, theme selection and dismissal paths.
The production popover is rendered with Poppins, SDF shapes and shared switches in
240 offscreen passes. The package changes only GuiRoot, HeaderBar, GuiPreferences
and PreferencesMenu; every other JAR entry must be byte-identical. Live interaction
and frame-rate performance still require a Minecraft check.
