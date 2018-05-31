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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.activities.FanartActivity;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.background.BackgroundService;
import org.gateshipone.malp.application.background.BackgroundServiceConnection;
import org.gateshipone.malp.application.callbacks.OnSaveDialogListener;
import org.gateshipone.malp.application.fragments.TextDialog;
import org.gateshipone.malp.application.fragments.serverfragments.ChoosePlaylistDialog;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.application.utils.OutputResponseMenuHandler;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.application.utils.VolumeButtonLongClickListener;
import org.gateshipone.malp.mpdservice.ConnectionManager;
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
import java.util.Locale;

public class NowPlayingView extends LinearLayout implements PopupMenu.OnMenuItemClickListener, ArtworkManager.onNewAlbumImageListener, ArtworkManager.onNewArtistImageListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = NowPlayingView.class.getSimpleName();

    private ServerStatusListener mStateListener;

    private ServerConnectionListener mConnectionStateListener;

    /**
     * Absolute pixel position of upper layout bound
     */
    private int mTopPosition;

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

    private StreamingStatusReceiver mStreamingStatusReceiver;

    private BackgroundServiceConnection mBackgroundServiceConnection;

    /**
     * Top buttons in the draggable header part.
     */
    private ImageButton mTopMenuButton;

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

    //private ImageView mVolumeIconButtons;

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

    private boolean mUseEnglishWikipedia;

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

    /**
     * Menu click listener. This method gets called when the user selects an item of the popup menu (right top corner).
     *
     * @param item MenuItem that was clicked.
     * @return Returns true if the item was handled by this method. False otherwise.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_playlist:
                final AlertDialog.Builder removeListBuilder = new AlertDialog.Builder(getContext());
                removeListBuilder.setTitle(getContext().getString(R.string.action_delete_playlist));
                removeListBuilder.setMessage(getContext().getString(R.string.dialog_message_delete_current_playlist));
                removeListBuilder.setPositiveButton(R.string.dialog_action_yes, (dialog, which) -> MPDQueryHandler.clearPlaylist());
                removeListBuilder.setNegativeButton(R.string.dialog_action_no, (dialog, which) -> {

                });
                removeListBuilder.create().show();
                break;
            case R.id.action_shuffle_playlist: {
                final AlertDialog.Builder shuffleListBuilder = new AlertDialog.Builder(getContext());
                shuffleListBuilder.setTitle(getContext().getString(R.string.action_shuffle_playlist));
                shuffleListBuilder.setMessage(getContext().getString(R.string.dialog_message_shuffle_current_playlist));
                shuffleListBuilder.setPositiveButton(R.string.dialog_action_yes, (dialog, which) -> MPDQueryHandler.shufflePlaylist());
                shuffleListBuilder.setNegativeButton(R.string.dialog_action_no, (dialog, which) -> {
                });
                shuffleListBuilder.create().show();
            }
                break;
            case R.id.action_save_playlist:
                OnSaveDialogListener plDialogCallback = new OnSaveDialogListener() {
                    @Override
                    public void onSaveObject(final String title) {
                        AlertDialog.Builder overWriteBuilder = new AlertDialog.Builder(getContext());
                        overWriteBuilder.setTitle(getContext().getString(R.string.action_overwrite_playlist));
                        overWriteBuilder.setMessage(getContext().getString(R.string.dialog_message_overwrite_playlist) + ' ' + title + '?');
                        overWriteBuilder.setPositiveButton(R.string.dialog_action_yes, (dialog, which) -> {
                            MPDQueryHandler.removePlaylist(title);
                            MPDQueryHandler.savePlaylist(title);
                        });
                        overWriteBuilder.setNegativeButton(R.string.dialog_action_no, (dialog, which) -> {

                        });
                        overWriteBuilder.create().show();

                    }

                    @Override
                    public void onCreateNewObject() {
                        // open dialog in order to save the current playlist as a playlist in the mediastore
                        TextDialog textDialog = new TextDialog();
                        Bundle args = new Bundle();
                        args.putString(TextDialog.EXTRA_DIALOG_TITLE, getResources().getString(R.string.dialog_save_playlist));
                        args.putString(TextDialog.EXTRA_DIALOG_TEXT, getResources().getString(R.string.default_playlist_title));

                        textDialog.setCallback(MPDQueryHandler::savePlaylist);
                        textDialog.setArguments(args);
                        textDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "SavePLTextDialog");
                    }
                };

                // open dialog in order to save the current playlist as a playlist in the mediastore
                ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog();
                Bundle args = new Bundle();
                args.putBoolean(ChoosePlaylistDialog.EXTRA_SHOW_NEW_ENTRY, true);

                choosePlaylistDialog.setCallback(plDialogCallback);
                choosePlaylistDialog.setArguments(args);
                choosePlaylistDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChoosePlaylistDialog");
                break;
            case R.id.action_add_url:
                TextDialog addURLDialog = new TextDialog();
                addURLDialog.setCallback(MPDQueryHandler::addPath);
                Bundle textDialogArgs = new Bundle();
                textDialogArgs.putString(TextDialog.EXTRA_DIALOG_TEXT, "http://...");
                textDialogArgs.putString(TextDialog.EXTRA_DIALOG_TITLE, getResources().getString(R.string.action_add_url));
                addURLDialog.setArguments(textDialogArgs);
                addURLDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "AddURLDialog");
                break;
            case R.id.action_jump_to_current:
                mPlaylistView.jumpToCurrentSong();
                break;
            case R.id.action_toggle_single_mode:
                if (null != mLastStatus) {
                    if (mLastStatus.getSinglePlayback() == 0) {
                        MPDCommandHandler.setSingle(true);
                    } else {
                        MPDCommandHandler.setSingle(false);
                    }
                }
                break;
            case R.id.action_toggle_consume_mode:
                if (null != mLastStatus) {
                    if (mLastStatus.getConsume() == 0) {
                        MPDCommandHandler.setConsume(true);
                    } else {
                        MPDCommandHandler.setConsume(false);
                    }
                }
                break;
            case R.id.action_open_fanart:
                Intent intent = new Intent(getContext(), FanartActivity.class);
                getContext().startActivity(intent);
                return true;
            case R.id.action_wikipedia_album:
                Intent albumIntent = new Intent(Intent.ACTION_VIEW);
                //albumIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/index.php?search=" + mLastTrack.getTrackAlbum() + "&title=Special:Search&go=Go"));
                if (mUseEnglishWikipedia) {
                    albumIntent.setData(Uri.parse("https://en.wikipedia.org/wiki/" + mLastTrack.getTrackAlbum()));
                } else {
                    albumIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/" + mLastTrack.getTrackAlbum()));
                }
                getContext().startActivity(albumIntent);
                return true;
            case R.id.action_wikipedia_artist:
                Intent artistIntent = new Intent(Intent.ACTION_VIEW);
                //artistIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/index.php?search=" + mLastTrack.getTrackAlbumArtist() + "&title=Special:Search&go=Go"));
                if (mUseEnglishWikipedia) {
                    artistIntent.setData(Uri.parse("https://en.wikipedia.org/wiki/" + mLastTrack.getTrackArtist()));
                } else {
                    artistIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/" + mLastTrack.getTrackArtist()));
                }
                getContext().startActivity(artistIntent);
                return true;
            case R.id.action_start_streaming: {
                if (mStreamingStatus == BackgroundService.STREAMING_STATUS.PLAYING || mStreamingStatus == BackgroundService.STREAMING_STATUS.BUFFERING) {
                    try {
                        mBackgroundServiceConnection.getService().stopStreamingPlayback();
                    } catch (RemoteException e) {

                    }
                } else {
                    try {
                        mBackgroundServiceConnection.getService().startStreamingPlayback();
                    } catch (RemoteException e) {

                    }
                }
                return true;
            }
            case R.id.action_share_current_song: {
                shareCurrentTrack();
                return true;
            }
            default:
                return false;
        }
        return false;
    }


    @Override
    public void newAlbumImage(MPDAlbum album) {
        if (mLastTrack.getTrackAlbum().equals(album.getName())) {
            //mCoverLoader.getImage(mLastTrack, true, mCoverImage.getWidth(), mCoverImage.getHeight());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getContext().getString(R.string.pref_volume_controls_key))) {
            setVolumeControlSetting();
        } else if (key.equals(getContext().getString(R.string.pref_use_english_wikipedia_key))) {
            mUseEnglishWikipedia = sharedPreferences.getBoolean(key, getContext().getResources().getBoolean(R.bool.pref_use_english_wikipedia_default));
        } else if (key.equals(getContext().getString(R.string.pref_show_npv_artist_image_key))) {
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
        String volumeControlView = sharedPref.getString(getContext().getString(R.string.pref_volume_controls_key), getContext().getString(R.string.pref_volume_control_view_default));

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
     * Handles touch inputs to some views, to make sure, the ViewDragHelper is called.
     *
     * @param ev Touch input event
     * @return True if handled by this view or false otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Call the drag helper
        //mDragHelper.processTouchEvent(ev);

        // Get the position of the new touch event
        final float x = ev.getX();
        final float y = ev.getY();

        // Check if the position lies in the bounding box of the header view (which is draggable)
        //boolean isHeaderViewUnder = mDragHelper.isViewUnder(mHeaderView, (int) x, (int) y);
        boolean isHeaderViewUnder = true;
        // Check if drag is handled by the helper, or the header or mainview. If not notify the system that input is not yet handled.
        //return isHeaderViewUnder && isViewHit(mHeaderView, (int) x, (int) y) || isViewHit(mMainView, (int) x, (int) y);
        return true;
    }


    /**
     * Checks if an input to coordinates lay within a View
     *
     * @param view View to check with
     * @param x    x value of the input
     * @param y    y value of the input
     * @return
     */
    private boolean isViewHit(View view, int x, int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    /**
     * Asks the ViewGroup about the size of all its children and paddings around.
     *
     * @param widthMeasureSpec  The width requirements for this view
     * @param heightMeasureSpec The height requirements for this view
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // FIXME check why super.onMeasure(widthMeasureSpec, heightMeasureSpec); causes
        // problems with scrolling header view.
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        //ViewGroup.LayoutParams imageParams = mCoverImage.getLayoutParams();
        //imageParams.height = mViewSwitcher.getHeight();
        //mCoverImage.setLayoutParams(imageParams);
        //mCoverImage.requestLayout();


        // Calculate the margin to smoothly resize text field
        //LayoutParams layoutParams = (LayoutParams) mHeaderTextLayout.getLayoutParams();
        //mHeaderTextLayout.setLayoutParams(layoutParams);
    }


    /**
     * Called after the layout inflater is finished.
     * Sets all global view variables to the ones inflated.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // header buttons
        mTopMenuButton = findViewById(R.id.now_playing_topMenuButton);

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

        mVolumeStepSize = 3;

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


        // Add listener to top menu button
        mTopMenuButton.setOnClickListener(this::showAdditionalOptionsMenu);

        // Add listener to bottom previous button
        mBottomPreviousButton.setOnClickListener(arg0 -> MPDCommandHandler.previousSong());

        // Add listener to bottom playpause button
        mBottomPlayPauseButton.setOnClickListener(arg0 -> MPDCommandHandler.togglePause());

        // Add listener to bottom next button
        mBottomNextButton.setOnClickListener(arg0 -> MPDCommandHandler.nextSong());

/*        mCoverImage.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FanartActivity.class);
            getContext().startActivity(intent);
        });
        mCoverImage.setVisibility(INVISIBLE);*/

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

        mUseEnglishWikipedia = sharedPref.getBoolean(getContext().getString(R.string.pref_use_english_wikipedia_key), getContext().getResources().getBoolean(R.bool.pref_use_english_wikipedia_default));

        mShowArtistImage = sharedPref.getBoolean(getContext().getString(R.string.pref_show_npv_artist_image_key), getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));
    }

    /**
     * Called to open the popup menu on the top right corner.
     *
     * @param v
     */
    private void showAdditionalOptionsMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        // Inflate the menu from a menu xml file
        popupMenu.inflate(R.menu.popup_menu_nowplaying);
        // Set the main NowPlayingView as a listener (directly implements callback)
        popupMenu.setOnMenuItemClickListener(this);
        // Real menu
        Menu menu = popupMenu.getMenu();

        // Set the checked menu item state if a MPDCurrentStatus is available
        if (null != mLastStatus) {
            MenuItem singlePlaybackItem = menu.findItem(R.id.action_toggle_single_mode);
            singlePlaybackItem.setChecked(mLastStatus.getSinglePlayback() == 1);

            MenuItem consumeItem = menu.findItem(R.id.action_toggle_consume_mode);
            consumeItem.setChecked(mLastStatus.getConsume() == 1);
        }

        // Check if streaming is configured for the current server
        boolean streamingEnabled = ConnectionManager.getInstance(getContext().getApplicationContext()).getStreamingEnabled();
        MenuItem streamingStartStopItem = menu.findItem(R.id.action_start_streaming);

        if (!streamingEnabled) {
            streamingStartStopItem.setVisible(false);
        } else {
            if (mStreamingStatus == BackgroundService.STREAMING_STATUS.PLAYING || mStreamingStatus == BackgroundService.STREAMING_STATUS.BUFFERING) {
                streamingStartStopItem.setTitle(getResources().getString(R.string.action_stop_streaming));
            } else {
                streamingStartStopItem.setTitle(getResources().getString(R.string.action_start_streaming));
            }
        }

        // Open the menu itself
        popupMenu.show();
    }


    /**
     * Called when a layout is requested from the graphics system.
     *
     * @param changed If the layout is changed (size, ...)
     * @param l       Left position
     * @param t       Top position
     * @param r       Right position
     * @param b       Bottom position
     */
