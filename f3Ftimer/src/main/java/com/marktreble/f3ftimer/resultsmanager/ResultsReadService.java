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

package com.marktreble.f3ftimer.resultsmanager;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.media.TTS;

import java.util.ArrayList;
import java.util.Locale;

public class ResultsReadService extends Service implements Thread.UncaughtExceptionHandler, TTS.onInitListenerProxy {

    private static final String TAG = "ResultsReader";

    private final String[] ordinal = {
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "th", "th", "th", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th",
            "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th"
    };
    private Context mContext;


    private Integer mRid;
    private String mRaceName = "";
    private String mDefaultSpeechLang;
    private boolean mSetFullVolume;

    private TTS mTts;

    public Handler mHandler = new Handler();

    private MediaPlayer mMediaPlayer;

    private int phase;
    private boolean pause;
    private boolean isTalking;
    private String toSpeak = null;
    private int pilotCursor;

    private ArrayList<String> mArrNames = new ArrayList<>();
    private ArrayList<String> mArrNumbers;
    private ArrayList<Float> mArrScores;

    private float mFTD;
    private String mFTDName;
    private int mFTDRound;

    /*
     * General life-cycle function overrides
     */

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;

        this.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE_FROM_UI));

        Thread.setDefaultUncaughtExceptionHandler(this);

        if (mDefaultSpeechLang == null || mDefaultSpeechLang.equals("")) {
            Locale default_lang = Locale.getDefault();
            mDefaultSpeechLang = String.format("%s_%s", default_lang.getLanguage(), default_lang.getCountry());
        }
        Log.i(TAG, "mDefaultSpeechLang=" + mDefaultSpeechLang);

        mTts = null;
        startSpeechSynthesiser();

        mMediaPlayer = MediaPlayer.create(this, R.raw.bennyhill);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroyed");
        super.onDestroy();

        if (mTts != null) {
            mTts.release();
            mTts = null;
        }

        mMediaPlayer.stop();
        mMediaPlayer.release();

        try {
            this.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startDriver(Context context, Integer race_id, String race_name, Bundle params) {
        Intent serviceIntent = new Intent(context, ResultsReadService.class);
        serviceIntent.putExtras(params);
        serviceIntent.putExtra("com.marktreble.f3ftimer.race_id", race_id);
        serviceIntent.putExtra("com.marktreble.f3ftimer.race_name", race_name);
        context.startService(serviceIntent);

    }

    public static boolean stop(ResultsReadActivity context) {
        if (context.isServiceRunning("com.marktreble.f3ftimer.resultsmanager.ResultsReadService")) {
            // Log.d("SERVER STOPPED", Log.getStackTraceString(new Exception()));
            Intent serviceIntent = new Intent(context, ResultsReadService.class);
            context.stopService(serviceIntent);
            return true;
        }
        return false;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // Get the race id
        if (intent.hasExtra("com.marktreble.f3ftimer.race_id")) {

            Bundle extras = intent.getExtras();
            mRid = extras.getInt("com.marktreble.f3ftimer.race_id", 0);
            mRaceName = extras.getString("com.marktreble.f3ftimer.race_name","");
        }

        mDefaultSpeechLang = intent.getStringExtra("pref_voice_lang");
        mSetFullVolume = intent.getBooleanExtra("pref_full_volume", true);

        getNamesArray();

        return (START_STICKY);
    }

    // TTS.onInitListenerProxy
    public void onInit(int status) {
        mTts.setSpeechFXLanguage(mDefaultSpeechLang, mDefaultSpeechLang);
    }

    public void onStart(String utteranceId) {
        isTalking = true;
        mMediaPlayer.setVolume(0.1f, 0.1f);
    }

    public void onDone(String utteranceId) {
        mMediaPlayer.setVolume(1f, 1f);
        isTalking = false;
        toSpeak = null;
        if (pause) {
            mMediaPlayer.pause();
        } else {
            readNext();
        }
    }

    public void onError(String utteranceId) {

    }

    // Binding for UI->Service Communication
    private final BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("UI->Service", "onReceive");
            if (intent.hasExtra(IComm.MSG_UI_CALLBACK)) {
                Bundle extras = intent.getExtras();
                String data = extras.getString(IComm.MSG_UI_CALLBACK);
                Log.d("UI->Service", data);

                if (data == null) return;

                if (data.equals("start_reading")) {
                    startReading();
                    return;
                }

                if (data.equals("pause_reading")) {
                    pauseReading();
                    return;
                }

                if (data.equals("resume_reading")) {
                    resumeReading();
                }
            }
        }
    };

    public void startReading() {
        pilotCursor = mArrNames.size();
        if (pilotCursor == 0) {
            mTts.speak("No rounds flown in the race", TextToSpeech.QUEUE_ADD);
            return;
        }
        mMediaPlayer.seekTo(0);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();
        phase = 1;
        readNext();
    }
    public void pauseReading() {
        pause = true;
        if (!isTalking) {
            mMediaPlayer.pause();
        }
    }

    public void resumeReading() {
        pause = false;
        mMediaPlayer.start();
        if (toSpeak != null) {
            mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
        } else {
            readNext();
        }
    }

    private void readNext() {
        if (pilotCursor == 0) {
            mMediaPlayer.setLooping(false);
            sendCommand("finished");
            return;
        }
        if (phase == 1) {
            // Intro
            phase = 2;
            if (pilotCursor <= 10) phase = 4;
            if (pilotCursor <= 3) phase = 6;
            if (pilotCursor <= 1) phase = 7;
            mHandler.postDelayed(() -> {
                toSpeak = String.format("Results of %s", mRaceName);
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, 4000);
            return;
        }

        if (phase == 2) {
            // From bottom to top 10
            int random = (int)(Math.random() * 500);
            mHandler.postDelayed(() -> {
                if (Math.random() > 0.3) {
                    toSpeak = String.format("In %s place, %s", ssOrds(pilotCursor), mArrNames.get(pilotCursor - 1));
                } else {
                    toSpeak = String.format("%s in %s", mArrNames.get(pilotCursor - 1), ssOrds(pilotCursor));
                }
                pilotCursor--;
                if (pilotCursor <= 10) phase = 3;
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, (pilotCursor%5 == 0) ? 3000 + random : (int)(Math.random() * 500));
            return;
        }

        if (phase == 3) {
            // Intro
            phase = 4;
            mHandler.postDelayed(() -> {
                toSpeak = "And now for the top 10";
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, 4000);
            return;
        }

        if (phase == 4) {
            // From 10 to 4 ( + Read points)
            mHandler.postDelayed(() -> {
                toSpeak = String.format(
                        "In %s place with %d points, is %s",
                        ssOrds(pilotCursor),
                        (int)Math.floor(mArrScores.get(pilotCursor - 1)),
                        mArrNames.get(pilotCursor - 1)
                );
                pilotCursor--;
                if (pilotCursor <= 3) phase = 5;
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, 3000);
            return;
        }

        if (phase == 5) {
            // Intro
            phase = 6;
            mHandler.postDelayed(() -> {
                toSpeak = "And now for the top 3";
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, 4000);
            return;
        }

        if (phase == 6) {
            // Podium
            mHandler.postDelayed(() -> {
                toSpeak = String.format(
                        "In %s place, with %d points, is %s",
                        ssOrds(pilotCursor),
                        (int)Math.floor(mArrScores.get(pilotCursor - 1)),
                        mArrNames.get(pilotCursor - 1)
                );
                pilotCursor--;
                if (pilotCursor <= 1) phase = 7;
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, 4000);
            return;
        }

        if (phase == 7) {
            // Winner
            phase = 8;
            mHandler.postDelayed(() -> {
                toSpeak = String.format(
                        "And the Winner, with %d points",
                        (int)Math.floor(mArrScores.get(pilotCursor - 1))
                );
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, 8000);
            return;
        }

        if (phase == 8) {
            // Winner
            mHandler.postDelayed(() -> {
                toSpeak = String.format("is %s", mArrNames.get(pilotCursor - 1));
                pilotCursor--;
                if (pilotCursor <= 1) phase = 7;
                if (pause) {
                    return;
                }
                mTts.speak(toSpeak, TextToSpeech.QUEUE_ADD);
            }, 4000);
        }
    }

    private void getNamesArray() {
        Results r = new Results();

        r.getResultsForRace(ResultsReadService.this, mRid, true);

        mArrNames = r.mArrNames;
        mArrNumbers = r.mArrNumbers;
        mArrScores = r.mArrScores;

        mFTD = r.mFTD;
        mFTDName = r.mFTDName;
        mFTDRound = r.mFTDRound;

    }

    private String ssOrds(int number) {
        return String.format("%d%s", number, ordinal[number - 1]);
    }

    // Binding for UI->Service Communication
    public void sendCommand(String cmd) {
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, cmd);
        sendBroadcast(i);
    }

    private void startSpeechSynthesiser() {
        mTts = TTS.sharedTTS(mContext, this);
        mTts.mSetFullVolume = mSetFullVolume;
    }

}

