package com.example.prueba;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Movie implements Serializable {
    // Campos que SI existen en tu Base de Datos
    private long id;
    private String title;
    private String posterUrl;
    private String description; // (overview)
    private String duration;    // (runtime convertido)
    private List<String> genres;

    // Estados de la app (no de la BD)
    private boolean isWatched = false;
    private boolean isInWatchlist = false;
    private boolean isLoading = false;

    // --- NUEVO CONSTRUCTOR (Solo elementos de la BD) ---
    public Movie(long id, String title, String posterUrl, String description, String duration, List<String> genres) {
        this.id = id;
        this.title = title;
        this.posterUrl = posterUrl;
        this.description = description;
        this.duration = duration;
        this.genres = genres != null ? genres : new ArrayList<>();
    }

    // Constructor para la carga ("loading skeletons")
    public static Movie createLoadingMovie() {
        long randomId = -(long)(Math.random() * 1000000);
        Movie m = new Movie(randomId, "", "", "", "", new ArrayList<>());
        m.isLoading = true;
        return m;
    }

    // --- GETTERS REALES ---
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getPosterUrl() { return posterUrl; }
    public String getDescription() { return description; }
    public String getDuration() { return duration; }
    public List<String> getGenres() { return genres; }

    // --- COMPATIBILIDAD (Para que no falle DetailActivity) ---
    // Como ya no tienes estos datos, devolvemos vacío o el poster
    public String getBackdropUrl() { return posterUrl; }
    public String getDirector() { return ""; }
    public String getYear() { return ""; }
    public boolean isSeries() { return false; }
    public String getRating() { return ""; }

    // --- ESTADOS ---
    public boolean isLoading() { return isLoading; }
    public boolean isWatched() { return isWatched; }
    public void setWatched(boolean watched) { isWatched = watched; }
    public boolean isInWatchlist() { return isInWatchlist; }
    public void setInWatchlist(boolean inWatchlist) { isInWatchlist = inWatchlist; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id == movie.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}