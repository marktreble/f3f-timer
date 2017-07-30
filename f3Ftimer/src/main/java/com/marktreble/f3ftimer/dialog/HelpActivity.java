/*
 * About Activity
 * Software Version/credits
 */
package com.marktreble.f3ftimer.dialog;
import com.marktreble.f3ftimer.R;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

public class HelpActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);

		PackageInfo pInfo;
		String v = "";
		String b = "";
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			v = pInfo.versionName;
			b = String.format("%d", pInfo.versionCode);
		} catch (PackageManager.NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TextView version = (TextView) findViewById(R.id.version);
		version.setText(String.format("%s %s (Build %s)", getString(R.string.version), v, b));
	}
	
}
