package com.microsoft.appcenter.assets;

import android.content.Context;

import com.microsoft.appcenter.assets.exceptions.AssetsInitializeException;

/**
 * A builder for {@link Assets.AssetsDeploymentInstance} class.
 */
public class AssetsBuilder {

    /**
     * Application deployment key.
     */
    private String mDeploymentKey;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Whether the application is running in debug mode.
     */
    private boolean mIsDebugMode;

    /**
     * CodePush server URL.
     */
    private String mServerUrl;

    /**
     * Public key for signed updates.
     */
    private String mPublicKey;

    /**
     * Path to the update entry folder.
     */
    private String mUpdateSubFolder;

    /**
     * Instance of the parent service with the inner api class.
     */
    private Assets mParentInstance;

    /**
     * App name for use when utilizing multiple CodePush instances to differentiate file locations.
     * If not provided, defaults to {@link AssetsConstants#ASSETS_DEFAULT_APP_NAME}.
     */
    private String mAppName;

    /**
     * Semantic version for app for use when getting updates.
     * If not provided, defaults to <code>versionName</code> field from <code>build.gradle</code>.
     */
    private String mAppVersion;

    /**
     * Base directory for CodePush files.
     * If not provided, defaults to /data/data/<package>/files ({@link Context#getFilesDir()}).
     */
    private String mBaseDirectory;

    /**
     * Creates a builder with initial parameters.
     *
     * @param deploymentKey application deployment key.
     * @param context       application context.
     */
    public AssetsBuilder(String deploymentKey, Context context, Assets parentInstance) {
        mDeploymentKey = deploymentKey;
        mContext = context;
        mParentInstance = parentInstance;
    }

    /**
     * Sets whether application is running in debug mode.
     *
     * @param isDebugMode whether application is running in debug mode.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setIsDebugMode(boolean isDebugMode) {
        mIsDebugMode = isDebugMode;
        return this;
    }

    /**
     * Sets name of application.
     *
     * @param appName name of application.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setAppName(String appName) {
        mAppName = appName;
        return this;
    }

    /**
     * Sets version of application.
     *
     * @param appVersion semantic version of application.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setAppVersion(String appVersion) {
        mAppVersion = appVersion;
        return this;
    }

    /**
     * Sets base directory for CodePush files.
     *
     * @param baseDirectory base directory for CodePush instance.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setBaseDir(String baseDirectory) {
        mBaseDirectory = baseDirectory;
        return this;
    }

    /**
     * Sets CodePush server URL.
     *
     * @param serverUrl CodePush server URL.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setServerUrl(String serverUrl) {
        mServerUrl = serverUrl;
        return this;
    }

    /**
     * Sets public key for signed updates.
     *
     * @param publicKey public key for signed updates.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setPublicKey(String publicKey) {
        mPublicKey = publicKey;
        return this;
    }

    /**
     * Sets path to the application entry point.
     *
     * @param updateSubFolder path to the application entry point.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setUpdateSubFolder(String updateSubFolder) {
        mUpdateSubFolder = updateSubFolder;
        return this;
    }

    /**
     * Builds {@link Assets.AssetsDeploymentInstance}.
     *
     * @return instance of {@link Assets.AssetsDeploymentInstance}.
     * @throws AssetsInitializeException initialization exception.
     */
    public Assets.AssetsDeploymentInstance build() throws AssetsInitializeException {
        return mParentInstance.new AssetsDeploymentInstance(
                this.mDeploymentKey,
                this.mContext,
                this.mIsDebugMode,
                this.mServerUrl,
                this.mPublicKey,
                this.mUpdateSubFolder,
                this.mAppVersion,
                this.mAppName,
                this.mBaseDirectory
        );
    }
}
