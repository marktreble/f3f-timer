package com.marktreble.f3ftimer.dialog;

import com.marktreble.f3ftimer.R;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SettingsActivity extends Activity {
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

}
