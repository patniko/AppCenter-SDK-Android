package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;

/**
 * Interface for listener of sync status event.
 */
public interface AssetsSyncStatusListener {

    /**
     * Callback for handling sync status changed event.
     *
     * @param syncStatus new synchronization status.
     */
    void syncStatusChanged(AssetsSyncStatus syncStatus);
}
