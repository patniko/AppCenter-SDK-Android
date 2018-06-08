package com.microsoft.appcenter.assets;

import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;

/**
 * Provides info regarding current app state and settings.
 */
public final class AssetsConfiguration {

    /**
     * Value of <code>versionName</code> parameter from <code>build.gradle</code>.
     */
    private String appVersion;

    /**
     * Android client unique id.
     */
    private String clientUniqueId;

    /**
     * CodePush deployment key.
     */
    private String deploymentKey;

    /**
     * CodePush acquisition server URL.
     */
    private String serverUrl;

    /**
     * Package hash of currently running update.
     * See {@link com.microsoft.appcenter.assets.enums.AssetsUpdateState} for details.
     */
    private String packageHash;

    /**
     * Get the appVersion value.
     *
     * @return appVersion value.
     */
    public String getAppVersion() {
        return this.appVersion;
    }

    /**
     * Get the clientUniqueId value.
     *
     * @return the clientUniqueId value.
     */
    public String getClientUniqueId() {
        return this.clientUniqueId;
    }

    /**
     * Get the deploymentKey value.
     *
     * @return the deploymentKey value.
     */
    public String getDeploymentKey() {
        return this.deploymentKey;
    }

    /**
     * Get the serverUrl value.
     *
     * @return the serverUrl value.
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * Get the packageHash value.
     *
     * @return the packageHash value.
     */
    public String getPackageHash() {
        return this.packageHash;
    }

    /**
     * Set the appVersion value.
     *
     * @param appVersion the appVersion value to set.
     * @return this instance.
     */
    public AssetsConfiguration setAppVersion(final String appVersion) throws AssetsIllegalArgumentException {
        if (appVersion != null) {
            this.appVersion = appVersion;
        } else {
            throw new AssetsIllegalArgumentException(this.getClass().getName(), "appVersion");
        }
        return this;
    }

    /**
     * Set the clientUniqueId value.
     *
     * @param clientUniqueId the clientUniqueId value to set.
     * @return this instance.
     */
    public AssetsConfiguration setClientUniqueId(final String clientUniqueId) throws AssetsIllegalArgumentException {
        if (clientUniqueId != null) {
            this.clientUniqueId = clientUniqueId;
        } else {
            throw new AssetsIllegalArgumentException(this.getClass().getName(), "clientUniqueId");
        }
        return this;
    }

    /**
     * Set the deploymentKey value.
     *
     * @param deploymentKey the deploymentKey value to set.
     * @return this instance.
     */
    public AssetsConfiguration setDeploymentKey(final String deploymentKey) throws AssetsIllegalArgumentException {
        if (deploymentKey != null) {
            this.deploymentKey = deploymentKey;
        } else {
            throw new AssetsIllegalArgumentException(this.getClass().getName(), "deploymentKey");
        }
        return this;
    }

    /**
     * Set the serverUrl value.
     *
     * @param serverUrl the serverUrl value to set.
     * @return this instance.
     */
    public AssetsConfiguration setServerUrl(final String serverUrl) throws AssetsIllegalArgumentException {
        if (serverUrl != null) {
            this.serverUrl = serverUrl;
        } else {
            throw new AssetsIllegalArgumentException(this.getClass().getName(), "serverUrl");
        }
        return this;
    }

    /**
     * Set the packageHash value.
     *
     * @param packageHash the serverUrl value to set.
     * @return this instance.
     */
    public AssetsConfiguration setPackageHash(final String packageHash) {
        this.packageHash = packageHash;
        return this;
    }
}