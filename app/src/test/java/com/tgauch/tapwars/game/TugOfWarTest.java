package com.tgauch.tapwars.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.tgauch.tapwars.enums.Player;

import org.junit.Test;

public class TugOfWarTest {

    @Test
    public void startsEven() {
        TugOfWar game = new TugOfWar(10);
        assertEquals(0.5f, game.getShare(Player.ONE), 0.0001f);
        assertEquals(0.5f, game.getShare(Player.TWO), 0.0001f);
        assertFalse(game.isOver());
    }

    @Test
    public void tapsPushTheBorder() {
        TugOfWar game = new TugOfWar(10);
        game.tap(Player.ONE);
        game.tap(Player.ONE);
        game.tap(Player.TWO);
        assertEquals(1, game.getPosition());
        assertEquals(0.55f, game.getShare(Player.ONE), 0.0001f);
        assertEquals(0.45f, game.getShare(Player.TWO), 0.0001f);
    }

    @Test
    public void firstToPushAllTheWayWins() {
        TugOfWar game = new TugOfWar(3);
        assertNull(game.tap(Player.TWO));
        assertNull(game.tap(Player.TWO));
        assertEquals(Player.TWO, game.tap(Player.TWO));
        assertTrue(game.isOver());
        assertEquals(1f, game.getShare(Player.TWO), 0.0001f);
    }

    @Test
    public void tapsAfterTheEndAreIgnored() {
        TugOfWar game = new TugOfWar(1);
        game.tap(Player.ONE);
        assertNull(game.tap(Player.TWO));
        assertEquals(Player.ONE, game.getWinner());
        assertEquals(1, game.getPosition());
    }

    @Test
    public void resetStartsOver() {
        TugOfWar game = new TugOfWar(1);
        game.tap(Player.ONE);
        game.reset();
        assertFalse(game.isOver());
        assertEquals(0, game.getPosition());
    }

    @Test
    public void setStateIsClamped() {
        TugOfWar game = new TugOfWar(5);
        game.setState(-99, Player.TWO);
        assertEquals(-5, game.getPosition());
        assertEquals(Player.TWO, game.getWinner());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveTarget() {
        new TugOfWar(0);
    }
}