/*    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Calculate the maximal range that the view is allowed to be dragged

        // New temporary top position, to fix the view at top or bottom later if state is idle.
        int newTop = mTopPosition;


        // Request the upper part of the NowPlayingView (header)
*//*        mHeaderView.layout(
                0,
                newTop,
                r,
                newTop + mHeaderView.getMeasuredHeight());

        // Request the lower part of the NowPlayingView (main part)
        mMainView.layout(
                0,
                newTop + mHeaderView.getMeasuredHeight(),
                r,
                newTop + b);*//*
    }*/

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

        getContext().getApplicationContext().unregisterReceiver(mStreamingStatusReceiver);

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

/*        if (mStreamingStatusReceiver == null) {
            mStreamingStatusReceiver = new StreamingStatusReceiver();
        }

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

        mUseEnglishWikipedia = sharedPref.getBoolean(getContext().getString(R.string.pref_use_english_wikipedia_key), getContext().getResources().getBoolean(R.bool.pref_use_english_wikipedia_default));

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
            int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_background_primary);

            Drawable drawable = getResources().getDrawable(R.drawable.cover_placeholder, null);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);

            // Show the placeholder image until the cover fetch process finishes
            //mCoverImage.clearAlbumImage();

            tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

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


    /**
     * Simple sharing for the current track.
     * <p>
     * This will only work if the track can be found in the mediastore.
     */
    private void shareCurrentTrack() {
        if (null == mLastTrack) {
            return;
        }
        String sharingText = getContext().getString(R.string.sharing_song_details, mLastTrack.getTrackTitle(), mLastTrack.getTrackArtist(), mLastTrack.getTrackAlbum());

        // set up intent for sharing
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, sharingText);
        shareIntent.setType("text/plain");

        // start sharing
        getContext().startActivity(Intent.createChooser(shareIntent, getContext().getString(R.string.dialog_share_song_details)));
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
     * Receives stream playback status updates. When stream playback is started the status
     * is necessary to show the right menu item.
     */
    private class StreamingStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BackgroundService.ACTION_STREAMING_STATUS_CHANGED)) {
                mStreamingStatus = BackgroundService.STREAMING_STATUS.values()[intent.getIntExtra(BackgroundService.INTENT_EXTRA_STREAMING_STATUS, 0)];
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
