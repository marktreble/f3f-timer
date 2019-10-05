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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.marktreble.f3ftimer.BaseActivity;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.GenericAlert;
import com.marktreble.f3ftimer.dialog.GenericListPicker;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.exportimport.F3ftimerApiExportRace;
import com.marktreble.f3ftimer.exportimport.F3xvaultApiExportRace;
import com.marktreble.f3ftimer.filesystem.F3XVaultExport;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ResultsRaceActivity extends BaseActivity
    implements ListView.OnClickListener {

    private Integer mRid;

    static int DLG_EXPORT = 2;

    static final int EXPORT_EMAIL = 100;
    static final int EXPORT_EMAIL_F3XV = 101;
    static final int EXPORT_F3F_TIMER = 102;
    static final int EXPORT_F3X_VAULT = 103;

    static final int SHARE_EMAIL = 100;
    static final int SHARE_SOCIAL_MEDIA = 101;

    static final String DIALOG = "dialog";

    GenericAlert mDLG;
    GenericListPicker mDLG2;

    /* TEMPORARY */
    // Needs a class creating for calcs
    private ArrayList<String> mArrNames;
    private ArrayList<String> mArrNumbers;
    private ArrayList<Pilot> mArrPilots;
    private ArrayList<Float> mArrScores;

    private float mFTD;
    private String mFTDName;
    private int mFTDRound;
    /* END */

    ArrayAdapter<String> mArrAdapter;
    ArrayList<String> mOptions;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.race);

        Intent intent = getIntent();
        if (intent.hasExtra("race_id")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mRid = extras.getInt("race_id");
            }
        }

        getNamesArray();
        setList();

        ListView lv = findViewById(android.R.id.list);
        lv.setAdapter(mArrAdapter);
    }

    private void getNamesArray() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        TextView tt = findViewById(R.id.race_title);
        tt.setText(race.name);

        mOptions = new ArrayList<>();
        mOptions.add(String.format("Round in Progress (R%d)", race.round));
        mOptions.add("Completed Rounds");
        mOptions.add("Leader Board");
        mOptions.add("Team Results");

    }

    private void setList() {
        mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow, mOptions) {
            @Override
            public @NonNull
            View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.listrow, parent, false);
                    row.setOnClickListener(ResultsRaceActivity.this);
                    row.setOnCreateContextMenuListener(ResultsRaceActivity.this);
                } else {
                    row = convertView;
                }

                row.setTag(position);

                TextView tv = row.findViewById(R.id.text1);
                tv.setText(mOptions.get(position));

                return row;
            }
        };
    }

    @Override
    public void onClick(View v) {
        int position = (int)v.getTag();
        Intent intent = null;
        switch (position) {
            case 0:
                intent = new Intent(this, ResultsRoundInProgressActivity.class);
                break;
            case 1:
                intent = new Intent(this, ResultsCompletedRoundsActivity.class);
                break;
            case 2:
                intent = new Intent(this, ResultsLeaderBoardActivity.class);
                break;
            case 3:
                intent = new Intent(this, ResultsTeamsActivity.class);
                break;
        }
        if (intent != null) {
            intent.putExtra("race_id", mRid);
            startActivityForResult(intent, mRid);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_share:
                share();
                return true;
            case R.id.menu_export:
                export();
                return true;
            case R.id.menu_pilot_manager:
                pilotManager();
                return true;
            case R.id.menu_race_manager:
                raceManager();
                return true;
            case R.id.menu_help:
                help();
                return true;
            case R.id.menu_about:
                about();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void share() {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        ArrayList<String> options = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.results_share_destinations)));
        mDLG2 = GenericListPicker.newInstance(
                getString(R.string.select_share_results_destination),
                options,
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        switch (resultCode) {
                            case SHARE_EMAIL:
                                share_email();
                                break;
                            case SHARE_SOCIAL_MEDIA:
                                share_social_media();
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG2, DIALOG);
        ft.commit();

    }

    private void share_email() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        this.getNamesArray2();

        StringBuilder results = new StringBuilder();
        String[] email_list = new String[mArrNames.size()];

        for (int i = 0; i < mArrNames.size(); i++) {
            results.append(String.format("%s %s %s\n", mArrNumbers.get(i), mArrNames.get(i), Float.toString(mArrScores.get(i))));

            // Generate list of email addresses to send to
            Pilot p = mArrPilots.get(i);
            String email = p.email;
            if (email.length() > 0)
                email_list[i] = email;

        }

        results.append("\n");
        results.append("Fastest time: ");
        results.append(mFTD);
        results.append(" by ");
        results.append(mFTDName);
        results.append(" in round ");
        results.append(mFTDRound);
        results.append("\n\n");
        results.append("\n\n");
        results.append("Result from f3ftimer (https://github.com/marktreble/f3f-timer)\n");

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, email_list);
        intent.putExtra(Intent.EXTRA_SUBJECT, race.name);
        intent.putExtra(Intent.EXTRA_TEXT, results.toString());

        Intent openInChooser = Intent.createChooser(intent, "Share Leaderboard");
        startActivityForResult(openInChooser, 0);
    }

    @SuppressWarnings("SetWorldReadable")
    private void share_social_media() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        this.getNamesArray2();

        // Generate results as an image
        int w = 320, h = (mArrNames.size() + 6) * 24;
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(w, h, conf);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(18);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        int y = 30;
        for (int i = 0; i < mArrNames.size(); i++) {
            y += 24;
            canvas.drawText(mArrNumbers.get(i), 16, y, paint);
            canvas.drawText(mArrNames.get(i), 48, y, paint);
            canvas.drawText(Float.toString(mArrScores.get(i)), 220, y, paint);

        }
        y += 48;

        canvas.drawText("Fastest time: " + mFTD, 16, y, paint);
        y += 20;
        canvas.drawText("by " + mFTDName + " in round " + mFTDRound, 16, y, paint);

        Intent intent = new Intent(Intent.ACTION_SEND);

        try {
            File file = new File(getExternalCacheDir(), race.name + ".png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            boolean success = file.setReadable(true, false);
            if (success) {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                intent.putExtra(Intent.EXTRA_TEXT, "Result from f3ftimer (https://github.com/marktreble/f3f-timer)");
                intent.setType("image/png");

                Intent openInChooser = Intent.createChooser(intent, "Share Leaderboard");

                startActivity(openInChooser);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void export() {
        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.cancel);

        ArrayList<String> options = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.results_export_destinations)));
        mDLG2 = GenericListPicker.newInstance(
                getString(R.string.select_export_results_destination),
                options,
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        switch (resultCode) {
                            case EXPORT_EMAIL:
                                export_email();
                                break;
                            case EXPORT_EMAIL_F3XV:
                                export_email_f3xv();
                                break;
                            case EXPORT_F3F_TIMER:
                                export_f3ftimer();
                                break;
                            case EXPORT_F3X_VAULT:
                                export_f3xvault();
                                break;
                        }
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(mDLG2, DIALOG);
        ft.commit();
    }

    private void pilotManager() {
        Intent intent = new Intent(mContext, PilotsActivity.class);
        startActivity(intent);
    }

    private void raceManager() {
        Intent intent = new Intent(mContext, RaceListActivity.class);
        startActivity(intent);
    }

    private void help() {
        Intent intent = new Intent(mContext, HelpActivity.class);
        startActivity(intent);
    }

    private void about() {
        Intent intent = new Intent(mContext, AboutActivity.class);
        startActivity(intent);
    }

    private void export_email() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        // re-write the results file just in case this has just been imported into this device.
        SpreadsheetExport exp = new SpreadsheetExport();

        if (!exp.writeResultsFile(mContext, race)) {
            finish();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, race.name);
        intent.putExtra(Intent.EXTRA_TEXT, "Results file attached");

        File file = exp.getDataStorageDir(race.name + ".txt");
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Uri uri = Uri.fromFile(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        Intent openInChooser = Intent.createChooser(intent, "Email Results File");
        startActivity(openInChooser);
    }

    private void export_email_f3xv() {
        RaceData datasource = new RaceData(this);
        datasource.open();
        Race race = datasource.getRace(mRid);
        datasource.close();

        // write the results file in F3XVault format
        F3XVaultExport exp = new F3XVaultExport();

        if (!exp.writeResultsFile(mContext, race)) {
            finish();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, race.name);
        intent.putExtra(Intent.EXTRA_TEXT, "Results file attached");

        File file = exp.getDataStorageDir(race.name + ".f3xv.txt");
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Uri uri = Uri.fromFile(file);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        Intent openInChooser = Intent.createChooser(intent, "Email Results File");
        startActivity(openInChooser);
    }

    private void export_f3ftimer() {
        Intent intent = new Intent(mContext, F3ftimerApiExportRace.class);
        startActivityForResult(intent, DLG_EXPORT);
    }

    private void export_f3xvault() {
        Intent intent = new Intent(mContext, F3xvaultApiExportRace.class);
        startActivityForResult(intent, DLG_EXPORT);
    }

    private void getNamesArray2() {

        Results r = new Results();
        r.getResultsForRace(ResultsRaceActivity.this, mRid, true);

        mArrNames = r.mArrNames;
        mArrPilots = r.mArrPilots;
        mArrNumbers = r.mArrNumbers;
        mArrScores = r.mArrScores;

        mFTD = r.mFTD;
        mFTDName = r.mFTDName;
        mFTDRound = r.mFTDRound;

    }

}
