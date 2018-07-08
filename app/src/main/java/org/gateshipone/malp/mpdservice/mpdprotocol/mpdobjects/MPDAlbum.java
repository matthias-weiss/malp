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


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.gateshipone.malp.application.adapters.LibraryItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MPDAlbum implements LibraryItem, Comparable<MPDAlbum>, Parcelable {

    public enum MPD_ALBUM_SORT_ORDER {
        TITLE, // Default value
        DATE
    }

    /* Album properties */
    @NonNull
    private String mName;

    /* Musicbrainz ID */
    @NonNull
    private String mMBID;

    /* Artists */
    @NonNull
    private MPDArtist mArtist;

    @NonNull
    private Date mDate;

    private boolean mImageFetching;

    public static final int  VIEW_TYPE = 1;
    private boolean mExpanded = false;

    public MPDAlbum(@NonNull String name, @NonNull MPDArtist artist) {
        mName = name;
        mMBID = "";
        mDate = new Date(0);
        mArtist = artist;
    }

    /* Getters */

    protected MPDAlbum(Parcel in) {
        mName = in.readString();
        mMBID = in.readString();
        mImageFetching = in.readByte() != 0;
        mDate = (Date) in.readSerializable();
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
        return mName;
    }

    @NonNull
    public String getMBID() {
        return mMBID;
    }

    @NonNull
    public String getArtistName() {
        return mArtist.getArtistName();
    }

    public void setMBID(@NonNull String mbid) {
        mMBID = mbid;
    }

    public void setDate(@NonNull Date date) {
        mDate = date;
    }

    public Date getDate() {
        return mDate;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MPDAlbum)) {
            return false;
        }

        MPDAlbum album = (MPDAlbum) object;
        return (mName.equals(album.mName)) && (mArtist.getArtistName().equals(album.getArtistName())) &&
                (mMBID.equals(album.mMBID)) && (mDate.equals(album.mDate));
    }

    @Override
    public int compareTo(@NonNull MPDAlbum another) {
        if (another.equals(this)) {
            return 0;
        }
        return mName.toLowerCase().compareTo(another.mName.toLowerCase());
    }

    @Override
    public int hashCode() {
        return (mName + mArtist.getArtistName() + mMBID).hashCode();
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
        return mName + "_" + mArtist.getArtistName() + "_" + mMBID + "_" + mDate;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mMBID);
        dest.writeByte((byte) (mImageFetching ? 1 : 0));
        dest.writeSerializable(mDate);
        //dest.writeSerializable(mArtist);
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

    public String getMainText() {
        return mName;
    }

    public String getPostfixText() {
        return mDate.toString();
    }

    public String getPrefixText() {
        return null;
    }

    public int getKidCount() {
        return getKidItems().size();
    }


    public List<LibraryItem> getKidItems() {
        List<LibraryItem> list = new ArrayList<>();

        for (Song song : mAlbum.getSortedSongs()) {
            list.add(new SongItem(this, song));
        }

        return list;
    }

    public int getLevel(){ return MPDAlbum.VIEW_TYPE;}

    public LibraryItem getParentItem() { return mArtist; }

    public boolean isExpanded() { return mExpanded;}

    public void setExpanded(boolean expanded) { mExpanded = expanded; }

    public int getViewType() { return MPDAlbum.VIEW_TYPE; }
}
