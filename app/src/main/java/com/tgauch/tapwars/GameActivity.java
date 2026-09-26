package com.tgauch.tapwars;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.tgauch.tapwars.enums.Player;
import com.tgauch.tapwars.game.TugOfWar;
import com.tgauch.tapwars.views.BattleGroundView;

/** Shared plumbing for a round of TapWars: the battlefield, the countdown and the end screen. */
public abstract class GameActivity extends AppCompatActivity implements BattleGroundView.Listener {

    private static final long COUNTDOWN_STEP_MS = 800;
    // Stops a frantic last tap from hitting "Rematch" by accident.
    private static final long END_BUTTONS_DELAY_MS = 1000;

    protected final TugOfWar game = new TugOfWar();
    protected final Handler handler = new Handler(Looper.getMainLooper());
    protected BattleGroundView battleGround;

    private View endButtons;
    private Button rematchButton;
    private boolean playing = false;
    private boolean roundOver = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();

        battleGround = findViewById(R.id.battleGround);
        battleGround.setListener(this);
        battleGround.setGame(game, Player.ONE);

        endButtons = findViewById(R.id.endButtons);
        rematchButton = findViewById(R.id.rematch);
        rematchButton.setOnClickListener(v -> onRematchClicked());
        findViewById(R.id.mainMenu).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    /** Resets the board and counts down 3, 2, 1 before taps start counting. */
    protected void startRound() {
        handler.removeCallbacksAndMessages(null);
        playing = false;
        roundOver = false;
        game.reset();
        endButtons.setVisibility(View.GONE);
        rematchButton.setEnabled(true);

        String[] steps = {"3", "2", "1"};
        for (int i = 0; i < steps.length; i++) {
            String step = steps[i];
            handler.postDelayed(() -> showBig(step), i * COUNTDOWN_STEP_MS);
        }
        handler.postDelayed(() -> {
            playing = true;
            showBig(getString(R.string.go));
        }, steps.length * COUNTDOWN_STEP_MS);
        handler.postDelayed(() -> {
            if (playing) {
                battleGround.setMessages(null, null, true);
            }
        }, (steps.length + 1) * COUNTDOWN_STEP_MS);
    }

    /** Stops the round and shows who won. */
    protected void endRound(Player winner) {
        if (roundOver) {
            return;
        }
        roundOver = true;
        playing = false;
        handler.removeCallbacksAndMessages(null);
        battleGround.invalidate();
        onRoundEnded(winner);
        handler.postDelayed(() -> endButtons.setVisibility(View.VISIBLE), END_BUTTONS_DELAY_MS);
    }

    protected boolean isPlaying() {
        return playing;
    }

    /** Disables the rematch button, e.g. while waiting for the other phone to agree. */
    protected void setRematchEnabled(boolean enabled) {
        rematchButton.setEnabled(enabled);
    }

    protected void hideEndButtons() {
        endButtons.setVisibility(View.GONE);
    }

    private void showBig(String text) {
        battleGround.setMessages(text, text, true);
    }

    /** Called once when a round finishes; show the result on {@link #battleGround}. */
    protected abstract void onRoundEnded(Player winner);

    protected abstract void onRematchClicked();
}
