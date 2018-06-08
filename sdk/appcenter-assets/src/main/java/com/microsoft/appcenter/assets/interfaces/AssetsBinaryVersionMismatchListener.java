package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;

/**
 * Interface for listener of binary version mismatch event.
 */
public interface AssetsBinaryVersionMismatchListener {

    /**
     * Callback for handling binary version mismatch event.
     *
     * @param update Remote package (from server).
     */
    void binaryVersionMismatchChanged(AssetsRemotePackage update);
}