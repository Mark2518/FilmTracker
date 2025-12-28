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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.home_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        loadDummyData();
        
        adapter = new CategoryAdapter(getContext(), categoryList);
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    private void loadDummyData() {
        categoryList = new ArrayList<>();
        DataRepository repo = DataRepository.getInstance();
        
        categoryList.add(new Category("Movies", repo.getMovies()));
        categoryList.add(new Category("Series", repo.getSeries()));
        categoryList.add(new Category("Action & Adventure", repo.getActionMovies()));
        categoryList.add(new Category("Sci-Fi & Future", repo.getSciFiMovies()));
        categoryList.add(new Category("Crime & Drama", repo.getCrimeMovies()));
        categoryList.add(new Category("All Content", repo.getAllMovies()));
    }
}
