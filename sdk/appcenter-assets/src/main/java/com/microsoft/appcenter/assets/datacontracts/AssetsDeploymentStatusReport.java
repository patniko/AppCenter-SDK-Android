package com.microsoft.appcenter.assets.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.assets.enums.AssetsDeploymentStatus;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;

/**
 * Represents a report about the deployment.
 */
public class AssetsDeploymentStatusReport extends AssetsDownloadStatusReport {

    /**
     * The version of the app that was deployed (for a native app upgrade).
     */
    @SerializedName("appVersion")
    private String appVersion;

    /**
     * Deployment key used when deploying the previous package.
     */
    @SerializedName("previousDeploymentKey")
    private String previousDeploymentKey;

    /**
     * The label (v#) of the package that was upgraded from.
     */
    @SerializedName("previousLabelOrAppVersion")
    private String previousLabelOrAppVersion;

    /**
     * Whether the deployment succeeded or failed.
     */
    @SerializedName("status")
    private AssetsDeploymentStatus status;

    /**
     * Stores information about installed/failed package.
     */
    @SerializedName("package")
    private transient AssetsPackage assetsPackage;

    /**
     * Gets the version of the app that was deployed and returns it.
     *
     * @return the version of the app that was deployed.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Sets the version of the app that was deployed.
     *
     * @param appVersion the version of the app that was deployed.
     */
    public void setAppVersion(String appVersion) throws AssetsIllegalArgumentException {
        if (appVersion != null) {
            this.appVersion = appVersion;
        } else {
            throw new AssetsIllegalArgumentException(this.getClass().getName(), "appVersion");
        }
    }

    /**
     * Gets deployment key used when deploying the previous package and returns it.
     *
     * @return deployment key used when deploying the previous package.
     */
    public String getPreviousDeploymentKey() {
        return previousDeploymentKey;
    }

    /**
     * Sets deployment key used when deploying the previous package.
     *
     * @param previousDeploymentKey deployment key used when deploying the previous package.
     */
    public void setPreviousDeploymentKey(String previousDeploymentKey) throws AssetsIllegalArgumentException {
        if (previousDeploymentKey != null) {
            this.previousDeploymentKey = previousDeploymentKey;
        } else {
            throw new AssetsIllegalArgumentException(this.getClass().getName(), "previousDeploymentKey");
        }
    }

    /**
     * Gets the label (v#) of the package that was upgraded from and returns it.
     *
     * @return the label (v#) of the package that was upgraded from.
     */
    public String getPreviousLabelOrAppVersion() {
        return previousLabelOrAppVersion;
    }

    /**
     * Sets the label (v#) of the package that was upgraded from.
     *
     * @param previousLabelOrAppVersion the label (v#) of the package that was upgraded from.
     */
    public void setPreviousLabelOrAppVersion(String previousLabelOrAppVersion) {
        this.previousLabelOrAppVersion = previousLabelOrAppVersion;
    }

    /**
     * Gets whether the deployment succeeded or failed and returns it.
     *
     * @return whether the deployment succeeded or failed.
     */
    public AssetsDeploymentStatus getStatus() {
        return status;
    }

    /**
     * Sets whether the deployment succeeded or failed.
     *
     * @param status whether the deployment succeeded or failed.
     */
    public void setStatus(AssetsDeploymentStatus status) {
        this.status = status;
    }

    /**
     * Sets local installed/failed package, (will not be serialized).
     *
     * @return local installed package.
     */
    public AssetsPackage getPackage() {
        return assetsPackage;
    }

    /**
     * Gets local installed/failed package, (will not be serialized).
     *
     * @param assetsPackage local installed/failed package, (will not be serialized).
     */
    public void setPackage(AssetsPackage assetsPackage) {
        this.assetsPackage = assetsPackage;
    }
}
