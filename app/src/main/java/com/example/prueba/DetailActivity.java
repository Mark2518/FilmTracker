package com.example.prueba;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Recibimos la película
        Movie movie = (Movie) getIntent().getSerializableExtra("movie");

        if (movie != null) {
            // 1. Enlazamos las vistas
            ImageView backdrop = findViewById(R.id.detail_backdrop);
            ImageView poster = findViewById(R.id.detail_poster);
            TextView title = findViewById(R.id.detail_title);
            TextView yearDirector = findViewById(R.id.detail_year_director);
            TextView genreView = findViewById(R.id.detail_genre);
            TextView description = findViewById(R.id.detail_description);

            android.widget.Button btnWatchlist = findViewById(R.id.btn_watchlist);
            android.widget.Button btnSeen = findViewById(R.id.btn_seen);
            android.widget.Button btnSaveProgress = findViewById(R.id.btn_save_progress);

            // 2. Asignamos los textos
            title.setText(movie.getTitle());

            // Construimos la línea de detalles
            StringBuilder yearDirectorText = new StringBuilder();
            if (!movie.getYear().isEmpty()) {
                yearDirectorText.append(movie.getYear());
            }
            if (!movie.getDuration().isEmpty()) {
                if (yearDirectorText.length() > 0) yearDirectorText.append(" • ");
                yearDirectorText.append(movie.getDuration());
            }
            if (!movie.getDirector().isEmpty()) {
                if (yearDirectorText.length() > 0) yearDirectorText.append(" • ");
                yearDirectorText.append(movie.getDirector());
            }
            yearDirector.setText(yearDirectorText.toString());

            // Géneros
            if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
                genreView.setText(android.text.TextUtils.join(", ", movie.getGenres()));
                genreView.setVisibility(android.view.View.VISIBLE);
            } else {
                genreView.setVisibility(android.view.View.GONE);
            }

            // Descripción
            description.setText(movie.getDescription());

            // --- CAMBIO 1: Usamos R.string para el botón guardar ---
            btnSaveProgress.setText(R.string.btn_save_progress);

            // 3. Estado inicial de los botones
            updateButtons(btnWatchlist, btnSeen, movie);

            // 4. Listeners de botones
            btnWatchlist.setOnClickListener(v -> {
                DataRepository repo = DataRepository.getInstance();
                User user = repo.getCurrentUser();
                if (user.isInWatchlist(movie)) {
                    repo.removeFromWatchlist(movie);
                    // --- CAMBIO 2: Toast traducido ---
                    android.widget.Toast.makeText(this, R.string.removed_from_watchlist, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    repo.addToWatchlist(movie);
                    // --- CAMBIO 3: Toast traducido ---
                    android.widget.Toast.makeText(this, R.string.added_to_watchlist, android.widget.Toast.LENGTH_SHORT).show();
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            btnSeen.setOnClickListener(v -> {
                DataRepository repo = DataRepository.getInstance();
                User user = repo.getCurrentUser();
                if (user.isSeen(movie)) {
                    repo.removeFromSeen(movie);
                    // --- CAMBIO 4: Toast traducido ---
                    android.widget.Toast.makeText(this, R.string.removed_from_seen, android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    repo.addToSeen(movie);
                    // --- CAMBIO 5: Toast traducido ---
                    android.widget.Toast.makeText(this, R.string.added_to_seen, android.widget.Toast.LENGTH_SHORT).show();
                }
                updateButtons(btnWatchlist, btnSeen, movie);
            });

            // 5. Lógica de guardar progreso (Resume)
            android.widget.EditText editResume = findViewById(R.id.edit_resume_minute);
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

                        // Validación de duración
                        int totalDuration = 0;
                        try {
                            String[] parts = movie.getDuration().split("h ");
                            if (parts.length == 2) {
                                int h = Integer.parseInt(parts[0]);
                                int m = Integer.parseInt(parts[1].replace("m", ""));
                                totalDuration = h * 60 + m;
                            }
                        } catch (Exception e) { /* Ignorar error de parseo */ }

                        if (totalDuration > 0 && minutes > totalDuration) {
                            // --- CAMBIO 6: Mensaje de error con formato (%1$d) traducido ---
                            String errorMsg = getString(R.string.msg_time_invalid, totalDuration);
                            android.widget.Toast.makeText(this, errorMsg, android.widget.Toast.LENGTH_LONG).show();
                        } else if (minutes < 0) {
                            // --- CAMBIO 7: Toast traducido ---
                            android.widget.Toast.makeText(this, R.string.msg_invalid_number, android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            DataRepository.getInstance().cacheMovie(movie);
                            DataRepository.getInstance().getCurrentUser().setResumePosition(movie, minutes);
                            // --- CAMBIO 8: Toast traducido ---
                            android.widget.Toast.makeText(this, R.string.msg_progress_saved, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        android.widget.Toast.makeText(this, R.string.msg_invalid_number, android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // 6. Cargar imágenes
            Glide.with(this).load(movie.getBackdropUrl()).into(backdrop);
            Glide.with(this).load(movie.getPosterUrl()).into(poster);
        }
    }

    // Actualiza el color y texto de los botones usando R.string
    private void updateButtons(android.widget.Button btnWatchlist, android.widget.Button btnSeen, Movie movie) {
        User user = DataRepository.getInstance().getCurrentUser();

        if (user.isInWatchlist(movie)) {
            // --- CAMBIO 9: Texto traducido ---
            btnWatchlist.setText(R.string.button_in_watchlist);
            btnWatchlist.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E054")));
        } else {
            // --- CAMBIO 10: Texto traducido ---
            btnWatchlist.setText(R.string.button_watchlist);
            btnWatchlist.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#445566")));
        }

        if (user.isSeen(movie)) {
            // --- CAMBIO 11: Texto traducido ---
            btnSeen.setText(R.string.button_in_seen);
            btnSeen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E054")));
        } else {
            // --- CAMBIO 12: Texto traducido ---
            btnSeen.setText(R.string.button_seen);
            btnSeen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#445566")));
        }
    }
}