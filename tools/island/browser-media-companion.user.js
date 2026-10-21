// ==UserScript==
// @name Phantom media-session companion (optional)
// @version 1.0
// @description Fill missing OS media metadata/position for the active media on this tab only.
// @match https://www.youtube.com/*
// @match https://music.youtube.com/*
// @match https://soundcloud.com/*
// @match https://open.spotify.com/*
// @grant none
// @run-at document-idle
// ==/UserScript==
(() => {
  'use strict';
  if (!navigator.mediaSession || typeof MediaMetadata === 'undefined') return;
  // No network, localhost endpoint, cookies, storage, credentials or other-tab reads.
  // This is opt-in and does not replace a service's valid title/artist/artwork.
  let ownedMetadata = null;
  const update = () => {
    const media = [...document.querySelectorAll('audio,video')]
      .find(item => !item.paused && Number.isFinite(item.duration) && item.duration > 0);
    if (!media) return;
    try {
      const current = navigator.mediaSession.metadata;
      if (!current || current === ownedMetadata || !current.title) {
        const title = document.querySelector('meta[property="og:title"]')?.content ||
          document.title.replace(/\s*[-–|]\s*(YouTube|SoundCloud|Spotify).*$/i, '').trim();
        if (title && (!ownedMetadata || ownedMetadata.title !== title)) {
          ownedMetadata = new MediaMetadata({ title, artist: '', album: '', artwork: [] });
          navigator.mediaSession.metadata = ownedMetadata;
        }
      }
      navigator.mediaSession.playbackState = 'playing';
      if (navigator.mediaSession.setPositionState) navigator.mediaSession.setPositionState({
        duration: media.duration, position: Math.max(0, Math.min(media.duration, media.currentTime)),
        playbackRate: media.playbackRate || 1
      });
    } catch (_) { /* A browser may withhold its OS media bridge; never break playback. */ }
  };
  document.addEventListener('pause', event => {
    if (!(event.target instanceof HTMLMediaElement)) return;
    if (![...document.querySelectorAll('audio,video')].some(item => !item.paused)) {
      try { navigator.mediaSession.playbackState = 'paused'; } catch (_) {}
    }
  }, true);
  document.addEventListener('play', update, true);
  document.addEventListener('seeked', update, true);
  const timer = setInterval(update, 1000);
  window.addEventListener('pagehide', () => clearInterval(timer), { once: true });
})();
