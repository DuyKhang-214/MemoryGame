package com.khang.memorygame;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Âm thanh (ToneGenerator, không cần file âm thanh) và rung. */
public class Feedback {
    private final Context context;
    private final Prefs prefs;
    private ToneGenerator tone;

    public Feedback(Context context, Prefs prefs) {
        this.context = context;
        this.prefs = prefs;
        try {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
        } catch (RuntimeException ignored) {
            tone = null;
        }
    }

    private void beep(int type, int ms) {
        if (tone != null && prefs.isSound()) {
            try { tone.startTone(type, ms); } catch (RuntimeException ignored) { }
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrate(long ms) {
        if (!prefs.isVibrate()) return;
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    public void click() { beep(ToneGenerator.TONE_DTMF_5, 40); }
    public void match() { beep(ToneGenerator.TONE_PROP_ACK, 150); vibrate(60); }
    public void miss() { beep(ToneGenerator.TONE_PROP_NACK, 150); }
    public void win() { beep(ToneGenerator.TONE_PROP_BEEP2, 400); vibrate(250); }

    public void release() {
        if (tone != null) {
            tone.release();
            tone = null;
        }
    }
}
