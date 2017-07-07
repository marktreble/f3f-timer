/*
 * Driver
 * Common functions for all driver HID drivers
 * Handles sounds + UI communications
 */

package com.marktreble.f3ftimer.driver;

import java.util.HashMap;
import java.util.Locale;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.languages.Languages;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;

import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class Driver implements TextToSpeech.OnInitListener {

	private static final String TAG = "Driver";

	private Context mContext;

	
	public Integer mPid;
	public Integer mRid;
	public Integer mRnd;
	public Race mRace;
	public Float mPilot_Time = .0f;
	public long mTimeOnCourse;
	public long mLastLegTime;
	public long[] mLegTimes = new long[10];
	public Integer mLeg = 0;
	public boolean mWindLegal = true;
	
	public MediaPlayer mPlayer;
	private boolean mSoundFXon;
	private boolean mSpeechFXon;
	
	private String mDefaultLang;
	private String mSpeechLang;
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
		
    	// Listen for inputs from the UI
		mContext.registerReceiver(onBroadcast, new IntentFilter("com.marktreble.f3ftimer.onUpdateFromUI"));

     	mPlayer  = MediaPlayer.create(mContext, R.raw.base1);
        
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
        
		mSoundFXon = (intent.getStringExtra("pref_buzzer").equals("true"));
		mSpeechFXon = (intent.getStringExtra("pref_voice").equals("true"));
		mSpeechLang = intent.getStringExtra("pref_voice_lang");
		Locale default_lang = Locale.getDefault();
		if (mSpeechLang == null || mSpeechLang.equals("")) mSpeechLang = String.format("%s_%s", default_lang.getISO3Language(), default_lang.getISO3Country());
		
		mTts = null;
		mDefaultLang = Locale.getDefault().getLanguage();
		
		if (mSpeechFXon) startSpeechSynthesiser();

		// Check timeout status of the round on start
		mHandler.post(checkTimeout);

        Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
        i.putExtra("com.marktreble.f3ftimer.service_callback", "driver_started");
        mContext.sendBroadcast(i);

	}


	public void destroy(){
        Log.i(TAG, "Destroyed");
        Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
        i.putExtra("com.marktreble.f3ftimer.service_callback", "driver_stopped");
        mContext.sendBroadcast(i);

        try {
  		 mContext.unregisterReceiver(onBroadcast);
		} catch (IllegalArgumentException e){
			e.printStackTrace();
		}
  		
  		if (mTts != null)
			mTts.shutdown();
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
					runFinalised();
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
					
					if (mSpeechFXon && mTts == null){
						startSpeechSynthesiser();
					}
					return;

				}
				
				if (data.equals("pref_voice_lang")){	
					if (mSpeechFXon){
						mSpeechLang = extras.getString("com.marktreble.f3ftimer.value");
				  		// Try to set speech lang - if not available then setSpeechFXLanguage returns the default Language
						mPilotLang = setSpeechFXLanguage();
					}
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
    
    /*
     * Binding for Service->UI Communication
     */

	public void startPilot(Bundle extras){
        
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
                    mPilotLang = mSpeechLang;
                    RacePilotData datasource2 = new RacePilotData(mContext);
                    datasource2.open();
                    Pilot pilot = datasource2.getPilot(mPid, mRid);
                    datasource2.close();
                    if (pilot.language!= null && !pilot.language.equals("")){
                        mPilotLang = String.format("%s_%s", pilot.language, pilot.nationality);
                    }
					/*
                    Log.i("startPilot:", "Race ID = "+Integer.toString(mRid));
                    Log.i("startPilot:", "Pilot ID = "+Integer.toString(mPid));
                    Log.i("startPilot:", "Round ID = "+Integer.toString(mRnd));
                    Log.i("startPilot:", pilot.toString());
                    Log.i("startPilot:", "lang = "+pilot.language + ":" + mPilotLang);
					*/

                    // Try to set speech lang - if not available then setSpeechFXLanguage returns the default Language
                    mPilotLang = setSpeechFXLanguage();

                    speak(String.format("%s %s", pilot.firstname, pilot.lastname), TextToSpeech.QUEUE_ADD);

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
	
	public void offCourse(){
		// Post to the UI that the model has exited the course
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "off_course");
		mContext.sendBroadcast(i);

		// Buzzer Sound
		if (mSoundFXon) mPlayer.start();
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
		if (mSoundFXon) mPlayer.start();
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
		long now = System.currentTimeMillis();
		long time = now - mLastLegTime;
		mLastLegTime+=time;
		mLegTimes[mLeg] = time; 
		mLeg++;
			
		// calculate the mean
		long mean = (now-mTimeOnCourse)/mLeg;
			
		// Estimate is current time + (mean*laps remaining)
		long estimate = (now-mTimeOnCourse) + (mean * (10-mLeg)); 
			
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "leg_complete");
		i.putExtra("com.marktreble.f3ftimer.time", estimate);
		i.putExtra("com.marktreble.f3ftimer.number", mLeg);
		mContext.sendBroadcast(i);

		// Buzzer Sound
		if (mSoundFXon) mPlayer.start();
		// Synthesized Call
		if (mSpeechFXon && mLeg<10 && mLeg>0){
            final String leg = Integer.toString(mLeg) + ((mLeg == 9)? " " + Languages.useLanguage(mContext, mPilotLang).getString(R.string.and_last):"");

            mHandler.postDelayed(new Runnable() {
                public void run() {
                    
                    speak(leg, TextToSpeech.QUEUE_ADD);
                }
            }, SPEECH_DELAY_TIME);
        }
	}

	public void runComplete(){
  		// Update the UI
  		Intent intent = new Intent("com.marktreble.f3ftimer.onUpdate");
		intent.putExtra("com.marktreble.f3ftimer.service_callback", "run_complete");
		intent.putExtra("com.marktreble.f3ftimer.time", mPilot_Time);
		mContext.sendBroadcast(intent);
		
	}
	
	public void runFinalised(){
		// Save the time to the database

		RacePilotData datasource = new RacePilotData(mContext);
		datasource.open();
		datasource.setPilotTimeInRound(mRid, mPid, mRnd, mPilot_Time);
        datasource.close();

		// Get the time
		String str_time = String.format("%.2f", mPilot_Time);
		str_time = str_time.replace(".", " ");

		// Get the pilot's name
		RacePilotData datasource2 = new RacePilotData(mContext);
		datasource2.open();
		Pilot pilot = datasource2.getPilot(mPid, mRid);
		datasource2.close();
		String str_name = String.format("%s %s", pilot.firstname, pilot.lastname);
		String str_nationality = pilot.nationality;


		// Speak the time
		if (mSpeechFXon) speak(str_time, TextToSpeech.QUEUE_ADD);
		Log.d(TAG, "TIME SPOKEN");
        
		// Update the .txt file
        new SpreadsheetExport().writeResultsFile(mContext, mRace);
		Log.d(TAG, "EXPORT FILE WRITTEN");
		SystemClock.sleep(1000);

		// Post back to the UI (RaceTimerActivity);
  		Intent intent = new Intent("com.marktreble.f3ftimer.onUpdate");
		intent.putExtra("com.marktreble.f3ftimer.service_callback", "run_finalised");
		mContext.sendOrderedBroadcast(intent, null);
		Log.d(TAG, "POST BACK TO UI");

		// Post to the Race Results Display Service

		// Get this from ResultsRoundInProgressActivity::getNamesArray
		// TODO
		String str_round_results = "[{\"name\":\"Mark Treble\",\"time\":\"33.23\"}]";

		Intent intent2 = new Intent("com.marktreble.f3ftimer.onExternalUpdate");
		intent2.putExtra("com.marktreble.f3ftimer.external_results_callback", "run_finalised");
		intent2.putExtra("com.marktreble.f3ftimer.pilot_nationality", str_nationality);
		intent2.putExtra("com.marktreble.f3ftimer.pilot_name", str_name);
		intent2.putExtra("com.marktreble.f3ftimer.pilot_time", String.format("%.2f", mPilot_Time));
		intent2.putExtra("com.marktreble.f3ftimer.current_round", mRnd);
		intent2.putExtra("com.marktreble.f3ftimer.current_round_results", str_round_results);


		mContext.sendBroadcast(intent2);
		Log.d(TAG, "POST BACK TO EXTERNAL RESULTS");

	}


	public void windLegal(){
		mWindLegal = true;
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "wind_legal");
		mContext.sendBroadcast(i);
	}
	
	public void windIllegal(){
		mWindLegal = false;
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "wind_illegal");
		mContext.sendBroadcast(i);
	}
	
	public void startPressed(){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "start_pressed");
		mContext.sendBroadcast(i);
		
	}
	
	public void ready(){
    	mLeg = 0;
	}

	private void startSpeechSynthesiser() {
        mTts = new TextToSpeech(mContext, this);
    }

    public void onInit(int status){
        mTTSStatus = status;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            initUtteranceListenerForMinICS();
        } else {
            initUtteranceListenerForOtherThanICS();
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
    
    public void beginRoundTimeout(){
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
	
	private void cancelDialog(){
		Intent i = new Intent("com.marktreble.f3ftimer.onUpdate");
		i.putExtra("com.marktreble.f3ftimer.service_callback", "cancel");
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

	private String setSpeechFXLanguage(){
        if (mTts == null){
            Log.i(TAG, "TTS IS NULL!");
        }
		String lang = Languages.setSpeechLanguage(mPilotLang, mSpeechLang, mTts);
		
		if (!lang.equals("")){

			Log.i(TAG, "TTS LANG: "+lang);
            
			mTts.setLanguage(Languages.stringToLocale(lang));

		} else {
			// Unchanged, so return the pilot language
			lang = mPilotLang;
		}
		
		return lang;
	}
    
    @TargetApi(21)
    private void speak(String text, int queueMode){
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.KITKAT) {
            utterance_ids.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, text);
            mTts.speak(text, queueMode, utterance_ids);
        } else {
            mTts.speak(text, queueMode, null, text);
        }
        
    }
}
