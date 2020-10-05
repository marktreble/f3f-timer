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

package com.marktreble.f3ftimer.exportimport;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import androidx.fragment.app.FragmentTransaction;

import com.marktreble.f3ftimer.dialog.GenericAlert;

public class F3xvaultApiExportRace extends BaseExport {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] buttons_array = new String[1];
        buttons_array[0] = getString(android.R.string.ok);

        mDLG = GenericAlert.newInstance(
                "TO DO...",
                "This feature will be implemented soon",
                buttons_array,
                new ResultReceiver(new Handler()) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        finish();
                    }
                }
        );

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.addToBackStack(null);
        ft.add(mDLG, DIALOG);
        ft.commit();
    }
}
