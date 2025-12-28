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

    private int currentOffset = 0;
    private String currentQuery = "";
    private Set<Movie> currentResults = new LinkedHashSet<>();
    
    // Handler para controlar el tiempo de escritura (Debounce)
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

        // 1. Inicializamos el adaptador UNA sola vez
        adapter = new MovieAdapter(getContext(), currentResults);
        recyclerView.setAdapter(adapter);

        // Cargar estado inicial (vacío o todas las pelis)
        updateList("");

        // 2. Listener para búsqueda en tiempo real
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Si el usuario sigue escribiendo, cancelamos la búsqueda anterior
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Esperamos 600ms después de que deje de escribir para buscar
                searchRunnable = () -> updateList(s.toString());
                searchHandler.postDelayed(searchRunnable, 600);
            }
        });

        btnLoadMore.setOnClickListener(v -> loadMore());

        return view;
    }

    private void updateList(String query) {
        currentQuery = query.trim();
        currentOffset = 0;
        
        if (currentQuery.isEmpty()) {
            btnLoadMore.setVisibility(View.GONE);
            loadingIndicator.setVisibility(View.GONE);
            // Mostrar todas las películas cacheadas si no hay búsqueda
            currentResults.clear();
            currentResults.addAll(DataRepository.getInstance().getAllMovies());
            adapter.updateMovies(currentResults);
        } else {
            performSearch();
        }
    }

    private void loadMore() {
        currentOffset += 20;
        performSearch();
    }

    private void performSearch() {
        loadingIndicator.setVisibility(View.VISIBLE);
        
        // PASO 1: Búsqueda Local Instantánea (para que se sienta rápido)
        if (currentOffset == 0) {
            Set<Movie> localResults = DataRepository.getInstance().search(currentQuery);
            currentResults.clear();
            currentResults.addAll(localResults);
            adapter.updateMovies(currentResults);
        }

        // PASO 2: Búsqueda en la Base de Datos (Turso)
        DataRepository.getInstance().searchMovies(currentQuery, 20, currentOffset, new DataRepository.SearchCallback() {
            @Override
            public void onResults(Set<Movie> movies) {
                if (!isAdded()) return; // Evitar crash si el usuario cambió de pantalla

                loadingIndicator.setVisibility(View.GONE);
                
                if (currentOffset == 0) {
                    // Si es una búsqueda nueva, la API manda sobre lo local
                    if (!movies.isEmpty()) {
                        // Opcional: Si quieres mezclar, comenta el clear(). 
                        // Pero para búsquedas, mejor mostrar exactamente lo que viene.
                        currentResults.clear(); 
                        currentResults.addAll(movies);
                    } else if (currentResults.isEmpty()) {
                         // Si API devuelve 0 y local tenía 0 -> No hay nada
                         Toast.makeText(getContext(), "No se encontraron resultados", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Si es "cargar más", añadimos al final
                    currentResults.addAll(movies);
                }

                // Actualizamos el adaptador existente
                adapter.updateMovies(currentResults);

                // Gestionar botón de ver más
                if (movies.size() >= 20) {
                    btnLoadMore.setVisibility(View.VISIBLE);
                } else {
                    btnLoadMore.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                loadingIndicator.setVisibility(View.GONE);
                // Si falla la red, al menos ya mostramos los resultados locales en el PASO 1
                if (currentResults.isEmpty()) {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}