package com.microsoft.appcenter.assets;

import android.content.Context;

import com.microsoft.appcenter.assets.exceptions.AssetsInitializeException;

/**
 * A builder for {@link Assets.AssetsAPI} class.
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
     * Public-key related resource descriptor.
     */
    private Integer mPublicKeyResourceDescriptor;

    /**
     * Path to the update entry folder.
     */
    private String mUpdateSubFolder;

    /**
     * Instance of the parent service with the inner api class.
     */
    private Assets mParentInstance;

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
     * Sets public-key related resource descriptor.
     *
     * @param publicKeyResourceDescriptor public-key related resource descriptor.
     * @return instance of {@link AssetsBuilder}.
     */
    public AssetsBuilder setPublicKeyResourceDescriptor(Integer publicKeyResourceDescriptor) {
        mPublicKeyResourceDescriptor = publicKeyResourceDescriptor;
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
     * Builds {@link Assets.AssetsAPI}.
     *
     * @return instance of {@link Assets.AssetsAPI}.
     * @throws AssetsInitializeException initialization exception.
     */
    public Assets.AssetsAPI build() throws AssetsInitializeException {
        return mParentInstance.new AssetsAPI(
                this.mDeploymentKey,
                this.mContext,
                this.mIsDebugMode,
                this.mServerUrl,
                this.mPublicKeyResourceDescriptor,
                this.mUpdateSubFolder
        );
    }
}
