package org.gateshipone.malp.application.fragments.serverfragments;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.WebradioAdapter;

public class WebradioFragment extends Fragment {

    private WebradioAdapter mWebradioAdapter;

    private RecyclerView mRecyclerView;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        FrameLayout rootView = (FrameLayout) inflater.inflate(R.layout.audio_source_fragment, container, false);
        mRecyclerView = rootView.findViewById(R.id.audio_source_recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);

        mWebradioAdapter = new WebradioAdapter(getContext(), mRecyclerView);
        mRecyclerView.setAdapter(mWebradioAdapter);

        return rootView;
    }

}