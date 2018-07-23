package com.microsoft.appcenter.assets.core;

import com.microsoft.appcenter.assets.interfaces.AssetsBinaryVersionMismatchListener;
import com.microsoft.appcenter.assets.interfaces.AssetsDownloadProgressListener;
import com.microsoft.appcenter.assets.interfaces.AssetsSyncStatusListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates listeners that {@link AssetsBaseCore} is using.
 */
@SuppressWarnings("WeakerAccess")
public class AssetsListeners {

    /**
     * List of {@link AssetsSyncStatusListener}.
     */
    public final List<AssetsSyncStatusListener> mSyncStatusListeners;

    /**
     * List of {@link AssetsDownloadProgressListener}.
     */
    public final List<AssetsDownloadProgressListener> mDownloadProgressListeners;

    /**
     * List of {@link AssetsBinaryVersionMismatchListener}.
     */
    public final List<AssetsBinaryVersionMismatchListener> mBinaryVersionMismatchListeners;

    /**
     * Create instance of {@link AssetsListeners}.
     */
    public AssetsListeners() {
        mSyncStatusListeners = new ArrayList<>();
        mDownloadProgressListeners = new ArrayList<>();
        mBinaryVersionMismatchListeners = new ArrayList<>();
    }
}
