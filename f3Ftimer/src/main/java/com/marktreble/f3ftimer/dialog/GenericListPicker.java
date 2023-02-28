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

import android.app.Dialog;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.helpers.parcelable.ParcelableHelper;

import java.util.ArrayList;

public class GenericListPicker extends DialogFragment
        implements View.OnClickListener {

    String mTitle;
    ArrayList<String> mOptions;
    String[] mButtons;
    View mView;
    ResultReceiver mReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mTitle = args.getString("title");
        mButtons = args.getStringArray("buttons");
        mReceiver = ParcelableHelper.getParcelableResultReceiver(args,"receiver");

        if (savedInstanceState == null) {
            mOptions = args.getStringArrayList("options");

        } else {
            mOptions = savedInstanceState.getStringArrayList("options");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), getTheme()){
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                requireActivity()
                        .getOnBackPressedDispatcher()
                        .addCallback(
                                requireActivity(),
                                new OnBackPressedCallback(true) {
                                    @Override
                                    public void handleOnBackPressed() {
                                        dismiss();
                                    }
                                });
            }
        };

        // request a window without the title
        Window w = dialog.getWindow();
        if (w != null) w.requestFeature(Window.FEATURE_NO_TITLE);

        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public void onClick(View v) {
        dismiss();
        int tag = (int)v.getTag();
        if (mReceiver != null) {
            mReceiver.send(tag, null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList("options", mOptions);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.list_picker_dialog, container, false);

        // Do all the stuff to initialize your custom view
        TextView title = mView.findViewById(R.id.title);

        title.setText(mTitle);


        ViewGroup options = mView.findViewById(R.id.options);
        for (int i = 0; i < mOptions.size(); i++) {
            View v = inflater.inflate(R.layout.list_picker_row, options, false);
            options.addView(v);
            TextView txt = v.findViewById(R.id.text1);
            txt.setText(mOptions.get(i));
            v.setTag(i + 100);
            v.setOnClickListener(GenericListPicker.this);
        }

        Button btn = null;
        for (int i = 0; i < mButtons.length; i++) {
            if (i == 0) btn = mView.findViewById(R.id.button1);
            if (i == 1) btn = mView.findViewById(R.id.button2);
            if (i == 2) btn = mView.findViewById(R.id.button3);

            btn.setVisibility(View.VISIBLE);
            btn.setText(mButtons[i]);
            btn.setTag(i);
            btn.setOnClickListener(GenericListPicker.this);
        }

        return mView;
    }

    public static GenericListPicker newInstance(String title, ArrayList<String> options, String[] buttons, ResultReceiver receiver) {
        GenericListPicker f = new GenericListPicker();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putStringArrayList("options", options);
        args.putStringArray("buttons", buttons);
        args.putParcelable("receiver", receiver);
        f.setArguments(args);
        return f;
    }
}
