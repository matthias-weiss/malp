/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *  
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package org.gateshipone.malp.application.fragments.serverfragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.fragments.AudioSourceViewPager;
import org.gateshipone.malp.application.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

public class AudioSourceTabsFragment extends Fragment {
    public final static String TAG = AudioSourceTabsFragment.class.getSimpleName();
    public final static String MY_MUSIC_REQUESTED_TAB = "ARG_REQUESTED_TAB";

    private AudioSourcePagerAdapter mAudioSourcePagerAdapter;

    private FABFragmentCallback mFABCallback = null;

    private AudioSourceViewPager mViewPager;

    private Menu mOptionMenu;


    /**
     * Constant for state saving
     */
    public final static String MYMUSICFRAGMENT_SAVED_INSTANCE_SEARCH_STRING = "MyMusicFragment.SearchString";


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_tab_pager, container, false);

        // create tabs
        /*TabLayout tabLayout = rootView.findViewById(R.id.my_music_tab_layout);

        // Icons
        final ColorStateList tabColors = tabLayout.getTabTextColors();
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.ic_recent_actors_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }
        drawable = res.getDrawable(R.drawable.ic_album_24dp, null);
        if (drawable != null) {
            Drawable icon = DrawableCompat.wrap(drawable);
            DrawableCompat.setTintList(icon, tabColors);
            tabLayout.addTab(tabLayout.newTab().setIcon(icon));
        }

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);*/

        mViewPager = rootView.findViewById(R.id.audio_source_viewpager);
        mAudioSourcePagerAdapter = new AudioSourcePagerAdapter(getChildFragmentManager());

        mAudioSourcePagerAdapter.addFragment(new LibraryFragment(), "Library");
        //mAudioSourcePagerAdapter.addFragment(new WebradioFragment(), "Webradio");
        //mAudioSourcePagerAdapter.addFragment(new PodcastFragment(), "Podcasts");

        mViewPager.setAdapter(mAudioSourcePagerAdapter);

        TabLayout tabLayout = rootView.findViewById(R.id.my_music_tab_layout);
        tabLayout.setupWithViewPager(mViewPager);

        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(false, null);
            mFABCallback.setupToolbar(getString(R.string.app_name), true, true, false);
        }
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);


        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.fragment_menu_library, menu);

        mOptionMenu = menu;

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_search).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_search).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
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
