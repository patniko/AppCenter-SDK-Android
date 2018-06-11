package com.microsoft.appcenter.assets.datacontracts;

import com.google.gson.annotations.SerializedName;

/**
 * Represents information about a remote package (on server).
 */
public class AssetsRemotePackage extends AssetsPackage {

    /**
     * Url to access package on server.
     */
    @SerializedName("downloadUrl")
    private String downloadUrl;

    /**
     * Size of the package.
     */
    @SerializedName("packageSize")
    private long packageSize;

    /**
     * Whether the client should trigger a store update.
     */
    @SerializedName("updateAppVersion")
    private boolean updateAppVersion;

    /**
     * Creates an instance of the class from the basic package.
     *
     * @param failedInstall    whether this update has been previously installed but was rolled back.
     * @param packageSize      the size of the package.
     * @param downloadUrl      url to access package on server.
     * @param updateAppVersion whether the client should trigger a store update.
     * @param assetsPackage  basic package containing the information.
     * @return instance of the {@link AssetsRemotePackage}.
     */
    public static AssetsRemotePackage createRemotePackage(final boolean failedInstall, final long packageSize,
                                                            final String downloadUrl, final boolean updateAppVersion,
                                                            final AssetsPackage assetsPackage) {
        AssetsRemotePackage assetsRemotePackage = new AssetsRemotePackage();
        assetsRemotePackage.setAppVersion(assetsPackage.getAppVersion());
        assetsRemotePackage.setDeploymentKey(assetsPackage.getDeploymentKey());
        assetsRemotePackage.setDescription(assetsPackage.getDescription());
        assetsRemotePackage.setFailedInstall(failedInstall);
        assetsRemotePackage.setMandatory(assetsPackage.isMandatory());
        assetsRemotePackage.setLabel(assetsPackage.getLabel());
        assetsRemotePackage.setPackageHash(assetsPackage.getPackageHash());
        assetsRemotePackage.setPackageSize(packageSize);
        assetsRemotePackage.setDownloadUrl(downloadUrl);
        assetsRemotePackage.setUpdateAppVersion(updateAppVersion);
        return assetsRemotePackage;
    }

    /**
     * Creates instance of the class from the update response from server.
     *
     * @param deploymentKey the deployment key that was used to originally download this update.
     * @param updateInfo    update info response from server.
     * @return instance of the {@link AssetsRemotePackage}.
     */
    public static AssetsRemotePackage createRemotePackageFromUpdateInfo(String deploymentKey, AssetsUpdateResponseUpdateInfo updateInfo) {
        AssetsRemotePackage assetsRemotePackage = new AssetsRemotePackage();
        assetsRemotePackage.setAppVersion(updateInfo.getAppVersion());
        assetsRemotePackage.setDeploymentKey(deploymentKey);
        assetsRemotePackage.setDescription(updateInfo.getDescription());
        assetsRemotePackage.setFailedInstall(false);
        assetsRemotePackage.setMandatory(updateInfo.isMandatory());
        assetsRemotePackage.setLabel(updateInfo.getLabel());
        assetsRemotePackage.setPackageHash(updateInfo.getPackageHash());
        assetsRemotePackage.setPackageSize(updateInfo.getPackageSize());
        assetsRemotePackage.setDownloadUrl(updateInfo.getDownloadUrl());
        assetsRemotePackage.setUpdateAppVersion(updateInfo.isUpdateAppVersion());
        return assetsRemotePackage;
    }

    /**
     * Creates a default package from the app version.
     *
     * @param appVersion       current app version.
     * @param updateAppVersion whether the client should trigger a store update.
     * @return instance of the {@link AssetsRemotePackage}.
     */
    public static AssetsRemotePackage createDefaultRemotePackage(final String appVersion, final boolean updateAppVersion) {
        AssetsRemotePackage assetsRemotePackage = new AssetsRemotePackage();
        assetsRemotePackage.setAppVersion(appVersion);
        assetsRemotePackage.setUpdateAppVersion(updateAppVersion);
        return assetsRemotePackage;
    }

    /**
     * Gets url to access package on server and returns it.
     *
     * @return url to access package on server.
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Sets url to access package on server.
     *
     * @param downloadUrl url to access package on server.
     */
    @SuppressWarnings("WeakerAccess")
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Gets size of the package and returns it.
     *
     * @return size of the package.
     */
    public long getPackageSize() {
        return packageSize;
    }

    /**
     * Sets size of the package.
     *
     * @param packageSize size of the package.
     */
    @SuppressWarnings("WeakerAccess")
    public void setPackageSize(long packageSize) {
        this.packageSize = packageSize;
    }

    /**
     * Gets whether the client should trigger a store update and returns it.
     *
     * @return whether the client should trigger a store update.
     */
    public boolean isUpdateAppVersion() {
        return updateAppVersion;
    }

    /**
     * Sets whether the client should trigger a store update.
     *
     * @param updateAppVersion whether the client should trigger a store update.
     */
    @SuppressWarnings("WeakerAccess")
    public void setUpdateAppVersion(boolean updateAppVersion) {
        this.updateAppVersion = updateAppVersion;
    }
}
