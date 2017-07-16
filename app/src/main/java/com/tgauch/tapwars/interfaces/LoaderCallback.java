package com.tgauch.tapwars.interfaces;

/**
 * Created by tag10 on 7/16/2017.
 */

public interface LoaderCallback {
    void onStart();
    void onTick();
    void onFinish(boolean wasTimeout);
}
