package org.gateshipone.malp.application.fragments.serverfragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.LibraryAdapter;
import org.gateshipone.malp.application.utils.PreferenceHelper;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;

public class LibraryFragment extends Fragment {

    protected ConnectionStateListener mConnectionStateListener;
    private LibraryAdapter mLibraryAdapter;

    private RecyclerView mRecyclerView;

    private boolean mUseList = false;

    private MPDAlbum.MPD_ALBUM_SORT_ORDER mAlbumSortOrder;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String libraryView           = sharedPref.getString(getString(R.string.pref_library_view_key), getString(R.string.pref_library_view_default));

        if (libraryView.equals(getString(R.string.pref_library_view_list_key))) {
            mUseList = true;
        }

        mAlbumSortOrder = PreferenceHelper.getMPDAlbumSortOrder(sharedPref, getContext());

        FrameLayout rootView = (FrameLayout) inflater.inflate(R.layout.audio_source_fragment, container, false);
        mRecyclerView = rootView.findViewById(R.id.audio_source_recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

        boolean useAlbumArtists = sharedPref.getBoolean(getString(R.string.pref_use_album_artists_key), getResources().getBoolean(R.bool.pref_use_album_artists_default));
        boolean useArtistSort   = sharedPref.getBoolean(getString(R.string.pref_use_artist_sort_key), getResources().getBoolean(R.bool.pref_use_artist_sort_default));
        mLibraryAdapter         = new LibraryAdapter(getContext(), mRecyclerView, useAlbumArtists, useArtistSort);
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
        //ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewArtistImageListener(mArtistAdapter);
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
            mLibraryAdapter.loadArtists();
        }
    }

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
