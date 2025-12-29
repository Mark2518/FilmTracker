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

        // Si ya hay datos cargados en memoria, los usamos directamente y no recargamos
        if (!DataRepository.getInstance().getAllMovies().isEmpty()) {
            updateUI();
        } else {
            loadData();
        }

        return view;
    }

    // --- CAMBIO IMPORTANTE: Eliminada la lógica de recarga en onResume ---
    @Override
    public void onResume() {
        super.onResume();
        // Hemos quitado la llamada a loadRecommendations aquí.
        // Esto evita que las películas se "barajen" aleatoriamente al volver de ver un detalle.
    }
    // ---------------------------------------------------------------------

    private void loadData() {
        showLoadingState();
        DataRepository.getInstance().refreshMovies(new DataRepository.DataCallback() {
            @Override
            public void onDataLoaded() {
                DataRepository.getInstance().loadRecommendations(new DataRepository.DataCallback() {
                    @Override
                    public void onDataLoaded() {
                        if (getActivity() != null) {
                            updateUI();
                        }
                    }
                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) updateUI();
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

        // Continue Watching
        java.util.Set<Movie> continueWatching = repo.getContinueWatchingMovies();
        if (!continueWatching.isEmpty())
            categoryList.add(new Category("Continue Watching", continueWatching));

        // Recommendations
        java.util.Set<Movie> recommendations = repo.getRecommendedMovies();
        if (!recommendations.isEmpty()) {
            categoryList.add(new Category("Recomendaciones", recommendations, () -> {
                android.widget.Toast.makeText(getContext(), "Refresing recommendations...", android.widget.Toast.LENGTH_SHORT).show();
                repo.refreshRecommendations(new DataRepository.DataCallback() {
                    @Override
                    public void onDataLoaded() {
                        if (getActivity() != null) updateCategoryItem("Recomendaciones", repo.getRecommendedMovies());
                    }
                    @Override
                    public void onError(String error) {}
                });
            }));
        }

        // Dynamic Genre Sections
        List<String> significantGenres = repo.getSignificantGenres();
        for (String genre : significantGenres) {
            java.util.Set<Movie> genreMovies = repo.getMoviesForGenre(genre);
            if (!genreMovies.isEmpty()) {
                String title = genre.substring(0, 1).toUpperCase() + genre.substring(1);
                categoryList.add(new Category(title, genreMovies, () -> {
                    if (getActivity() != null) {
                        java.util.Set<Movie> newMovies = repo.getMoviesForGenre(genre);
                        for (int i = 0; i < categoryList.size(); i++) {
                            if (categoryList.get(i).getTitle().equalsIgnoreCase(title)) {
                                categoryList.set(i, new Category(title, newMovies, categoryList.get(i).getOnRefresh()));
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                }));
            }
        }

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