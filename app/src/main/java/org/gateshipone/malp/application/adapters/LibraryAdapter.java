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

package org.gateshipone.malp.application.adapters;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder>
        implements ArtworkManager.onNewArtistImageListener {

    private boolean mUseAlbumArtists;
    private boolean mUseArtistSort;
    private int     mListItemHeight;

    private MPDResponseArtistList pArtistResponseHandler;
    private MPDResponseAlbumList  pAlbumResponseHandler;
    private MPDResponseFileList   pTrackResponseHandler;
    private ArtworkManager        mArtworkManager;

    private final ArrayList<LibraryItem>    mList = new ArrayList<>();
    private final Context                   mContext;
    private int                             mLevelIndicatorWidth = 0;
    private final Vector<ExpandedItem>      mExpanded = new Vector<ExpandedItem>();
    private RecyclerView                    mRecyclerView;


    public class ExpandedItem {
        public LibraryItem        mItem;
        LibraryAdapter.ViewHolder mHolder;
        public int                mPosition;
        public int                mNrOfChildren;

        public ExpandedItem(LibraryItem item, LibraryAdapter.ViewHolder holder, int position, int nrOfChildren) {
            mItem         = item;
            mHolder       = holder;
            mPosition     = position;
            mNrOfChildren = nrOfChildren;
        }
    }

    private static class ArtistResponseHandler extends MPDResponseArtistList {
        private WeakReference<LibraryAdapter> mAdapter;

        private ArtistResponseHandler(LibraryAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void handleArtists(List<MPDArtist> artistList) {
            LibraryAdapter adapter = mAdapter.get();
            if (adapter != null) {
                adapter.updateArtists(artistList);
            }
        }
    }

    public static class AlbumResponseHandler extends MPDResponseAlbumList {
        private WeakReference<LibraryAdapter> mAdapter;

        private AlbumResponseHandler(LibraryAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void handleAlbums(List<MPDAlbum> albumList, int position) {
            LibraryAdapter adapter = mAdapter.get();

            if (adapter != null) {
                adapter.insertChildren(albumList, position);
            }
        }
    }


    public static class TrackResponseHandler extends MPDResponseFileList {
        private WeakReference<LibraryAdapter> mAdapter;

        private TrackResponseHandler(LibraryAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
        }

        @Override
        public void handleTracks(List<MPDFileEntry> fileList, int start, int end, int position) {
            LibraryAdapter adapter = mAdapter.get();
            List<MPDTrack> tracklist = new ArrayList<>();

            for(MPDFileEntry file: fileList) {
                if (file instanceof MPDTrack) {
                    tracklist.add((MPDTrack)file);
                }
            }

            if (adapter != null && tracklist.size() > 0) {
                adapter.insertChildren(tracklist, position);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout mItemContainer;
        //public ImageView        mImage;
        public TextView         mPrefixText;
        public TextView         mMainText;
        public TextView         mPostfixText;

        public View             mDivider;
        public ImageButton      mPlayReplace;
        public ImageButton      mPlayInsertAfterCursor;
        public ImageButton      mPlayAppend;

        public ViewHolder(ConstraintLayout itemContainer, int viewType) {
            super(itemContainer);

            mItemContainer = itemContainer;
            //mImage = (ImageView) rowContainer.findViewById(R.id.row_image);
            mPrefixText = (TextView) mItemContainer.findViewById(R.id.recycler_item_library_prefix_text);
            mMainText   = (TextView) mItemContainer.findViewById(R.id.recycler_item_library_main_text);
            mPostfixText = (TextView) mItemContainer.findViewById(R.id.recycler_item_library_postfix_text);

            mDivider               = (View)        mItemContainer.findViewById(R.id.recycler_item_library_divider);
            mPlayReplace           = (ImageButton) mItemContainer.findViewById(R.id.recycler_item_library_play_replace);
            mPlayInsertAfterCursor = (ImageButton) mItemContainer.findViewById(R.id.recycler_item_library_play_insert_after_cursor);
            mPlayAppend            = (ImageButton) mItemContainer.findViewById(R.id.recycler_item_library_play_append);

            if (viewType == MPDArtist.VIEW_TYPE) {
                mPostfixText.setVisibility(View.GONE);
            }

            if (viewType != MPDTrack.VIEW_TYPE) {
                mPrefixText.setVisibility(View.GONE);
            }

            mDivider.setVisibility(View.GONE);
            mPlayReplace.setVisibility(View.GONE);
            mPlayInsertAfterCursor.setVisibility(View.GONE);
            mPlayAppend.setVisibility(View.GONE);
        }

    }

    public LibraryAdapter(Context context, RecyclerView recyclerView, boolean useAlbumArtists, boolean useArtistSort) {
        super();

        mContext             = context;
        mRecyclerView        = recyclerView;
        mLevelIndicatorWidth = 48;
        mUseAlbumArtists     = useAlbumArtists;
        mUseArtistSort       = useArtistSort;

        mListItemHeight = (int)context.getResources().getDimension(R.dimen.material_list_item_height);

        pArtistResponseHandler = new LibraryAdapter.ArtistResponseHandler(this);
        pAlbumResponseHandler  = new LibraryAdapter.AlbumResponseHandler(this);
        pTrackResponseHandler  = new LibraryAdapter.TrackResponseHandler(this);

        loadArtists();

        mArtworkManager = ArtworkManager.getInstance(mContext.getApplicationContext());
    }

    public void loadArtists() {
        if( !mUseAlbumArtists) {
            if(!mUseArtistSort) {
                MPDQueryHandler.getArtists(pArtistResponseHandler);
            } else {
                MPDQueryHandler.getArtistSort(pArtistResponseHandler);
            }
        } else {
            if(!mUseArtistSort) {
                MPDQueryHandler.getAlbumArtists(pArtistResponseHandler);
            } else {
                MPDQueryHandler.getAlbumArtistSort(pArtistResponseHandler);
            }
        }
    }

    @Override
    public LibraryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item_library, parent, false);

        LibraryAdapter.ViewHolder vh = new LibraryAdapter.ViewHolder(v, viewType);

        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                vh.itemView.getLayoutParams();
        params.leftMargin = mLevelIndicatorWidth * viewType;
        vh.itemView.setLayoutParams(params);

        return vh;
    }

    @Override
    public void onBindViewHolder(final LibraryAdapter.ViewHolder holder, final int position) {

        final LibraryItem item = mList.get(position);

        TextView label = (TextView) holder.mItemContainer.findViewById(R.id.recycler_item_library_main_text);
        label.setText(item.getMainText());

        if (item.getViewType() != MPDArtist.VIEW_TYPE) {
            holder.mPostfixText.setVisibility(View.VISIBLE);
            holder.mPostfixText.setText(item.getPostfixText());
        } else {
            holder.mPostfixText.setVisibility(View.GONE);
        }

        if (item.getViewType() == MPDTrack.VIEW_TYPE) {
            holder.mPrefixText.setVisibility(View.VISIBLE);
            holder.mPrefixText.setText(item.getPrefixText());
        }  else {
            holder.mPrefixText.setVisibility(View.GONE);
        }

        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                holder.itemView.getLayoutParams();
        params.leftMargin = mLevelIndicatorWidth * item.getLevel();
        holder.itemView.setLayoutParams(params);

        if (item.getViewType() != MPDArtist.VIEW_TYPE && item.isExpanded()) {
            showAdd2PlaylistButtons(holder);
        } else {
            hideAdd2PlaylistButtons(holder);
        }

        holder.mItemContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (item.isExpanded()) {
                            collapseChildren(item, position);
                        } else {
                            expandChildren(item, position, holder);
                        }
                    }
                });
            }
        });

    }

    private void hideAdd2PlaylistButtons(LibraryAdapter.ViewHolder holder) {
        holder.mDivider.setVisibility(View.GONE);
        holder.mPlayReplace.setVisibility(View.GONE);
        holder.mPlayInsertAfterCursor.setVisibility(View.GONE);
        holder.mPlayAppend.setVisibility(View.GONE);
    }

    private void showAdd2PlaylistButtons(LibraryAdapter.ViewHolder holder) {
        holder.mDivider.setVisibility(View.VISIBLE);
        holder.mPlayReplace.setVisibility(View.VISIBLE);
        holder.mPlayInsertAfterCursor.setVisibility(View.VISIBLE);
        holder.mPlayAppend.setVisibility(View.VISIBLE);
    }

    private void expandChildren(LibraryItem item, int position, LibraryAdapter.ViewHolder holder) {

        if(mRecyclerView == null) {
            return; // adapter detached
        }

        ExpandedItem exItem;

        // before expanding an element, ensure there is no other element expanded at the same
        // level; the index of elements in mExpanded also represents their level
        if (mExpanded.size() > item.getLevel()) {
            exItem = mExpanded.get(item.getLevel());
            collapseChildren(exItem.mItem, exItem.mPosition);
        }

        mExpanded.add(item.getLevel(), new ExpandedItem(item, holder, position, 0));

        switch (item.getViewType()) {
            case MPDArtist.VIEW_TYPE:
                item.getKidItems(pAlbumResponseHandler, position);
                break;
            case MPDAlbum.VIEW_TYPE:
                item.getKidItems(pTrackResponseHandler, position);
                showAdd2PlaylistButtons(holder);
                notifyItemChanged(position);
                break;
            case MPDTrack.VIEW_TYPE:
                item.setExpanded(true);
                showAdd2PlaylistButtons(holder);
                notifyItemChanged(position);
                break;
        }
    }

    private void collapseChildren(LibraryItem item, int position) {

        if (!item.isExpanded()) {
            return;
        }

        int remove_count = 0;
        ExpandedItem exItem;

        while (mExpanded.size() > item.getLevel()) {
            exItem = mExpanded.remove(mExpanded.size() - 1);
            if (exItem.mNrOfChildren > 0) {
                remove_count += exItem.mNrOfChildren;
                mList.subList(exItem.mPosition + 1, exItem.mPosition + 1 + exItem.mNrOfChildren).clear();
            }
            exItem.mItem.setExpanded(false);

            if (exItem.mItem.getViewType() != MPDArtist.VIEW_TYPE) {
                hideAdd2PlaylistButtons(exItem.mHolder);
                notifyItemChanged(exItem.mPosition);
            }

        }
        if (remove_count > 0) {
            notifyItemRangeRemoved(position + 1, remove_count);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public void newArtistImage(MPDArtist artist) {
        notifyDataSetChanged();
    }

    public void updateArtists(List<MPDArtist> artistList) {
        if (mExpanded.size() > 0) {
            mExpanded.clear();
        }
        mList.clear();
        mList.addAll(artistList);
    }

    public void insertChildren(List<? extends LibraryItem> childList, int position) {
        LibraryItem item = mList.get(position);
        int nrOfChildren = childList.size();

        mExpanded.get(item.getLevel()).mNrOfChildren = nrOfChildren;

        if (nrOfChildren > 0) {
            mList.addAll(position + 1, childList);

            notifyItemRangeInserted(position + 1, nrOfChildren);
        }

        item.setExpanded(true);

/*        if (item.getViewType() != MPDArtist.VIEW_TYPE) {
            showAdd2PlaylistButtons(position);
            notifyItemChanged(position);
        }*/
    }
}
