package com.example.prueba;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.LinkedHashSet;
import java.util.Set;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private EditText searchInput;
    private Button btnLoadMore;
    private ProgressBar loadingIndicator;

    // Variables de control
    private int currentOffset = 0;
    private String currentQuery = "";
    private static final int PAGE_SIZE = 15; // Límite de 15 peliculas

    private Set<Movie> currentResults = new LinkedHashSet<>();
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        searchInput = view.findViewById(R.id.search_input);
        recyclerView = view.findViewById(R.id.search_recycler_view);
        btnLoadMore = view.findViewById(R.id.btn_load_more_search);
        loadingIndicator = view.findViewById(R.id.search_loading_indicator);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new MovieAdapter(getContext(), currentResults);
        recyclerView.setAdapter(adapter);

        updateList("");

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Delay de 600ms para no saturar la búsqueda mientras escribe
                searchRunnable = () -> updateList(s.toString());
                searchHandler.postDelayed(searchRunnable, 600);
            }
        });

        // Listener del botón "Cargar más"
        btnLoadMore.setOnClickListener(v -> loadMore());

        return view;
    }

    private void updateList(String query) {
        currentQuery = query.trim();
        currentOffset = 0;

        if (currentQuery.isEmpty()) {
            btnLoadMore.setVisibility(View.GONE);
            loadingIndicator.setVisibility(View.GONE);

            currentResults.clear();
            currentResults.addAll(DataRepository.getInstance().getAllMovies());
            adapter.updateMovies(currentResults);
        } else {
            performSearch(false);
        }
    }

    private void loadMore() {
        // Avanzamos el offset en bloques de 15
        currentOffset += PAGE_SIZE;
        performSearch(true);
    }

    private void performSearch(boolean isLoadMore) {
        if (!isLoadMore) {
            loadingIndicator.setVisibility(View.VISIBLE);
            btnLoadMore.setVisibility(View.GONE);
        } else {
            btnLoadMore.setText("Cargando...");
            btnLoadMore.setEnabled(false);
        }

        DataRepository.getInstance().searchMovies(currentQuery, PAGE_SIZE, currentOffset, new DataRepository.SearchCallback() {
            @Override
            public void onResults(Set<Movie> movies) {
                if (!isAdded()) return;

                loadingIndicator.setVisibility(View.GONE);

                if (!isLoadMore) {
                    if (!movies.isEmpty()) {
                        currentResults.clear();
                        currentResults.addAll(movies);
                    } else {
                        currentResults.clear();
                        Toast.makeText(getContext(), "No se encontraron resultados", Toast.LENGTH_SHORT).show();
                    }
                    adapter.updateMovies(currentResults);
                } else {
                    if (!movies.isEmpty()) {
                        adapter.addMovies(movies);
                        Toast.makeText(getContext(), "Nuevas películas cargadas", Toast.LENGTH_SHORT).show();
                    }
                    btnLoadMore.setText("Load More Results");
                    btnLoadMore.setEnabled(true);
                }

                if (movies.size() >= PAGE_SIZE) {
                    btnLoadMore.setVisibility(View.VISIBLE);
                } else {
                    btnLoadMore.setVisibility(View.GONE); // No hay más páginas
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                loadingIndicator.setVisibility(View.GONE);
                btnLoadMore.setText("Load More Results");
                btnLoadMore.setEnabled(true);

                if (currentResults.isEmpty()) {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}