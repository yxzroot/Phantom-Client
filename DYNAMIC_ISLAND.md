# DynamicIsland — Phantom 1.8.9

The separately built JAR adds **Render → DynamicIsland**, disabled/unbound by
default. It has NOT been installed over the user's running Lunar session.
AspectRatio's already-installed update is independent and retained in this build.

## Architecture inspected and reused

- Module/category/registration/config: existing `Module`, `ModuleManager`,
  `ComboSetting`, `TickSetting`, `SliderSetting`, `PickerRGBSetting` codecs/widgets.
  This repository is a source-overlay project: existing classes live in the base
  JAR. The isolated builder follows the other visual modules, compiling Java 8,
  remapping MCP → SRG, then narrowly patching registration into that actual JAR.
- World HUD: existing Guava `Render2DEvent`. Optional menu HUD uses Forge
  `GuiScreenEvent.DrawScreenEvent.Post`. Forge client ticks and local attack events
  supply warnings, key actions and combat targets. No combat behavior is modified.
- Notifications: existing `NotificationRenderer.moduleStateChanged` and
  `NotificationManager.show`. Original delivery remains when disabled or when
  Replace Existing Notifications is off. Config-load toggle floods are suppressed.
- Fonts: Phantom's Poppins `PhantomTTF`; Unicode falls back to Minecraft's font.
  Theme: `PhantomHUD.visualAccent`. Shapes/shadow: existing `AirDraw` helpers.
- Existing GUI `Anim` springs were inspected; they are tied to GUI preferences and
  critically damped. The HUD instead uses a small analytic damped spring with its
  own speed/overshoot controls, preserving velocity through retargets.
- GL protection reuses `AuroraGLState`. A private cached FBO/SDF mask clips all
  content to rounded ends without modifying Minecraft's depth/stencil attachment.
  FBO, viewport, shader, textures, blending, matrices, depth, lighting, fog,
  scissor/stencil configuration and texture caches are restored; final color is white.

## Implemented states and API

Idle (Name/FPS/Clock/Nothing), module toggles with animated iOS-style switch,
custom/legacy notifications, optional target-health feedback, stopwatch/countdown,
health/durability warnings, compact Now Playing and expanded 1/3-line lyrics.
Five pending events merge duplicate keys, prioritize warnings, and drop lower
priority entries when full. Music resumes without a forced idle transition.

```java
DynamicIsland.post("Config Loaded", "Settings restored", "i", 3000L);
DynamicIsland.startTimer("Countdown", 60); // seconds; <= 0 starts a stopwatch
DynamicIsland.stopTimer();
```

All visual/event/music settings are in Phantom's existing settings UI and config.
The four action bindings are unbound by default in Minecraft **Options → Controls →
Phantom DynamicIsland**, using Forge's standard registered KeyBindings/options.txt
persistence. Phantom's existing module bind remains the island's on/off bind.
Its current generic settings API has one module bind, not arbitrary action binds;
no separate custom keybinding GUI or duplicate modules were added.

## OS media adapters and provider compatibility

Windows: bundled PowerShell/WinRT GSMTC helper, one-second UTF-8 JSON lines,
asynchronous process reader, bounded thumbnail transfer. Position is advanced from
GSMTC's LastUpdatedTime anchor, then interpolated monotonically between polls.
Linux: asynchronous `playerctl`/MPRIS polling; install playerctl through your distro
if missing. No Spotify credentials, Discord connection or player-specific account
is required. The adapters read only exposed media metadata, never browser history,
cookies, passwords or unrelated tabs. They do not send playback-control commands.

Compatibility targets are Spotify desktop/web, SoundCloud, YouTube, YouTube Music,
and Riff. They use ONE renderer/lyrics engine. Browsers may identify themselves
only as Chrome/Edge/Firefox instead of exposing the originating service; that
provider label is preserved honestly rather than guessed from a song title.
Session arbitration favors playing/recently active sessions, has 1.5-second
hysteresis, keeps a paused session if no other player plays, and drops stale data.
Artwork errors leave track metadata intact. Closing a player clears its snapshot.

