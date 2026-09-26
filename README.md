# TapWars
TapWars is a two-player Android tapping game. Each player owns one side of the screen, and every
tap pushes the border towards the other player. Push it all the way across and you win — so
whoever taps fastest takes the screen.

## Game modes
- **Play on this phone** — lay the phone flat between you. Player 1 taps the bottom half, player 2
  taps the top half (the top half's text is drawn upside down so it reads correctly from the other
  end). Multi-touch is supported, so use as many fingers as you like.
- **Play with a nearby phone** — two phones connect over
  [Nearby Connections](https://developers.google.com/nearby/connections/overview) (Bluetooth /
  Wi-Fi, no internet needed). Each player taps anywhere on their own screen. Both phones show a
  matching code before the game starts.

## How it works
- `game/TugOfWar` holds the rules: each tap moves the border one step; 25 steps from the middle wins.
- In Nearby mode the phone that sent the connection request is the **host**: it runs the game and
  sends the authoritative border position to the guest, so both phones always agree on the winner.
  The guest just sends its taps. The protocol lives in `game/Message`.

## Building
Requires JDK 17+ and the Android SDK (API 35).

```
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests for the game rules and protocol
```

`tapwars-stable.apk` in the repo root is a prebuilt debug APK you can sideload.
