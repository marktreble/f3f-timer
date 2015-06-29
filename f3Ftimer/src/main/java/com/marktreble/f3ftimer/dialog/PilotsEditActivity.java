/*
 * PilotsEditActivity
 * Called by PilotsActivity when + button is pressed or pilot is edited
 * Presented in single page popup
 * >firstname
 * >lastname
 * >email
 * >frequency
 * >models
 */
package com.marktreble.f3ftimer.dialog;


import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import com.marktreble.f3ftimer.data.pilot.*;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.languages.Languages;
import com.marktreble.f3ftimer.R;

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
		
    	EditText firstname = (EditText) findViewById(R.id.editText1);
    	EditText lastname = (EditText) findViewById(R.id.editText2);
    	EditText email = (EditText) findViewById(R.id.editText3);
    	EditText frequency = (EditText) findViewById(R.id.editText4);
    	EditText models = (EditText) findViewById(R.id.editText5);
    	Spinner nationality = (Spinner) findViewById(R.id.spinner6);
        Spinner language = (Spinner) findViewById(R.id.spinner7);
        Button done_button = (Button) findViewById(R.id.button1);

        done_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
            }
        });
		CharSequence[] countries = getResources().getTextArray(R.array.nationalities);
		String[] str_countries = new String[countries.length];
		int i=0;
		for (CharSequence country : countries){
			str_countries[i++] = (String) country.toString();
		}
		mNationality_adapter = new ArrayAdapter<String>(this, R.layout.iconspinner , R.id.ics_label, str_countries){
   	   		@Override
   	   		public View getView(int position, View convertView, ViewGroup parent) {
   	   			View view = getCustomView(position, convertView, parent);
   	   			ImageView icon = (ImageView)view.findViewById(R.id.ics_icon);
   	   			LinearLayout.LayoutParams lp = (LayoutParams) icon.getLayoutParams();
   	   			lp.leftMargin = 0;
   	   			icon.setLayoutParams(lp);
   	   			return view;
   	   		}
   	   		
   	   		public View getDropDownView(int position, View convertView, ViewGroup parent) {
	   			return getCustomView(position, convertView, parent);
	   		}
	   		
   	   		public View getCustomView(int position, View convertView, ViewGroup parent) {
                View row;
                                
                if (null == convertView) {
                row = getLayoutInflater().inflate(R.layout.iconspinner, parent, false);
                } else {
                row = convertView;
                }
                
        		CharSequence[] codes = getResources().getTextArray(R.array.countrycodes);

        		TextView label= (TextView) row.findViewById(R.id.ics_label);
        	    label.setText(getItem(position));

        	    ImageView icon=(ImageView)row.findViewById(R.id.ics_icon);

        	    String code =  ((String) codes[position]).toLowerCase();
        	    Drawable img = null;
        	    if (!code.equals("")){
	        	    String uri = "@drawable/" + code;
	        	    int imageResource = getResources().getIdentifier(uri, null, getPackageName());
	        	    //icon.setImageResource(imageResource);
	        	    img = getResources().getDrawable(imageResource);
        	    }
        	    icon.setImageDrawable(img);
        	    
                return row;
            }
   	   	};
    	//mNationality_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	nationality.setAdapter(mNationality_adapter);
    	
    	//mLanguage_adapter = ArrayAdapter.createFromResource(this,      R.array.languages, android.R.layout.simple_spinner_item);
    	
    	
        String[] languages = Languages.getAvailableLanguages(this);

    	mLanguage_adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, languages){

            @Override
            public CharSequence getItem(int position){
                String code = (String)super.getItem(position);
                CharSequence label = getResources().getString(getResources().getIdentifier(code, "string", getPackageName()));
                return label;
            }
        };
        
    	mLanguage_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	language.setAdapter(mLanguage_adapter);

		Intent intent= getIntent(); // gets the previously created intent
		Bundle extras = intent.getExtras();
		mCaller = extras.getString("caller");
		if (intent.hasExtra("pilot_id")){
			mPid = extras.getInt("pilot_id");
            mRid = extras.getInt("race_id");
			
			Pilot p = new Pilot();
			if (mCaller.equals("pilotmanager")){
				// Get pilot from database and populate fields
	        	PilotData datasource = new PilotData(mContext);
	    		datasource.open();
	    		p = datasource.getPilot(mPid);
	    		datasource.close();
			}
			
			if (mCaller.equals("racemanager")){
				// Get pilot from database and populate fields
	        	RacePilotData datasource = new RacePilotData(mContext);
	    		datasource.open();
	    		p = datasource.getPilot(mPid, mRid);
	    		datasource.close();
			}
        	
        	firstname.setText(p.firstname);
        	lastname.setText(p.lastname);
        	email.setText(p.email);
        	frequency.setText(p.frequency);
        	models.setText(p.models);
        	
        	int pos;
        	
        	String[] countrycodes = getResources().getStringArray(R.array.countrycodes);
        	pos = 0;
        	for (i=0;i<countrycodes.length; i++){
        		if (countrycodes[i].equals(p.nationality))
        			pos = i;
        	}
        	nationality.setSelection(pos);
        	

        	pos = 0;
        	for (i=0;i<languages.length; i++){
        		if (languages[i].equals(p.language))
        			pos = i;
        	}
        	language.setSelection(pos);

		} else {
            String[] countrycodes = getResources().getStringArray(R.array.countrycodes);
            int pos = 0;
            String dflt = "GB";
            for (i=0;i<countrycodes.length; i++){
                if (countrycodes[i].equals(dflt))
                   pos = i;
            }
            nationality.setSelection(pos);


            pos = 0;
            dflt = "en";
            for (i=0;i<languages.length; i++){
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

	}

    private boolean done(){
        // Get entered data, and save to/update database
        EditText firstname = (EditText) findViewById(R.id.editText1);
        EditText lastname = (EditText) findViewById(R.id.editText2);
        EditText email = (EditText) findViewById(R.id.editText3);
        EditText frequency = (EditText) findViewById(R.id.editText4);
        EditText models = (EditText) findViewById(R.id.editText5);
        Spinner nationality = (Spinner) findViewById(R.id.spinner6);
        Spinner language = (Spinner) findViewById(R.id.spinner7);

        Pilot p = new Pilot();
        p.firstname = capitalise(firstname.getText().toString().trim());
        p.lastname = capitalise(lastname.getText().toString().trim());
        p.email = email.getText().toString().trim().toLowerCase();
        p.frequency = frequency.getText().toString().trim();
        p.models = capitalise(models.getText().toString().trim());
        p.nationality = (String) getResources().getStringArray(R.array.countrycodes)[nationality.getSelectedItemPosition()];
        String[] languages = Languages.getAvailableLanguages(mContext);
        if (language.getSelectedItemPosition()>=0)
            p.language = (String) languages[language.getSelectedItemPosition()];

        String regEx = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}\\b";

        Pattern pattern = Pattern.compile(regEx);
        Matcher m = pattern.matcher(p.email);

        if(m.find() || p.email.length() == 0){
            if (mCaller.equals("pilotmanager")){
                PilotData datasource = new PilotData(mContext);
                datasource.open();
                if (mPid == 0){
                    datasource.savePilot(p);
                } else {
                    p.id = mPid;
                    datasource.updatePilot(p);
                }
                datasource.close();
            }

            if (mCaller.equals("racemanager")){
                RacePilotData datasource = new RacePilotData(mContext);
                datasource.open();
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

            TextView text = (TextView) layout.findViewById(R.id.text);
            text.setText("Email address is invalid");

            Toast toast = new Toast(getApplicationContext());
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setView(layout);
            toast.show();
        }
        return false;
    }
	@SuppressLint("DefaultLocale")
	private String capitalise(String str){
		if (str.length() == 0) return "";
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

}
