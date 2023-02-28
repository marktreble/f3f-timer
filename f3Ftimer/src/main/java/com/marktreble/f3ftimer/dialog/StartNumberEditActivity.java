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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Looper;
import android.view.Window;

import com.google.android.material.tabs.TabLayoutMediator;
import com.marktreble.f3ftimer.F3FtimerApplication;
import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.constants.IComm;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;

import java.util.ArrayList;
import java.util.List;

public class StartNumberEditActivity extends AppCompatActivity {

    ArrayList<Pilot> mArrPilots;
    String mStartNumber;
    String mStartName;

    private Handler mHandler;
    private Context mContext;
    private float mAnimationDelay;

    TabLayout tabLayout;
    ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((F3FtimerApplication) getApplication()).setTransparentTheme(this);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.start_number);

        tabLayout = findViewById(R.id.tabs);
        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        int race_id = getIntent().getIntExtra("race_id", 0);


        RacePilotData datasource = new RacePilotData(this);
        datasource.open();
        mArrPilots = datasource.getAllPilotsForRace(race_id, 0, 0, 0);
        datasource.close();

        mHandler = new Handler(Looper.getMainLooper());

        mContext = this;

        if (savedInstanceState != null) {
            mStartNumber = savedInstanceState.getString("mStartNumber");
            mStartName = savedInstanceState.getString("mStartName");
            mAnimationDelay = savedInstanceState.getFloat("mAnimationDelay", 9999);

            if (mAnimationDelay > 0 && mAnimationDelay < 500) {
                mHandler.postDelayed(animate, (long) mAnimationDelay);
            }
        }

    }

    public void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("mStartNumber", mStartNumber);
        outState.putString("mStartName", mStartName);
        outState.putFloat("mAnimationDelay", mAnimationDelay);
    }

    private void setupViewPager(ViewPager2 viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        adapter.addFragment(new StartNumberEditFrag2(), "Set");
        adapter.addFragment(new StartNumberEditFrag1(), "Random");
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(
                        adapter.mFragmentTitleList.get(position)
                )
        ).attach();
    }

    static class ViewPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        public final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }


        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getItemCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
    }

    void startRandomAnimation() {
        mAnimationDelay = 5;
        mHandler.post(animate);
    }

    private Runnable animate = new Runnable() {
        @Override
        public void run() {
            int random = 0;
            while (!isValidStartNumber(String.format("%d", random))) {
                random = (int) Math.floor(Math.random() * mArrPilots.size()) + 1;
            }
            mAnimationDelay *= 1.1;
            // Post notification to update fragment
            Intent i = new Intent(IComm.RCV_UPDATE);
            i.putExtra(IComm.MSG_UI_UPDATE, "random_start_number");
            mContext.sendBroadcast(i);

            if (mAnimationDelay < 500) {
                mHandler.postDelayed(animate, (long) mAnimationDelay);
            }
        }
    };

    private boolean isValidStartNumber(String start) {
        if (start == null) return false;
        if (start.trim().equals("")) return false;

        String name = "";
        int n = Integer.parseInt(start, 10);
        if (n == 0) return false;

        for (Pilot p : mArrPilots) {
            if (p.number.equals(start)) {
                name = String.format("%s %s", p.firstname, p.lastname);
            }
        }
        if (name.trim().equals("")) return false;

        mStartNumber = start;
        mStartName = name;
        return true;
    }
}


