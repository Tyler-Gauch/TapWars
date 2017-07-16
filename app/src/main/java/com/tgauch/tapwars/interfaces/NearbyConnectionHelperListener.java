package com.tgauch.tapwars.interfaces;

import com.tgauch.tapwars.enums.NearbyConnectionHelperState;

/**
 * Created by tag10 on 7/15/2017.
 */

public interface NearbyConnectionHelperListener {
    void onStateChanged(NearbyConnectionHelperState state);
}
