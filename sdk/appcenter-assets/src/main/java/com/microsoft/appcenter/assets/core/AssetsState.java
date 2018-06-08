package com.microsoft.appcenter.assets.core;

import com.microsoft.appcenter.assets.enums.AssetsInstallMode;

/**
 * Encapsulates state of {@link AssetsBaseCore}.
 */
@SuppressWarnings("WeakerAccess")
public class AssetsState {

    /**
     * Indicates whether a new update running for the first time.
     */
    public boolean mDidUpdate;

    /**
     * Indicates whether there is a need to send rollback report.
     */
    public boolean mNeedToReportRollback;

    /**
     * Indicates whether current install mode.
     */
    public AssetsInstallMode mCurrentInstallModeInProgress;

    /**
     * Indicates whether is running binary version of app.
     */
    public boolean mIsRunningBinaryVersion;

    /**
     * Indicates whether sync already in progress.
     */
    public boolean mSyncInProgress;

    /**
     * Minimum background duration value.
     */
    public int mMinimumBackgroundDuration;
}
