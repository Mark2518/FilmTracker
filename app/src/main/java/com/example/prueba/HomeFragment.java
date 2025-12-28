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
                    // User requested NO updates on scroll. Disabling infinite scroll.
                    /*
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount 
                        && firstVisibleItemPosition >= 0) {
                        loadMoreData();
                    }
                    */
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
        // If repository is empty (still loading initial data), do not attempt to refresh recommendations
        // which would trigger an empty updateUI and clear the loading skeletons.
        if (DataRepository.getInstance().getAllMovies().isEmpty()) {
            return;
        }

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
        showLoadingState();
        // android.widget.Toast.makeText(getContext(), "Loading movies...", android.widget.Toast.LENGTH_SHORT).show();
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

        // Continue Watching (No refresh needed usually, but could add if desired)
        //Hola
        // Continue Watching (No refresh needed usually, but could add if desired)
        //Hola
        java.util.Set<Movie> continueWatching = repo.getContinueWatchingMovies();
        if (!continueWatching.isEmpty())
            categoryList.add(new Category("Continue Watching", continueWatching));

        // Recommendations
        java.util.Set<Movie> recommendations = repo.getRecommendedMovies();
        // Always show recommendations section even if empty initially to allow refresh? 
        // Or wait for data? The requirement says "initially 20 random". 
        // repo.loadRecommendations handles the initial fetch if empty.
        if (!recommendations.isEmpty()) {
            categoryList.add(new Category("Recomendaciones", recommendations, () -> {
             android.widget.Toast.makeText(getContext(), "Refresing recommendations...", android.widget.Toast.LENGTH_SHORT).show();
                repo.refreshRecommendations(new DataRepository.DataCallback() {
                    @Override
                    public void onDataLoaded() {
                        if (getActivity() != null) updateCategoryItem("Recomendaciones", repo.getRecommendedMovies());
                    }
 
                    @Override
                    public void onError(String error) {
                        if (getActivity() != null)
                            android.widget.Toast.makeText(getContext(), "Error: " + error, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }));
        }

        // Dynamic Genre Sections
        List<String> significantGenres = repo.getSignificantGenres();
        for (String genre : significantGenres) {
            java.util.Set<Movie> genreMovies = repo.getMoviesForGenre(genre);
            if (!genreMovies.isEmpty()) {
                // Capitalize first letter
                String title = genre.substring(0, 1).toUpperCase() + genre.substring(1);

                categoryList.add(new Category(title, genreMovies, () -> {
                    // Refresh this specific genre section
                    // Since getMoviesForGenre shuffles, we just need to rebuild the category list and notify adapter
                    // But we can't just call updateUI() because that would refresh EVERYTHING / re-shuffle everything?
                    // No, getMoviesForGenre shuffles every time it is called.
                    // If we call updateUI(), all genres will re-shuffle.
                    // To avoid that, we might strictly need to update just this index, but for simplicity/mvp,
                    // refreshing one section causing a general UI update is acceptable,
                    // OR we can make the "refresh" button just re-fetch this one list and notify adapter.
                    // For now, let's just trigger a full updateUI for simplicity as it's fast on local cache.
                    // Actually, if we want to keep others stable, we should implement a specific update.

                    // Re-fetching just this genre
                    if (getActivity() != null) {
                        java.util.Set<Movie> newMovies = repo.getMoviesForGenre(genre);
                        // Find the category in the list and update it
                        for (int i = 0; i < categoryList.size(); i++) {
                            if (categoryList.get(i).getTitle().equalsIgnoreCase(title)) {
                                // We need to update the movies in the category object.
                                // Category object is immutable regarding movie list reference usually, check Category.java
                                // Category.java has final list? No, just generated in constructor.
                                // Let's replace the category object in the list.
                                categoryList.set(i, new Category(title, newMovies, categoryList.get(i).getOnRefresh()));
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                }));
            }
        }

        // Fallback or "All Movies" if nothing else? 
        // Requirement says "sections of significant genres".

        if (categoryList.isEmpty()) {
            java.util.Set<Movie> all = repo.getAllMovies();
            if (!all.isEmpty()) {
                List<Movie> allList = new ArrayList<>(all);
                List<Movie> subList = allList.subList(0, Math.min(allList.size(), 20));
                categoryList.add(new Category("All Content", new java.util.LinkedHashSet<>(subList)));
            }
        }

        adapter.notifyDataSetChanged();
    }
        private void showLoadingState() {
        categoryList.clear();
        String[] loadingSections = {"Trending", "Action", "Drama", "Comedy", "Sci-Fi"};
        for (String section : loadingSections) {
             java.util.Set<Movie> dummyMovies = new java.util.LinkedHashSet<>();
             for (int i = 0; i < 5; i++) {
                 dummyMovies.add(Movie.createLoadingMovie());
             }
             categoryList.add(new Category(section, dummyMovies));
        }
        adapter.notifyDataSetChanged();
    }

    private void updateCategoryItem(String title, java.util.Set<Movie> newMovies) {
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).getTitle().equalsIgnoreCase(title)) {
                Category old = categoryList.get(i);
                categoryList.set(i, new Category(title, newMovies, old.getOnRefresh()));
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }
}