Riff source inspected at commit `d7a929fe9b8acc2dc329afae9c772a3774d9525f`
(package version 42.3.0): `package.json`, `app.js`, `main.js`, `preload.js`,
`server.js`, and `plugins.js`. It is Electron. `app.js`'s `updateMediaSession`
publishes title/artist/album/artwork and playback state through MediaMetadata;
`updateMediaPositionState` publishes duration, currentTime and playbackRate from
the active native/secondary audio element. Both are called by the shared playback
paths (streaming, local files, pause/resume and seek). Therefore Riff's YouTube
Music/YouTube/SoundCloud/local playback targets the same OS adapter. Its Discord
RPC and authenticated localhost server are NOT dependencies. No invented Riff
endpoint or copied Riff source is shipped. See the [inspected source](https://github.com/rootscripts/riff/blob/d7a929fe9b8acc2dc329afae9c772a3774d9525f/app.js).

For incomplete website media metadata, an optional user-installed companion is
provided at `tools/island/browser-media-companion.user.js`. It fills missing title
and timing from media on its own allowed-service tab only, with no network/IPC or
storage access. It cannot override an OS/browser that entirely withholds media
sessions. Older Riff versions without Media Session publishing need updating;
there is no promise that every wrapper/browser build exposes all OS fields.

## Lyrics, artwork and synchronization

### Riff compatibility repair (2026-10-20)

Live diagnostics found that WinRT thumbnail cleanup could return a raw COM object
without a `Dispose` method. Its cleanup exception was incorrectly dropping the
entire session. Cleanup is now non-throwing, and `-Once -Diagnostics` can inspect
the Windows helper without an endless process. On this machine the recovered OS
session was a browser, while Riff 42.3.0 was not publishing its playing track there.
The empty pill itself was expected: the saved Idle Content setting is Nothing.

An additional optional `RiffBridge` adapter now receives metadata from Riff's real
plugin API (`getTrack`, `audio`, `isPlaying`, and track/play/pause subscriptions).
The installed companion is `LocalAppData/Riff/plugins/phantom-dynamic-island.js`.
It publishes only current title/artist/album/artwork URL/position/duration/playback
state. It does not touch playback, account information, history or Riff's server.
The original Riff app.asar is unchanged. Restart Riff to load the enabled plugin.

The receiver binds only `127.0.0.1:49827`, requires a random per-user 256-bit pairing
value in the request body, limits message size, rejects non-local/browser origins,
exposes no readable metadata endpoint, and stops with Now Playing/the module.
Pairing is saved in `<Minecraft>/phantom/island-riff-bridge.json`; never share that
file or the generated plugin (both contain the local pairing value). The committed
template `tools/island/phantom-riff-bridge.template.js` contains no credentials.
Direct Riff playback is preferred over generic browser playback, then uses the
same immutable snapshot/renderer/lyrics pipeline. Paused, stopped/closed, missing
metadata and stale companion states retain their normal behavior.

Tests cover the authenticated Java receiver and a JavaScript fixture using Riff's
actual API shape, including pause/position/duration and cleanup. Actual end-to-end
Riff → plugin → Lunar behavior still needs the post-install restart check.

Online Lyrics defaults OFF. Local files take priority:
`<Minecraft data directory>/phantom/lyrics/Artist - Title.lrc`.
Filename/path traversal is blocked. The worker parses multiple/fractional LRC
timestamps, offset tags, duplicate timestamps and enhanced word tags; malformed
lines are ignored. Immutable timelines are binary-searched at playback time.

With Online Lyrics on, only title/artist/album/duration go to
[LRCLIB](https://lrclib.net/docs). Connect/read deadlines, disk/memory caches,
not-found caching, request deduplication and stale-track rejection are included.
Shared normalized identity reuses lyrics across providers. Per-song offsets are
stored in `phantom/island-song-offsets.properties` and change immediately without
refetching. No lyrics are fabricated when local/online data is unavailable.

Lyrics scroll through a retargetable spring, dim when paused, snap/fade across
large seeks, use enhanced word timing or estimated character-weighted timing,
and draw a soft accent karaoke sweep using cached glyph positions. Long genuine
instrumental gaps optionally show subtle dots. Marquee has start/end pauses and
a continuous return. Equalizer stops on pause; progress and elapsed/total time
follow the same interpolated clock. Artwork decoding/scaling happens on the
worker, uploads/deletion on the render thread; the cache holds at most 16 covers.

## Files / build / verification

### Smooth lyrics and compact mode (2026-10-20)

Compact mode now shows at most three words rather than marquee-scrolling a full
line through the narrow pill. Phrase changes follow enhanced word timestamps,
or bounded character-weighted estimates for plain line LRC, and cross-fade/slide
over 140 ms. Extra-long phrases may use one/two words to keep the current word
inside the pill. Sung text fills white, independently of the album/theme accent.

Both layouts can progressively extend a held vowel (e.g. `love` → `loveeeee`)
when enhanced word timing indicates a long note. Cached word variants blend with
the held interval; plain line-only lyrics do not fabricate precise vowel timing.
The compact-only render path also explicitly establishes its depth/cull/texture
baseline, rather than relying on a preceding song-title draw to do that work.

**Lyrics only** is a saved toggle in DynamicIsland settings. It displays just the
current lyric inside the normal 120×28 compact pill (before the user's Scale),
without album art, metadata, progress or event expansion. Long lines marquee;
line changes slide/cross-fade. Disable the toggle to return to ordinary island
notifications and music. Show Lyrics/Lyrics Lines continue to govern normal mode.

PlaybackClock smooths small metadata timing corrections while respecting explicit
seek revisions, pauses and buffering. LyricMotion retains scroll velocity and
blends line focus instead of snapping highlights. Intro/instrumental dots fade
instead of abruptly replacing a line. Enhanced LRC word timings are used when
available; plain line-only LRC still estimates word fill and instrumental gaps,
not audio/vocal recognition. Per-song Lyrics Offset remains available.

Riff companion 1.2 listens to native audio seek/play/pause/waiting/rate events,
reports buffering and seek revisions immediately, coalesces pending updates and
removes listeners on unload. It needs one Riff restart after replacing the plugin.
The same existing authenticated loopback pairing is preserved.

### Lyrics lookup repair (2026-10-20)

The original exact-only LRCLIB lookup could cache `{}` for a failed match even
when the search endpoint had synced lyrics. `LyricsLookup` now validates search
candidates against normalized artist/title and a small duration tolerance, then
prefers the closest duration/album. Plain-only or unrelated artist/version hits
cannot accidentally become synced lyrics. Search is also attempted when the
exact endpoint is temporarily unavailable. Old empty negative caches are ignored;
new completed misses are versioned and expire after 30 minutes. Transient service
failures retry off-thread after 30 seconds rather than remaining stuck loading.
No media bridge/plugin or rendering behavior was changed by this repair.

The production Java worker was tested against the live service for the reported
track, `dancing alone` by `requiem for you`: 27 timed lines were recovered despite
a deliberately seeded legacy `{}` cache. No lyric text was logged. Automated
selection tests reject a different artist, wrong duration and empty search results.

Created `src/keystrokesmod/client/module/modules/render/DynamicIsland.java` and
`render/island/{IslandSpring,IslandEvent,IslandQueue,IslandOptions,IslandCanvas,
IslandRenderer,IslandLog,IslandIO,MediaSnapshot,MediaSessionManager,LyricTimeline,
LyricsManager}.java`, the Windows helper, optional browser companion, this document,
`tools/{build-island.ps1,PatchIslandJar.java,IslandTest.java}`. `.gitignore` excludes
isolated island builds/reference checkout. The output patches only its own classes,
one module registration and the two existing notification entry points.

Build: `tools/build-island.ps1 -CheckOpenGL` (optional explicit `-BaseJar`).
Checks cover 60/144/240Hz analytic spring agreement, queue deduplication/capacity/
priority, LRC/enhanced parsing, asynchronous local lyrics/offline behavior, persisted
offsets, rapid skipping/no stale lyrics, filename safety, play/pause/position clamps,
provider normalization, real rounded SDF clipping, production Phantom fonts/artwork/
music, and 240 offscreen render passes with GL/FBO/viewport restoration.
Unrelated JAR entries, including SkyChanger, AspectRatio, Trail, SwordGlow and GUI,
are compared byte-for-byte. Windows helper startup with zero sessions was tested.

Not yet live-verified: Lunar startup with this new island, all eight states in a
real world, actual Spotify/SoundCloud/YouTube/YouTube Music/Riff playback, Linux
playerctl, online LRCLIB fetching, browser companion, controls/config reload, GUI
scale/fullscreen combinations, and real 240 FPS performance. Offscreen simulated
passes are correctness checks, not a live benchmark. Do not call these providers
verified until someone tests them with their actual applications.
# Cyrillic and compact-lyric follow-up

General coverage pipeline: see [LYRICS_RESEARCH.md](LYRICS_RESEARCH.md) for primary-source research, architecture and tests. Runtime lookup is now generic hypothesis/ranking rather than exact-only acceptance; timed results are preferred, plain text is retained and marked `~`/estimated, and a second documented provider is available. Search cancellation, positive/shared/offline caching, provider cooldowns and explicit instrumental status replace repeated endpoint-only repairs. No artist/song allowlist, account token, microphone/audio capture, protected-site scraping or Riff plugin update is involved.

Long-line / uploader follow-up: instrumental dots no longer infer silence from a guessed vocal duration when the next LRC timestamp is more than 12 seconds away. A nonempty cue remains visible until the next cue; explicit empty cues and the period before the first cue still represent breaks. Line-only word interpolation uses the actual next timestamp instead of prematurely consuming a long line. Unknown final-word release times do not produce arbitrarily stretched held-vowel variants. Instrumental Dots OFF is respected even before the first cue (a quiet Intro/Instrumental label is used instead).

`Artist - Title` upload metadata is considered as an alternate credited artist/title pair, while direct matches and artist/duration validation remain intact. This covers the screenshot's `Кино - Хочу перемен` uploaded by `Alexander Pintz`, verified through a live production lookup. Live/remix/slowed version suffixes are not mistaken for artist credits. Negative-cache lookup version is now 5. Temporary service failures retry after 5/10/20/40/60 seconds rather than remaining unavailable for 30 seconds after every isolated error.

Tests cover long 50-second Russian cue intervals in the actual compact renderer, visible text rather than dots, explicit blank breaks, recovery on the next cue at 60/144/240Hz, embedded credits, and preservation of wrong-artist/version rejection. Upstream LRC timestamp accuracy is not audio analysis and may still vary by recording.

Upload-title follow-up: the exact Riff metadata `+emoslut666 specialsadism` / `voidvoice717` / 102 seconds now resolves to the matching timed track after removing the leading `+credit` token for lookup only. The displayed song title and artist are not rewritten. Artist and duration validation still reject unrelated and slowed/longer versions. CPU fixtures and a live production lookup cover this exact case without logging lyrics. Failed HTTP requests now show **Lyrics service busy** instead of **No synced lyrics**, remaining fallbacks can recover, and failed searches never become a cached definitive miss. Old negative caches are invalidated by lookup version 4.

The Island now uses its own bounded Unicode atlas pages: Poppins where supported and a same-size logical SansSerif fallback otherwise. Russian/Cyrillic no longer switches to Minecraft's smaller bitmap font. Mixed-script measurements, karaoke color fills, and drawing all use the same advances; Unicode is compiled as UTF-8. The shared GUI/HUD font is unchanged.

Compact phrases follow the playback clock directly, not the expanded lyric-scrolling spring. Their 85 ms transition keeps the incoming phrase immediately readable instead of fading it from zero. Instrumental/seek state is reset cleanly; duplicate blank LRC timestamps cannot erase a nonempty lyric. A small pill with unavailable lyrics reports Loading lyrics / No synced lyrics / Online lyrics off rather than silently remaining empty.

Lookup cleans safe video/lyric suffixes (including Russian suffixes), handles Topic and featured-artist metadata, and tries a broader full-text query while retaining artist, title, duration, and version checks. Empty results no longer stay in memory forever; old negative-cache entries are ignored and misses/transient failures retry with backoff. Local UTF-8 LRC files and per-song offsets retain priority. No Riff plugin change or re-pairing is required.

Regression checks include visible Cyrillic karaoke pixels, font sizing, three-word explicit timing, Unicode suffix matching, empty-timestamp recovery, seeks/pauses, GL restoration, and a live emoslut666 lookup without logging lyric content. Word timing remains an estimate for line-only LRC; exact word sync requires enhanced word timestamps. No microphone/audio analysis or invented transcription is performed.
