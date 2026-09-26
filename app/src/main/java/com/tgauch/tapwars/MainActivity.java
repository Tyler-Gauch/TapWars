package com.tgauch.tapwars;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

/** The main menu: pick a name and a game mode. */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "tapwars";
    private static final String KEY_NAME = "player_name";
    private static final int MAX_NAME_LENGTH = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText nameInput = findViewById(R.id.playerName);
        nameInput.setText(getPlayerName(this));
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String name = s.toString().trim();
                if (!name.isEmpty()) {
                    prefs(MainActivity.this).edit().putString(KEY_NAME, name).apply();
                }
            }
        });

        findViewById(R.id.playLocal).setOnClickListener(
                v -> startActivity(new Intent(this, LocalGameActivity.class)));
        findViewById(R.id.playNearby).setOnClickListener(
                v -> startActivity(new Intent(this, NearbyGameActivity.class)));
    }

    /** @return the saved player name, creating a random guest name the first time. */
    public static String getPlayerName(Context context) {
        SharedPreferences prefs = prefs(context);
        String name = prefs.getString(KEY_NAME, null);
        if (name == null || name.trim().isEmpty()) {
            name = "Guest" + (1000 + new Random().nextInt(9000));
            prefs.edit().putString(KEY_NAME, name).apply();
        }
        name = name.trim();
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
