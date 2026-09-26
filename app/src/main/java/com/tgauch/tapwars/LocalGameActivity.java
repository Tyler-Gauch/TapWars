package com.tgauch.tapwars;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.tgauch.tapwars.enums.Player;

/**
 * Two players, one phone. Lay the phone flat between you: player one taps the bottom half,
 * player two taps the top half.
 */
public class LocalGameActivity extends GameActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        battleGround.setMirrored(true);
        battleGround.setPlayers(
                getString(R.string.player_one), ContextCompat.getColor(this, R.color.player_one),
                getString(R.string.player_two), ContextCompat.getColor(this, R.color.player_two));
        startRound();
    }

    @Override
    public void onSideTapped(boolean bottomHalf) {
        if (!isPlaying()) {
            return;
        }
        Player winner = game.tap(bottomHalf ? Player.ONE : Player.TWO);
        battleGround.invalidate();
        if (winner != null) {
            endRound(winner);
        }
    }

    @Override
    protected void onRoundEnded(Player winner) {
        String win = getString(R.string.you_win);
        String lose = getString(R.string.you_lose);
        boolean bottomWon = winner == Player.ONE;
        battleGround.setMessages(bottomWon ? win : lose, bottomWon ? lose : win, false);
    }

    @Override
    protected void onRematchClicked() {
        startRound();
    }
}
