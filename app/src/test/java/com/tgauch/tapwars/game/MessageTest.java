package com.tgauch.tapwars.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.tgauch.tapwars.enums.Player;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class MessageTest {

    private static Message roundTrip(Message message) {
        return Message.decode(message.encode());
    }

    @Test
    public void simpleMessagesRoundTrip() {
        assertEquals(Message.Type.START, roundTrip(Message.start()).type);
        assertEquals(Message.Type.TAP, roundTrip(Message.tap()).type);
        assertEquals(Message.Type.REMATCH, roundTrip(Message.rematch()).type);
    }

    @Test
    public void stateRoundTrips() {
        Message decoded = roundTrip(Message.state(-7, null));
        assertEquals(Message.Type.STATE, decoded.type);
        assertEquals(-7, decoded.position);
        assertNull(decoded.winner);

        decoded = roundTrip(Message.state(25, Player.ONE));
        assertEquals(25, decoded.position);
        assertEquals(Player.ONE, decoded.winner);
    }

    @Test
    public void garbageIsRejected() {
        assertNull(Message.decode(null));
        assertNull(decode(""));
        assertNull(decode("Victory"));
        assertNull(decode("STATE:abc:-"));
        assertNull(decode("STATE:1"));
        assertNull(decode("STATE:1:THREE"));
        assertNull(decode("TAP:1"));
    }

    private static Message decode(String text) {
        return Message.decode(text.getBytes(StandardCharsets.UTF_8));
    }
}
