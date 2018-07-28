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

public class WebradioAdapter extends RecyclerView.Adapter<WebradioAdapter.ViewHolder> {

    private final Context                   mContext;
    private RecyclerView                    mRecyclerView;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout mItemContainer;
        //public ImageView        mImage;
        public TextView         mMainText;

        public View             mDivider;
        public ImageButton      mPlayReplace;
        public ImageButton      mPlayInsertAfterCursor;
        public ImageButton      mPlayAppend;

        public ViewHolder(ConstraintLayout itemContainer, int viewType) {
            super(itemContainer);

            mItemContainer = itemContainer;
            //mImage = (ImageView) rowContainer.findViewById(R.id.row_image);
            mMainText   = (TextView) mItemContainer.findViewById(R.id.recycler_item_webradio_main_text);

            mDivider               = (View)        mItemContainer.findViewById(R.id.recycler_item_webradio_divider);
            mPlayReplace           = (ImageButton) mItemContainer.findViewById(R.id.recycler_item_webradio_play_replace);
            mPlayInsertAfterCursor = (ImageButton) mItemContainer.findViewById(R.id.recycler_item_webradio_play_insert_after_cursor);
            mPlayAppend            = (ImageButton) mItemContainer.findViewById(R.id.recycler_item_webradio_play_append);

            mDivider.setVisibility(View.GONE);
            mPlayReplace.setVisibility(View.GONE);
            mPlayInsertAfterCursor.setVisibility(View.GONE);
            mPlayAppend.setVisibility(View.GONE);
        }
    }

    public WebradioAdapter(Context context, RecyclerView recyclerView) {
        super();

        mContext             = context;
        mRecyclerView        = recyclerView;

        //loadSubscribedWebradios();

        //mArtworkManager = ArtworkManager.getInstance(mContext.getApplicationContext());
    }

    @Override
    public WebradioAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item_webradio, parent, false);

        WebradioAdapter.ViewHolder vh = new WebradioAdapter.ViewHolder(v, viewType);


        return vh;
    }

    @Override
    public void onBindViewHolder(final WebradioAdapter.ViewHolder holder, final int position) {
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
