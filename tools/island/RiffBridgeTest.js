'use strict';
const fs = require('fs');
const assert = require('assert');
const path = require('path');
const source = fs.readFileSync(path.join(__dirname, 'phantom-riff-bridge.template.js'), 'utf8');
let posted, interval, removed = 0;
const listeners = new Map();
const audio = { currentTime: 12.5, duration: 180, playbackRate: 1, paused: false, readyState: 4,
  addEventListener: (event, fn) => listeners.set(event, fn), removeEventListener: event => listeners.delete(event) };
const api = {
  getTrack: () => ({ title: 'Fixture song', artist: 'Fixture artist', id: 'fixture', platform: 'local' }),
  audio: () => audio, isPlaying: () => true,
  on: () => () => removed++
};
const run = new Function('Riff', 'document', 'fetch', 'setInterval', 'clearInterval', 'setTimeout', 'clearTimeout', 'AbortController', source);
const plugin = run(api, { querySelectorAll: () => [audio] }, async (_, options) => {
  posted = JSON.parse(options.body); return { status: 204 };
}, fn => { interval = fn; return 1; }, () => {}, setTimeout, clearTimeout, AbortController);
const flush = () => new Promise(resolve => setImmediate(resolve));
(async () => {
  await flush();
  assert.strictEqual(posted.sessions[0].title, 'Fixture song');
  assert.strictEqual(posted.sessions[0].position, 12.5);
  assert.strictEqual(posted.sessions[0].duration, 180);
  assert.strictEqual(posted.sessions[0].playing, true);
  assert.strictEqual(posted.sessions[0].buffering, false);
  listeners.get('waiting')({ type: 'waiting' }); await flush();
  assert.strictEqual(posted.sessions[0].buffering, true);
  audio.currentTime = 12.2;
  listeners.get('seeked')({ type: 'seeked' }); await flush();
  assert.strictEqual(posted.sessions[0].revision, 1);
  assert.strictEqual(posted.sessions[0].position, 12.2);
  assert.strictEqual(posted.sessions[0].buffering, false);
  audio.paused = true;
  listeners.get('pause')({ type: 'pause' }); await flush();
  assert.strictEqual(posted.sessions[0].playing, false, 'Native pause must beat the lagging UI flag');
  api.isPlaying = () => false;
  interval(); await flush();
    assert.strictEqual(posted.sessions[0].playing, false);
    plugin.stop(); await flush();
    assert.strictEqual(removed, 3);
    assert.strictEqual(listeners.size, 0);assert.strictEqual(posted.sessions.length, 0);
    console.log('PASS: actual Riff APIs, immediate small-seek revision, pause and buffering, listener cleanup and clear');
})().catch(error => { console.error(error); process.exitCode = 1; });
