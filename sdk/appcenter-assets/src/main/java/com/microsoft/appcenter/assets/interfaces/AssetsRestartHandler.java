package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;

/**
 * Handler for restart events.
 */
public interface AssetsRestartHandler {

    /**
     * Called when application is ready to load a new bundle.
     *
     * @param onlyIfUpdateIsPending   <code>true</code> if restart only if update is pending.
     * @param assetsRestartListener listener to notify when restart has finished.
     * @throws AssetsMalformedDataException error thrown when the actual data is broken.
     */
    void performRestart(AssetsRestartListener assetsRestartListener, boolean onlyIfUpdateIsPending) throws AssetsMalformedDataException;
}
