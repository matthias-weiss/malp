
package org.gateshipone.malp.application.fragments.serverfragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.activities.SettingsActivity;
import org.gateshipone.malp.application.fragments.AudioSourceViewPager;

import java.util.ArrayList;
import java.util.List;

public class AudioSourceTabsFragment extends Fragment {
    public final static String TAG = AudioSourceTabsFragment.class.getSimpleName();

    private AudioSourcePagerAdapter mAudioSourcePagerAdapter;

    private AudioSourceViewPager mViewPager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_tab_pager, container, false);

        mViewPager = rootView.findViewById(R.id.audio_source_viewpager);
        mAudioSourcePagerAdapter = new AudioSourcePagerAdapter(getChildFragmentManager());

        mAudioSourcePagerAdapter.addFragment(new LibraryFragment(), "Library");
        //mAudioSourcePagerAdapter.addFragment(new WebradioFragment(), "Webradio");
        //mAudioSourcePagerAdapter.addFragment(new PodcastFragment(), "Podcasts");

        mViewPager.setAdapter(mAudioSourcePagerAdapter);

        TabLayout tabLayout = rootView.findViewById(R.id.audio_source_tab_layout);
        tabLayout.setupWithViewPager(mViewPager);

        ImageButton showSettings = rootView.findViewById(R.id.show_settings_button);
        showSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(), SettingsActivity.class);
                startActivity(i);
            }
        });
        //setHasOptionsMenu(true);
        return rootView;
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class AudioSourcePagerAdapter extends FragmentStatePagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public AudioSourcePagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
