package com.example.prueba;

import java.util.ArrayList;
import java.util.List;

public class DataRepository {
    private static DataRepository instance;
    private User currentUser;
    private MovieDatabaseHelper dbHelper;

    private DataRepository() {
        currentUser = new User("Guest User");
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public void init(android.content.Context context) {
         if (dbHelper == null) {
             dbHelper = new MovieDatabaseHelper(context);
         }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<Movie> getAllMovies() {
        if (dbHelper != null) {
            return dbHelper.getAllMovies();
        }
        return new ArrayList<>();
    }

    public List<Movie> getMovies() {
        List<Movie> movies = new ArrayList<>();
        for (Movie m : getAllMovies()) {
            if (!m.isSeries()) movies.add(m);
        }
        return movies;
    }

    public List<Movie> getSeries() {
        List<Movie> series = new ArrayList<>();
        for (Movie m : getAllMovies()) {
            if (m.isSeries()) series.add(m);
        }
        return series;
    }

    public List<Movie> getActionMovies() {
        // Simple filter simulation
        List<Movie> action = new ArrayList<>();
        for (Movie m : getAllMovies()) {
            if (m.getDescription().toLowerCase().contains("action") || 
                m.getDescription().toLowerCase().contains("fight") ||
                m.getDescription().toLowerCase().contains("war") ||
                m.getTitle().equals("The Dark Knight") ||
                m.getTitle().equals("Inception")) {
                action.add(m);
            }
        }
        return action;
    }

    public List<Movie> getSciFiMovies() {
        List<Movie> scifi = new ArrayList<>();
        for (Movie m : getAllMovies()) {
            if (m.getDescription().toLowerCase().contains("space") || 
                m.getDescription().toLowerCase().contains("future") ||
                m.getDescription().toLowerCase().contains("technology") ||
                m.getTitle().equals("The Matrix")) {
                scifi.add(m);
            }
        }
        return scifi;
    }

    public List<Movie> getCrimeMovies() {
        List<Movie> crime = new ArrayList<>();
        for (Movie m : getAllMovies()) {
            if (m.getDescription().toLowerCase().contains("crime") || 
                m.getDescription().toLowerCase().contains("mob") ||
                m.getDescription().toLowerCase().contains("gangster") ||
                m.getTitle().equals("Pulp Fiction")) {
                crime.add(m);
            }
        }
        return crime;
    }

    public List<Movie> search(String query) {
        List<Movie> results = new ArrayList<>();
        for (Movie m : getAllMovies()) {
            if (m.getTitle().toLowerCase().contains(query.toLowerCase())) {
                results.add(m);
            }
        }
        return results;
    }
}
