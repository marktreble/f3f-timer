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
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.marktreble.f3ftimer.R;

public class GenericAlert extends DialogFragment
        implements View.OnClickListener {

    String mTitle;
    String mMessage;
    String[] mButtons;
    View mView;
    ResultReceiver mReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mTitle = args.getString("title");
        mMessage = args.getString("message");
        mButtons = args.getStringArray("buttons");
        mReceiver = getArguments().getParcelable("receiver");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), getTheme()){
            @Override
            public void onBackPressed() {
                //do your stuff
                dismiss();
                getActivity().finish();
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
        int tag = (int)v.getTag();
        if (mReceiver != null) {
            mReceiver.send(tag, null);
        }
        dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.alert_dialog, container, false);

        // Do all the stuff to initialize your custom view
        TextView title = mView.findViewById(R.id.title);
        TextView message = mView.findViewById(R.id.message);

        title.setText(mTitle);
        message.setText(mMessage);

        Button btn = null;
        for (int i = 0; i < mButtons.length; i++) {
            if (i == 0) btn = mView.findViewById(R.id.button1);
            if (i == 1) btn = mView.findViewById(R.id.button2);
            if (i == 2) btn = mView.findViewById(R.id.button3);

            btn.setVisibility(View.VISIBLE);
            btn.setText(mButtons[i]);
            btn.setTag(i);
            btn.setOnClickListener(GenericAlert.this);
        }

        return mView;
    }

    public static GenericAlert newInstance(String title, String message, String[] buttons, ResultReceiver receiver) {
        GenericAlert f = new GenericAlert();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putStringArray("buttons", buttons);
        args.putParcelable("receiver", receiver);
        f.setArguments(args);
        return f;
    }
}
