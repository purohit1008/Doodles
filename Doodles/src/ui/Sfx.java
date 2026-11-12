package ui;

import javax.sound.sampled.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiny synthesized sound effects (no audio files). Every call is fire-and-forget and silently
 * does nothing if the machine has no audio output.
 */
public final class Sfx {

    private Sfx() {}

    public enum Sound { POP, CORRECT, OTHER_CORRECT, TICK, ROUND_START, ROUND_END, POWER, HIT, FANFARE, SELECT }

    private static final float RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    private static final Map<Sound, byte[]> CACHE = new EnumMap<>(Sound.class);
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sfx");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean enabled = true;

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean on) { enabled = on; }

    public static void play(Sound s) {
        if (!enabled) return;
        POOL.submit(() -> {
            try {
                byte[] data;
                synchronized (CACHE) { data = CACHE.computeIfAbsent(s, Sfx::render); }
                Clip clip = AudioSystem.getClip();
                clip.open(FORMAT, data, 0, data.length);
                clip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) clip.close(); });
                clip.start();
            } catch (Throwable ignored) {
                // no sound device: the game simply stays quiet
            }
        });
    }

    // ---- synthesis ----

    /** note = {frequencyStart, frequencyEnd, milliseconds, volume} */
    private static byte[] render(Sound s) {
        double[][] notes = switch (s) {
            case POP -> new double[][]{{700, 1250, 90, 0.9}};
            case CORRECT -> new double[][]{{523, 523, 90, 1}, {659, 659, 90, 1}, {784, 784, 90, 1}, {1047, 1047, 220, 1}};
            case OTHER_CORRECT -> new double[][]{{660, 660, 120, 0.6}};
            case TICK -> new double[][]{{1050, 1050, 45, 0.7}};
            case ROUND_START -> new double[][]{{440, 440, 110, 0.9}, {660, 660, 190, 0.9}};
            case ROUND_END -> new double[][]{{660, 660, 110, 0.8}, {523, 523, 110, 0.8}, {392, 392, 220, 0.8}};
            case POWER -> new double[][]{{300, 950, 230, 0.8}};
            case HIT -> new double[][]{{240, 160, 220, 1}};
            case FANFARE -> new double[][]{{523, 523, 120, 1}, {523, 523, 120, 1}, {523, 523, 120, 1}, {659, 659, 260, 1},
                    {587, 587, 120, 1}, {659, 659, 120, 1}, {784, 784, 420, 1}};
            case SELECT -> new double[][]{{780, 780, 55, 0.7}};
        };
        int total = 0;
        for (double[] n : notes) total += (int) (RATE * n[2] / 1000.0);
        byte[] out = new byte[total * 2];
        int pos = 0;
        for (double[] n : notes) {
            int len = (int) (RATE * n[2] / 1000.0);
            double phase = 0;
            for (int i = 0; i < len; i++) {
                double t = i / (double) len;
                double f = n[0] + (n[1] - n[0]) * t;
                phase += 2 * Math.PI * f / RATE;
                double env = Math.min(1, i / 120.0) * Math.pow(1 - t, 1.6);       // quick attack, soft decay
                double v = (Math.sin(phase) * 0.8 + Math.sin(phase * 2) * 0.15) * env * n[3] * 0.32;
                short sv = (short) Math.max(-32767, Math.min(32767, v * 32767));
                out[pos++] = (byte) (sv & 0xFF);
                out[pos++] = (byte) ((sv >> 8) & 0xFF);
            }
        }
        return out;
    }
}
