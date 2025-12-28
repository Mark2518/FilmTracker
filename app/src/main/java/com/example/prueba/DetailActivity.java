package com.example.prueba;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Movie movie = (Movie) getIntent().getSerializableExtra("movie");

        if (movie != null) {
            ImageView backdrop = findViewById(R.id.detail_backdrop);
            ImageView poster = findViewById(R.id.detail_poster);
            TextView title = findViewById(R.id.detail_title);
            TextView yearDirector = findViewById(R.id.detail_year_director);
            RatingBar rating = findViewById(R.id.detail_rating);
            TextView description = findViewById(R.id.detail_description);

            TextView cast = findViewById(R.id.detail_cast);
            android.widget.Button btnWatchlist = findViewById(R.id.btn_watchlist);
            android.widget.Button btnSeen = findViewById(R.id.btn_seen);
            android.widget.Button btnSeen = findViewById(R.id.btn_seen);

            title.setText(movie.getTitle());
            yearDirector.setText(movie.getYear() + " • " + movie.getDuration() + " • " + movie.getDirector());
            rating.setRating(movie.getRating());
            description.setText(movie.getDescription());
            cast.setText(movie.getCast());

            updateButtons(btnWatchlist, btnSeen, movie);

            btnWatchlist.setOnClickListener(v -> {
                User user = DataRepository.getInstance().getCurrentUser();
                if (user.isInWatchlist(movie)) {
                    user.removeFromWatchlist(movie);
                } else {
                    user.addToWatchlist(movie);
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            btnSeen.setOnClickListener(v -> {
                User user = DataRepository.getInstance().getCurrentUser();
                if (user.isSeen(movie)) {
                    user.removeFromSeen(movie);
                } else {
                    user.addToSeen(movie);
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            Glide.with(this).load(movie.getBackdropUrl()).into(backdrop);
            Glide.with(this).load(movie.getPosterUrl()).into(poster);
        }
    }

    private void updateButtons(android.widget.Button btnWatchlist, android.widget.Button btnSeen, Movie movie) {
        User user = DataRepository.getInstance().getCurrentUser();
        
        if (user.isInWatchlist(movie)) {
            btnWatchlist.setText("In Watchlist");
            btnWatchlist.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E054")));
        } else {
            btnWatchlist.setText("Watchlist");
            btnWatchlist.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#445566")));
        }

        if (user.isSeen(movie)) {
            btnSeen.setText("Seen");
            btnSeen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E054")));
        } else {
            btnSeen.setText("Seen");
            btnSeen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#445566")));
        }
    }
}
