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

import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;

public class RaceTimerFrag extends Fragment {

    protected View mView;

    public void setPilotName() {
        RaceTimerActivity a = (RaceTimerActivity) getActivity();
        String name = String.format("%s %s", a.mPilot.firstname, a.mPilot.lastname);

        if (name.trim().equals("")) {
            name = "noname! " + a.mPilot.id;
        }

        TextView pilot_name = mView.findViewById(R.id.current_pilot);
        TextView min_name = mView.findViewById(R.id.minpilot);
        pilot_name.setText(name);
        min_name.setText(name);

        Drawable flag = a.mPilot.getFlag(a);
        if (flag != null) {
            pilot_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
            min_name.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
            pilot_name.setCompoundDrawablePadding(padding);
            min_name.setCompoundDrawablePadding(padding);
        }

        TextView pilot_number = mView.findViewById(R.id.number);
        TextView min_number = mView.findViewById(R.id.minnumber);
        pilot_number.setText(a.mNumber);
        min_number.setText(a.mNumber);
    }

    public void setWindWarning(boolean on) {
        TextView warning = mView.findViewById(R.id.wind_warning);
        warning.setVisibility((on) ? View.VISIBLE : View.INVISIBLE);
    }

    public void startPressed() {
        // Abstract
    }

    public void setMinimized() {
        View min = mView.findViewById(R.id.minimised);
        min.setVisibility(View.VISIBLE);
        View full = mView.findViewById(R.id.full);
        full.setVisibility(View.GONE);
    }

    public void setExpanded() {
        View min = mView.findViewById(R.id.minimised);
        min.setVisibility(View.GONE);
        View full = mView.findViewById(R.id.full);
        full.setVisibility(View.VISIBLE);
    }
}
