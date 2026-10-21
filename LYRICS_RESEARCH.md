# General lyric coverage repair

## Research and provider choice

- [LRCLIB's API documentation](https://lrclib.net/docs) describes both `syncedLyrics` (timed LRC) and `plainLyrics` (text without timing), anonymous access, metadata lookup and search. Phantom previously required nonempty synced LRC and discarded plain-only matches. This was a real coverage bug, not evidence that those songs had no lyrics.
- [LRCLIB's own architecture](https://github.com/tranxuanthang/lrclib/blob/main/ARCHITECTURE.md) documents temporary search-index unavailability and `503`/`Retry-After` responses. Searching another endpoint on the same overloaded provider is not an independent fallback. The new transport respects provider cooldowns and tries another provider rather than hammering the same host.
- [lyrics.ovh's published API](https://github.com/NTag/lyrics.ovh) documents `GET /v1/{artist}/{title}`, returning plain lyrics or 404. It is the independent text-only fallback. Its data is not treated as word-timed karaoke. This implementation calls the public API; it does not copy the provider's code or scrape lyric websites itself.
- [Musixmatch's official SDK documentation](https://github.com/musixmatch/musixmatch-sdk) requires an API key. No key or paid service was supplied, so no private endpoints, borrowed tokens, purchases, or fictitious integration were added. Larger catalogs could be integrated later through an authorized provider account.

No service guarantees that every recording, underground upload, cover, remix, language or instrumental has text and matching timestamps. The repair improves retrieval and displays available text without claiming to invent missing lyrics or recognize vocals from audio.

## Implementation

`LyricMatching` generates metadata hypotheses for direct title/artist, embedded credits, reversed title/artist uploads, leading `+credit`, collaborations, Unicode punctuation/accents and generic Cyrillic-to-Latin normalization. Bounded typo matching retains artist evidence, number checks, recording/version checks and duration ranking. There is no song/artist allowlist in runtime code. Unknown or weak matches are not accepted just to fill the pill.

`LyricsResolver` prefers valid timed results. It retains plain matches while continuing a bounded search for sync, then tries the independent fallback when needed. A mismatched recording duration may supply text only under strict artist/title checks; it cannot silently become accurately timed data. Requests have byte/time limits, provider cooldowns, cancellation checks and `Retry-After` handling. No lyric content is logged.

`LyricsManager` runs everything on one daemon worker. Epoch checks prevent stale search publication after skips, interrupted tasks are cancelled, available lyrics remain visible during retry, and positive results are cached immediately. Metadata-validated shared caches reuse songs between differently formatted uploads/providers and work offline. Plain cached results may later upgrade to timed ones. Old negative caches no longer suppress the new matching pipeline. Local UTF-8 LRC and saved per-song offsets remain supported.

`LyricTimeline.estimate` preserves the plain text with bounded duration-proportional display estimates, not audio alignment. Compact mode shows an amber `~` for this fallback; expanded mode identifies the provider and says **estimated**. No held-vowel detection is fabricated for plain text. Explicit provider instrumental flags have their own status rather than being called a failed lyric search.

Online Lyrics still controls network access. Only track metadata goes to these providers; no music audio, microphone data, Riff credentials, browser cookies or account tokens are transmitted. The Riff plugin requires no update or restart.

## Verification

`LyricsPipelineTest` uses synthetic catalog fixtures, including unfamiliar names, direct/reversed/uploader layouts, title typos, collaborations and Cyrillic transliteration. It checks wrong-artist/version/number rejection, plain results, secondary-provider failover, provider cooldowns, cancellation, timed upgrades, immediate offline/shared cache reuse and stale-epoch rejection. These are generic behavior tests, not production exceptions for those songs.

The live audit independently found synced results for six samples spanning Russian uploads and mainstream English tracks: the two Kino examples, the prefixed upload previously shown, Adele, Daft Punk and The Weeknd. A separate request verified that the secondary API actually returned plain lyrics. No lyric content was printed. Six successful samples demonstrate operation, **not universal coverage**.

`IslandTest` continues to exercise production GL/FBO/font state, Cyrillic, explicit word timing, long-cue visibility, compact 120×28 bounds, expanded mode and the actual amber estimated marker. Packaging validates Java 8 and leaves other modules' JAR entries byte-identical. In-game playback and upstream timestamp quality remain user testing items.

## Separate visual idea

The requested world shapes are ambient particles, independent of vanilla rain: Still and Drift behavior with custom symbols. No ambient-particle module was added in this lyric repair; existing sky, tree, trail and sword effects were preserved.
