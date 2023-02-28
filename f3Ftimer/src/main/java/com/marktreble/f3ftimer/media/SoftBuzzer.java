/*
 *     ___________ ______   _______
 *    / ____/__  // ____/  /_  __(_)___ ___  ___  _____
 *   / /_    /_ </ /_       / / / / __ `__ \/ _ \/ ___/
 *  / __/  ___/ / __/      / / / / / / / / /  __/ /
 * /_/    /____/_/        /_/ /_/_/ /_/ /_/\___/_/
 *
 * Open Source F3F timer UI and scores database
 *
 */

package com.marktreble.f3ftimer.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Build;

import com.marktreble.f3ftimer.helpers.soundpool.SoundPoolHelper;

public class SoftBuzzer {
    private static SoundPool soundPool;
    private int[] soundArray;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void init() {
        soundPool = SoundPoolHelper.getSoundPool(1);
    }

    public void setSounds(Context context, Intent intent, String[] sounds) {
        // soundArray is loaded from preferences
        soundArray = new int[sounds.length];
        int i = 0;
        for (String sound : sounds) {
            String value = intent.getStringExtra(sound);
            int id = context.getResources().getIdentifier(value, "raw", context.getPackageName());
            soundArray[i++] = soundPool.load(context, id, 1);
        }
    }

    public void setSound(Context context, String key, String value, String[] sounds) {
        int i = 0;
        for (String sound : sounds) {
            if (key.equals(sound)) {
                int id = context.getResources().getIdentifier(value, "raw", context.getPackageName());
                soundArray[i] = soundPool.load(context, id, 1);
            }
            i++;
        }
    }

    public void destroy() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void soundOffCourse() {
        soundPool.play(soundArray[0], 1, 1, 1, 0, 1f);
    }

    public void soundOnCourse() {
        soundPool.play(soundArray[1], 1, 1, 1, 0, 1f);
    }

    public void soundTurn() {
        soundPool.play(soundArray[2], 1, 1, 1, 0, 1f);
    }

    public void soundTurn9() {
        soundPool.play(soundArray[3], 1, 1, 1, 0, 1f);
    }

    public void soundPenalty() {
        soundPool.play(soundArray[4], 1, 1, 1, 0, 1f);
    }

}
