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
        String currentLang = prefs.getString("My_Lang", "en");

        if (currentLang.equals("es")) {
            radioEs.setChecked(true);
        } else {
            radioEn.setChecked(true);
        }

        btnApply.setOnClickListener(v -> {
            String selectedLang = radioEs.isChecked() ? "es" : "en";
            // Siempre aplicamos para asegurar la recarga
            setLocale(selectedLang);
        });
    }

    private void setLocale(String lang) {
        // A. Guardar preferencia
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("My_Lang", lang);
        editor.apply();

        // B. Cambiar Locale del sistema
        Locale myLocale = new Locale(lang);
        Locale.setDefault(myLocale);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.setLocale(myLocale);
        res.updateConfiguration(conf, dm);

        // C. IMPORTANTE: Borrar caché para que se descarguen las traducciones
        DataRepository.getInstance().clearCache();

        // D. Reiniciar la APP completa
        Intent refresh = new Intent(this, MainActivity.class);
        refresh.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(refresh);
        finish();
    }
}