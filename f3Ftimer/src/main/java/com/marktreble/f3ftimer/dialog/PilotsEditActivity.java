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

package com.marktreble.f3ftimer.dialog;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.pilot.PilotData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.languages.Languages;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PilotsEditActivity extends Activity {

    private Integer mPid = 0;
    private Integer mRid = 0;
    private String mCaller = "";

    private Context mContext;

    ArrayAdapter<String> mNationality_adapter;
    ArrayAdapter<CharSequence> mLanguage_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pilot_edit);

        mContext = this;

        EditText firstname =  findViewById(R.id.editText1);
        EditText lastname =  findViewById(R.id.editText2);
        EditText email =  findViewById(R.id.editText3);
        EditText frequency =  findViewById(R.id.editText4);
        EditText models =  findViewById(R.id.editText5);
        Spinner nationality =  findViewById(R.id.spinner6);
        Spinner language =  findViewById(R.id.spinner7);
        EditText nac =  findViewById(R.id.editText6);
        EditText fai =  findViewById(R.id.editText7);
        TextView teamlabel =  findViewById(R.id.textView10);
        AutoCompleteTextView team = findViewById(R.id.editText8);
        Button done_button = findViewById(R.id.button1);

        done_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
            }
        });
        CharSequence[] countries = getResources().getTextArray(R.array.nationalities);
        String[] str_countries = new String[countries.length];
        int i = 0;
        for (CharSequence country : countries) {
            str_countries[i++] = country.toString();
        }
        mNationality_adapter = new ArrayAdapter<String>(this, R.layout.iconspinner, R.id.ics_label, str_countries) {
            @Override
            public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = getCustomView(position, convertView, parent);
                ImageView icon =  view.findViewById(R.id.ics_icon);
                LinearLayout.LayoutParams lp = (LayoutParams) icon.getLayoutParams();
                lp.leftMargin = 0;
                icon.setLayoutParams(lp);
                return view;
            }

            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                return getCustomView(position, convertView, parent);
            }

            View getCustomView(int position, View convertView, ViewGroup parent) {
                View row;

                if (null == convertView) {
                    row = getLayoutInflater().inflate(R.layout.iconspinner, parent, false);
                } else {
                    row = convertView;
                }

                CharSequence[] codes = getResources().getTextArray(R.array.countrycodes);

                TextView label =  row.findViewById(R.id.ics_label);
                label.setText(getItem(position));

                ImageView icon = row.findViewById(R.id.ics_icon);

                String code = ((String) codes[position]).toLowerCase();
                Drawable img = null;
                if (!code.equals("")) {
                    String uri = "@drawable/" + code;
                    int imageResource = getResources().getIdentifier(uri, null, getPackageName());
                    //icon.setImageResource(imageResource);
                    img = getResources().getDrawable(imageResource);
                }
                icon.setImageDrawable(img);

                return row;
            }
        };

        nationality.setAdapter(mNationality_adapter);

        String[] languages = Languages.getAvailableLanguages(this);

        mLanguage_adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, languages) {

            @Override
            public CharSequence getItem(int position) {
                String code = (String) super.getItem(position);
                CharSequence label = "";
                try {
                    label = getResources().getString(getResources().getIdentifier(code, "string", getPackageName()));
                } catch (NotFoundException ex) {
                    Log.d("CODE", "Language code not found is resources: " + code);
                }
                return label;
            }
        };

        mLanguage_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        language.setAdapter(mLanguage_adapter);

        Intent intent = getIntent(); // gets the previously created intent
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        mCaller = extras.getString("caller");

        if (intent.hasExtra("pilot_id")) {
            mPid = extras.getInt("pilot_id");
            mRid = extras.getInt("race_id");

            Pilot p = new Pilot();
            if (mCaller.equals("pilotmanager")) {
                // Get pilot from database and populate fields
                PilotData datasource = new PilotData(mContext);
                datasource.open();
                p = datasource.getPilot(mPid);
                datasource.close();
            }

            if (mCaller.equals("racemanager")) {
                // Get pilot from database and populate fields
                RacePilotData datasource = new RacePilotData(mContext);
                datasource.open();
                p = datasource.getPilot(mPid, mRid);
                Log.i("PPP", p.toString());
                datasource.close();
            }

            firstname.setText(p.firstname);
            lastname.setText(p.lastname);
            email.setText(p.email);
            frequency.setText(p.frequency);
            models.setText(p.models);
            team.setText(p.team);
            nac.setText(p.nac_no);
            fai.setText(p.fai_id);

            int pos;

            String[] countrycodes = getResources().getStringArray(R.array.countrycodes);
            pos = 0;
            for (i = 0; i < countrycodes.length; i++) {
                if (countrycodes[i].equals(p.nationality))
                    pos = i;
            }
            nationality.setSelection(pos);


            pos = 0;
            for (i = 0; i < languages.length; i++) {
                if (languages[i].equals(p.language))
                    pos = i;
            }
            language.setSelection(pos);

        } else {
            String[] countrycodes = getResources().getStringArray(R.array.countrycodes);
            int pos = 0;
            String dflt = "GB";
            for (i = 0; i < countrycodes.length; i++) {
                if (countrycodes[i].equals(dflt))
                    pos = i;
            }
            nationality.setSelection(pos);


            pos = 0;
            dflt = "en";
            for (i = 0; i < languages.length; i++) {
                if (languages[i].equals(dflt))
                    pos = i;
            }
            language.setSelection(pos);
        }


        models.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    return done();
                }
                return false;
            }
        });

        if (mCaller.equals("racemanager")) {
            // Show team
            teamlabel.setVisibility(View.VISIBLE);
            team.setVisibility(View.VISIBLE);

            RacePilotData datasource = new RacePilotData(mContext);
            datasource.open();
            final String[] TEAMS = datasource.getTeams(mRid);
            datasource.close();

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, TEAMS);

            team.setAdapter(adapter);
        }

    }

    private boolean done() {
        // Get entered data, and save to/update database
        EditText firstname =  findViewById(R.id.editText1);
        EditText lastname =  findViewById(R.id.editText2);
        EditText email =  findViewById(R.id.editText3);
        EditText frequency =  findViewById(R.id.editText4);
        EditText models =  findViewById(R.id.editText5);
        Spinner nationality =  findViewById(R.id.spinner6);
        Spinner language =  findViewById(R.id.spinner7);
        EditText team =  findViewById(R.id.editText8);
        EditText nac =  findViewById(R.id.editText6);
        EditText fai =  findViewById(R.id.editText7);


        Pilot p = new Pilot();
        p.firstname = capitalise(firstname.getText().toString().trim());
        p.lastname = capitalise(lastname.getText().toString().trim());
        p.email = email.getText().toString().trim().toLowerCase();
        p.frequency = frequency.getText().toString().trim();
        p.models = capitalise(models.getText().toString().trim());
        p.nationality = getResources().getStringArray(R.array.countrycodes)[nationality.getSelectedItemPosition()];
        String[] languages = Languages.getAvailableLanguages(mContext);
        if (language.getSelectedItemPosition() >= 0)
            p.language = languages[language.getSelectedItemPosition()];

        p.nac_no = nac.getText().toString().trim().toUpperCase();
        p.fai_id = fai.getText().toString().trim().toUpperCase();


        String regEx = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\b";

        Pattern pattern = Pattern.compile(regEx);
        Matcher m = pattern.matcher(p.email);

        if (m.find() || p.email.length() == 0) {
            if (mCaller.equals("pilotmanager")) {
                PilotData datasource = new PilotData(mContext);
                datasource.open();
                if (mPid == 0) {
                    datasource.savePilot(p);
                } else {
                    p.id = mPid;
                    datasource.updatePilot(p);
                }
                datasource.close();
            }

            if (mCaller.equals("racemanager")) {
                RacePilotData datasource = new RacePilotData(mContext);
                datasource.open();
                p.team = team.getText().toString().trim();
                p.id = mPid;
                datasource.updatePilot(p);
                datasource.close();
            }
            // finish this activity and refresh the pilots list
            setResult(RESULT_OK, null);
            finish();
            return true;
        } else {
            // Invalid email address, so show toast
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast,
                    (ViewGroup) findViewById(R.id.toast_layout_root));

            TextView text =  layout.findViewById(R.id.text);
            text.setText(getString(R.string.invalid_email));

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    private String capitalise(String str) {
        if (str.length() == 0) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
