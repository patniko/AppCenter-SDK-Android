package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;

/**
 * Listener for restart events.
 */
public interface AssetsRestartListener {

    /**
     * Called when application has performed a restart.
     *
     * @throws AssetsMalformedDataException error thrown when the actual data is broken.
     */
    void onRestartFinished() throws AssetsMalformedDataException;
}
