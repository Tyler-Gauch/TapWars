package com.tgauch.tapwars.enums;

/**
 * Created by tag10 on 7/16/2017.
 */

public enum MainState {

    UNKNOWN, // start of app
    MAIN_MENU_WAITING, //not connected waiting for the user to give input
    IN_MATCH, // connected and in a game
    END_GAME_WAITING, //connected in end game waiting for user to give input
    DISCONNECTED, //we disconnected from the other player
    LOOKING_FOR_PLAYER,
    ASKING_FOR_REMATCH,
    WAITING_FOR_OTHER_PLAYER
}
