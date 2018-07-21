package com.microsoft.appcenter.assets.core;

import com.microsoft.appcenter.assets.managers.AssetsAcquisitionManager;
import com.microsoft.appcenter.assets.managers.AssetsRestartManager;
import com.microsoft.appcenter.assets.managers.AssetsTelemetryManager;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;

/**
 * Encapsulates managers that {@link AssetsBaseCore} is using.
 */
@SuppressWarnings("WeakerAccess")
public class AssetsManagers {

    /**
     * Instance of {@link AssetsUpdateManager}.
     */
    public final AssetsUpdateManager mUpdateManager;

    /**
     * Instance of {@link AssetsTelemetryManager}.
     */
    public final AssetsTelemetryManager mTelemetryManager;

    /**
     * Instance of {@link SettingsManager}.
     */
    public final SettingsManager mSettingsManager;

    /**
     * Instance of {@link AssetsRestartManager}.
     */
    public final AssetsRestartManager mRestartManager;

    /**
     * Instance of {@link AssetsAcquisitionManager}.
     */
    public final AssetsAcquisitionManager mAcquisitionManager;

    /**
     * Creates instance of {@link AssetsManagers}.
     *
     * @param updateManager      instance of {@link AssetsUpdateManager}.
     * @param telemetryManager   instance of {@link AssetsTelemetryManager}.
     * @param settingsManager    instance of {@link SettingsManager}.
     * @param restartManager     instance of {@link AssetsRestartManager}.
     * @param acquisitionManager instance of {@link AssetsAcquisitionManager}.
     */
    public AssetsManagers(
            AssetsUpdateManager updateManager,
            AssetsTelemetryManager telemetryManager,
            SettingsManager settingsManager,
            AssetsRestartManager restartManager,
            AssetsAcquisitionManager acquisitionManager) {
        this.mUpdateManager = updateManager;
        this.mTelemetryManager = telemetryManager;
        this.mSettingsManager = settingsManager;
        this.mRestartManager = restartManager;
        this.mAcquisitionManager = acquisitionManager;
    }
}
