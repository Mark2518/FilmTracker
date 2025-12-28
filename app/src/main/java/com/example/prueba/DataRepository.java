package com.example.prueba;

import java.util.ArrayList;
import java.util.List;

public class DataRepository {
    private static DataRepository instance;
    private User currentUser;
    private List<Movie> cachedMovies;
    private TursoClient tursoClient;

    private DataRepository() {
        currentUser = new User("Guest User");
        cachedMovies = new ArrayList<>();
        tursoClient = new TursoClient();
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public void init(android.content.Context context) {
        // Context not strictly needed for Turso HTTP client unless using ConnectivityManager
    }
    
    public interface DataCallback {
        void onDataLoaded();
        void onError(String error);
    }

    public void refreshMovies(DataCallback callback) {
        tursoClient.fetchMovies(new TursoClient.MovieCallback() {
            @Override
            public void onSuccess(List<Movie> movies) {
                cachedMovies.clear();
                cachedMovies.addAll(movies);
                if (callback != null) callback.onDataLoaded();
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(cachedMovies);
    }

    public List<Movie> getMovies() {
        List<Movie> movies = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (!m.isSeries()) movies.add(m);
        }
        return movies;
    }

    public List<Movie> getSeries() {
        List<Movie> series = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (m.isSeries()) series.add(m);
        }
        return series;
    }

    public List<Movie> getActionMovies() {
        List<Movie> action = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (m.getDescription().toLowerCase().contains("action") || 
                m.getDescription().toLowerCase().contains("fight")) {
                action.add(m);
            }
        }
        return action;
    }

    public List<Movie> getSciFiMovies() {
        List<Movie> scifi = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (m.getDescription().toLowerCase().contains("space") || 
                m.getDescription().toLowerCase().contains("future")) {
                scifi.add(m);
            }
        }
        return scifi;
    }

    public List<Movie> getCrimeMovies() {
        List<Movie> crime = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (m.getDescription().toLowerCase().contains("crime") || 
                m.getDescription().toLowerCase().contains("mob")) {
                crime.add(m);
            }
        }
        return crime;
    }

    public List<Movie> search(String query) {
        List<Movie> results = new ArrayList<>();
        for (Movie m : cachedMovies) {
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) {
                results.add(m);
            }
        }
        return results;
    }
}
