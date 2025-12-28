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

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private EditText searchInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        searchInput = view.findViewById(R.id.search_input);
        recyclerView = view.findViewById(R.id.search_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // Initial load (empty or popular)
        updateList("");

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void updateList(String query) {
        List<Movie> results;
        if (query.isEmpty()) {
            results = DataRepository.getInstance().getAllMovies();
        } else {
            results = DataRepository.getInstance().search(query);
        }
        
        adapter = new MovieAdapter(getContext(), results);
        recyclerView.setAdapter(adapter);
    }
}
