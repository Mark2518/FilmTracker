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
            android.widget.Button btnSaveProgress = findViewById(R.id.btn_save_progress);
            android.widget.Button btnLanguage = findViewById(R.id.btn_language);

            title.setText(movie.getTitle());
            yearDirector.setText(movie.getYear() + " • " + movie.getDuration() + " • " + movie.getDirector());
            if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                genreView.setText(android.text.TextUtils.join(", ", movie.getGenres()));
                genreView.setVisibility(android.view.View.VISIBLE);
            } else {
                genreView.setVisibility(android.view.View.GONE);
            }
            description.setText(movie.getDescription());
            btnSaveProgress.setText(R.string.btn_save_progress);
            
            // Set initial text for language button
            String currentLang = getResources().getConfiguration().getLocales().get(0).getLanguage();
            btnLanguage.setText(currentLang.equalsIgnoreCase("es") ? "ES" : "EN");

            btnLanguage.setOnClickListener(v -> {
                String lang = getResources().getConfiguration().getLocales().get(0).getLanguage();
                if (lang.equalsIgnoreCase("es")) {
                    setLocale("en");
                } else {
                    setLocale("es");
                }
            });

            updateButtons(btnWatchlist, btnSeen, movie);

            btnWatchlist.setOnClickListener(v -> {
                DataRepository repo = DataRepository.getInstance();
                User user = repo.getCurrentUser();
                if (user.isInWatchlist(movie)) {
                    repo.removeFromWatchlist(movie);
                    android.widget.Toast.makeText(this, R.string.removed_from_watchlist, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    repo.addToWatchlist(movie);
                    android.widget.Toast.makeText(this, R.string.added_to_watchlist, android.widget.Toast.LENGTH_SHORT).show();
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            btnSeen.setOnClickListener(v -> {
                DataRepository repo = DataRepository.getInstance();
                User user = repo.getCurrentUser();
                if (user.isSeen(movie)) {
                    repo.removeFromSeen(movie);
                    android.widget.Toast.makeText(this, R.string.removed_from_seen, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    repo.addToSeen(movie);
                    android.widget.Toast.makeText(this, R.string.added_to_seen, android.widget.Toast.LENGTH_SHORT).show();
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            android.widget.EditText editResume = findViewById(R.id.edit_resume_minute);
            
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
                            // ignore
                        }

                        if (totalDuration > 0 && minutes > totalDuration) {
                             new androidx.appcompat.app.AlertDialog.Builder(this)
                                 .setTitle(R.string.title_time_invalid)
                                 .setMessage(getString(R.string.msg_time_invalid, totalDuration))
                                 .setPositiveButton(R.string.btn_ok, null)
                                 .show();
                        } else if (minutes < 0) {
                             android.widget.Toast.makeText(this, R.string.msg_invalid_number, android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            DataRepository.getInstance().cacheMovie(movie);
                            DataRepository.getInstance().getCurrentUser().setResumePosition(movie, minutes);
                            android.widget.Toast.makeText(this, R.string.msg_progress_saved, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        android.widget.Toast.makeText(this, R.string.msg_invalid_number, android.widget.Toast.LENGTH_SHORT).show();
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
            btnWatchlist.setText(R.string.button_in_watchlist);
            btnWatchlist.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E054")));
        } else {
            btnWatchlist.setText(R.string.button_watchlist);
            btnWatchlist.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#445566")));
        }

        if (user.isSeen(movie)) {
            btnSeen.setText(R.string.button_in_seen);
            btnSeen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E054")));
        } else {
            btnSeen.setText(R.string.button_seen);
            btnSeen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#445566")));
        }
    }
    private void setLocale(String langCode) {
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        recreate();
    }
}
