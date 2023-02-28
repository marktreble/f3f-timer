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

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.marktreble.f3ftimer.languages.Languages;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class TTS implements TextToSpeech.OnInitListener {

    private static final String TAG = "TTS";
    private static TTS sharedTTS;
    private TextToSpeech mttsengine;
    private onInitListenerProxy mInitListener;
    public int mTTSStatus;
    HashMap<String, String> utterance_ids = new HashMap<>();

    private final WeakReference<Context> mContext;

    public boolean mSetFullVolume = true;


    public interface onInitListenerProxy {
        void onInit(int status);

        void onStart(String utteranceId);

        void onDone(String utteranceId);

        void onError(String utteranceId);
    }

    /**
     * Constructor
     *
     * @param context Context
     */
    private TTS(Context context) {
        Log.i(TAG, "STARTING TTS ENGINE");
        mttsengine = new TextToSpeech(context, this);
        mContext = new WeakReference<>(context);
    }


    /**
     * TTS Engine Singleton creator
     *
     * @param context Context
     * @param listener TTS.onInitListenerProxy
     * @return TTS
     */
    public static TTS sharedTTS(Context context, TTS.onInitListenerProxy listener) {
        if (sharedTTS == null) { //if there is no instance available... create new one
            sharedTTS = new TTS(context);
        }

        if (listener != null)
            sharedTTS.setListener(listener);

        return sharedTTS;
    }

    /**
     * Set listener callbacks
     *
     * @param listener TTS.onInitListenerProxy
     */
    public void setListener(TTS.onInitListenerProxy listener) {
        mInitListener = listener;
    }

    /**
     * Getter for the TTS Engine
     *
     * @return TextToSpeech
     */
    public TextToSpeech ttsengine() {
        return mttsengine;
    }

    /**
     * Clean Up
     */
    public void release() {
        if (mttsengine != null) {
            mttsengine.shutdown();
            mttsengine = null;
        }
        sharedTTS = null;
    }

    /**
     * Initialise the TTS progress listener
     *
     * @param status Int
     */
    public void onInit(int status) {
        mTTSStatus = status;

        if (status == TextToSpeech.SUCCESS) {
            Log.d("TTS", "Initilization Succeeded!");
            initUtteranceListenerForMinICS();
            mInitListener.onInit(status);
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    public String setSpeechFXLanguage(String language, String defaultLang) {
        String lang;
        lang = Languages.setSpeechLanguage(language, defaultLang, ttsengine());
        if (lang != null && !lang.equals("")) {
            Log.i(TAG, "TTS set speech engine language: " + lang);
            ttsengine().setLanguage(Languages.stringToLocale(lang));
        } else {
            // Unchanged, so return the pilot language
            lang = language;
        }
        Log.i(TAG, "TTS LANG: " + lang);

        return lang;
    }

    @SuppressWarnings("deprecation")
    public void speak(String text, int queueMode) {
        setAudioVolume();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsengine().speak(text, queueMode, null, text);
        } else {
            utterance_ids.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
            ttsengine().speak(text, queueMode, utterance_ids);
        }

    }

    public void setAudioVolume() {
        if (mSetFullVolume) {
            AudioManager audioManager = (AudioManager) mContext.get().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
            }
        }
    }

    private void initUtteranceListenerForMinICS() {
        mttsengine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                mInitListener.onStart(utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                mInitListener.onDone(utteranceId);
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onError(String utteranceId) {
                mInitListener.onError(utteranceId);
            }

            @Override
            public void onError(String utteranceId, final int errorCode) {
                mInitListener.onError(utteranceId);
            }
        });
    }
}
