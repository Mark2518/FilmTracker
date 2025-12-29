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
import java.util.Set;
import java.util.ArrayList;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private Context context;
    private List<Movie> movieList;

    public MovieAdapter(Context context, Set<Movie> movieSet) {
        this.context = context;
        this.movieList = new ArrayList<>(movieSet);
    }

    // Actualiza la lista completa (para nueva búsqueda)
    public void updateMovies(Set<Movie> newMovies) {
        this.movieList.clear();
        this.movieList.addAll(newMovies);
        notifyDataSetChanged();
    }

    public void addMovies(Set<Movie> moreMovies) {
        int startPos = this.movieList.size();
        this.movieList.addAll(moreMovies);
        notifyItemRangeInserted(startPos, moreMovies.size());
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);

        if (movie.isLoading()) {
            holder.poster.setImageResource(android.R.drawable.ic_menu_help);
        } else {
            Glide.with(context)
                    .load(movie.getPosterUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.poster);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!movie.isLoading()) {
                Intent intent = new Intent(context, DetailActivity.class);
                intent.putExtra("movie", movie);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.movie_poster);
        }
    }
}