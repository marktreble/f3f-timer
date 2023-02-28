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

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;

import java.util.List;
import java.util.Map;

public class ResultsReadActivity extends ResultsRaceBaseActivity {

    private Button btn_start;
    private int btnState;

    private int mRid;
    private String mRaceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.results_read);

        Intent intent = getIntent();
        if (intent.hasExtra("race_id")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mRid = extras.getInt("race_id");
            }
        }

        getNamesArray();

        btn_start = findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnPressed();
            }
        });

        if (savedInstanceState == null) {
            resetButton();
            startService();
        } else {
            btnState = savedInstanceState.getInt("btnState", 0);
            restoreButton();
        }

        registerReceiver(onBroadcast, new IntentFilter(IComm.RCV_UPDATE));

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("btnState", btnState);
    }

    private void btnPressed() {
        if (btnState == 0) {
            sendCommand("start_reading");
            btnState = 1;
            restoreButton();
            return;
        }

        if (btnState == 1) {
            sendCommand("pause_reading");
            btnState = 2;
            restoreButton();
            return;
        }

        if (btnState == 2) {
            sendCommand("resume_reading");
            btnState = 1;
            restoreButton();
            return;
        }
    }

    private void resetButton() {
        btnState = 0;
        restoreButton();
    }

    private void restoreButton() {
        switch (btnState) {
            case 0:
                btn_start.setText("Play");
                break;
            case 1:
                btn_start.setText("Pause");
                break;
            case 2:
                btn_start.setText("Resume");
                break;

        }
    }

    private void getNamesArray() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        TextView tt = findViewById(R.id.race_title);
        tt.setText(race.name);

        mRaceName = race.name;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            ResultsReadService.stop(this);
        }

        unregisterReceiver(onBroadcast);
    }

    public void startService() {
        Log.d("READRESULTS", "STARTING SERVICE");

        ResultsReadService.stop(this);

        Bundle extras = getIntent().getExtras();
        if (extras == null)
            extras = new Bundle();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> keys = sharedPref.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            if (entry.getValue() instanceof String)
                extras.putString(entry.getKey(), (String) entry.getValue());

            if (entry.getValue() instanceof Boolean)
                extras.putBoolean(entry.getKey(), (Boolean) entry.getValue());

        }
        ResultsReadService.startDriver(this, mRid, mRaceName, extras);

    }

    // Binding for UI->Service Communication
    public void sendCommand(String cmd) {
        Intent i = new Intent(IComm.RCV_UPDATE_FROM_UI);
        i.putExtra(IComm.MSG_UI_CALLBACK, cmd);
        sendBroadcast(i);
    }

    @SuppressWarnings("deprecation")
    public boolean isServiceRunning(String serviceClassName) {
        final ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

            for (android.app.ActivityManager.RunningServiceInfo runningServiceInfo : services) {
                if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private BroadcastReceiver onBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle extras = intent.getExtras();
            String data = null;
            if (extras != null) {
                data = extras.getString(IComm.MSG_SERVICE_CALLBACK);
            }
            if (data == null) {
                return;
            }

            if (data.equals("finished")) {
                resetButton();
            }
        }
    };
}
