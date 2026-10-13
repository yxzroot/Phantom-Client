package keystrokesmod.client.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.combat.LeftClicker;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.SliderSetting;

/** Bounded asynchronous clip pool, with normalized click samples and adjustable gain. */
public final class SoundUtils {
    private static final int VOICES = 3;
    private static final ArrayBlockingQueue<Request> requests = new ArrayBlockingQueue<>(32);
    private static final Map<String, Pool> sounds = new HashMap<>();
    private static final Set<String> unavailable = new HashSet<>();
    private static boolean started;

    private SoundUtils() {}

    private static final class Request {
        final String name;
        final int volume;
        Request(String name, int volume) { this.name = name; this.volume = volume; }
    }

    private static final class Pool {
        final AudioFormat format;
        final byte[] samples;
        final float baseline;
        final float maxGain;
        final Clip[] voices;
        final int[] levels;
        Pool(AudioFormat format, byte[] samples, float baseline, float maxGain, Clip[] voices, int volume) {
            this.format = format;
            this.samples = samples;
            this.baseline = baseline;
            this.maxGain = maxGain;
            this.voices = voices;
            this.levels = new int[voices.length];
            java.util.Arrays.fill(this.levels, volume);
        }
    }

    public static void playSound(String name) {
        if (name == null) return;
        if (name.equals("oldmouse")) name = "oldMouse";
        if (!(name.equals("click") || name.equals("click1") || name.equals("g3032")
                || name.equals("g502") || name.equals("gpro") || name.equals("hp")
                || name.equals("microsoft") || name.equals("oldMouse") || name.equals("sus"))) return;

        int volume = 100;
        if (Raven.moduleManager != null) {
            Module clicker = Raven.moduleManager.getModuleByClazz(LeftClicker.class);
            if (clicker != null) {
                Setting setting = clicker.getSettingByName("Click Volume");
                if (setting instanceof SliderSetting)
                    volume = (int) ((SliderSetting) setting).getInput();
            }
        }
        volume = Math.max(0, Math.min(200, volume));
        if (volume == 0) {
            requests.clear();
            return;
        }
        synchronized (SoundUtils.class) {
            if (!started) {
                started = true;
                Thread worker = new Thread(SoundUtils::run, "Raven click audio");
                worker.setDaemon(true);
                worker.start();
            }
        }
        // Never wait for loading/decoding/audio-device operations on the game thread.
        requests.offer(new Request(name, volume));
    }

    private static void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Request request = requests.take();
                if (unavailable.contains(request.name)) continue;
                Pool pool = sounds.get(request.name);
                if (pool == null) {
                    pool = load(request.name, request.volume);
                    if (pool == null) {
                        unavailable.add(request.name);
                        continue;
                    }
                    sounds.put(request.name, pool);
                }
                for (int i = 0; i < pool.voices.length; i++) {
                    Clip clip = pool.voices[i];
                    if (clip.isRunning()) continue;
                    if (pool.levels[i] != request.volume) {
                        byte[] pcm = adjust(pool.samples, pool.baseline, pool.maxGain, request.volume);
                        clip.close();
                        clip.open(pool.format, pcm, 0, pcm.length);
                        pool.levels[i] = request.volume;
                    }
                    clip.setFramePosition(0);
                    clip.start();
                    break;
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
                // No audio device or exhausted mixer: never crash or stall Minecraft.
            }
        }
    }

    private static Pool load(String name, int volume) {
        java.net.URL resource = SoundUtils.class.getResource("/assets/raindots/sounds/" + name + ".wav");
        if (resource == null) return null;
        ArrayList<Clip> voices = new ArrayList<>(VOICES);
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(resource)) {
            AudioFormat format = audio.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                    || format.getSampleSizeInBits() != 16 || format.isBigEndian()) return null;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = audio.read(buffer)) != -1) bytes.write(buffer, 0, length);
            byte[] samples = bytes.toByteArray();
            int peak = 1;
            for (int i = 0; i + 1 < samples.length; i += 2) {
                int sample = (short) ((samples[i] & 255) | (samples[i + 1] << 8));
                peak = Math.max(peak, Math.abs(sample));
            }
            // Most bundled mouse sounds have tiny peaks. At 100%, bring quiet
            // modes to ~48% peak; never turn an already loud asset down.
            float baseline = Math.max(1.0F, 15728.0F / peak);
            float maxGain = Math.max(1.0F, 31457.0F / peak);
            byte[] pcm = adjust(samples, baseline, maxGain, volume);
            for (int i = 0; i < VOICES; i++) {
                Clip voice = null;
                try {
                    voice = AudioSystem.getClip();
                    voice.open(format, pcm, 0, pcm.length);
                    voices.add(voice);
                } catch (Exception failure) {
                    if (voice != null) voice.close();
                    if (voices.isEmpty()) throw failure;
                    break; // Mixers with only one available voice still work.
                }
            }
            return new Pool(format, samples, baseline, maxGain, voices.toArray(new Clip[0]), volume);
        } catch (Exception failure) {
            for (Clip voice : voices) voice.close();
            return null;
        }
    }

    private static byte[] adjust(byte[] source, float baseline, float maxGain, int volume) {
        byte[] output = new byte[source.length];
        // 200% reaches ~96% peak on normalized sounds, without clipping loud assets.
        double gain = Math.min(maxGain, baseline * volume / 100.0);
        for (int i = 0; i + 1 < source.length; i += 2) {
            int original = (short) ((source[i] & 255) | (source[i + 1] << 8));
            int scaled = (int) Math.round(original * gain);
            scaled = Math.max(-32768, Math.min(32767, scaled));
            output[i] = (byte) scaled;
            output[i + 1] = (byte) (scaled >> 8);
        }
        return output;
    }
}
