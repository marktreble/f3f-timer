/*
 * Driver
 * Common functions for all driver HID drivers
 * Handles sounds + UI communications
 */

package com.marktreble.f3ftimer.driver;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.languages.Languages;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;


public class Driver implements TextToSpeech.OnInitListener {

	private static final String TAG = "Driver";

	private Context mContext;
    private RaceData datasource;
	
	public Integer mPid;
	public Integer mRid;
    public Integer mRnd = -1;
	public Race mRace;
	public Float mPilot_Time = .0f;
	public long mTimeOnCourse;
    public Integer mLeg = 0;
    public boolean mWindLegal = true;
    private long mFastestLegTime[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private float mFastestFlightTime = 0.0f;
    private String mFastestFlightPilot = "";
    private Integer mPenalty;

	public long mLastLegTime;
	public long[] mLegTimes = new long[10];


	private boolean mSoundFXon;
	private boolean mSpeechFXon;
	
	private String mDefaultLang;
	private String mDefaultSpeechLang;
	private String mPilotLang;
	
	private TextToSpeech mTts;
	private int mTTSStatus;

	public Handler mHandler = new Handler();
	
	private String mCalled;
	private boolean mOmitOffCourse;
	private boolean mLateEntry;

	static float SHOW_TIMEOUT_DELAY = 3f; // minutes
	static int ROUND_TIMEOUT = 33; // minutes
	
    private final static int SPEECH_DELAY_TIME = 250;

    HashMap<String, String> utterance_ids = new HashMap<>();

    private static SoundPool soundPool;
    private int[] sm = new int[2];

    private static boolean alreadyfinalised = false;
	private static boolean alreadyReceivedFinalizeReq = false;

    protected boolean mWindMeasurement = true;


	public Driver(Context context){
		mContext = context;
	}
	
	public void start(Intent intent){
		
		if (intent == null){
			Log.i(TAG, "Null intent sent to driver");
			return;
		}
		// Get the race id
		if (intent.hasExtra("com.marktreble.f3ftimer.race_id")){

			Bundle extras = intent.getExtras();
			mRid = extras.getInt("com.marktreble.f3ftimer.race_id");
    	}
        mWindMeasurement = (intent.getStringExtra("pref_wind_measurement").equals("true"));

        mDefaultLang = Locale.getDefault().getLanguage();

		
    	// Listen for inputs from the UI
		mContext.registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdateFromUI"));

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }

        sm[0] = soundPool.load(mContext, R.raw.beep, 1);
        sm[1] = soundPool.load(mContext, R.raw.base1, 1);

		// why not let the user set the volume to the preferred level?
		//AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
        
		mSoundFXon = (intent.getStringExtra("pref_buzzer").equals("true"));
		mSpeechFXon = (intent.getStringExtra("pref_voice").equals("true"));
		mDefaultSpeechLang = intent.getStringExtra("pref_voice_lang");
		if (mDefaultSpeechLang == null || mDefaultSpeechLang.equals("")) {
			Locale default_lang = Locale.getDefault();
			mDefaultSpeechLang = String.format("%s_%s", default_lang.getLanguage(), default_lang.getCountry());
		}
        Log.i(TAG, "mDefaultSpeechLang=" + mDefaultSpeechLang);
		mDefaultLang = Locale.getDefault().getLanguage();
		mPilotLang = mDefaultSpeechLang;

		mTts = null;

		startSpeechSynthesiser();

		datasource = new RaceData(mContext);
		datasource.open();

		// Check timeout status of the round on start
		if (intent.hasExtra("com.marktreble.f3ftimer.race_id")) {
			mRace = datasource.getRace(mRid);
			if (mRace.status != Race.STATUS_COMPLETE) mHandler.post(checkTimeout);
			else cancelTimeout();
		} else {
			mHandler.post(checkTimeout);
		}
	}


