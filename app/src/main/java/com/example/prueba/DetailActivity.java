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
            TextView genreView = findViewById(R.id.detail_genre);

            TextView description = findViewById(R.id.detail_description);
            android.widget.Button btnWatchlist = findViewById(R.id.btn_watchlist);
            android.widget.Button btnSeen = findViewById(R.id.btn_seen);

            title.setText(movie.getTitle());
            yearDirector.setText(movie.getYear() + " • " + movie.getDuration() + " • " + movie.getDirector());
            if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                genreView.setText(android.text.TextUtils.join(", ", movie.getGenres()));
                genreView.setVisibility(android.view.View.VISIBLE);
            } else {
                genreView.setVisibility(android.view.View.GONE);
            }
            description.setText(movie.getDescription());


            updateButtons(btnWatchlist, btnSeen, movie);

            btnWatchlist.setOnClickListener(v -> {
                DataRepository repo = DataRepository.getInstance();
                User user = repo.getCurrentUser();
                if (user.isInWatchlist(movie)) {
                    repo.removeFromWatchlist(movie);
                } else {
                    repo.addToWatchlist(movie);
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            btnSeen.setOnClickListener(v -> {
                DataRepository repo = DataRepository.getInstance();
                User user = repo.getCurrentUser();
                if (user.isSeen(movie)) {
                    repo.removeFromSeen(movie);
                } else {
                    repo.addToSeen(movie);
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            android.widget.EditText editResume = findViewById(R.id.edit_resume_minute);
            android.widget.Button btnSaveProgress = findViewById(R.id.btn_save_progress);

            // Load saved progress
            User currentUserForProgress = DataRepository.getInstance().getCurrentUser();
            int savedMinute = currentUserForProgress.getResumePosition(movie);
            if (savedMinute > 0) {
                editResume.setText(String.valueOf(savedMinute));
            }

            btnSaveProgress.setOnClickListener(v -> {
                String input = editResume.getText().toString();
                if (!input.isEmpty()) {
                    try {
                        int minutes = Integer.parseInt(input);
                        
                        // Parse duration string "Xh Ym" back to total minutes
                        int totalDuration = 0;
                        try {
                             String[] parts = movie.getDuration().split("h ");
                             if (parts.length == 2) {
                                 int h = Integer.parseInt(parts[0]);
                                 int m = Integer.parseInt(parts[1].replace("m", ""));
                                 totalDuration = h * 60 + m;
                             }
                        } catch (Exception e) {
                            // Fallback if parsing fails, maybe assume valid? Or ignore max check.
                            // Let's rely on standard format produced by TursoClient
                        }

                        if (totalDuration > 0 && minutes > totalDuration) {
                             android.widget.Toast.makeText(this, "Time exceeds duration (" + totalDuration + "m)", android.widget.Toast.LENGTH_SHORT).show();
                        } else if (minutes < 0) {
                             android.widget.Toast.makeText(this, "Invalid time", android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            DataRepository.getInstance().cacheMovie(movie);
                            DataRepository.getInstance().getCurrentUser().setResumePosition(movie, minutes);
                            android.widget.Toast.makeText(this, "Progress saved!", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        android.widget.Toast.makeText(this, "Invalid number", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
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
