package com.marktreble.f3ftimer.helpers.soundpool;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

public class SoundPoolHelper {
    /**
     * Compatibility wrapper for instantiating a SoundPool
     *
     * This provides a normalised function across ALL API levels.
     *
     * @param maxStreams maximum number of streams
     * @return SoundPool
     */
    @SuppressWarnings("deprecation")
    public static SoundPool getSoundPool(Integer maxStreams) {
        SoundPool soundPool = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool
                    .Builder()
                    .setMaxStreams(maxStreams)
                    .build();
        } else {
            soundPool = new SoundPool(
                    maxStreams,
                    AudioManager.STREAM_MUSIC,
                    0
            );
        }
        return soundPool;
    }
}
