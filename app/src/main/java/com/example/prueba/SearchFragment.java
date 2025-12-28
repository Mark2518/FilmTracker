package com.example.prueba;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Set;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private EditText searchInput;

    @Nullable
    private android.widget.Button btnLoadMore;
    private android.widget.ProgressBar loadingIndicator;
    private int currentOffset = 0;
    private String currentQuery = "";
    private java.util.Set<Movie> currentResults = new java.util.LinkedHashSet<>();
    private boolean isSearching = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        searchInput = view.findViewById(R.id.search_input);
        recyclerView = view.findViewById(R.id.search_recycler_view);
        btnLoadMore = view.findViewById(R.id.btn_load_more_search);
        loadingIndicator = view.findViewById(R.id.search_loading_indicator);
        
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // Initial load (empty or popular)
        updateList("");

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                
                updateList(v.getText().toString());
                return true;
            }
            return false;
        });

        btnLoadMore.setOnClickListener(v -> loadMore());

        return view;
    }

    private void updateList(String query) {
        currentQuery = query;
        currentOffset = 0;
        currentResults.clear();
        btnLoadMore.setVisibility(View.GONE);

        if (query.isEmpty()) {
            Set<Movie> results = DataRepository.getInstance().getAllMovies();
            currentResults.addAll(results);
            adapter = new MovieAdapter(getContext(), currentResults);
            recyclerView.setAdapter(adapter);
        } else {
            performSearch();
        }
    }

    private void loadMore() {
        currentOffset += 20;
        performSearch();
    }

    private void performSearch() {
        if (isSearching) return;
        isSearching = true;
        
        loadingIndicator.setVisibility(View.VISIBLE);
        if (currentOffset == 0) {
            // Hide list if new search to show clean slate, or keep it? 
            // Better to clear if new search.
            // But if we clear here, we might flash empty screen.
            // Let's rely on clearing in onResults or just replacing.
        } else {
            btnLoadMore.setVisibility(View.GONE); // Hide load more while loading next
        }

        DataRepository.getInstance().searchMovies(currentQuery, 20, currentOffset, new DataRepository.SearchCallback() {
            @Override
            public void onResults(Set<Movie> movies) {
                isSearching = false;
                loadingIndicator.setVisibility(View.GONE);
                
                if (currentOffset == 0) {
                    currentResults.clear();
                    currentResults.addAll(movies);
                    adapter = new MovieAdapter(getContext(), currentResults);
                    recyclerView.setAdapter(adapter);
                } else {
                    int startPos = currentResults.size();
                    currentResults.addAll(movies);
                    adapter.notifyItemRangeInserted(startPos, movies.size());
                }

                if (movies.size() == 20) {
                    btnLoadMore.setVisibility(View.VISIBLE);
                } else {
                    btnLoadMore.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                isSearching = false;
                loadingIndicator.setVisibility(View.GONE);
                android.widget.Toast.makeText(getContext(), "Search failed: " + error, android.widget.Toast.LENGTH_SHORT).show();
                if (currentResults.size() > 0 && currentResults.size() % 20 == 0) {
                     btnLoadMore.setVisibility(View.VISIBLE); // Show retry option if appropriate
                }
            }
        });
    }
}
