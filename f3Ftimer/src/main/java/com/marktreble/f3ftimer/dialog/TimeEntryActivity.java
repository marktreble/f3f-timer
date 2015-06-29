/*
 * Time Entry Activity
 * Called by RaceActivity when manual entry is selected from context menu
 * Presented in single page popup
 * >time
 */
package com.marktreble.f3ftimer.dialog;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.racemanager.RaceActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
public class TimeEntryActivity extends Activity {

	Integer mPid = 0;
    Integer mRound = 0;
	private Intent mIntent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.time_entry);
		
		mIntent= getIntent(); // gets the previously created intent
		if (mIntent.hasExtra("pilot_id")){
			Bundle extras = mIntent.getExtras();
            mPid = extras.getInt("pilot_id");
            mRound = extras.getInt("round");
			
        	EditText time = (EditText) findViewById(R.id.editText1);


        	time.setOnEditorActionListener(new OnEditorActionListener() {
        		@Override
        		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        			if (actionId == EditorInfo.IME_ACTION_DONE) {
        				// Get entered data, and save to/update database
        				EditText time = (EditText) findViewById(R.id.editText1);
        				
        				mIntent.putExtra("time", time.getText().toString());
                        mIntent.putExtra("pilot", mPid);
                        mIntent.putExtra("round", mRound);
        				setResult(RaceActivity.RESULT_OK, mIntent);
		        		finish();
		        		return true;
        			}
        			return false;
        		}
        	});
		}
	}
	
}
