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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.CurrentPlaylistAdapter;
import org.gateshipone.malp.application.utils.ScrollSpeedListener;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

public class CurrentPlaylistView extends LinearLayout implements AdapterView.OnItemClickListener {
    Context mContext;
    private int mWidth;
    private int mHeight;

    /**
     * Adapter used by the ListView
     */
    private CurrentPlaylistAdapter mPlaylistAdapter;

    public CurrentPlaylistView(Context context) {
        this(context, null, 0);
    }
    public CurrentPlaylistView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public CurrentPlaylistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Inflate the layout for this fragment
        LayoutInflater.from(context).inflate(R.layout.listview_layout, this, true);

        // Get the main ListView of this fragment
        ListView listView = this.findViewById(R.id.main_listview);

        // Create the needed adapter for the ListView
        mPlaylistAdapter = new CurrentPlaylistAdapter(getContext(), listView);

        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(new ScrollSpeedListener(mPlaylistAdapter, listView));

        // Return the ready inflated and configured fragment view.
        mContext = context;
        mWidth = getWidth();
        mHeight = getHeight();

    }

    /**
     * Play the selected track.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MPDCommandHandler.playSongIndex(position);
    }

/*    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();

        if (mHeight == 0 || mWidth == 0){
            if (mWidth == 0) {
                mWidth = r - l;
            }
            if (mHeight == 0){
                mHeight = b - t;
            }
            setMeasuredDimension(mWidth, mHeight);
        }
        Log.d("-------------DEBUG-----------onLayout", "heigth " + mHeight);
        Log.d("-------------DEBUG-----------onLayout", "width " + mWidth);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = 0;
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        Log.d("-------------DEBUG-----------onMeasure", "measured heigth " + height);
        Log.d("-------------DEBUG-----------onMeasure", "measured width " + width);

        if (height == 0 || width == 0){
            setMeasuredDimension(mWidth, mHeight);
        }
        width = getMeasuredWidth();
        height = getMeasuredHeight();
        Log.d("-------------DEBUG-----------onMeasure", "updated heigth " + height);
        Log.d("-------------DEBUG-----------onMeasure", "updated width " + width);
    }*/

    public void onResume() {
        mPlaylistAdapter.onResume();
    }

    public void onPause() {
        mPlaylistAdapter.onPause();
    }

    public MPDFileEntry getItem(int position) {
        if (null != mPlaylistAdapter ) {
            return (MPDFileEntry)mPlaylistAdapter.getItem(position);
        }
        return null;
    }

    public CurrentPlaylistAdapter.VIEW_TYPES getItemViewType(int position) {
        return CurrentPlaylistAdapter.VIEW_TYPES.values()[mPlaylistAdapter.getItemViewType(position)];
    }

    public void removeAlbumFrom(int position) {
        mPlaylistAdapter.removeAlbumFrom(position);
    }

    /**
     * Triggers a jump to the currently playing song. Not animated.
     */
    public void jumpToCurrentSong() {
        mPlaylistAdapter.jumpToCurrent();
    }

}


