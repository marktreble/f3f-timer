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

package com.marktreble.f3ftimer.driver;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static java.lang.Math.floor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExportInterface;
import com.marktreble.f3ftimer.languages.Languages;
import com.marktreble.f3ftimer.media.SoftBuzzer;
import com.marktreble.f3ftimer.media.TTS;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;

public class Driver implements TTS.onInitListenerProxy, SpreadsheetExportInterface {

    private static final String TAG = "Driver";

    private static final String[] sounds = {"pref_buzz_off_course", "pref_buzz_on_course", "pref_buzz_turn", "pref_buzz_turn9", "pref_buzz_penalty"};

    private final Context mContext;


    Integer mPid;
    private Integer mRid;
    private Integer mRnd;
    private Race mRace;
    Float mPilot_Time = .0f;
    private long mTimeOnCourse;
    private long mLastLegTime;
    private final long[] mLegTimes = new long[10];
    private Integer mLeg = 0;
    private boolean mWindLegal = true;
    private final long[] mFastestLegTime = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private float mFastestFlightTime = 0.0f;
    private String mFastestFlightPilot = "";
    private Pilot mPilot;

    private boolean mAudibleWindWarning = false;
    public boolean mModelLaunched = false;
    private int mDelayed;


    private Integer mPenalty;

    private boolean mSoundFXon;
    private boolean mSpeechFXon;

    private String mDefaultLang;
    private String mDefaultSpeechLang;
    private String mPilotLang;

    private TTS mTts;
    private boolean mSetFullVolume;

    public Handler mHandler = new Handler(Looper.getMainLooper());

    private String mCalled;
    private boolean mOmitOffCourse;
    private boolean mLateEntry;

    static float SHOW_TIMEOUT_DELAY = 3f; // minutes
    static int ROUND_TIMEOUT = 30; // minutes

    boolean mShowTimeoutExplicit = false;

    private final static int SPEECH_DELAY_TIME = 250;

    private SoftBuzzer softBuzzer;

    private static boolean alreadyfinalised = false;
    private static boolean alreadyReceivedFinalizeReq = false;

    protected boolean mWindMeasurement = true;

    public Driver(Context context) {
        mContext = context;
    }

