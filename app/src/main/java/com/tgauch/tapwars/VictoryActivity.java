package com.tgauch.tapwars;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.views.BattleGroundView;

/**
 * Created by tag10 on 7/15/2017.
 */

public class VictoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.victory_layout);
        TextView victoryText = (TextView) findViewById(R.id.victoryText);
        Player winner = (Player) getIntent().getExtras().getSerializable("winner");

        victoryText.setText("Player " + (winner == Player.ONE ? "One" : "Two") + " is victorious!");
    }
}
