package com.example.prueba;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ProfileFragment extends Fragment {

    private RecyclerView watchlistRecyclerView;
    private RecyclerView seenRecyclerView;
    private RecyclerView continueWatchingRecyclerView;
    private android.widget.TextView labelContinueWatching;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        watchlistRecyclerView = view.findViewById(R.id.watchlist_recycler_view);
        seenRecyclerView = view.findViewById(R.id.seen_recycler_view);
        continueWatchingRecyclerView = view.findViewById(R.id.continue_watching_recycler_view);
        labelContinueWatching = view.findViewById(R.id.label_continue_watching);

        watchlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        seenRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        continueWatchingRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        DataRepository repo = DataRepository.getInstance();
        User user = repo.getCurrentUser();
        
        // Continue Watching
        java.util.Set<Movie> continueWatchingList = repo.getContinueWatchingMovies();
        if (!continueWatchingList.isEmpty()) {
            labelContinueWatching.setVisibility(View.VISIBLE);
            continueWatchingRecyclerView.setVisibility(View.VISIBLE);
            HorizontalMovieAdapter continueAdapter = new HorizontalMovieAdapter(getContext(), continueWatchingList);
            continueWatchingRecyclerView.setAdapter(continueAdapter);
        } else {
            labelContinueWatching.setVisibility(View.GONE);
            continueWatchingRecyclerView.setVisibility(View.GONE);
        }
        
        HorizontalMovieAdapter watchlistAdapter = new HorizontalMovieAdapter(getContext(), user.getWatchlist());
        watchlistRecyclerView.setAdapter(watchlistAdapter);

        HorizontalMovieAdapter seenAdapter = new HorizontalMovieAdapter(getContext(), user.getSeenList());
        seenRecyclerView.setAdapter(seenAdapter);
    }
}
