package com.example.prueba;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class HorizontalMovieAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MOVIE = 0;
    private static final int TYPE_REFRESH = 1;

    private Context context;
    private List<Movie> movieList;
    private Runnable onRefresh;

    public HorizontalMovieAdapter(Context context, java.util.Set<Movie> movieSet) {
        this(context, movieSet, null);
    }

    public HorizontalMovieAdapter(Context context, java.util.Set<Movie> movieSet, Runnable onRefresh) {
        this.context = context;
        this.movieList = new java.util.ArrayList<>(movieSet);
        this.onRefresh = onRefresh;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_REFRESH) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_refresh_card, parent, false);
            return new RefreshViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_horizontal, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_REFRESH) {
            holder.itemView.setOnClickListener(v -> {
                if (onRefresh != null) onRefresh.run();
            });
            return;
        }

        MovieViewHolder movieHolder = (MovieViewHolder) holder;
        Movie movie = movieList.get(position);
        Glide.with(context)
                .load(movie.getPosterUrl())
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(movieHolder.poster);

        movieHolder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("movie", movie);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        if (onRefresh != null) {
            return movieList.size() + 1;
        }
        return movieList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (onRefresh != null && position == movieList.size()) {
            return TYPE_REFRESH;
        }
        return TYPE_MOVIE;
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.movie_poster);
        }
    }
    
    public static class RefreshViewHolder extends RecyclerView.ViewHolder {
        public RefreshViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
