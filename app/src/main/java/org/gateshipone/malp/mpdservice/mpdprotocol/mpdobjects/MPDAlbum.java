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

package org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects;


import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.LibraryItem;
import org.gateshipone.malp.application.loaders.AlbumTracksLoader;
import org.gateshipone.malp.application.utils.App;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MPDAlbum implements LibraryItem, MPDGenericItem, Comparable<MPDAlbum>, Parcelable {

    public enum MPD_ALBUM_SORT_ORDER {
        TITLE, // Default value
        DATE
    }

    /* Album properties */
    @NonNull
    private String mAlbumName;

    /* Musicbrainz ID */
    @NonNull
    private String mMBID;

    /* Artists */
    @NonNull
    private String mArtistName;
    private String mArtistSortName;

    @NonNull
    private String mDate;

    private boolean mImageFetching;

    public static final int  VIEW_TYPE = 1;
    private boolean mExpanded = false;
    private List<LibraryItem> pTrackList;
    private MPDAlbum.TrackResponseHandler pTrackResponseHandler;
    private boolean mUseArtistSort;

    private static class TrackResponseHandler extends MPDResponseFileList {
        private WeakReference<MPDAlbum> mAlbum;

        private TrackResponseHandler(MPDAlbum album) {
            mAlbum = new WeakReference<>(album);
        }


        @Override
        public void handleTracks(List<MPDFileEntry> fileList, int start, int end) {
            MPDAlbum album = mAlbum.get();
            List<MPDTrack> tracklist = new ArrayList<>();

            for(MPDFileEntry file: fileList) {
                if (file instanceof MPDTrack) {
                    tracklist.add((MPDTrack)file);
                }
            }

            if (album != null && tracklist.size() > 0) {
                album.setTrackList(tracklist);
            }
        }
    }

    public MPDAlbum(@NonNull String name) {
        mAlbumName = name;
        mMBID = "";
        mDate = "";
        mArtistName = "";
        mArtistSortName = "";
        pTrackResponseHandler = new MPDAlbum.TrackResponseHandler(this);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        mUseArtistSort = sharedPref.getBoolean(App.getContext().getString(R.string.pref_use_artist_sort_key), App.getContext().getResources().getBoolean(R.bool.pref_use_artist_sort_default));
    }

    /* Getters */

    protected MPDAlbum(Parcel in) {
        mAlbumName = in.readString();
        mMBID = in.readString();
        mArtistName = in.readString();
        mArtistSortName = in.readString();
        mImageFetching = in.readByte() != 0;
        mDate = in.readString();
        mUseArtistSort = in.readByte() != 0;
        pTrackResponseHandler = new MPDAlbum.TrackResponseHandler(this);
    }

    public static final Creator<MPDAlbum> CREATOR = new Creator<MPDAlbum>() {
        @Override
        public MPDAlbum createFromParcel(Parcel in) {
            return new MPDAlbum(in);
        }

        @Override
        public MPDAlbum[] newArray(int size) {
            return new MPDAlbum[size];
        }
    };

    @NonNull
    public String getName() {
        return mAlbumName;
    }

    @NonNull
    public String getMBID() {
        return mMBID;
    }

    @NonNull
    public String getArtistName() {
        return mArtistName;
    }

    public void setArtistName(String name) {
        mArtistName = name;
    }

    public void setArtistSortName(String name) {
        mArtistSortName = name;
    }

    public String getArtistSortName() {
        return mArtistSortName;
    }

    public void setMBID(@NonNull String mbid) {
        mMBID = mbid;
    }

    public void setDate(@NonNull String date) {
        mDate = date;
    }

    public String getDate() {
        return mDate;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MPDAlbum)) {
            return false;
        }

        MPDAlbum album = (MPDAlbum) object;
        return (mAlbumName.equals(album.mAlbumName)) && (mArtistName.equals(album.getArtistName())) &&
                (mMBID.equals(album.mMBID)) && (mDate.equals(album.mDate));
    }

    @Override
    public int compareTo(@NonNull MPDAlbum another) {
        if (another.equals(this)) {
            return 0;
        }
        return mAlbumName.toLowerCase().compareTo(another.mAlbumName.toLowerCase());
    }

    @Override
    public int hashCode() {
        return (mAlbumName + mArtistName + mMBID).hashCode();
    }

    public synchronized void setFetching(boolean fetching) {
        mImageFetching = fetching;
    }

    public synchronized boolean getFetching() {
        return mImageFetching;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        return mAlbumName + "_" + mArtistName + "_" + mMBID + "_" + mDate;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAlbumName);
        dest.writeString(mMBID);
        dest.writeString(mArtistName);
        dest.writeString(mArtistSortName);
        dest.writeByte((byte) (mImageFetching ? 1 : 0));
        dest.writeString(mDate);
        dest.writeByte((byte) (mUseArtistSort ? 1 : 0));
    }

    public static class MPDAlbumDateComparator implements Comparator<MPDAlbum> {

        @Override
        public int compare(MPDAlbum o1, MPDAlbum o2) {
            if (o2.equals(o1)) {
                return 0;
            }
            return o1.mDate.compareTo(o2.mDate);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MPDAlbum && obj.equals(this);
        }
    }

    @Override
    public String getSectionTitle() {
        return null;
    }

    public void setTrackList(List<MPDTrack> trackList) {
        pTrackList.clear();
        pTrackList.addAll(trackList);
    }

    public String getMainText() {
        return mAlbumName;
    }

    public String getPostfixText() {
        return mDate;
    }

    public String getPrefixText() {
        return null;
    }

    public int getKidCount() {
        return getKidItems().size();
    }


    public List<LibraryItem> getKidItems() {

        if (mUseArtistSort && !mArtistSortName.isEmpty()) {
            MPDQueryHandler.getArtistSortAlbumTracks(pTrackResponseHandler, mAlbumName, mArtistSortName, mMBID);
        } else {
            MPDQueryHandler.getArtistAlbumTracks(pTrackResponseHandler, mAlbumName, mArtistName, mMBID);
        }

        return pTrackList;
    }

    public int getLevel(){ return MPDAlbum.VIEW_TYPE;}

    public boolean isExpanded() { return mExpanded;}

    public void setExpanded(boolean expanded) { mExpanded = expanded; }

    public int getViewType() { return MPDAlbum.VIEW_TYPE; }
}
