package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.DownloadProgress;

/**
 * Interface for download progress of update callback.
 */
public interface DownloadProgressCallback {

    /**
     * Callback function for handling progress of downloading of update.
     *
     * @param downloadProgress Progress of downloading of update.
     */
    void call(DownloadProgress downloadProgress);
}
