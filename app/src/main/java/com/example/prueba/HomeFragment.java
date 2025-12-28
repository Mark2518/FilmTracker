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
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<Category> categoryList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.home_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        categoryList = new ArrayList<>();
        adapter = new CategoryAdapter(getContext(), categoryList);
        recyclerView.setAdapter(adapter);

        // Infinite Scroll Listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount 
                        && firstVisibleItemPosition >= 0) {
                        loadMoreData();
                    }
                }
            }
        });

        loadData();
        
        return view;
    }
    
    private boolean isLoadingMore = false;
    
    private void loadMoreData() {
        if (isLoadingMore) return;
        isLoadingMore = true;
        
        DataRepository.getInstance().loadMoreMovies(new DataRepository.DataCallback() {
            @Override
            public void onDataLoaded() {
                isLoadingMore = false;
                if (getActivity() != null) {
                    updateUI();
                }
            }

            @Override
            public void onError(String error) {
                isLoadingMore = false;
                // Optional: show error or just ignore for seamless experience
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh recommendations when returning to home in case user added movies
        DataRepository.getInstance().loadRecommendations(new DataRepository.DataCallback() {
            @Override
            public void onDataLoaded() {
                if (getActivity() != null) updateUI();
            }

            @Override
            public void onError(String error) {
                // Ignore
            }
        });
    }

    private void loadData() {
        android.widget.Toast.makeText(getContext(), "Loading movies...", android.widget.Toast.LENGTH_SHORT).show();
        DataRepository.getInstance().refreshMovies(new DataRepository.DataCallback() {
            @Override
            public void onDataLoaded() {
                // Initial load of recommendations
                DataRepository.getInstance().loadRecommendations(new DataRepository.DataCallback() {
                    @Override
                    public void onDataLoaded() {
                        if (getActivity() != null) {
                            updateUI();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            updateUI(); // Update anyway
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    android.widget.Toast.makeText(getContext(), "Error: " + error, android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateUI() {
        categoryList.clear();
        DataRepository repo = DataRepository.getInstance();
        
        // Only add categories if they have items
        List<Movie> continueWatching = repo.getContinueWatchingMovies();
        if (!continueWatching.isEmpty()) categoryList.add(new Category("Continue Watching", continueWatching));

        List<Movie> recommendations = repo.getRecommendedMovies();
        if (!recommendations.isEmpty()) categoryList.add(new Category("Recomendaciones", recommendations));

        List<Movie> movies = repo.getMovies();
        if (!movies.isEmpty()) categoryList.add(new Category("Movies", movies));
        
        List<Movie> series = repo.getSeries();
        if (!series.isEmpty()) categoryList.add(new Category("Series", series));
        
        List<Movie> action = repo.getActionMovies();
        if (!action.isEmpty()) categoryList.add(new Category("Action & Adventure", action));
        
        List<Movie> scifi = repo.getSciFiMovies();
        if (!scifi.isEmpty()) categoryList.add(new Category("Sci-Fi & Future", scifi));
        
        List<Movie> crime = repo.getCrimeMovies();
        if (!crime.isEmpty()) categoryList.add(new Category("Crime & Drama", crime));
        
        List<Movie> all = repo.getAllMovies();
        if (!all.isEmpty()) categoryList.add(new Category("All Content", all));

        adapter.notifyDataSetChanged();
    }
}