	public void destroy(){
        Log.i(TAG, "Destroyed");

		datasource.close();

        try {
  		 mContext.unregisterReceiver(onBroadcast);
		} catch (IllegalArgumentException e){
			e.printStackTrace();
		}
  		
  		if (mTts != null) {
			// Suppress error generated in TTS engine
			try {
				mTts.stop();
				mTts.shutdown();
			} catch (Exception e){
				e.printStackTrace();
			}
		}

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
	}

	// Binding for UI->Service Communication
    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("UI->Service", "onReceive");
			if (intent.hasExtra("com.marktreble.f3ftimer.ui_callback")){
				Bundle extras = intent.getExtras();
				String data = extras.getString("com.marktreble.f3ftimer.ui_callback");
				Log.d("UI->Service", data);

				if (data == null) return;

                if (data.equals("show_round_timeout")){
					Log.i("DRIVER", "SHOW_ROUND_TIMEOUT");
                    show_round_timeout_explicitly();
                    return;
                }
				if (data.equals("start_pilot")){
					startPilot(extras);
					return;
				}
				
				if (data.equals("working_time")){
					startWorkingTime();
					return;
				}
				
				if (data.equals("launch")){	
					modelLaunched();
					return;
				}

                if (data.equals("baseA")){
                    ((DriverInterface)mContext).baseA();
					return;
                }

                if (data.equals("baseB")){
                    ((DriverInterface)mContext).baseB();
					return;
                }

				if (data.equals("abort")){
					mHandler.removeCallbacks(announceWorkingTime);
					((DriverInterface)mContext).sendAbort();
					return;
				}

                if (data.equals("finalise")){
                    int delayed = extras.getInt("com.marktreble.f3ftimer.delayed");
                    runFinalised(delayed);
					return;
				}

                if (data.equals("begin_timeout")){
                    beginRoundTimeout();
					return;
                }
                
				if (data.equals("timeout_resumed")){	
					startTimeoutDelay();
					return;
				}
				
				if (data.equals("cancel_timeout")){
					cancelTimeout();
					return;
				}

				/* Callbacks from SettingsActivity */
				if (data.equals("pref_buzzer")){	
					mSoundFXon = extras.getBoolean("com.marktreble.f3ftimer.value");
					return;
				}

				if (data.equals("pref_voice")){	
					mSpeechFXon = extras.getBoolean("com.marktreble.f3ftimer.value");
					startSpeechSynthesiser();
					return;
				}
				
				if (data.equals("pref_voice_lang")){	
					if (mSpeechFXon){
						mDefaultSpeechLang = extras.getString("com.marktreble.f3ftimer.value");
				  		// Try to set speech lang - if not available then setSpeechFXLanguage returns the default Language
						mPilotLang = setSpeechFXLanguage(mPilotLang);
					}
					return;
				}

                if (data.equals("pref_wind_measurement")) {
                    mWindMeasurement = intent.getExtras().getBoolean("com.marktreble.f3ftimer.value");
                    Log.d("TcpIoService", "pref_wind_measurement=" + mWindMeasurement);
                    return;
                }

				if (data.length()>2){
					if (data.substring(0,2).equals("::"))
						((DriverInterface)mContext).finished(data.substring(2));
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
				if ( number >0 && !data.equals(mCalled)){
					_count(data);
					mCalled = data;
				}

				if ( number<=10)
					mOmitOffCourse = true;

				if ( number == 0)
					mLateEntry = true;
			}
		}
	};

	private void callbackToUI(String cmd, HashMap<String, String> params){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		if (params != null) {
			for (String key : params.keySet()){
				i.putExtra(key, params.get(key));
			}
		}

		i.putExtra("com.marktreble.f3ftimer.service_callback", cmd);
		Log.d("CallBackToUI", cmd);
		mContext.sendBroadcast(i);
	}

    /*
     * Binding for Service->UI Communication
     */

    public void driverConnected(String icon){
		HashMap<String, String> params = new HashMap<>();
		params.put("icon", icon);
		callbackToUI("driver_started", params);
	}

	public void driverDisconnected(String icon){
		HashMap<String, String> params = new HashMap<>();
		params.put("icon", icon);
		callbackToUI("driver_stopped", params);
	}

	public void startPilot(Bundle extras){
        
		mPid = extras.getInt("com.marktreble.f3ftimer.pilot_id");
		mRid = extras.getInt("com.marktreble.f3ftimer.race_id");
        int lastRound = mRnd;
		mRnd = extras.getInt("com.marktreble.f3ftimer.round");
		
        for (int i = 0; i < mFastestLegTime.length; i++) {
            mFastestLegTime[i] = 0;
        }
        Integer tmpPid[] = {0};
        datasource.getFastestLegTimes(mRid, mRnd, tmpPid, mFastestLegTime);
        float tmpFloat[] = {0.0f};
        datasource.getFastestFlightTime(mRid, mRnd, tmpPid, tmpFloat);
        if (tmpPid[0] != 0) {
            RacePilotData datasource2 = new RacePilotData(mContext);
            datasource2.open();
            Pilot fastestPilot = datasource2.getPilot(tmpPid[0], mRid);
            datasource2.close();
            mFastestFlightPilot = String.format("%s %s", fastestPilot.firstname, fastestPilot.lastname);
        }
        // the pilot id should be the same as for fastest leg times
        mFastestFlightTime = tmpFloat[0];
  		mCalled = "";


		mOmitOffCourse = false;
		mLateEntry = false;
		mLeg = 0;

        mPenalty = 0;

  		if (mSpeechFXon && mTTSStatus == TextToSpeech.SUCCESS){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "SHOWING TTS PROGRESS");
                    Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
                    i.putExtra("com.marktreble.f3ftimer.service_callback", "show_progress");
                    mContext.sendBroadcast(i);
                }
            }, 100);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    RacePilotData datasource2 = new RacePilotData(mContext);
                    datasource2.open();
                    Pilot pilot = datasource2.getPilot(mPid, mRid);
                    datasource2.close();
                    if (pilot.language!= null && !pilot.language.equals("")){
                        mPilotLang = String.format("%s_%s", pilot.language, pilot.nationality);
                    } else {
                        mPilotLang = mDefaultSpeechLang;
                    }
					/*
                    Log.i("startPilot:", "Race ID = "+Integer.toString(mRid));
                    Log.i("startPilot:", "Pilot ID = "+Integer.toString(mPid));
                    Log.i("startPilot:", "Round ID = "+Integer.toString(mRnd));
                    Log.i("startPilot:", pilot.toString());
                    Log.i("startPilot:", "lang = "+pilot.language + ":" + mPilotLang);
					*/

                    // Try to set speech lang - if not available then setSpeechFXLanguage returns the default Language
                    mPilotLang = setSpeechFXLanguage(mPilotLang);

                    String text = Languages.useLanguage(mContext, mPilotLang).getString(R.string.next_pilot);
                    Languages.useLanguage(mContext, mDefaultLang);
                    speak(String.format("%s %s %s",text , pilot.firstname, pilot.lastname), TextToSpeech.QUEUE_ADD);
                }
            }, 200);
  		}
  		startTimeoutDelay();
	}
	
	public void startWorkingTime(){
		cancelTimeout();
		if (mSpeechFXon){
			mHandler.postDelayed(announceWorkingTime, 1000);
		}
	}

	Runnable announceWorkingTime = new Runnable(){
		@Override
		public void run() {
			Resources r = Languages.useLanguage(mContext, mPilotLang);
			String lang = r.getString(R.string.working_time_started);
			Languages.useLanguage(mContext, mDefaultLang);
			speak(lang, TextToSpeech.QUEUE_ADD);
		}
	};

	public void modelLaunched(){
		mHandler.removeCallbacks(announceWorkingTime);
		cancelTimeout();
		
	    // Send Launch command to HID
		((DriverInterface)mContext).sendLaunch();
		
		// Synthesized Call
		if (mSpeechFXon){
			Resources r = Languages.useLanguage(mContext, mPilotLang);
			String lang = r.getString(R.string.model_launched);
	    	Languages.useLanguage(mContext, mDefaultLang);
	    	speak(lang, TextToSpeech.QUEUE_ADD);
		}

	}
	
	public void _count(String number){	
		if (mSpeechFXon)
			speak(number, TextToSpeech.QUEUE_ADD);
	}	
	
	public void incPenalty(){
        mPenalty++;
		// Buzzer Sound
		if (mSoundFXon) soundPool.play(sm[0], 1, 1, 1, 0, 1f); //mPlayerBeep.start();

        Intent i = new Intent("com.marktreble.f3ftimer.onLiveUpdate");
        i.putExtra("com.marktreble.f3ftimer.value.penalty", mPenalty);
        mContext.sendBroadcast(i);
    }

	public void offCourse(){
		// Post to the UI that the model has exited the course
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "off_course");
		mContext.sendBroadcast(i);

		// Buzzer Sound
		if (mSoundFXon) soundPool.play(sm[1], 1, 1, 1, 0, 1f); //mPlayer.start();
		// Synthesized Call
		if (mSpeechFXon && !mOmitOffCourse){
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    String lang = Languages.useLanguage(mContext, mPilotLang).getString(R.string.off_course);
                    Languages.useLanguage(mContext, mDefaultLang);
                    speak(lang, TextToSpeech.QUEUE_ADD);
                }
            }, SPEECH_DELAY_TIME);
		}
	}
	
	public void onCourse(){	
		// Post to the UI that the model has entered the course and the timer starts
		mTimeOnCourse = System.currentTimeMillis();
		mLastLegTime = mTimeOnCourse;
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "on_course");
		mContext.sendBroadcast(i);
			
		// Buzzer Sound
		if (mSoundFXon) soundPool.play(sm[1], 1, 1, 1, 0, 1f);
		// Synthesized Call
		if (mSpeechFXon){
            mHandler.postDelayed(new Runnable() {
                public void run() {
					String lang;
					if (mLateEntry) {
						lang = Languages.useLanguage(mContext, mPilotLang).getString(R.string.late_entry);
					} else {
						lang = Languages.useLanguage(mContext, mPilotLang).getString(R.string.on_course);

					}
					Languages.useLanguage(mContext, mDefaultLang);
					speak(lang, TextToSpeech.QUEUE_ADD);

                }
            }, SPEECH_DELAY_TIME);
		}
	}

	public void legComplete(){
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
		mLastLegTime+=time;
		mLegTimes[mLeg] = time; 
		mLeg++;
			
		// calculate the mean
		long mean = (now-mTimeOnCourse)/mLeg;
			
		// Estimate is current time + (mean*laps remaining)
		long estimate = (now-mTimeOnCourse) + (mean * (10-mLeg)); 
			
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "leg_complete");
		i.putExtra("com.marktreble.f3ftimer.estimate", estimate);
		i.putExtra("com.marktreble.f3ftimer.number", mLeg);
		i.putExtra("com.marktreble.f3ftimer.legTime", time);
		i.putExtra("com.marktreble.f3ftimer.delta", deltaTime);
		i.putExtra("com.marktreble.f3ftimer.fastestLegTime", mFastestLegTime[mLeg-1]);
		i.putExtra("com.marktreble.f3ftimer.fastestFlightPilot", mFastestFlightPilot);
		mContext.sendBroadcast(i);

		if (deltaTime < 0) {
			mFastestLegTime[mLeg-1] = time;
		}

		// Buzzer Sound
		if (mSoundFXon) soundPool.play(sm[1], 1, 1, 1, 0, 1f);

		// Synthesized Call
		if (mSpeechFXon){
			if (mLeg<10 && mLeg>0){
				final String leg = Integer.toString(mLeg) + ((mLeg == 9)? " " + Languages.useLanguage(mContext, mPilotLang).getString(R.string.and_last):"");

				mHandler.postDelayed(new Runnable() {
					public void run() {
						speak(leg, TextToSpeech.QUEUE_ADD);
					}
				}, SPEECH_DELAY_TIME);
			} else if (mLeg == 10) {
				SystemClock.sleep(500); // double length signal
				soundPool.play(sm[1], 1, 1, 1, 0, 1f);//mPlayer.start();
			}
		}
	}

	public void runComplete(){
  		// Update the UI
  		Intent intent = new Intent("com.marktreble.f3ftimer.onUpdate");
		intent.putExtra("com.marktreble.f3ftimer.service_callback", "run_complete");
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
	}
	
	public void runFinalised(int delayed) {
		if (!alreadyfinalised) {
			if (!alreadyReceivedFinalizeReq) {
				alreadyReceivedFinalizeReq = true;
				// Save the time to the database
				RacePilotData datasource1 = new RacePilotData(mContext);
				datasource1.open();
				datasource1.setPilotTimeInRound(mRid, mPid, mRnd, mPilot_Time);

				// Get the time
				String str_time = String.format("%.2f", mPilot_Time);
				str_time = str_time.replace(".", " ");

				// Get the pilot's name
				Pilot pilot = datasource1.getPilot(mPid, mRid);
				String str_name = String.format("%s %s", pilot.firstname, pilot.lastname);
				String str_nationality = pilot.nationality;

				datasource1.close();

				if (mFastestFlightTime == 0 || mPilot_Time < mFastestFlightTime) {
					mFastestFlightTime = mPilot_Time;
					mFastestFlightPilot = String.format("%s %s", pilot.firstname, pilot.lastname);
					datasource.setFastestFlightTime(mRid, mRnd, pilot.id, mFastestFlightTime);
				}
				datasource.setFastestLegTimes(mRid, mRnd, pilot.id, mFastestLegTime);
				if (mPenalty > 0) {
					// Post to the UI that the currently active pilot got a penalty
					Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
					i.putExtra("com.marktreble.f3ftimer.service_callback", "incPenalty");
					i.putExtra("com.marktreble.f3ftimer.pilot_id", mPid);
					i.putExtra("com.marktreble.f3ftimer.penalty", mPenalty);
					mContext.sendOrderedBroadcast(i, null);
					Log.d(TAG, "POST PENALTY BACK TO UI");
					mPenalty = 0;
				}

				// Speak the time
				if (mSpeechFXon) speak(str_time, TextToSpeech.QUEUE_ADD);
				Log.d(TAG, "TIME SPOKEN");

				// Update the .txt file
				new SpreadsheetExport().writeResultsFile(mContext, mRace);
				Log.d(TAG, "EXPORT FILE WRITTEN");
				SystemClock.sleep(1000);

				// Post to the Race Results Display Service
				Results r = new Results();
				r.getOrderedRoundInProgress(mContext, mRid);

				String topthree = "";
				String[] position = {"1st", "2nd", "3rd"};

				for (int count = 0; count < 3; count++) {
					if (r.mArrNames.size() > count) {
						Pilot p = r.mArrPilots.get(count);
						topthree += String.format("%s %s %.2f   ", position[count], StringUtils.stripAccents(r.mArrNames.get(count)), p.time);
					}
				}

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

			if (delayed != 0) {
				// Post back to the UI (RaceTimerActivity) after timeout;
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (!alreadyfinalised) {
							alreadyfinalised = true;
							Intent intent = new Intent("com.marktreble.f3ftimer.onUpdate");
							intent.putExtra("com.marktreble.f3ftimer.service_callback", "run_finalised");
							mContext.sendOrderedBroadcast(intent, null);
							Log.d(TAG, "POST BACK TO UI");
						}
					}
				}, 5000);
			} else {
				// Post back to the UI (RaceTimerActivity), when the user clicks the button before the timeout runs out;
				alreadyfinalised = true;
				Intent intent = new Intent("com.marktreble.f3ftimer.onUpdate");
				intent.putExtra("com.marktreble.f3ftimer.service_callback", "run_finalised");
				mContext.sendOrderedBroadcast(intent, null);
				Log.d(TAG, "POST BACK TO UI");
			}
		}
	}


	public void windLegal(){
        if (mWindMeasurement) {
            if (!mWindLegal) {
                mWindLegal = true;
                Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
                i.putExtra("com.marktreble.f3ftimer.service_callback", "wind_legal");
                mContext.sendBroadcast(i);
            }
        }
	}
	
	public void windIllegal(){
        if (mWindMeasurement) {
            if (mWindLegal) {
                mWindLegal = false;
                Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
                i.putExtra("com.marktreble.f3ftimer.service_callback", "wind_illegal");
                mContext.sendBroadcast(i);

                if (mSpeechFXon) {
					// use contest language instead of pilot language here, so that the operator can understand this
					String text = Languages.useLanguage(mContext, mDefaultSpeechLang).getString(R.string.wind_warning);
					Languages.useLanguage(mContext, mDefaultLang);
					setSpeechFXLanguage(mDefaultSpeechLang);
					speak(text, TextToSpeech.QUEUE_ADD);
					setSpeechFXLanguage(mPilotLang);
				}
            }
        }
	}
	
	public void startPressed(){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "start_pressed");
		mContext.sendBroadcast(i);
		
	}

    public void abort() {
        mHandler.removeCallbacks(announceWorkingTime);
        ((DriverInterface) mContext).sendAbort();
    }
	
	public void ready(){
    	mLeg = 0;
	}

	private void startSpeechSynthesiser() {
		if (mSpeechFXon && mTts == null) {
			mTts = new TextToSpeech(mContext, this);
		}
    }

    public void onInit(int status){
        mTTSStatus = status;
		if (status == TextToSpeech.SUCCESS) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				initUtteranceListenerForMinICS();
			} else {
				initUtteranceListenerForOtherThanICS();
			}
			/* set default language for the case that TTS couldn't read preferred locale from settings.  */
			mTts.setLanguage(Locale.getDefault());
		}
    }

    @TargetApi(15)
    private void initUtteranceListenerForMinICS(){
        Log.i(TAG, "SETTING UP UTTERANCE PROGRESS FOR >=ICS");
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                Intent i2 = new Intent("com.marktreble.f3ftimer.onUpdate");
                i2.putExtra("com.marktreble.f3ftimer.service_callback", "hide_progress");
                mContext.sendBroadcast(i2);
            }

            @Override
            public void onError(String utteranceId) {
                Log.i(TAG, "UTTERANCE_ERROR");

                Intent i2 = new Intent("com.marktreble.f3ftimer.onUpdate");
                i2.putExtra("com.marktreble.f3ftimer.service_callback", "hide_progress");
                mContext.sendBroadcast(i2);
            }
        });
    }
    
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
        
    }
    
    public void	beginRoundTimeout(){
		Log.i(TAG, "Timeout Started");

        // Start round inactive timeout (3 minutes)
        long startRoundTimeout = System.currentTimeMillis();

        // Save timestamp to sharedPreferences
        String key = "Timeout"+Integer.toString(mRid);
        SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = timeout.edit();
        editor.putLong("start", startRoundTimeout);
        editor.apply();

        startTimeoutDelay();
    }
    
	private void startTimeoutDelay(){
    	if (mRace.status == Race.STATUS_COMPLETE) return;

		String key = "Timeout"+Integer.toString(mRid);
		SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
		long start = timeout.getLong("start", 0);
		if (start>0){
			long elapsed = System.currentTimeMillis() - start;
			Log.i(TAG, "Time Elapsed: "+elapsed);
			// Check again in 3 minutes, or time remaining if <3 mins remaining
			long t = (long) Math.min(SHOW_TIMEOUT_DELAY*60*1000, (60*1000 * ROUND_TIMEOUT)-elapsed);
			mHandler.postDelayed(checkTimeout, t);
		}
	}

	private void cancelTimeout(){
		// Reset the round timeout
		SharedPreferences timeout = mContext.getSharedPreferences("Timeout"+Integer.toString(mRid), Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = timeout.edit();
	    editor.putLong("start", 0);
	    editor.apply();
	}
	
	void cancelDialog(){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "cancel");
        i.putExtra("com.marktreble.f3ftimer.pilot_id", mPid);
		mContext.sendBroadcast(i);
	}

    void scoreZeroAndCancelDialogAndNextPilot(){
        Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
        i.putExtra("com.marktreble.f3ftimer.service_callback", "score_zero_and_cancel");
        i.putExtra("com.marktreble.f3ftimer.pilot_id", mPid);
		mContext.sendBroadcast(i);
	}
	
	private Runnable checkTimeout = new Runnable(){
		public void run(){
			String key = "Timeout"+Integer.toString(mRid);
			SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
			final long start = timeout.getLong("start", 0);
			if (start>0){
				long elapsed = System.currentTimeMillis() - start;
				float seconds = (float)elapsed/1000;
				
				if (seconds>60 * ROUND_TIMEOUT){
					// Hide the Pilot dialog if it's listening
					cancelDialog();
					
					// Invoke the scrub round dialog
					mHandler.postDelayed(new Runnable(){
						public void run(){
							Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
							i.putExtra("com.marktreble.f3ftimer.service_callback", "show_timeout_complete");
							mContext.sendBroadcast(i);
						}
					}, 500);
					
				} else if (seconds>60 * SHOW_TIMEOUT_DELAY){ // minutes
					// Hide the Pilot dialog if it's listening
					cancelDialog();

					// Invoke the round timeout countdown dialog
					mHandler.postDelayed(new Runnable(){
						public void run(){
							Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
							i.putExtra("com.marktreble.f3ftimer.service_callback", "show_timeout");
							i.putExtra("start", start);
							mContext.sendBroadcast(i);
						}
					}, 500);
				} else {
					// Check again in SHOW_TIMEOUT_DELAY minutes less the elapsed time
					startTimeoutDelay();
				}
			}
		}
	};

	private void show_round_timeout_explicitly(){
        String key = "Timeout"+Integer.toString(mRid);
        SharedPreferences timeout = mContext.getSharedPreferences(key, Context.MODE_PRIVATE);
        final long start = timeout.getLong("start", 0);
        if (start>0){
            long elapsed = System.currentTimeMillis() - start;
            float seconds = (float)elapsed/1000;

            if (seconds>60 * ROUND_TIMEOUT){
                // Hide the Pilot dialog if it's listening
                cancelDialog();

                // Invoke the scrub round dialog
                mHandler.postDelayed(new Runnable(){
                    public void run(){
                        Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
                        i.putExtra("com.marktreble.f3ftimer.service_callback", "show_timeout_complete");
                        mContext.sendBroadcast(i);
                    }
                }, 500);

            } else { // minutes
                // Hide the Pilot dialog if it's listening
                cancelDialog();

                // Invoke the round timeout countdown dialog
                mHandler.postDelayed(new Runnable(){
                    public void run(){
                        Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
                        i.putExtra("com.marktreble.f3ftimer.service_callback", "show_timeout");
                        i.putExtra("start", start);
                        mContext.sendBroadcast(i);
                    }
                }, 500);
            }
        } else {
			Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
			i.putExtra("com.marktreble.f3ftimer.service_callback", "show_timeout_not_started");
			mContext.sendBroadcast(i);
		}
    }

	private String setSpeechFXLanguage(String language){
       String lang;
        if (!mSpeechFXon) {
            lang = language;
        } else if (mTts == null) {
			Log.i(TAG, "TTS IS NULL!");
			lang = language;
		} else {
			lang = Languages.setSpeechLanguage(language, mDefaultSpeechLang, mTts);
			if (mTts != null && lang != null && !lang.equals("")) {
				Log.i(TAG, "TTS set speech engine language: " + lang);
				mTts.setLanguage(Languages.stringToLocale(lang));
			} else {
				// Unchanged, so return the pilot language
				lang = language;
			}
			Log.i(TAG, "TTS LANG: " + lang);
		}

		return lang;
	}
    
    @TargetApi(21)
    private void speak(String text, int queueMode) {
		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.KITKAT) {
			utterance_ids.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
			mTts.speak(text, queueMode, utterance_ids);
		} else {
			mTts.speak(text, queueMode, null, text);
		}
	}
}
