# Config tab repair

The current user's on-disk setup was backed up before testing, including
`keystrokes/configs` and `clientconfig.kv`. A byte-identical named copy,
**Preserved setup 2026-10-21**, is also available in their Config tab. The original
active `default` profile and client settings were not changed by installation.
Neither backups nor personal profile contents belong in a GitHub commit.

## Bugs fixed

- The old `getConfigs()` rescanned disk and created new Config objects on every
  frame. ConfigTab keyed animations by object identity, continually replacing
  rows and resetting their reveal/clickability. The manager now returns stable
  cached objects; refresh happens on opening the tab or explicit file actions.
- Create used `copyConfig(active, name)`: it copied an old disk snapshot, not the
  player's current settings. It now captures live module values, writes a valid
  file, selects the new profile and refreshes the list immediately.
- The Config constructor created an empty file before ConfigManager checked
  whether the default file existed. A fresh default could therefore remain
  empty, and copying null data could leave an invisible invalid profile.
  Construction is now read-only, and missing/empty defaults get valid data.
- Every row's Save called the parameterless manager save, silently overwriting
  the active profile rather than the clicked profile. Save now targets its row
  without loading it or changing which profile is active.
- Invalid loads could leave `applyingConfig` stuck or change the active pointer
  before parsing/applying succeeded. Loads validate first, capture the current
  setup for rollback, select the target only after success, and restore the flag
  in a finally block.
- Incomplete older profiles now merge stored module/setting values with current
  data instead of aborting a module on missing settings or resetting newly added
  modules. Unknown root metadata and GUI preferences survive writes.
- Invalid names, duplicate names (case-insensitive), path traversal and Windows
  device filenames are rejected visibly in the tab, rather than failing silently.

## File safety

Writes use UTF-8. Reads also accept legacy Windows-1252 profiles (including `§`
color codes) and UTF-16 BOM files, without rewriting them just to discover/load
them. The initial UTF-8-only reader hid the user's old ANSI profiles; this
follow-up explicitly tests their real files read-only as well as synthetic
Windows-1252/UTF-16 fixtures. On the next explicit save, the old original bytes
are preserved in `.previous` before the UTF-8 replacement is written.

Writes use a temporary file in the same folder, then an atomic replace
where supported. An existing file is copied to `<name>.bplus.previous` before
replacement. Failed writes report an error. Confirmed deletion moves the file
to a recoverable `.deleted-<timestamp>` name; deleting the active profile saves
the live setup to a new Recovered profile rather than resetting settings or
overwriting an unrelated profile. Only `.bplus` files appear in the list.

The fix is scoped to Config, ConfigManager, ConfigTab and their inner classes.
It preserves the existing public APIs and all unrelated runtime classes. Profile
settings use the original Module/Setting serialization, including color pickers,
presets, toggles, keybinds and custom saved statistics. Global client settings
still use the existing separate clientconfig lifecycle.

## Verification

`tools/build-config-fix.ps1 -CheckOpenGL` tests real files in freshly generated
temporary folders and operates the actual Config-tab buttons in a real LWJGL
Pbuffer. Tests cover fresh defaults, Create/live snapshot, correct-row Save,
Load/restart, stable row objects, Cancel/confirmed Delete, inline error recovery,
Unicode names, duplicate/unsafe names, backup files, invalid-load rollback and
preserving settings on active deletion. A test-only Raven bootstrap fixture
prevents tests from initializing the full client or its network updater; that
fixture is never packaged. Packaging verifies Java 8/dataflow and byte-identical
preservation of unrelated JAR entries.

An in-game Create → change a setting → Load check is still recommended after
restarting Lunar. The good setup remains available under the preserved name.
GitHub publication remains stopped.

## Config-list scrolling follow-up

The previous wheel path moved only about nine logical pixels per notch, and
ConfigTab never connected ScrollView's scrollbar to click/drag/release events.
The Config list now moves 64 logical pixels per notch with smooth easing and
has a persistent thumb in a separate gutter. GuiRoot routes scrollbar dragging,
release, and lost-focus cleanup. Page Up/Down, Home/End, and arrow keys navigate
the list when the name field is not focused.

Entrance fades reserve full row heights, so the scroll range is usable right
away; saved-position restoration no longer overwrites early wheel input.
Removing rows can still collapse smoothly. Resizing clamps the range, and the
last row has enough bottom padding for its buttons to remain accessible.

The GL regression creates 24 isolated profiles and exercises first/last-row
access, Load at the bottom, wheel distance, keyboard navigation, entrance input,
scrollbar dragging/release, resize, position restoration, and actual GuiRoot
input routing. The old build fails the wheel-distance regression at 8.89 pixels.
Packaging validates exact Minecraft/SRG and client member references. Only
ConfigTab, its Visual inner class, and GuiRoot change versus the installed
encoding-fix build; all config-persistence classes and unrelated entries remain
byte-identical. Real profiles are audited read-only, never used as test fixtures.
