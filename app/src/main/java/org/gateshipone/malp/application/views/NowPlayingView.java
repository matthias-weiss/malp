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

package org.gateshipone.malp.application.views;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.background.BackgroundService;
import org.gateshipone.malp.application.background.BackgroundServiceConnection;
import org.gateshipone.malp.application.fragments.TextDialog;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.application.utils.VolumeButtonLongClickListener;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.MPDStatusChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.lang.ref.WeakReference;

public class NowPlayingView extends ConstraintLayout implements ArtworkManager.onNewAlbumImageListener, ArtworkManager.onNewArtistImageListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = NowPlayingView.class.getSimpleName();

    private ServerStatusListener mStateListener;

    private ServerConnectionListener mConnectionStateListener;

    /**
     * Flag whether the views switches between album cover and artist image
     */
    private boolean mShowArtistImage = false;

    private BackgroundService.STREAMING_STATUS mStreamingStatus;

    /**
     * Main cover imageview
     */
    //private AlbumArtistView mCoverImage;

    /**
     * Small cover image, part of the draggable header
     */
    private ImageView mTopCoverImage;

    /**
     * View that contains the playlist ListVIew
     */
    private CurrentPlaylistView mPlaylistView;

    /**
     * Asynchronous loader for coverimages for TrackItems.
     */
    private CoverBitmapLoader mCoverLoader = null;

    private BackgroundServiceConnection mBackgroundServiceConnection;

    /**
     * Buttons in the bottom part of the view
     */
    private ImageButton mBottomPreviousButton;
    private ImageButton mBottomPlayPauseButton;
    private ImageButton mBottomNextButton;

    /**
     * Seekbar used for seeking and informing the user of the current playback position.
     */
    private SeekBar mPositionSeekbar;

    private TextView mVolumeText;

    private ImageButton mVolumeMinus;
    private ImageButton mVolumePlus;

    private VolumeButtonLongClickListener mPlusListener;
    private VolumeButtonLongClickListener mMinusListener;

    private int mVolumeStepSize;

    /**
     * Various textviews for track information
     */
    private TextView mTrackName;
    private TextView mTrackArtist;
    private TextView mTrackAlbum;
    private TextView mElapsedTime;
    private TextView mDuration;

    private MPDCurrentStatus mLastStatus;
    private MPDTrack mLastTrack;

    public NowPlayingView(Context context) {
        this(context, null, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mStateListener = new ServerStatusListener();
        mConnectionStateListener = new ServerConnectionListener(this, getContext().getMainLooper());
        mLastStatus = new MPDCurrentStatus();
        mLastTrack = new MPDTrack("");
    }

    @Override
    public void newAlbumImage(MPDAlbum album) {
        if (mLastTrack.getTrackAlbum().equals(album.getName())) {
            //mCoverLoader.getImage(mLastTrack, true, mCoverImage.getWidth(), mCoverImage.getHeight());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getContext().getString(R.string.pref_show_npv_artist_image_key))) {
            mShowArtistImage = sharedPreferences.getBoolean(key, getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));

            // Show artist image if artwork is requested
            if (mShowArtistImage) {
                //mCoverLoader.getArtistImage(mLastTrack, true, mCoverImage.getWidth(), mCoverImage.getHeight());
            } else {
                // Hide artist image
                //mCoverImage.clearArtistImage();
            }
        } else if (key.equals(getContext().getString(R.string.pref_volume_steps_key))) {
            setVolumeControlSetting();
        }
    }


    private void setVolumeControlSetting() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        mVolumeStepSize = sharedPref.getInt(getContext().getString(R.string.pref_volume_steps_key), getResources().getInteger(R.integer.pref_volume_steps_default));
        mPlusListener.setVolumeStepSize(mVolumeStepSize);
        mMinusListener.setVolumeStepSize(mVolumeStepSize);
    }

    @Override
    public void newArtistImage(MPDArtist artist) {
        if (mShowArtistImage && mLastTrack.getTrackArtist().equals(artist.getArtistName())) {
            //mCoverLoader.getArtistImage(artist, false, mCoverImage.getWidth(), mCoverImage.getHeight());
        }
    }

    /**
     * Called after the layout inflater is finished.
     * Sets all global view variables to the ones inflated.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // bottom buttons
        mBottomPreviousButton = findViewById(R.id.now_playing_bottomPreviousButton);
        mBottomPlayPauseButton = findViewById(R.id.now_playing_bottomPlayPauseButton);
        //mBottomStopButton = findViewById(R.id.now_playing_bottomStopButton);
        mBottomNextButton = findViewById(R.id.now_playing_bottomNextButton);

        // Main cover image
        //mCoverImage = findViewById(R.id.now_playing_cover);
        // Small header cover image
        mTopCoverImage = findViewById(R.id.now_playing_topCover);

        // View with the ListView of the playlist
        mPlaylistView = findViewById(R.id.now_playing_playlist);

        // For marquee scrolling the TextView need selected == true
        mTrackName = findViewById(R.id.now_playing_trackName);
        mTrackName.setSelected(true);

        mTrackArtist = findViewById(R.id.now_playing_trackArtist);
        mTrackArtist.setSelected(true);

        mTrackAlbum = findViewById(R.id.now_playing_trackAlbum);
        mTrackAlbum.setSelected(true);

        // Textviews directly under the seekbar
        mElapsedTime = findViewById(R.id.now_playing_elapsedTime);
        mDuration = findViewById(R.id.now_playing_duration);

        // seekbar (position)
        mPositionSeekbar = findViewById(R.id.now_playing_seekBar);
        mPositionSeekbar.setOnSeekBarChangeListener(new PositionSeekbarListener());

        mVolumeText = findViewById(R.id.volume_button_text);

        mVolumeMinus = findViewById(R.id.volume_button_minus);

        mVolumeMinus.setOnClickListener(v -> MPDCommandHandler.decreaseVolume(mVolumeStepSize));

        mVolumePlus = findViewById(R.id.volume_button_plus);
        mVolumePlus.setOnClickListener(v -> MPDCommandHandler.increaseVolume(mVolumeStepSize));

        /* Create two listeners that start a repeating timer task to repeat the volume plus/minus action */
        mPlusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_UP, mVolumeStepSize);
        mMinusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_DOWN, mVolumeStepSize);

        /* Set the listener to the plus/minus button */
        mVolumeMinus.setOnLongClickListener(mMinusListener);
        mVolumeMinus.setOnTouchListener(mMinusListener);

        mVolumePlus.setOnLongClickListener(mPlusListener);
        mVolumePlus.setOnTouchListener(mPlusListener);

        // Add listener to bottom previous button
        mBottomPreviousButton.setOnClickListener(arg0 -> MPDCommandHandler.previousSong());

        // Add listener to bottom playpause button
        mBottomPlayPauseButton.setOnClickListener(arg0 -> MPDCommandHandler.togglePause());

        // Add listener to bottom next button
        mBottomNextButton.setOnClickListener(arg0 -> MPDCommandHandler.nextSong());

        //mCoverImage.setVisibility(INVISIBLE);

        mCoverLoader = new CoverBitmapLoader(getContext(), new CoverReceiverClass());

        // Register with MPDStateMonitoring system
        MPDStateMonitoringHandler.getHandler().registerStatusListener(mStateListener);
        MPDInterface.mInstance.addMPDConnectionStateChangeListener(mConnectionStateListener);

        mPlaylistView.onResume();
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewAlbumImageListener(this);
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewArtistImageListener(this);


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        setVolumeControlSetting();

        mShowArtistImage = sharedPref.getBoolean(getContext().getString(R.string.pref_show_npv_artist_image_key), getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));
    }


    /**
     * Stop the refresh timer when the view is not visible to the user anymore.
     * Unregister the receiver for NowPlayingInformation intends, not needed anylonger.
     */
    public void onPause() {
        // Unregister listener
        MPDStateMonitoringHandler.getHandler().unregisterStatusListener(mStateListener);
        MPDInterface.mInstance.removeMPDConnectionStateChangeListener(mConnectionStateListener);
        mPlaylistView.onPause();

        if (null != mBackgroundServiceConnection) {
            mBackgroundServiceConnection.closeConnection();
            mBackgroundServiceConnection = null;
        }

        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewAlbumImageListener(this);
        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewArtistImageListener(this);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Resumes refreshing operation because the view is visible to the user again.
     * Also registers to the NowPlayingInformation intends again.
     */
    public void onResume() {

        // get the playbackservice, when the connection is successfully established the timer gets restarted

        // Reenable scrolling views after resuming
        if (mTrackName != null) {
            mTrackName.setSelected(true);
        }

        if (mTrackArtist != null) {
            mTrackArtist.setSelected(true);
        }

        if (mTrackAlbum != null) {
            mTrackAlbum.setSelected(true);
        }

/*
        if (null == mBackgroundServiceConnection) {
            mBackgroundServiceConnection = new BackgroundServiceConnection(getContext().getApplicationContext(), new BackgroundServiceConnectionListener());
        }
        mBackgroundServiceConnection.openConnection();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BackgroundService.ACTION_STREAMING_STATUS_CHANGED);
        getContext().getApplicationContext().registerReceiver(mStreamingStatusReceiver, filter);*/

        // Register with MPDStateMonitoring system
        MPDStateMonitoringHandler.getHandler().registerStatusListener(mStateListener);
        MPDInterface.mInstance.addMPDConnectionStateChangeListener(mConnectionStateListener);

        mPlaylistView.onResume();
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewAlbumImageListener(this);
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewArtistImageListener(this);


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        setVolumeControlSetting();

        mShowArtistImage = sharedPref.getBoolean(getContext().getString(R.string.pref_show_npv_artist_image_key), getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));
    }


    private void updateMPDStatus(MPDCurrentStatus status) {
        MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();

        // update play buttons
        switch (state) {
            case MPD_PLAYING:
                mBottomPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_fill_48dp);
                break;
            case MPD_PAUSING:
            case MPD_STOPPED:
                mBottomPlayPauseButton.setImageResource(R.drawable.ic_play_circle_fill_48dp);
                break;
        }

        // Update position seekbar & textviews
        mPositionSeekbar.setMax(status.getTrackLength());
        if (!mPositionSeekbar.isPressed()) {
            mPositionSeekbar.setProgress(status.getElapsedTime());
        }

        mElapsedTime.setText(FormatHelper.formatTracktimeFromS(status.getElapsedTime()));
        mDuration.setText(FormatHelper.formatTracktimeFromS(status.getTrackLength()));

        // Update volume seekbar
        int volume = status.getVolume();

        mVolumeText.setText(String.valueOf(volume) + '%');

        mLastStatus = status;
    }

    private void updateMPDCurrentTrack(MPDTrack track) {
        // Check if track title is set, otherwise use track name, otherwise path
        String title = track.getVisibleTitle();
        mTrackName.setText(title);


        if (!track.getTrackArtist().isEmpty()) {
            mTrackArtist.setText(track.getTrackArtist());
        } else {
            mTrackArtist.setText(track.getPath());
        }
        if (!track.getTrackAlbum().isEmpty()) {
            mTrackAlbum.setText(track.getTrackAlbum());
        } else {
            mTrackAlbum.setText("");
        }

        if (null == mLastTrack || !track.getTrackAlbum().equals(mLastTrack.getTrackAlbum()) || !track.getTrackAlbumMBID().equals(mLastTrack.getTrackAlbumMBID())) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_on_primary);

            Drawable drawable = getResources().getDrawable(R.drawable.cover_placeholder, null);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);

            // Show the placeholder image until the cover fetch process finishes
            //mCoverImage.clearAlbumImage();

            tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_on_primary);

            drawable = getResources().getDrawable(R.drawable.cover_placeholder_128dp, null);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);


            // The same for the small header image
            mTopCoverImage.setImageDrawable(drawable);
            // Start the cover loader
            //mCoverLoader.getImage(track, true, mCoverImage.getWidth(), mCoverImage.getHeight());
        }

        if (mShowArtistImage && (null == mLastTrack || !track.getTrackArtist().equals(mLastTrack.getTrackArtist()) || !track.getTrackArtistMBID().equals(mLastTrack.getTrackAlbumArtistMBID()) )) {
            //mCoverImage.clearArtistImage();

            //mCoverLoader.getArtistImage(track, true, mCoverImage.getWidth(), mCoverImage.getHeight());
        }

        mLastTrack = track;

    }


    private class ServerStatusListener extends MPDStatusChangeHandler {

        @Override
        protected void onNewStatusReady(MPDCurrentStatus status) {
            updateMPDStatus(status);
        }

        @Override
        protected void onNewTrackReady(MPDTrack track) {
            updateMPDCurrentTrack(track);
        }
    }

    private static class ServerConnectionListener extends MPDConnectionStateChangeHandler {

        private WeakReference<NowPlayingView> mNPV;

        ServerConnectionListener(NowPlayingView npv, Looper looper) {
            super(looper);
            mNPV = new WeakReference<>(npv);
        }

        @Override
        public void onConnected() {
            mNPV.get().updateMPDStatus(MPDStateMonitoringHandler.getHandler().getLastStatus());
        }

        @Override
        public void onDisconnected() {
            mNPV.get().updateMPDStatus(new MPDCurrentStatus());
            mNPV.get().updateMPDCurrentTrack(new MPDTrack(""));
        }
    }

    private class PositionSeekbarListener implements SeekBar.OnSeekBarChangeListener {
        /**
         * Called if the user drags the seekbar to a new position or the seekbar is altered from
         * outside. Just do some seeking, if the action is done by the user.
         *
         * @param seekBar  Seekbar of which the progress was changed.
         * @param progress The new position of the seekbar.
         * @param fromUser If the action was initiated by the user.
         */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                // FIXME Check if it is better to just update if user releases the seekbar
                // (network stress)
                MPDCommandHandler.seekSeconds(progress);
            }
        }

        /**
         * Called if the user starts moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }

        /**
         * Called if the user ends moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Private class that handles when the CoverGenerator finishes its fetching of cover images.
     */
    private class CoverReceiverClass implements CoverBitmapLoader.CoverBitmapListener {

        /**
         * Called when a bitmap is created
         *
         * @param bm Bitmap ready for use in the UI
         */
        @Override
        public void receiveBitmap(final Bitmap bm, final CoverBitmapLoader.IMAGE_TYPE type) {
            if (bm != null) {
                Activity activity = (Activity) getContext();
                if (activity != null) {
                    // Run on the UI thread of the activity because we are modifying gui elements.
                    activity.runOnUiThread(() -> {
                        if (type == CoverBitmapLoader.IMAGE_TYPE.ALBUM_IMAGE) {
                            // Set the main cover image
                            //mCoverImage.setAlbumImage(bm);
                            // Set the small header image
                            mTopCoverImage.setImageBitmap(bm);
                        } else if (type == CoverBitmapLoader.IMAGE_TYPE.ARTIST_IMAGE) {
                            //mCoverImage.setArtistImage(bm);
                        }
                    });
                }
            }
        }
    }


    /**
     * Private class to handle when a {@link android.content.ServiceConnection} to the {@link BackgroundService}
     * is established. When the connection is established, the stream playback status is retrieved.
     */
    private class BackgroundServiceConnectionListener implements BackgroundServiceConnection.OnConnectionStatusChangedListener {

        @Override
        public void onConnected() {
            try {
                mStreamingStatus = BackgroundService.STREAMING_STATUS.values()[mBackgroundServiceConnection.getService().getStreamingStatus()];
            } catch (RemoteException e) {

            }
        }

        @Override
        public void onDisconnected() {

        }
    }


}
