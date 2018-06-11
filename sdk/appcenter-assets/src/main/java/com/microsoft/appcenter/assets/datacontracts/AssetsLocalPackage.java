package com.microsoft.appcenter.assets.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the downloaded package.
 */
public class AssetsLocalPackage extends AssetsPackage {

    /**
     * Indicates whether this update is in a "pending" state.
     * When <code>true</code>, that means the update has been downloaded and installed, but the app restart
     * needed to apply it hasn't occurred yet, and therefore, its changes aren't currently visible to the end-user.
     */
    @SerializedName("isPending")
    private boolean isPending;

    /**
     * The path to the update entry point (e.g. android.js.bundle for RN, or path to updates generally).
     */
    @SerializedName("entryPoint")
    private String entryPoint;

    /**
     * Indicates whether this is the first time the update has been run after being installed.
     */
    @SerializedName("isFirstRun")
    private boolean isFirstRun;

    /**
     * Whether this package is intended for debug mode.
     */
    @SerializedName("_isDebugOnly")
    private boolean isDebugOnly;

    /**
     * The time when binary of the update was modified (built).
     */
    @SerializedName("binaryModifiedTime")
    private String binaryModifiedTime;

    /**
     * Creates an instance of the package from basic package.
     *
     * @param failedInstall   whether this update has been previously installed but was rolled back.
     * @param isFirstRun      whether this is the first time the update has been run after being installed.
     * @param isPending       whether this update is in a "pending" state.
     * @param isDebugOnly     whether this package is intended for debug mode.
     * @param entryPoint   the path to the application entry point (e.g. android.js.bundle for RN, index.html for Cordova).
     * @param assetsPackage basic package containing the information.
     * @return instance of the {@link AssetsLocalPackage}.
     */
    public static AssetsLocalPackage createLocalPackage(final boolean failedInstall, final boolean isFirstRun,
                                                          final boolean isPending, final boolean isDebugOnly, String entryPoint,
                                                          final AssetsPackage assetsPackage) {
        AssetsLocalPackage assetsLocalPackage = new AssetsLocalPackage();
        assetsLocalPackage.setAppVersion(assetsPackage.getAppVersion());
        assetsLocalPackage.setDeploymentKey(assetsPackage.getDeploymentKey());
        assetsLocalPackage.setDescription(assetsPackage.getDescription());
        assetsLocalPackage.setFailedInstall(failedInstall);
        assetsLocalPackage.setMandatory(assetsPackage.isMandatory());
        assetsLocalPackage.setLabel(assetsPackage.getLabel());
        assetsLocalPackage.setPackageHash(assetsPackage.getPackageHash());
        assetsLocalPackage.setPending(isPending);
        assetsLocalPackage.setFirstRun(isFirstRun);
        assetsLocalPackage.setDebugOnly(isDebugOnly);
        assetsLocalPackage.setEntryPoint(entryPoint);
        return assetsLocalPackage;
    }

    public static AssetsLocalPackage createEmptyPackageForCheckForUpdateQuery(String appVersion) {
        AssetsLocalPackage assetsLocalPackage = new AssetsLocalPackage();
        assetsLocalPackage.setAppVersion(appVersion);
        assetsLocalPackage.setDeploymentKey("");
        assetsLocalPackage.setDescription("");
        assetsLocalPackage.setFailedInstall(false);
        assetsLocalPackage.setMandatory(false);
        assetsLocalPackage.setLabel("");
        assetsLocalPackage.setPackageHash("");
        assetsLocalPackage.setPending(false);
        assetsLocalPackage.setFirstRun(false);
        assetsLocalPackage.setDebugOnly(false);
        assetsLocalPackage.setEntryPoint("");
        return assetsLocalPackage;
    }

    /**
     * Gets whether this update is in a "pending" state and returns it.
     *
     * @return whether this update is in a "pending" state.
     */
    public boolean isPending() {
        return isPending;
    }

    /**
     * Sets whether this update is in a "pending" state.
     *
     * @param pending whether this update is in a "pending" state.
     */
    @SuppressWarnings("WeakerAccess")
    public void setPending(boolean pending) {
        isPending = pending;
    }

    /**
     * Gets whether this is the first time the update has been run after being installed and returns it.
     *
     * @return whether this is the first time the update has been run after being installed.
     */
    public boolean isFirstRun() {
        return isFirstRun;
    }

    /**
     * Sets whether this is the first time the update has been run after being installed.
     *
     * @param firstRun whether this is the first time the update has been run after being installed.
     */
    @SuppressWarnings("WeakerAccess")
    public void setFirstRun(boolean firstRun) {
        isFirstRun = firstRun;
    }

    /**
     * Gets whether this package is intended for debug mode and returns it.
     *
     * @return whether this package is intended for debug mode.
     */
    public boolean isDebugOnly() {
        return isDebugOnly;
    }

    /**
     * Sets whether this package is intended for debug mode.
     *
     * @param debugOnly whether this package is intended for debug mode.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDebugOnly(boolean debugOnly) {
        isDebugOnly = debugOnly;
    }

    /**
     * Gets the value of the path to the application entry point.
     *
     * @return the path to the application entry point.
     */
    public String getEntryPoint() {
        return entryPoint;
    }

    /**
     * Sets the path to the update entry point.
     *
     * @param entryPoint the path to the update entry point.
     */
    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getBinaryModifiedTime() {
        return binaryModifiedTime;
    }

    public void setBinaryModifiedTime(String binaryModifiedTime) {
        this.binaryModifiedTime = binaryModifiedTime;
    }
}
