package com.tgauch.tapwars.game;

import com.tgauch.tapwars.enums.Player;

/**
 * The rules of TapWars, independent of any UI or networking.
 *
 * <p>Each tap pushes the border one step towards the opponent. The border starts in the middle;
 * the first player to push it all the way to the other side wins.
 */
public class TugOfWar {

    public static final int DEFAULT_TAPS_TO_WIN = 25;

    private final int tapsToWin;
    // Positive values favour player ONE, negative values favour player TWO.
    private int position = 0;
    private Player winner = null;

    public TugOfWar() {
        this(DEFAULT_TAPS_TO_WIN);
    }

    public TugOfWar(int tapsToWin) {
        if (tapsToWin <= 0) {
            throw new IllegalArgumentException("tapsToWin must be positive");
        }
        this.tapsToWin = tapsToWin;
    }

    /**
     * Registers a tap from the given player.
     *
     * @return the winner if this tap ended the game, otherwise null.
     */
    public Player tap(Player player) {
        if (winner != null) {
            return null;
        }
        position += player == Player.ONE ? 1 : -1;
        if (position >= tapsToWin) {
            winner = Player.ONE;
        } else if (position <= -tapsToWin) {
            winner = Player.TWO;
        }
        return winner;
    }

    /** Overrides the state, e.g. with authoritative values received from the host. */
    public void setState(int position, Player winner) {
        this.position = Math.max(-tapsToWin, Math.min(tapsToWin, position));
        this.winner = winner;
    }

    public void reset() {
        position = 0;
        winner = null;
    }

    public int getPosition() {
        return position;
    }

    public int getTapsToWin() {
        return tapsToWin;
    }

    public Player getWinner() {
        return winner;
    }

    public boolean isOver() {
        return winner != null;
    }

    /**
     * @return how much of the battlefield belongs to the given player, from 0 (nothing) to 1
     *     (everything). Both players start at 0.5.
     */
    public float getShare(Player player) {
        float share = 0.5f + (position / (float) tapsToWin) / 2f;
        return player == Player.ONE ? share : 1f - share;
    }
}
