package com.tgauch.tapwars.game;

import com.tgauch.tapwars.enums.Player;

import java.nio.charset.StandardCharsets;

/**
 * Messages exchanged between two phones over Nearby Connections.
 *
 * <p>The host (player ONE) owns the game state. The guest only sends {@link Type#TAP} and {@link
 * Type#REMATCH}; the host answers with {@link Type#START}, {@link Type#STATE} updates and
 * eventually a winner, so both phones always agree on the result.
 */
public final class Message {

    public enum Type {
        /** host -> guest: a round is starting, begin the countdown. */
        START,
        /** guest -> host: the guest tapped. */
        TAP,
        /** host -> guest: the authoritative border position and winner (if any). */
        STATE,
        /** either way: this player wants to play again. */
        REMATCH,
    }

    public final Type type;
    public final int position;
    public final Player winner;

    private Message(Type type, int position, Player winner) {
        this.type = type;
        this.position = position;
        this.winner = winner;
    }

    public static Message start() {
        return new Message(Type.START, 0, null);
    }

    public static Message tap() {
        return new Message(Type.TAP, 0, null);
    }

    public static Message rematch() {
        return new Message(Type.REMATCH, 0, null);
    }

    public static Message state(int position, Player winner) {
        return new Message(Type.STATE, position, winner);
    }

    public byte[] encode() {
        String text = type.name();
        if (type == Type.STATE) {
            text += ":" + position + ":" + (winner == null ? "-" : winner.name());
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /** @return the decoded message, or null if the bytes aren't a valid message. */
    public static Message decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        String[] parts = new String(bytes, StandardCharsets.UTF_8).split(":");
        try {
            Type type = Type.valueOf(parts[0]);
            if (type != Type.STATE) {
                return parts.length == 1 ? new Message(type, 0, null) : null;
            }
            if (parts.length != 3) {
                return null;
            }
            int position = Integer.parseInt(parts[1]);
            Player winner = parts[2].equals("-") ? null : Player.valueOf(parts[2]);
            return new Message(type, position, winner);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
