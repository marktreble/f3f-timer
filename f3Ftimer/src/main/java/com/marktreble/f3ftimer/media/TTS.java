package com.marktreble.f3ftimer.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

/**
 * Created by marktreble on 10/01/2018.
 */

public class TTS implements TextToSpeech.OnInitListener {

    private static final String TAG = "TTS";
    private static TTS sharedTTS;
    private TextToSpeech mttsengine;
    private onInitListenerProxy mInitListener;
    public int mTTSStatus;


    public interface onInitListenerProxy {
        void onInit(int status);
        void onStart(String utteranceId);
        void onDone(String utteranceId);
        void onError(String utteranceId);
    }

    private TTS(Context context){
        Log.i(TAG, "STARTING TTS ENGINE");
        mttsengine = new TextToSpeech(context, this);
    }


    public static TTS sharedTTS(Context context, TTS.onInitListenerProxy listener){
        if (sharedTTS == null){ //if there is no instance available... create new one
            sharedTTS =new TTS(context);
        }

        if (listener != null)
            sharedTTS.setListener(listener);

        return sharedTTS;
    }

    public void setListener(TTS.onInitListenerProxy listener){
        mInitListener = listener;
    }

    public TextToSpeech ttsengine(){
        return mttsengine;
    }

    public void release(){
        mttsengine.shutdown();
        mttsengine = null;
        sharedTTS = null;
    }

    public void onInit(int status){
        mTTSStatus = status;

        initUtteranceListenerForMinICS();
        /* DEPRECATED
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            initUtteranceListenerForMinICS();
        } else {
            initUtteranceListenerForOtherThanICS();
        }*/
        mInitListener.onInit(status);
    }

    @TargetApi(15)
    private void initUtteranceListenerForMinICS(){
        mttsengine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                mInitListener.onStart(utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                mInitListener.onDone(utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                mInitListener.onError(utteranceId);

            }
        });
    }

    /* DEPRECATED
    @TargetApi(11)
    private void initUtteranceListenerForOtherThanICS(){
        Log.i(TAG, "SETTING UP UTTERANCE PROGRESS FOR <ICS");
        mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
            @Override
            public void onUtteranceCompleted(String utteranceId) {
                Intent i2 = new Intent("com.marktreble.f3ftimer.onUpdate");
                i2.putExtra("com.marktreble.f3ftimer.service_callback", "hide_progress");
                mContext.sendBroadcast(i2);
            }
        });

    }*/


}
