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


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.util.List;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.ArtistsAdapter;
import org.gateshipone.malp.application.adapters.LibraryAdapter;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.listviewitems.AbsImageListViewItem;
import org.gateshipone.malp.application.loaders.ArtistsLoader;
import org.gateshipone.malp.application.utils.PreferenceHelper;
import org.gateshipone.malp.application.utils.ScrollSpeedListener;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;

public class LibraryFragment extends Fragment {

    protected ConnectionStateListener mConnectionStateListener;
    /**
     * GridView adapter object used for this GridView
     */
    //private ArtistsAdapter mArtistAdapter;
    private LibraryAdapter mLibraryAdapter;

    private RecyclerView mRecyclerView;

    /**
     * Save the last position here. Gets reused when the user returns to this view after selecting sme
     * albums.
     */
    private int mLastPosition = -1;


    //private ArtistSelectedCallback mSelectedCallback;


    private FABFragmentCallback mFABCallback = null;

    private boolean mUseList = false;

    private MPDAlbum.MPD_ALBUM_SORT_ORDER mAlbumSortOrder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String libraryView = sharedPref.getString(getString(R.string.pref_library_view_key), getString(R.string.pref_library_view_default));

        if (libraryView.equals(getString(R.string.pref_library_view_list_key))) {
            mUseList = true;
        }

        mAlbumSortOrder = PreferenceHelper.getMPDAlbumSortOrder(sharedPref, getContext());

        FrameLayout rootView = (FrameLayout) inflater.inflate(R.layout.library_fragment, container, false);
        mRecyclerView = rootView.findViewById(R.id.library_recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mLibraryAdapter = new LibraryAdapter(getContext(), mRecyclerView);
        mRecyclerView.setAdapter(mLibraryAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshContent();
        Activity activity = getActivity();
        if (activity != null) {
            mConnectionStateListener = new LibraryFragment.ConnectionStateListener(this, activity.getMainLooper());
            MPDInterface.mInstance.addMPDConnectionStateChangeListener(mConnectionStateListener);
        }
/*        if (null != mFABCallback) {
            mFABCallback.setupFAB(false, null);
            mFABCallback.setupToolbar(getString(R.string.app_name), true, true, false);
        }
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewArtistImageListener(mArtistAdapter);*/
    }

    @Override
    public void onPause() {
        super.onPause();
        synchronized (this) {
            getLoaderManager().destroyLoader(0);
            MPDInterface.mInstance.removeMPDConnectionStateChangeListener(mConnectionStateListener);
            mConnectionStateListener = null;
        }
        //ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewArtistImageListener(mArtistAdapter);
    }

    protected void refreshContent() {
        if ( !isDetached()) {
            //getLoaderManager().restartLoader(0, getArguments(), this);
            mLibraryAdapter.loadArtists();
        }
    }

/*
    */
/**
     * Called when the fragment is first attached to its context.
     *//*

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mSelectedCallback = (ArtistSelectedCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    */
/**
     * This method creates a new loader for this fragment.
     *
     * @param id
     * @param args
     * @return
     *//*

    @Override
    public Loader<List<MPDArtist>> onCreateLoader(int id, Bundle args) {
        // Read albumartists/artists preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean useAlbumArtists = sharedPref.getBoolean(getString(R.string.pref_use_album_artists_key), getResources().getBoolean(R.bool.pref_use_album_artists_default));
        boolean useArtistSort = sharedPref.getBoolean(getString(R.string.pref_use_artist_sort_key), getResources().getBoolean(R.bool.pref_use_artist_sort_default));
        return new ArtistsLoader(getActivity(), useAlbumArtists, useArtistSort);
    }

    */
/**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param data   Data of the loader
     *//*

    @Override
    public void onLoadFinished(Loader<List<MPDArtist>> loader, List<MPDArtist> data) {
        super.onLoadFinished(loader, data);
        // Set the actual data to the adapter.
        mArtistAdapter.swapModel(data);

        // Reset old scroll position
       if (mLastPosition >= 0) {
            mAdapterView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    */
/**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     *//*

    @Override
    public void onLoaderReset(Loader<List<MPDArtist>> loader) {
        // Clear the model data of the adapter.
        mArtistAdapter.swapModel(null);
    }

    */
/**
     * Create the context menu.
     *//*


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_artist, menu);
    }
*/

/*
    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
/*    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case R.id.fragment_artist_action_enqueue:
                enqueueArtist(info.position);
                return true;
            case R.id.fragment_artist_action_play:
                playArtist(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mLastPosition = position;

        MPDArtist artist = (MPDArtist) mArtistAdapter.getItem(position);

        Bitmap bitmap = null;

        // Check if correct view type, to be safe
        if (view instanceof AbsImageListViewItem) {
            bitmap = ((AbsImageListViewItem) view).getBitmap();
        }

        mSelectedCallback.onArtistSelected(artist, bitmap);
    }

    public interface ArtistSelectedCallback {
        void onArtistSelected(MPDArtist artistname, Bitmap bitmap);
    }


    private void enqueueArtist(int index) {
        MPDArtist artist = (MPDArtist) mArtistAdapter.getItem(index);

        MPDQueryHandler.addArtist(artist.getArtistName(), mAlbumSortOrder);
    }

    private void playArtist(int index) {
        MPDArtist artist = (MPDArtist) mArtistAdapter.getItem(index);

        MPDQueryHandler.playArtist(artist.getArtistName(), mAlbumSortOrder);
    }

    public void applyFilter(String name) {
        mArtistAdapter.applyFilter(name);
    }

    public void removeFilter() {
        mArtistAdapter.removeFilter();
    }*/

    private static class ConnectionStateListener extends MPDConnectionStateChangeHandler {
        private WeakReference<LibraryFragment> pFragment;

        public ConnectionStateListener(LibraryFragment fragment, Looper looper) {
            super(looper);
            pFragment = new WeakReference<>(fragment);
        }

        @Override
        public void onConnected() {
            pFragment.get().refreshContent();
        }

        @Override
        public void onDisconnected() {
            LibraryFragment fragment = pFragment.get();
            if(fragment == null) {
                return;
            }
            synchronized (fragment) {
                if (!fragment.isDetached()) {
                    if(fragment.getLoaderManager().hasRunningLoaders()) {
                        fragment.getLoaderManager().destroyLoader(0);
                    }
                }
            }
        }
    }
}
