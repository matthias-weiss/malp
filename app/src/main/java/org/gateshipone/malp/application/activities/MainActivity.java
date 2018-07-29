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

package org.gateshipone.malp.application.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.TextView;


import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.CurrentPlaylistAdapter;
import org.gateshipone.malp.application.fragments.serverfragments.AudioSourceTabsFragment;
import org.gateshipone.malp.application.utils.App;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.application.callbacks.AddPathToPlaylist;
import org.gateshipone.malp.application.fragments.serverfragments.ChoosePlaylistDialog;
import org.gateshipone.malp.application.fragments.serverfragments.SongDetailsDialog;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.application.views.CurrentPlaylistView;
import org.gateshipone.malp.application.views.NowPlayingView;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDException;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDProfileManager;

public class MainActivity extends GenericActivity {

    private static final String TAG = "MainActivity";

    public final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW = "org.malp.requestedview";
    public final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW = "org.malp.requestedview.nowplaying";

    private boolean mUseArtistSort;

    private View mDecorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDecorView = getWindow().getDecorView();

        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mUseArtistSort = sharedPref.getBoolean(getString(R.string.pref_use_artist_sort_key), getResources().getBoolean(R.bool.pref_use_artist_sort_default));

        registerForContextMenu(findViewById(R.id.main_listview));