    public void start(Intent intent) {

        if (intent == null) {
            Log.i(TAG, "Null intent sent to driver");
            return;
        }
        // Get the race id
        if (intent.hasExtra("com.marktreble.f3ftimer.race_id")) {

            Bundle extras = intent.getExtras();
            mRid = extras.getInt("com.marktreble.f3ftimer.race_id", 0);
            mWindMeasurement = extras.getBoolean("pref_wind_measurement", false);
            mSetFullVolume = extras.getBoolean("pref_full_volume", true);
            mAudibleWindWarning = extras.getBoolean("pref_audible_wind_warning", false);

        }

        // Listen for inputs from the UI
        mContext.registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE_FROM_UI));


        softBuzzer = new SoftBuzzer();
        softBuzzer.destroy();
        softBuzzer.init();

        mSoundFXon = intent.getBooleanExtra("pref_buzzer", false);
        if (mSoundFXon) {
            softBuzzer.setSounds(mContext, intent, sounds);
        }
        mSpeechFXon = intent.getBooleanExtra("pref_voice", false);
        mDefaultSpeechLang = intent.getStringExtra("pref_voice_lang");
        if (mDefaultSpeechLang == null || mDefaultSpeechLang.equals("")) {
            Locale default_lang = Locale.getDefault();
            mDefaultSpeechLang = String.format("%s_%s", default_lang.getLanguage(), default_lang.getCountry());
        }
        Log.i(TAG, "mDefaultSpeechLang=" + mDefaultSpeechLang);
        mDefaultLang = Locale.getDefault().getLanguage();
        mPilotLang = mDefaultSpeechLang;

        mTts = null;
        mDefaultLang = Locale.getDefault().getLanguage();

        if (mSpeechFXon) startSpeechSynthesiser();

        // Check timeout status of the round on start
        mHandler.post(checkTimeout);

    }

    // TTS.onInitListenerProxy
    public void onInit(int status) {

    }

    public void onStart(String utteranceId) {

    }

    public void onDone(String utteranceId) {
        Intent i2 = new Intent(IComm.RCV_UPDATE);
        i2.putExtra(IComm.MSG_SERVICE_CALLBACK, "hide_progress");
        mContext.sendBroadcast(i2);
    }

    public void onError(String utteranceId) {

    }

    public void destroy() {
        Log.i(TAG, "Destroyed");

        mHandler.removeCallbacks(checkTimeout);

        try {
            mContext.unregisterReceiver(onBroadcast);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (softBuzzer != null) {
            softBuzzer.destroy();
            softBuzzer = null;
        }

        if (mTts != null) {
            mTts.release();
            mTts = null;
        }
    }

    // Binding for UI->Service Communication
    private final BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            Log.d("UI->Service", "onReceive");
            if (intent.hasExtra(IComm.MSG_WIND_SPEED)) {
                Bundle extras = intent.getExtras();
                String windSpeed = extras.getString(IComm.MSG_WIND_SPEED);
                String windDirection = extras.getString(IComm.MSG_WIND_DIRECTION);

                if (windSpeed != null && windDirection != null) {
                    if (mSpeechFXon) {
                        mTts.setAudioVolume();

                        Resources r = Languages.useLanguage(mContext, mPilotLang);
                        String lang = String.format(
                                r.getString(R.string.wind_speed_announcement),
                                windSpeed
                        );
                        mHandler.postDelayed(
                                () -> mTts.speak(lang, TextToSpeech.QUEUE_ADD),
                                1);
                    }
                }
            }
            if (intent.hasExtra(IComm.MSG_UI_CALLBACK)) {
                Bundle extras = intent.getExtras();
                String data = extras.getString(IComm.MSG_UI_CALLBACK);

                if (data == null) return;

                if (data.equals("show_round_timeout")) {
                    Log.i("DRIVER", "SHOW_ROUND_TIMEOUT");
                    showRoundTimeoutExplicitly();
                    return;
                }
                if (data.equals("start_pilot")) {
                    startPilot(extras);
                    return;
                }

                if (data.equals("working_time")) {
                    startWorkingTime();
                    return;
                }

                if (data.equals("launch")) {
                    modelLaunched();
                    return;
                }

                if (data.equals("baseA")) {
                    ((DriverInterface) mContext).baseA();
                    return;
                }

                if (data.equals("baseB")) {
                    ((DriverInterface) mContext).baseB();
                    return;
                }

                if (data.equals("abort")) {
                    cancelWorkingTime();
                    ((DriverInterface) mContext).sendAbort();
                    return;
                }

                if (data.equals("finalise")) {
                    int delayed = extras.getInt("com.marktreble.f3ftimer.delayed", 0);
                    runFinalised(delayed);
                    return;
                }

                if (data.equals("begin_timeout")) {
                    beginRoundTimeout();
                    return;
                }

                if (data.equals("timeout_resumed")) {
                    //startTimeoutDelay();
                    resumeRoundTimeout();
                    return;
                }

                if (data.equals("cancel_timeout")) {
                    cancelRoundTimeout();
                    return;
                }

                /* Callbacks from SettingsActivity */
                if (data.equals("pref_buzzer")) {
                    mSoundFXon = extras.getBoolean(IComm.MSG_VALUE);
                    return;
                }

                if (data.equals("pref_buzz_off_course")
                        || data.equals("pref_buzz_on_course")
                        || data.equals("pref_buzz_turn")
                        || data.equals("pref_buzz_turn9")
                        || data.equals("pref_buzz_penalty")) {
                    String value = extras.getString(IComm.MSG_VALUE);
                    softBuzzer.setSound(mContext, data, value, sounds);
                    return;
                }

                if (data.equals("pref_voice")) {
                    mSpeechFXon = extras.getBoolean(IComm.MSG_VALUE);

                    if (mSpeechFXon && mTts == null) {
                        startSpeechSynthesiser();
                    }
                    return;

                }

                if (data.equals("pref_voice_lang")) {
                    if (mSpeechFXon) {
                        mDefaultSpeechLang = extras.getString(IComm.MSG_VALUE);
                        // Try to set speech lang - if not available then setSpeechFXLanguage returns the default Language
                        if (mSpeechFXon && mTts != null) {
                            mPilotLang = mTts.setSpeechFXLanguage(mPilotLang, mDefaultSpeechLang);
                        }
                    }
                    return;
                }

                if (data.equals("pref_full_volume")) {

                    mSetFullVolume = extras.getBoolean(IComm.MSG_VALUE);
                    mTts.mSetFullVolume = mSetFullVolume;
                    return;
                }

                if (data.equals("pref_audible_wind_warning")) {
                    mAudibleWindWarning = extras.getBoolean(IComm.MSG_VALUE);
                    return;
                }

                if (data.equals("working_time_20")) {
                    if (!mCalled.equals("20")) {
                        _count("20 seconds working time");
                        mCalled = "20";
                    }
                    return;
                }

                if (data.equals("working_time_15")) {
                    if (!mCalled.equals("15")) {
                        _count("15 seconds working time");
                        mCalled = "15";
                    }
                    return;
                }

                if (data.length() > 2) {
                    if (data.startsWith("::"))
                        ((DriverInterface) mContext).finished(data.substring(2));
                    return;
                }

                // Synthesized Countdown
                int number = -1;
                try {
                    number = Integer.parseInt(data);
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    // Ignore it
                }

                if (number == 12) {
                    mOmitOffCourse = true;
                    mCalled = data;
                    return;
                }

                if (number > 0 && !data.equals(mCalled)) {
                    _count(data);
                    mCalled = data;
                }


                if (number == 0) {
                    mLateEntry = true;
                    timeExpired();
                }

            }
        }
    };

    private void callbackToUI(String cmd, HashMap<String, String> params) {
        Intent i = new Intent(IComm.RCV_UPDATE);
        if (params != null) {
            for (String key : params.keySet()) {
                i.putExtra(key, params.get(key));
            }
        }

        i.putExtra(IComm.MSG_SERVICE_CALLBACK, cmd);
        Log.d("CallBackToUI", cmd);
        mContext.sendBroadcast(i);
    }

    /*
     * Binding for Service->UI Communication
     */

    public void driverConnected(String icon) {
        HashMap<String, String> params = new HashMap<>();
        params.put("icon", icon);
        callbackToUI("driver_started", params);
    }

    public void driverDisconnected(String icon) {
        HashMap<String, String> params = new HashMap<>();
        params.put("icon", icon);
        callbackToUI("driver_stopped", params);
    }

    public void startPilot(@NonNull Bundle extras) {

        mPid = extras.getInt("com.marktreble.f3ftimer.pilot_id");
        mRid = extras.getInt("com.marktreble.f3ftimer.race_id");
        mRnd = extras.getInt("com.marktreble.f3ftimer.round");

        RaceData datasource = new RaceData(mContext);
        datasource.open();
        mRace = datasource.getRace(mRid);
        datasource.close();

        mCalled = "";


        mOmitOffCourse = false;
        mLateEntry = false;
        mLeg = 0;
        mPenalty = 0;
        mTimeOnCourse = 0;
        mModelLaunched = false;


        if (mSpeechFXon && mTts.mTTSStatus == TextToSpeech.SUCCESS) {
            mTts.setAudioVolume();
            mHandler.postDelayed(() -> {
                Log.i(TAG, "SHOWING TTS PROGRESS");
                Intent i = new Intent(IComm.RCV_UPDATE);
                i.putExtra(IComm.MSG_SERVICE_CALLBACK, "show_progress");
                mContext.sendBroadcast(i);
            }, 100);

            mHandler.postDelayed(() -> {
                RacePilotData datasource2 = new RacePilotData(mContext);
                datasource2.open();
                Pilot pilot = datasource2.getPilot(mPid, mRid);
                datasource2.close();
                if (pilot.language != null && !pilot.language.equals("")) {
                    mPilotLang = String.format("%s_%s", pilot.language, pilot.nationality);
                } else {
                    mPilotLang = mDefaultSpeechLang;
                }

                Log.i(TAG, "SETTING LANGUAGE: " + mPilotLang + " " + mDefaultSpeechLang);
                // Try to set speech lang - if not available then setSpeechFXLanguage returns the default Language
                mPilotLang = mTts.setSpeechFXLanguage(mPilotLang, mDefaultSpeechLang);

                if (mSpeechFXon) {
                    Log.i(TAG, "SETTING VOLUME");
                    mTts.setAudioVolume();
                    Log.i(TAG, "SPEAKING");
                    mTts.speak(String.format("%s %s", pilot.firstname, pilot.lastname), TextToSpeech.QUEUE_ADD);
                }

            }, 200);
        }
        resumeRoundTimeout();
    }

    public void startWorkingTime() {
        cancelRoundTimeout();
        if (mSpeechFXon) {
            mTts.setAudioVolume();
            mHandler.postDelayed(announceWorkingTime, 1);
        }
    }

    public void cancelWorkingTime() {
        if (mSpeechFXon) {
            mHandler.removeCallbacks(announceWorkingTime);
        }
    }

    Runnable announceWorkingTime = new Runnable() {
        @Override
        public void run() {
            Resources r = Languages.useLanguage(mContext, mPilotLang);
            String lang = r.getString(R.string.working_time_started);
            Languages.useLanguage(mContext, mDefaultLang);
            mTts.speak(lang, TextToSpeech.QUEUE_ADD);
        }
    };

    public void modelLaunched() {
        cancelWorkingTime();
        cancelRoundTimeout();

        // Send Launch command to HID
        ((DriverInterface) mContext).sendLaunch();

        // Synthesized Call
        if (mSpeechFXon) {
            mTts.setAudioVolume();
            Resources r = Languages.useLanguage(mContext, mPilotLang);
            String lang = r.getString(R.string.model_launched);
            Languages.useLanguage(mContext, mDefaultLang);
            mTts.speak(lang, TextToSpeech.QUEUE_ADD);
        }

        mModelLaunched = true;
    }

    public void _count(String number) {
        if (mSpeechFXon) {
            mTts.setAudioVolume();
            mTts.speak(number, TextToSpeech.QUEUE_ADD);
        }
    }

    public void offCourse() {
        // Post to the UI that the model has exited the course
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "off_course");
        mContext.sendBroadcast(i);

        // Buzzer Sound
        //if (mSoundFXon) mPlayer.start();

        if (mSoundFXon) {
            mTts.setAudioVolume();
            softBuzzer.soundOffCourse();
        }

        // Synthesized Call
        if (mSpeechFXon && !mOmitOffCourse) {
            mTts.setAudioVolume();
            mHandler.postDelayed(() -> {
                String lang = Languages.useLanguage(mContext, mPilotLang).getString(R.string.off_course);
                Languages.useLanguage(mContext, mDefaultLang);
                mTts.speak(lang, TextToSpeech.QUEUE_ADD);
            }, SPEECH_DELAY_TIME);
        }
    }

    public void timeExpired() {
        mTimeOnCourse = System.currentTimeMillis();
    }

    public void onCourse() {
        // Post to the UI that the model has entered the course and the timer starts
        if (mTimeOnCourse == 0) {
            mTimeOnCourse = System.currentTimeMillis();
        }
        mLastLegTime = mTimeOnCourse;
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "on_course");
        mContext.sendBroadcast(i);

        // Buzzer Sound
        //if (mSoundFXon) mPlayer.start();
        if (mSoundFXon) {
            mTts.setAudioVolume();
            softBuzzer.soundOnCourse();
        }

        // Synthesized Call
        if (mSpeechFXon) {
            mTts.setAudioVolume();
            mHandler.postDelayed(() -> {
                String lang;
                if (mLateEntry) {
                    lang = Languages.useLanguage(mContext, mPilotLang).getString(R.string.late_entry);
                } else {
                    lang = Languages.useLanguage(mContext, mPilotLang).getString(R.string.on_course);

                }
                Languages.useLanguage(mContext, mDefaultLang);
                mTts.speak(lang, TextToSpeech.QUEUE_ADD);

            }, SPEECH_DELAY_TIME);
        }
    }

    public void legComplete() {
        if (mLeg >= mFastestLegTime.length) {
            // prevent processing of extra button pushes
            return;
        }

        long now = System.currentTimeMillis();
        long time = now - mLastLegTime;
        long deltaTime = time - mFastestLegTime[mLeg];
        if (mFastestLegTime[mLeg] == 0) {
            mFastestLegTime[mLeg] = time;
            deltaTime = 0;
        }
        mLastLegTime += time;
        mLegTimes[mLeg] = time;
        mLeg++;

        if (mLeg == 10) {
            mModelLaunched = false;
        }

        // calculate the mean
        long mean = (now - mTimeOnCourse) / mLeg;

        // Estimate is current time + (mean*laps remaining)
        long estimate = (now - mTimeOnCourse) + (mean * (10 - mLeg));

        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "leg_complete");
        i.putExtra("com.marktreble.f3ftimer.estimate", estimate);
        i.putExtra("com.marktreble.f3ftimer.number", mLeg);
        i.putExtra("com.marktreble.f3ftimer.legTime", time);
        i.putExtra("com.marktreble.f3ftimer.delta", deltaTime);
        i.putExtra("com.marktreble.f3ftimer.fastestLegTime", mFastestLegTime[mLeg - 1]);
        i.putExtra("com.marktreble.f3ftimer.fastestFlightPilot", mFastestFlightPilot);
        mContext.sendBroadcast(i);

        if (deltaTime < 0) {
            mFastestLegTime[mLeg - 1] = time;
        }
        // Buzzer Sound
        //if (mSoundFXon) mPlayer.start();
        if (mSoundFXon) {
            mTts.setAudioVolume();

            if (mLeg < 9) {
                softBuzzer.soundTurn();
            } else {
                softBuzzer.soundTurn9();
            }
        }

        // Synthesized Call
        if (mSpeechFXon && mLeg < 10 && mLeg > 0) {
            mTts.setAudioVolume();
            final String leg = mLeg + ((mLeg == 9) ? " " + Languages.useLanguage(mContext, mPilotLang).getString(R.string.and_last) : "");

            mHandler.postDelayed(() -> mTts.speak(leg, TextToSpeech.QUEUE_ADD), SPEECH_DELAY_TIME);
        }
    }

    public void incPenalty() {
        mPenalty++;
        // Buzzer Sound
        if (mSoundFXon) {
            mTts.setAudioVolume();
            softBuzzer.soundPenalty();
        }

        Intent i = new Intent(IComm.RCV_LIVE_UPDATE);
        i.putExtra("com.marktreble.f3ftimer.value.penalty", mPenalty);
        mContext.sendBroadcast(i);
    }

    public void runComplete() {
        // Update the UI
        Intent intent = new Intent(IComm.RCV_UPDATE);
        intent.putExtra(IComm.MSG_SERVICE_CALLBACK, "run_complete");
        intent.putExtra("com.marktreble.f3ftimer.time", mPilot_Time);
        if (mFastestFlightTime == 0) {
            intent.putExtra("com.marktreble.f3ftimer.fastestFlightTime", mPilot_Time);
            RacePilotData datasource2 = new RacePilotData(mContext);
            datasource2.open();
            Pilot fastestPilot = datasource2.getPilot(mPid, mRid);
            datasource2.close();
            String fastestPilotStr = String.format("%s %s", fastestPilot.firstname, fastestPilot.lastname);
            intent.putExtra("com.marktreble.f3ftimer.fastestFlightPilot", fastestPilotStr);
        } else {
            intent.putExtra("com.marktreble.f3ftimer.fastestFlightTime", mFastestFlightTime);
            intent.putExtra("com.marktreble.f3ftimer.fastestFlightPilot", mFastestFlightPilot);
        }
        mContext.sendBroadcast(intent);
        alreadyfinalised = false;
        alreadyReceivedFinalizeReq = false;
        mModelLaunched = false;

    }

    public void runFinalised(int delayed) {
        Log.d(TAG, "FINALISE");
        mDelayed = delayed;
        if (!alreadyfinalised) {
            Log.d(TAG, "!ALREADY FINALISED");
            if (!alreadyReceivedFinalizeReq) {
                Log.d(TAG, "!ALREADY RECEIVED FINALISE REQ");
                alreadyReceivedFinalizeReq = true;
                // Save the time to the database
                RacePilotData datasource1 = new RacePilotData(mContext);
                datasource1.open();
                datasource1.setPilotTimeInRound(mRid, mPid, mRnd, mPilot_Time);

                // Get the time
                String str_time = String.format("%.2f", mPilot_Time);
                str_time = str_time.replace(".", " ");

                // Get the pilot's name
                mPilot = datasource1.getPilot(mPid, mRid);

                datasource1.close();

                RaceData datasource = new RaceData(mContext);
                datasource.open();
                if (mFastestFlightTime == 0 || mPilot_Time < mFastestFlightTime) {
                    mFastestFlightTime = mPilot_Time;
                    mFastestFlightPilot = String.format("%s %s", mPilot.firstname, mPilot.lastname);
                    datasource.setFastestFlightTime(mRid, mRnd, mPilot.id, mFastestFlightTime);
                }
                datasource.setFastestLegTimes(mRid, mRnd, mPilot.id, mFastestLegTime);
                datasource.close();

                if (mPenalty > 0) {
                    // Post to the UI that the currently active pilot got a penalty
                    Intent i = new Intent(IComm.RCV_UPDATE);
                    i.putExtra(IComm.MSG_SERVICE_CALLBACK, "incPenalty");
                    i.putExtra("com.marktreble.f3ftimer.pilot_id", mPid);
                    i.putExtra("com.marktreble.f3ftimer.penalty", mPenalty);
                    mContext.sendOrderedBroadcast(i, null);
                    Log.d(TAG, "POST PENALTY BACK TO UI");
                    mPenalty = 0;
                }

                // Speak the time
                if (mSpeechFXon) {
                    mTts.setAudioVolume();
                    mTts.speak(str_time, TextToSpeech.QUEUE_ADD);
                }
                Log.d(TAG, "TIME SPOKEN");

                SpreadsheetExport e = new SpreadsheetExport();
                e.callbackInterface = this;
                e.writeResultsFile(mContext, mRace);
            }
        }
    }

    private void postUpdateToDisplay() {
        // Post to the Race Results Display Service
        Results r = new Results();
        r.getOrderedRoundInProgress(mContext, mRid);

        StringBuilder topthree = new StringBuilder();
        String[] position = {"1st", "2nd", "3rd"};

        for (int count = 0; count < 3; count++) {
            if (r.mArrNames.size() > count) {
                Pilot p = r.mArrPilots.get(count);
                topthree.append(
                        String.format("%s %s %.2f   ", position[count], StringUtils.stripAccents(r.mArrNames.get(count)), p.time)
                );

            }
        }

        String str_name = String.format("%s %s", mPilot.firstname, mPilot.lastname);
        String str_nationality = mPilot.nationality;

        String str_round_results = String.format("Round %d positions: %s", mRnd, topthree);

        //str_round_results = "Testing...";

        Intent intent2 = new Intent("com.marktreble.f3ftimer.onExternalUpdate");
        intent2.putExtra("com.marktreble.f3ftimer.external_results_callback", "run_finalised");
        intent2.putExtra("com.marktreble.f3ftimer.pilot_nationality", str_nationality);
        intent2.putExtra("com.marktreble.f3ftimer.pilot_name", str_name);
        intent2.putExtra("com.marktreble.f3ftimer.pilot_time", String.format("%.2f", mPilot_Time));
        intent2.putExtra("com.marktreble.f3ftimer.current_round", String.format("%d", mRnd));
        intent2.putExtra("com.marktreble.f3ftimer.current_round_results", str_round_results);


        mContext.sendBroadcast(intent2);
        Log.d(TAG, "POST BACK TO EXTERNAL RESULTS: " + str_round_results);
    }

    @Override
    public void onSpreadsheetWritten(boolean success) {
        Log.d(TAG, "SPREADSHEETWRITTENDr");
        postUpdateToDisplay();

        if (mDelayed != 0) {
            // Post back to the UI (RaceTimerActivity) after timeout;
            mHandler.postDelayed(() -> {
                if (!alreadyfinalised) {
                    alreadyfinalised = true;
                    Intent intent = new Intent(IComm.RCV_UPDATE);
                    intent.putExtra(IComm.MSG_SERVICE_CALLBACK, "run_finalised");
                    mContext.sendOrderedBroadcast(intent, null);
                    Log.d(TAG, "POST BACK TO UI");
                }
            }, 5000);
        } else {
            // Post back to the UI (RaceTimerActivity), when the user clicks the button before the timeout runs out;
            alreadyfinalised = true;
            Intent intent = new Intent(IComm.RCV_UPDATE);
            intent.putExtra(IComm.MSG_SERVICE_CALLBACK, "run_finalised");
            mContext.sendOrderedBroadcast(intent, null);
            Log.d(TAG, "POST BACK TO UI");
        }
    }

    void windLegal() {
        if (mWindMeasurement) {
            if (!mWindLegal) {
                mWindLegal = true;
                Intent i = new Intent(IComm.RCV_UPDATE);
                i.putExtra(IComm.MSG_SERVICE_CALLBACK, "conditions_legal");
                mContext.sendBroadcast(i);
            }
        }
    }

    void windIllegal() {
        if (mWindMeasurement) {
            if (mWindLegal) {
                mWindLegal = false;
                Intent i = new Intent(IComm.RCV_UPDATE);
                i.putExtra(IComm.MSG_SERVICE_CALLBACK, "conditions_illegal");
                mContext.sendBroadcast(i);

                if (mSpeechFXon) {
                    mTts.setAudioVolume();
                    // use contest language instead of pilot language here, so that the operator can understand this
                    String text = Languages.useLanguage(mContext, mDefaultSpeechLang).getString(R.string.wind_warning);
                    Languages.useLanguage(mContext, mDefaultLang);
                    //setSpeechFXLanguage(mDefaultSpeechLang);
                    mTts.speak(text, TextToSpeech.QUEUE_ADD);
                    //setSpeechFXLanguage(mPilotLang);
                }
            }
        }
    }

    public void startPressed() {
        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "start_pressed");
        mContext.sendBroadcast(i);

    }

    public void ready() {
        mLeg = 0;
    }

    private void startSpeechSynthesiser() {
        mTts = TTS.sharedTTS(mContext, this);
        mTts.mSetFullVolume = mSetFullVolume;
    }


    public void beginRoundTimeout() {
        Log.i(TAG, "Timeout Started");

        // Start round inactive timeout (3 minutes)
        long startRoundTimeout = System.currentTimeMillis();

        // Save timestamp to sharedPreferences
        String key = "Timeout" + mRid;
        SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = timeout.edit();
        editor.putLong("start", startRoundTimeout);
        editor.putLong("last_show_dialog", startRoundTimeout);
        editor.apply();

        //startTimeoutDelay();
        mHandler.postDelayed(checkTimeout, 60000);
    }

    public void resumeRoundTimeout() {
        String key = "Timeout" + mRid;
        SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = timeout.edit();
        editor.putLong("last_show_dialog", System.currentTimeMillis());
        editor.apply();
    }

    private void cancelRoundTimeout() {
        // Reset the round timeout
        SharedPreferences timeout = mContext.getSharedPreferences("Timeout" + mRid, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = timeout.edit();
        editor.putLong("start", 0);
        editor.apply();
    }

    private void cancelDialog() {

        Intent i = new Intent(IComm.RCV_UPDATE);
        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "cancel");
        mContext.sendBroadcast(i);
    }

    private final Runnable checkTimeout = new Runnable() {
        public void run() {
            String key = "Timeout" + mRid;
            SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
            final long start = timeout.getLong("start", 0);
            final long last_show_dialog = timeout.getLong("last_show_dialog", 0);
            if (start > 0) {
                float totalSeconds = (float) (System.currentTimeMillis() - start) / 1000;
                float secondsSinceLastShow = (float) (System.currentTimeMillis() - last_show_dialog) / 1000;
                double iTime = floor(((60 * ROUND_TIMEOUT) - (totalSeconds-1)) / 60);

                /* Round Timer Expired */
                if (totalSeconds > 60 * ROUND_TIMEOUT) {
                    // Hide the Pilot dialog if it's listening
                    cancelDialog();

                    // Invoke the scrub round dialog
                    mHandler.postDelayed(() -> {
                        Intent i = new Intent(IComm.RCV_UPDATE);
                        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "show_timeout_complete");
                        mContext.sendBroadcast(i);
                    }, 500);
                    return;

                } else if (secondsSinceLastShow >= 60 * SHOW_TIMEOUT_DELAY
                    || mShowTimeoutExplicit
                    || iTime < 3) { // minutes
                    // Hide the Pilot dialog if it's listening
                    cancelDialog();

                    // Invoke the round timeout countdown dialog
                    mHandler.postDelayed(() -> {
                        Intent i = new Intent(IComm.RCV_UPDATE);
                        i.putExtra(IComm.MSG_SERVICE_CALLBACK, "show_timeout");
                        i.putExtra("start", start);
                        mContext.sendBroadcast(i);
                    }, 500);
                }
                long nextIterationTimeInterval = 60 - ((long)totalSeconds % 60);

                mHandler.postDelayed(checkTimeout, nextIterationTimeInterval * 1000);

                // If showed explicit then don't speak!
                if (mShowTimeoutExplicit) {
                    mShowTimeoutExplicit = false;
                    return;
                }

                int[] speakOn = { 25, 20, 15, 10, 5, 4, 3, 2, 1 };
                if (contains(speakOn, (int)iTime)) {
                    Resources r = Languages.useLanguage(mContext, mDefaultSpeechLang);
                    mTts.setSpeechFXLanguage(mDefaultSpeechLang, mDefaultSpeechLang);

                    final String str_time = String.format(
                            "%d %s",
                            (int) iTime,
                            r.getString(R.string.minutes_remaining)
                    );
                    Languages.useLanguage(mContext, mDefaultLang);
                    if (mSpeechFXon) {
                        mTts.setAudioVolume();
                        mTts.speak(str_time, TextToSpeech.QUEUE_ADD);
                    }
                }
            }
        }
    };

    private void showRoundTimeoutExplicitly() {
        String key = "Timeout" + mRid;
        SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
        final long start = timeout.getLong("start", 0);
        if (start > 0) {
            mShowTimeoutExplicit = true;
            mHandler.removeCallbacks(checkTimeout);
            mHandler.post(checkTimeout);
        } else {
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra(IComm.MSG_SERVICE_CALLBACK, "show_timeout_not_started");
            mContext.sendBroadcast(i);
        }
    }
}
