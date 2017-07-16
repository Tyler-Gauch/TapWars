package com.tgauch.tapwars.interfaces;

import com.tgauch.tapwars.enums.Player;

public interface BattleGroundEventListener {

    public void onVictory(Player winner);

    public void onBattleStart();

}