        if (MPDProfileManager.getInstance(this).getProfiles().size() == 0) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.welcome_dialog_title));
            builder.setMessage(getResources().getString(R.string.welcome_dialog_text));


            builder.setPositiveButton(R.string.dialog_action_ok, (dialog, id) -> {
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        if (savedInstanceState == null) {
            Fragment audioSourceTabsFragment = new AudioSourceTabsFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.audio_sources_view, audioSourceTabsFragment).commit();
        }

        App.setContext(this);

        hideSystemUI();
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (item.getItemId()) {
            case android.R.id.home:
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    onBackPressed();
                }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.main_listview ) {
            int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_menu_current_playlist_track, menu);

            // Check if the menu is created for the currently playing song. If this is the case, do not show play as next item.
            MPDCurrentStatus status = MPDStateMonitoringHandler.getHandler().getLastStatus();
            if (status != null && position == status.getCurrentSongIndex()) {
                menu.findItem(R.id.action_song_play_next).setVisible(false);
            }


            CurrentPlaylistView currentPlaylistView = findViewById(R.id.now_playing_playlist);
            if (currentPlaylistView.getItemViewType(position) == CurrentPlaylistAdapter.VIEW_TYPES.TYPE_SECTION_TRACK_ITEM) {
                menu.findItem(R.id.action_remove_album).setVisible(true);
            }
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        CurrentPlaylistView currentPlaylistView = findViewById(R.id.now_playing_playlist);

        if (currentPlaylistView != null) {

            MPDTrack track = (MPDTrack) currentPlaylistView.getItem(info.position);

            switch (item.getItemId()) {

                case R.id.action_song_play_next:
                    MPDQueryHandler.playIndexAsNext(info.position);
                    return true;
                case R.id.action_add_to_saved_playlist:
                    // open dialog in order to save the current playlist as a playlist in the mediastore
                    ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog();
                    Bundle args = new Bundle();
                    args.putBoolean(ChoosePlaylistDialog.EXTRA_SHOW_NEW_ENTRY, true);
                    choosePlaylistDialog.setCallback(new AddPathToPlaylist(track, this));
                    choosePlaylistDialog.setArguments(args);
                    choosePlaylistDialog.show(getSupportFragmentManager(), "ChoosePlaylistDialog");
                    return true;
                case R.id.action_remove_song:
                    MPDQueryHandler.removeSongFromCurrentPlaylist(info.position);
                    return true;
                case R.id.action_remove_album:
                    currentPlaylistView.removeAlbumFrom(info.position);
                    return true;
                case R.id.action_show_artist:
                    //if (mUseArtistSort) {
                    //    onArtistSelected(new MPDArtist(track.getTrackArtistSort()), null);
                    //} else {
                    //    onArtistSelected(new MPDArtist(track.getTrackArtist()), null);
                    //}
                    return true;
                case R.id.action_show_album:
                    MPDAlbum tmpAlbum = new MPDAlbum(track.getTrackAlbum());
                    // Set album artist
                    if (!track.getTrackAlbumArtist().isEmpty()) {
                        tmpAlbum.setArtistName(track.getTrackAlbumArtist());
                    } else {
                        tmpAlbum.setArtistName(track.getTrackArtist());
                    }

                    // Set albumartistsort
                    //if (!track.getTrackAlbumArtistSort().isEmpty()) {
                    //    tmpAlbum.setArtistSortName(track.getTrackAlbumArtistSort());
                    //} else {
                    //    tmpAlbum.setArtistSortName(track.getTrackArtistSort());
                    //}

                    tmpAlbum.setMBID(track.getTrackAlbumMBID());
                    //onAlbumSelected(tmpAlbum, null);
                    return true;
                case R.id.action_show_details:
                    // Open song details dialog
                    SongDetailsDialog songDetailsDialog = new SongDetailsDialog();
                    Bundle songArgs = new Bundle();
                    songArgs.putParcelable(SongDetailsDialog.EXTRA_FILE, track);
                    songDetailsDialog.setArguments(songArgs);
                    songDetailsDialog.show(getSupportFragmentManager(), "SongDetails");
                    return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        final NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            /*
             * Check if the activity got an extra in its intend to show the nowplayingview directly.
             * If yes then pre set the dragoffset of the draggable helper.
             */
            Intent resumeIntent = getIntent();
            if (resumeIntent != null && resumeIntent.getExtras() != null && resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW) != null &&
                    resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW).equals(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW)) {
                getIntent().removeExtra(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW);
            }
            nowPlayingView.onResume();
        }
        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        NowPlayingView nowPlayingView = findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {

            nowPlayingView.onPause();
        }
    }

    @Override
    protected void onConnected() {
        setNavbarHeader(ConnectionManager.getInstance(getApplicationContext()).getProfileName());
    }

    @Override
    protected void onDisconnected() {
        setNavbarHeader(getString(R.string.app_name_nice));
    }

    @Override
    protected void onMPDError(MPDException.MPDServerException e) {
        View layout = findViewById(R.id.main_activity_layout);
        if (layout != null) {
            String errorText = getString(R.string.snackbar_mpd_server_error_format,e.getErrorCode(), e.getCommandOffset(), e.getServerMessage());
            Snackbar sb = Snackbar.make(layout, errorText, Snackbar.LENGTH_LONG);

            // style the snackbar text
            TextView sbText = sb.getView().findViewById(android.support.design.R.id.snackbar_text);
            sbText.setTextColor(ThemeUtils.getThemeColor(this, R.attr.malp_color_text_accent));
            sb.show();
        }
    }

    @Override
    protected void onMPDConnectionError(MPDException.MPDConnectionException e) {
        View layout = findViewById(R.id.main_activity_layout);
        if (layout != null) {
            String errorText = getString(R.string.snackbar_mpd_connection_error_format,e.getError());

            Snackbar sb = Snackbar.make(layout, errorText, Snackbar.LENGTH_LONG);

            // style the snackbar text
            TextView sbText = sb.getView().findViewById(android.support.design.R.id.snackbar_text);
            sbText.setTextColor(ThemeUtils.getThemeColor(this, R.attr.malp_color_text_accent));
            sb.show();
        }
    }

    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    public void setNavbarHeader(String text) {
        TextView header = findViewById(R.id.navdrawer_header_text);
        if (header == null) {
            return;
        }

        if (text == null) {
            header.setText("");
        }
        header.setText(text);
    }
}
