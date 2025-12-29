package com.example.prueba;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup radioGroup;
    private RadioButton radioEs, radioEn;
    private Button btnApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        radioGroup = findViewById(R.id.radioGroupLanguage);
        radioEs = findViewById(R.id.radioEs);
        radioEn = findViewById(R.id.radioEn);
        btnApply = findViewById(R.id.btnApplySettings);

        // 1. Cargar el estado actual
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String currentLang = prefs.getString("My_Lang", "en"); // Inglés por defecto

        if (currentLang.equals("es")) {
            radioEs.setChecked(true);
        } else {
            radioEn.setChecked(true);
        }

        // 2. Escuchar el botón de aplicar
        btnApply.setOnClickListener(v -> {
            String selectedLang = "en";
            if (radioEs.isChecked()) {
                selectedLang = "es";
            }

            // Si el idioma es diferente al actual, lo cambiamos
            if (!selectedLang.equals(currentLang)) {
                setLocale(selectedLang);
            } else {
                finish(); // Simplemente cerrar si no hubo cambios
            }
        });
    }

    private void setLocale(String lang) {
        // A. Guardar en preferencias para el futuro
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("My_Lang", lang);
        editor.apply();

        // B. Cambiar la configuración del sistema para la app
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.setLocale(myLocale);
        res.updateConfiguration(conf, dm);

        // C. Reiniciar la APP completa para que los cambios surtan efecto en todas partes
        Intent refresh = new Intent(this, MainActivity.class);
        // Estas flags borran todas las actividades anteriores y empiezan de nuevo
        refresh.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(refresh);
        finish();
    }
}