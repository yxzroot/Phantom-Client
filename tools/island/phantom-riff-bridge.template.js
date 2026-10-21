/**
 * @name Phantom DynamicIsland bridge
 * @author Phantom
 * @version 1.2.0
 * @description Sends current playback metadata to your paired Phantom HUD over authenticated loopback only.
 */
const endpoint = 'http://127.0.0.1:__PORT__/phantom-media';
const token = '__TOKEN__';
let stopped = false, pending = false, queued = false, revision = 0, watched = null, waiting = false;
const audioEvents = ['seeking', 'seeked', 'play', 'pause', 'ended', 'waiting', 'stalled', 'playing', 'canplay', 'ratechange'];
function audioChanged(event) {
  if (event.type === 'seeking' || event.type === 'seeked') revision++;
  if (event.type === 'waiting' || event.type === 'stalled' && watched && watched.readyState < 3) waiting = true;
  if (event.type === 'playing' || event.type === 'canplay' || event.type === 'seeked') waiting = false;
  publish();
}
function watch(audio) {
  if (audio === watched) return;
  if (watched && watched.removeEventListener) audioEvents.forEach(event => watched.removeEventListener(event, audioChanged));
  watched = audio; waiting = false;
  if (watched && watched.addEventListener) audioEvents.forEach(event => watched.addEventListener(event, audioChanged));
}
const finite = value => Number.isFinite(Number(value)) ? Math.max(0, Number(value)) : 0;
async function publish(clear = false) {
  if (stopped && !clear) return;
  if (pending) { queued = true; return; }
  pending = true;
  const abort = new AbortController();
  const timeout = setTimeout(() => abort.abort(), 4000);
  try {
    const track = !clear && Riff.getTrack();
    const nativeAudio = !clear && Riff.audio();
    const active = !clear && [...document.querySelectorAll('audio')].find(audio => !audio.paused);
    const audio = active || nativeAudio;
    if (!clear) watch(audio);
    const duration = audio && Number.isFinite(audio.duration) ? finite(audio.duration) : finite(track && track.duration);
    const sessions = track && track.title ? [{
      title: String(track.title), artist: String(track.artist || ''), album: String(track.album || ''),
      id: String(track.id || track.url || track.title), playing: !!Riff.isPlaying() && !(audio && (audio.paused || audio.ended)),
      position: finite(audio && audio.currentTime), duration,
      revision, buffering: !!(audio && (waiting || audio.seeking || Number.isFinite(audio.readyState) && audio.readyState < 3)),
      rate: audio && audio.playbackRate > 0 ? audio.playbackRate : 1,
      artworkUrl: /^https?:\/\//i.test(track.thumbnail || '') ? track.thumbnail : ''
    }] : [];
    await fetch(endpoint, {
      method: 'POST', credentials: 'omit', cache: 'no-store', signal: abort.signal,
      headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
      body: JSON.stringify({ token, sessions })
    });
  } catch (_) { /* Minecraft may be closed. Do not interrupt Riff playback. */ }
  finally { clearTimeout(timeout); pending = false; if (queued) { queued = false; publish(stopped); } }
}
const subscriptions = ['trackchange', 'play', 'pause'].map(event => Riff.on(event, () => publish()));
const timer = setInterval(() => publish(), 1000);
publish();
return { stop() { stopped = true; clearInterval(timer); subscriptions.forEach(off => off()); watch(null); publish(true); } };
