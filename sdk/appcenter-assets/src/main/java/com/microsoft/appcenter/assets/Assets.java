package com.microsoft.appcenter.assets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.assets.core.AssetsAndroidCore;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsSyncOptions;
import com.microsoft.appcenter.assets.enums.AssetsUpdateState;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsInitializeException;
import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;
import com.microsoft.appcenter.assets.interfaces.AssetsDownloadProgressListener;
import com.microsoft.appcenter.assets.interfaces.AssetsSyncStatusListener;
import com.microsoft.appcenter.assets.utils.AndroidUtils;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import java.io.IOException;

/**
 * Assets service.
 */
public class Assets extends AbstractAppCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Assets";

    /**
     * TAG used in logging for Assets.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Application deployment key (<code>null</code> if request to build the api
     * has not yet been made.
     */
    private static String mDeploymentKey;

    /**
     * Application context.
     */
    private static Context mContext;

    /**
     * An instance of {@link AppCenterFuture} containing a pending request to retrieve the builder.
     * <code>null</code> if there is no request.
     */
    private static DefaultAppCenterFuture<AssetsBuilder> builderFuture;

    /**
     * Group for sending logs.
     */
    @VisibleForTesting
    static final String ERROR_GROUP = "groupErrors";

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    protected String getGroupName() {
        return ERROR_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    /**
     * Singleton.
     */
    @SuppressLint("StaticFieldLeak")
    private static Assets sInstance = null;

    @NonNull
    public static synchronized Assets getInstance() {
        if (sInstance == null) {
            sInstance = new Assets();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    @Override public boolean isAppSecretRequired() {
        return false;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, String appSecret, String transmissionTargetToken, @NonNull Channel channel) {
        super.onStarted(context, appSecret, transmissionTargetToken, channel);
        mContext = context;
        if (builderFuture != null) {
            builderFuture.complete(new AssetsBuilder(mDeploymentKey, context, getInstance()));
        }
    }

    /**
     * Creates a builder to get access to all API methods.
     *
     * @param deploymentKey application deployment key.
     * @return instance of {@link AssetsBuilder}.
     */
    public static AppCenterFuture<AssetsBuilder> getBuilder(String deploymentKey) {
        mDeploymentKey = deploymentKey;
        builderFuture = new DefaultAppCenterFuture<>();

        /* If the service is already started, resolve right away. */
        if (mContext != null) {
            builderFuture.complete(new AssetsBuilder(mDeploymentKey, mContext, getInstance()));
        }
        return builderFuture;
    }

    /**
     * Exposed android API.
     */
    public class AssetsAPI {

        /**
         * Instance of {@link AssetsAndroidCore}.
         */
        private AssetsAndroidCore mAndroidCore;

        /**
         * Creates instance of {@link AssetsAPI}.
         *
         * @param deploymentKey               application deployment key.
         * @param context                     application context.
         * @param isDebugMode                 whether the application is running in debug mode.
         * @param serverUrl                   CodePush server url.
         * @param publicKeyResourceDescriptor public-key related resource descriptor.
         * @param updateEntryPoint            path to the update contents inside of the package.
         * @throws AssetsInitializeException initialization exception.
         */
        AssetsAPI(
                @NonNull String deploymentKey,
                @NonNull Context context,
                boolean isDebugMode,
                @Nullable String serverUrl,
                @Nullable Integer publicKeyResourceDescriptor,
                @Nullable String updateEntryPoint
        ) throws AssetsInitializeException {
            try {
                mAndroidCore = new AssetsAndroidCore(
                        deploymentKey,
                        context,
                        isDebugMode,
                        serverUrl,
                        new AssetsAndroidPublicKeyProvider(publicKeyResourceDescriptor, context),
                        new AssetsAndroidEntryPointProvider(updateEntryPoint),
                        AndroidUtils.getInstance());
            } catch (AssetsInitializeException e) {
                AppCenterLog.error(Assets.LOG_TAG, e.getMessage());
                throw e;
            }
        }

        /**
         * Gets an update directory.
         *
         * @return path to the directory containing the updates.
         */
        public String getCurrentUpdateEntryPoint() {
            try {
                return mAndroidCore.getCurrentUpdateEntryPoint();
            } catch (AssetsGetPackageException | IOException e) {
                AppCenterLog.error(Assets.LOG_TAG, e.getMessage());
                return null;
            }
        }

        /**
         * Gets native Assets configuration.
         *
         * @return native Assets configuration.
         */
        public AssetsConfiguration getConfiguration() {
            try {
                return mAndroidCore.getNativeConfiguration();
            } catch (AssetsNativeApiCallException e) {
                AppCenterLog.error(Assets.LOG_TAG, e.getMessage());
                return null;
            }
        }

        /**
         * Asks the CodePush service whether the configured app deployment has an update available
         * using deploymentKey already set in constructor.
         *
         * @return remote package info if there is an update, <code>null</code> otherwise.
         */
        public AppCenterFuture<AssetsRemotePackage> checkForUpdate() {
            final DefaultAppCenterFuture<AssetsRemotePackage> future = new DefaultAppCenterFuture<>();

            postAsyncGetter(new Runnable() {

                @Override
                public void run() {
                    try {
                        AssetsRemotePackage remotePackage = mAndroidCore.checkForUpdate();
                        future.complete(remotePackage);
                    } catch (AssetsNativeApiCallException e) {
                        AppCenterLog.error(LOG_TAG, e.getMessage());
                    }
                }
            }, future, null);
            return future;

        }

        /**
         * Asks the CodePush service whether the configured app deployment has an update available
         * using specified deployment key.
         *
         * @param deploymentKey deployment key to use.
         * @return remote package info if there is an update, <code>null</code> otherwise.
         */
        public AppCenterFuture<AssetsRemotePackage> checkForUpdate(final String deploymentKey) {
            final DefaultAppCenterFuture<AssetsRemotePackage> future = new DefaultAppCenterFuture<>();
            postAsyncGetter(new Runnable() {

                @Override
                public void run() {
                    try {
                        AssetsRemotePackage remotePackage = mAndroidCore.checkForUpdate(deploymentKey);
                        future.complete(remotePackage);
                    } catch (AssetsNativeApiCallException e) {
                        AppCenterLog.error(LOG_TAG, e.getMessage());
                    }
                }
            }, future, null);
            return future;
        }

        /**
         * Retrieves the metadata for an installed update (e.g. description, mandatory)
         * whose state matches the specified <code>updateState</code> parameter.
         *
         * @param updateState current update state.
         * @return installed update metadata.
         */
        public AppCenterFuture<AssetsLocalPackage> getUpdateMetadata(final AssetsUpdateState updateState) {
            final DefaultAppCenterFuture<AssetsLocalPackage> future = new DefaultAppCenterFuture<>();
            postAsyncGetter(new Runnable() {

                @Override
                public void run() {
                    try {
                        AssetsLocalPackage assetsLocalPackage = mAndroidCore.getUpdateMetadata(updateState);
                        future.complete(assetsLocalPackage);
                    } catch (AssetsNativeApiCallException e) {
                        AppCenterLog.error(LOG_TAG, e.getMessage());
                    }
                }
            }, future, null);
            return future;
        }

        /**
         * Clears all the updates and resets to binary.
         */
        public void clearUpdates() {
            try {
                mAndroidCore.clearUpdates();
            } catch (IOException e) {
                AppCenterLog.error(LOG_TAG, e.getMessage());
            }
        }

        /**
         * Retrieves the metadata for an installed update (e.g. description, mandatory)
         * whose state matches {@link AssetsUpdateState#RUNNING}.
         *
         * @return installed update metadata.
         */
        public AppCenterFuture<AssetsLocalPackage> getUpdateMetadata() {
            final DefaultAppCenterFuture<AssetsLocalPackage> future = new DefaultAppCenterFuture<>();
            postAsyncGetter(new Runnable() {

                @Override
                public void run() {
                    try {
                        AssetsLocalPackage assetsLocalPackage = mAndroidCore.getUpdateMetadata(AssetsUpdateState.RUNNING);
                        future.complete(assetsLocalPackage);
                    } catch (AssetsNativeApiCallException e) {
                        AppCenterLog.error(LOG_TAG, e.getMessage());
                    }
                }
            }, future, null);
            return future;
        }

        /**
         * Synchronizes your app assets with the latest release to the configured deployment using default sync options.
         */
        public AppCenterFuture<Void> sync() {
            final DefaultAppCenterFuture<Void> future = new DefaultAppCenterFuture<>();
            postAsyncGetter(new Runnable() {

                @Override
                public void run() {
                    try {
                        mAndroidCore.sync();
                        future.complete(null);
                    } catch (AssetsNativeApiCallException e) {
                        AppCenterLog.error(LOG_TAG, e.getMessage());
                    }
                }
            }, future, null);
            return future;
        }

        /**
         * Synchronizes your app assets with the latest release to the configured deployment.
         *
         * @param syncOptions sync options.
         */
        public AppCenterFuture<Void> sync(final AssetsSyncOptions syncOptions) {
            final DefaultAppCenterFuture<Void> future = new DefaultAppCenterFuture<>();
            postAsyncGetter(new Runnable() {

                @Override
                public void run() {
                    try {
                        mAndroidCore.sync(syncOptions);
                        future.complete(null);
                    } catch (AssetsNativeApiCallException e) {
                        AppCenterLog.error(LOG_TAG, e.getMessage());
                    }
                }
            }, future, null);
            return future;
        }

        /**
         * Adds listener for sync status change event.
         *
         * @param syncStatusListener listener for sync status change event.
         */
        public void addSyncStatusListener(AssetsSyncStatusListener syncStatusListener) {
            mAndroidCore.addSyncStatusListener(syncStatusListener);
        }

        /**
         * Adds listener for download progress change event.
         *
         * @param downloadProgressListener listener for download progress change event.
         */
        public void addDownloadProgressListener(AssetsDownloadProgressListener downloadProgressListener) {
            mAndroidCore.addDownloadProgressListener(downloadProgressListener);
        }

        /**
         * Removes listener for sync status change event.
         *
         * @param syncStatusListener listener for sync status change event.
         */
        public void removeSyncStatusListener(AssetsSyncStatusListener syncStatusListener) {
            mAndroidCore.removeSyncStatusListener(syncStatusListener);
        }

        /**
         * Removes listener for download progress change event.
         *
         * @param downloadProgressListener listener for download progress change event.
         */
        public void removeDownloadProgressListener(AssetsDownloadProgressListener downloadProgressListener) {
            mAndroidCore.removeDownloadProgressListener(downloadProgressListener);
        }
    }
}
