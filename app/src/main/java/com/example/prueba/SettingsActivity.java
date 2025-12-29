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

    private RadioButton radioEs, radioEn;
    private Button btnApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        radioEs = findViewById(R.id.radioEs);
        radioEn = findViewById(R.id.radioEn);
        btnApply = findViewById(R.id.btnApplySettings);

        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        String currentLang = prefs.getString("My_Lang", "en");

        if (currentLang.equals("es")) {
            radioEs.setChecked(true);
        } else {
            radioEn.setChecked(true);
        }

        btnApply.setOnClickListener(v -> {
            String selectedLang = radioEs.isChecked() ? "es" : "en";
            setLocale(selectedLang);
        });
    }

    private void setLocale(String lang) {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("My_Lang", lang);
        editor.apply();

        Locale myLocale = new Locale(lang);
        Locale.setDefault(myLocale);
        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        conf.setLocale(myLocale);

        DataRepository.getInstance().clearCache();

        Intent refresh = new Intent(this, MainActivity.class);
        refresh.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(refresh);
        finish();
    }
}