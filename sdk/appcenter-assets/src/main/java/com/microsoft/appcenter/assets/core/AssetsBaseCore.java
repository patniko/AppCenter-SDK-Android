package com.microsoft.appcenter.assets.core;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsConfiguration;
import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.apirequests.DownloadPackageTask;
import com.microsoft.appcenter.assets.datacontracts.AssetsDeploymentStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPendingUpdate;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsSyncOptions;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateDialog;
import com.microsoft.appcenter.assets.enums.AssetsInstallMode;
import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;
import com.microsoft.appcenter.assets.enums.AssetsUpdateState;
import com.microsoft.appcenter.assets.exceptions.AssetsDownloadPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsGeneralException;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;
import com.microsoft.appcenter.assets.exceptions.AssetsInitializeException;
import com.microsoft.appcenter.assets.exceptions.AssetsInstallException;
import com.microsoft.appcenter.assets.exceptions.AssetsInvalidPublicKeyException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsMergeException;
import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;
import com.microsoft.appcenter.assets.exceptions.AssetsPlatformUtilsException;
import com.microsoft.appcenter.assets.exceptions.AssetsQueryUpdateException;
import com.microsoft.appcenter.assets.exceptions.AssetsReportStatusException;
import com.microsoft.appcenter.assets.exceptions.AssetsRollbackException;
import com.microsoft.appcenter.assets.exceptions.AssetsUnzipException;
import com.microsoft.appcenter.assets.interfaces.AssetsEntryPointProvider;
import com.microsoft.appcenter.assets.interfaces.AssetsBinaryVersionMismatchListener;
import com.microsoft.appcenter.assets.interfaces.AssetsConfirmationCallback;
import com.microsoft.appcenter.assets.interfaces.AssetsConfirmationDialog;
import com.microsoft.appcenter.assets.interfaces.AssetsDownloadProgressListener;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;
import com.microsoft.appcenter.assets.interfaces.AssetsPublicKeyProvider;
import com.microsoft.appcenter.assets.interfaces.AssetsRestartHandler;
import com.microsoft.appcenter.assets.interfaces.AssetsRestartListener;
import com.microsoft.appcenter.assets.interfaces.AssetsSyncStatusListener;
import com.microsoft.appcenter.assets.interfaces.DownloadProgressCallback;
import com.microsoft.appcenter.assets.managers.AssetsAcquisitionManager;
import com.microsoft.appcenter.assets.managers.AssetsRestartManager;
import com.microsoft.appcenter.assets.managers.AssetsTelemetryManager;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;
import com.microsoft.appcenter.assets.utils.AssetsUpdateUtils;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import static android.text.TextUtils.isEmpty;
import static com.microsoft.appcenter.assets.Assets.LOG_TAG;
import static com.microsoft.appcenter.assets.AssetsConstants.PACKAGE_FILE_NAME;
import static com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage.createLocalPackage;
import static com.microsoft.appcenter.assets.enums.AssetsCheckFrequency.ON_APP_START;
import static com.microsoft.appcenter.assets.enums.AssetsDeploymentStatus.SUCCEEDED;
import static com.microsoft.appcenter.assets.enums.AssetsInstallMode.IMMEDIATE;
import static com.microsoft.appcenter.assets.enums.AssetsInstallMode.ON_NEXT_RESTART;
import static com.microsoft.appcenter.assets.enums.AssetsInstallMode.ON_NEXT_RESUME;
import static com.microsoft.appcenter.assets.enums.AssetsInstallMode.ON_NEXT_SUSPEND;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.AWAITING_USER_ACTION;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.CHECKING_FOR_UPDATE;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.DOWNLOADING_PACKAGE;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.SYNC_IN_PROGRESS;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UNKNOWN_ERROR;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UPDATE_IGNORED;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UPDATE_INSTALLED;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UP_TO_DATE;
import static com.microsoft.appcenter.assets.enums.AssetsUpdateState.LATEST;
import static com.microsoft.appcenter.assets.enums.AssetsUpdateState.PENDING;
import static com.microsoft.appcenter.assets.enums.AssetsUpdateState.RUNNING;

/**
 * Base core for Assets. Singleton.
 */
public abstract class AssetsBaseCore {

    /**
     * Deployment key for checking for updates.
     */
    @SuppressWarnings("WeakerAccess")
    protected String mDeploymentKey;

    /**
     * CodePush server URL.
     */
    @SuppressWarnings("WeakerAccess")
    protected static String mServerUrl = "https://codepush.azurewebsites.net/";

    /**
     * Public key for code signing verification.
     */
    @SuppressWarnings("WeakerAccess")
    protected static String mPublicKey;

    /**
     * Entry point for application.
     */
    @SuppressWarnings("WeakerAccess")
    protected String mEntryPoint;

    /**
     * Application context.
     */
    @SuppressWarnings("WeakerAccess")
    protected final Context mContext;

    /**
     * Indicates whether application is running in debug mode.
     */
    @SuppressWarnings("WeakerAccess")
    protected final boolean mIsDebugMode;

    /**
     * Current app version.
     */
    @SuppressWarnings("WeakerAccess")
    protected String mAppVersion;

    /**
     * Current state of an update.
     */
    @SuppressWarnings("WeakerAccess")
    protected AssetsState mState;

    /**
     * Used utilities.
     */
    @SuppressWarnings("WeakerAccess")
    protected AssetsUtilities mUtilities;

    /**
     * Used managers.
     */
    @SuppressWarnings("WeakerAccess")
    protected AssetsManagers mManagers;

    /**
     * Used listeners.
     */
    @SuppressWarnings("WeakerAccess")
    protected AssetsListeners mListeners;

    /**
     * Instance of {@link AssetsConfirmationDialog}.
     */
    @SuppressWarnings("WeakerAccess")
    protected AssetsConfirmationDialog mConfirmationDialog;

    /**
     * Current instance of {@link AssetsBaseCore}.
     */
    protected static AssetsBaseCore mCurrentInstance;

    /**
     * Creates instance of {@link AssetsBaseCore}. Default constructor.
     * We pass {@link Application} and app secret here, too, because we can't initialize AppCenter in another constructor and then call this.
     * However, AppCenter must be initialized before creating anything else.
     *
     * @param deploymentKey         deployment key.
     * @param context               application context.
     * @param isDebugMode           indicates whether application is running in debug mode.
     * @param serverUrl             CodePush server url.
     * @param publicKeyProvider     instance of {@link AssetsPublicKeyProvider}.
     * @param entryPointProvider instance of {@link AssetsEntryPointProvider}.
     * @param platformUtils         instance of {@link AssetsPlatformUtils}.
     * @throws AssetsInitializeException error occurred during the initialization.
     */
    protected AssetsBaseCore(
            @NonNull String deploymentKey,
            @NonNull Context context,
            boolean isDebugMode,
            String serverUrl,
            AssetsPublicKeyProvider publicKeyProvider,
            AssetsEntryPointProvider entryPointProvider,
            AssetsPlatformUtils platformUtils
    ) throws AssetsInitializeException {

        /* Initialize configuration. */
        mDeploymentKey = deploymentKey;
        mContext = context.getApplicationContext();
        mIsDebugMode = isDebugMode;
        if (serverUrl != null) {
            mServerUrl = serverUrl;
        }
        try {
            mPublicKey = publicKeyProvider.getPublicKey();
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            mAppVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException | AssetsInvalidPublicKeyException e) {
            throw new AssetsInitializeException("Unable to get package info for " + mContext.getPackageName(), e);
        }

        /* Initialize utilities. */
        FileUtils fileUtils = FileUtils.getInstance();
        AssetsUtils utils = AssetsUtils.getInstance(fileUtils);
        AssetsUpdateUtils updateUtils = AssetsUpdateUtils.getInstance(fileUtils, utils);
        mUtilities = new AssetsUtilities(utils, fileUtils, updateUtils, platformUtils);

        /* Initialize managers. */
        String documentsDirectory = mContext.getFilesDir().getAbsolutePath();
        AssetsUpdateManager updateManager = new AssetsUpdateManager(documentsDirectory, platformUtils, fileUtils, utils, updateUtils);
        final SettingsManager settingsManager = new SettingsManager(mContext, utils);
        AssetsTelemetryManager telemetryManager = new AssetsTelemetryManager(settingsManager);
        AssetsRestartManager restartManager = new AssetsRestartManager(new AssetsRestartHandler() {
            @Override
            public void performRestart(AssetsRestartListener assetsRestartListener, boolean onlyIfUpdateIsPending) throws AssetsMalformedDataException {
                restartInternal(assetsRestartListener, onlyIfUpdateIsPending);
            }
        });
        AssetsAcquisitionManager acquisitionManager = new AssetsAcquisitionManager(utils, fileUtils);
        mManagers = new AssetsManagers(updateManager, telemetryManager, settingsManager, restartManager, acquisitionManager);
        
        /* Initializes listeners */
        mListeners = new AssetsListeners();
        
        /* Initialize state */
        mState = new AssetsState();
        try {

            /* Clear debug cache if needed. */
            if (mIsDebugMode && mManagers.mSettingsManager.isPendingUpdate(null)) {
                mUtilities.mPlatformUtils.clearDebugCache(mContext);
            }
        } catch (IOException | AssetsMalformedDataException e) {
            throw new AssetsInitializeException(e);
        }
        /* Initialize update after restart. */
        try {
            initializeUpdateAfterRestart();
        } catch (AssetsGetPackageException | AssetsPlatformUtilsException | AssetsRollbackException | AssetsGeneralException | AssetsMalformedDataException e) {
            throw new AssetsInitializeException(e);
        }
        mCurrentInstance = this;

        /* entryPointProvider.getEntryPoint() implementation for RN uses static instance on AssetsBaseCore
         * so we place it here to avoid null pointer reference. */
        try {
            mEntryPoint = entryPointProvider.getEntryPoint();
        } catch (AssetsNativeApiCallException e) {
            throw new AssetsInitializeException(e);
        }
    }

    /**
     * Adds listener for sync status change event.
     *
     * @param syncStatusListener listener for sync status change event.
     */
    public void addSyncStatusListener(AssetsSyncStatusListener syncStatusListener) {
        mListeners.mSyncStatusListeners.add(syncStatusListener);
    }

    /**
     * Adds listener for download progress change event.
     *
     * @param downloadProgressListener listener for download progress change event.
     */
    public void addDownloadProgressListener(AssetsDownloadProgressListener downloadProgressListener) {
        mListeners.mDownloadProgressListeners.add(downloadProgressListener);
    }

    /**
     * Removes listener for sync status change event.
     *
     * @param syncStatusListener listener for sync status change event.
     */
    public void removeSyncStatusListener(AssetsSyncStatusListener syncStatusListener) {
        mListeners.mSyncStatusListeners.remove(syncStatusListener);
    }

    /**
     * Removes listener for download progress change event.
     *
     * @param downloadProgressListener listener for download progress change event.
     */
    public void removeDownloadProgressListener(AssetsDownloadProgressListener downloadProgressListener) {
        mListeners.mDownloadProgressListeners.remove(downloadProgressListener);
    }

    /**
     * Gets native Assets configuration.
     *
     * @return native Assets configuration.
     */
    @SuppressWarnings("WeakerAccess")
    public AssetsConfiguration getNativeConfiguration() throws AssetsNativeApiCallException {
        AssetsConfiguration configuration = new AssetsConfiguration();
        try {
            configuration.setAppVersion(mAppVersion);

            configuration.setClientUniqueId(Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID));
            configuration.setDeploymentKey(mDeploymentKey);
            configuration.setServerUrl(mServerUrl);
            configuration.setPackageHash(mUtilities.mUpdateUtils.getHashForBinaryContents(mContext, mIsDebugMode));
        } catch (AssetsIllegalArgumentException | AssetsMalformedDataException e) {
            throw new AssetsNativeApiCallException(e);
        }
        return configuration;
    }

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches {@link AssetsUpdateState#RUNNING}.
     *
     * @return installed update metadata.
     * @throws AssetsNativeApiCallException if error occurred during the operation.
     */
    public AssetsLocalPackage getUpdateMetadata() throws AssetsNativeApiCallException {
        return getUpdateMetadata(RUNNING);
    }

    /**
     * Retrieves the metadata for an installed update (e.g. description, mandatory)
     * whose state matches the specified <code>updateState</code> parameter.
     *
     * @param updateState current update state.
     * @return installed update metadata.
     * @throws AssetsNativeApiCallException if error occurred during the operation.
     */
    @SuppressWarnings("WeakerAccess")
    public AssetsLocalPackage getUpdateMetadata(AssetsUpdateState updateState) throws AssetsNativeApiCallException {
        if (updateState == null) {
            updateState = RUNNING;
        }
        AssetsLocalPackage currentPackage;
        try {
            currentPackage = mManagers.mUpdateManager.getCurrentPackage();
        } catch (AssetsGetPackageException e) {
            throw new AssetsNativeApiCallException(e);
        }
        if (currentPackage == null) {
            return null;
        }
        Boolean currentUpdateIsPending = false;
        Boolean isDebugOnly = false;
        if (!isEmpty(currentPackage.getPackageHash())) {
            String currentHash = currentPackage.getPackageHash();
            try {
                currentUpdateIsPending = mManagers.mSettingsManager.isPendingUpdate(currentHash);
            } catch (AssetsMalformedDataException e) {
                throw new AssetsNativeApiCallException(e);
            }
        }
        if (updateState == PENDING && !currentUpdateIsPending) {

            /* The caller wanted a pending update but there isn't currently one. */
            return null;
        } else if (updateState == RUNNING && currentUpdateIsPending) {

            /* The caller wants the running update, but the current one is pending, so we need to grab the previous. */
            AssetsLocalPackage previousPackage;
            try {
                previousPackage = mManagers.mUpdateManager.getPreviousPackage();
            } catch (AssetsGetPackageException e) {
                throw new AssetsNativeApiCallException(e);
            }
            if (previousPackage == null) {
                return null;
            }
            return previousPackage;
        } else {

            /*
             * The current package satisfies the request:
             * 1) Caller wanted a pending, and there is a pending update
             * 2) Caller wanted the running update, and there isn't a pending
             * 3) Caller wants the latest update, regardless if it's pending or not
             */
            if (mState.mIsRunningBinaryVersion) {

                /*
                 * This only matters in Debug builds. Since we do not clear "outdated" updates,
                 * we need to indicate to the JS side that somehow we have a current update on
                 * disk that is not actually running.
                 */
                isDebugOnly = true;
            }

            /* Enable differentiating pending vs. non-pending updates */
            String packageHash = currentPackage.getPackageHash();
            currentPackage.setFailedInstall(existsFailedUpdate(packageHash));
            currentPackage.setFirstRun(isFirstRun(packageHash));
            currentPackage.setPending(currentUpdateIsPending);
            currentPackage.setDebugOnly(isDebugOnly);
            return currentPackage;
        }
    }

    /**
     * Gets current installed package.
     *
     * @return current installed package.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     * @deprecated use {@link #getUpdateMetadata()} instead.
     */
    @SuppressWarnings("WeakerAccess")
    public AssetsLocalPackage getCurrentPackage() throws AssetsNativeApiCallException {
        return getUpdateMetadata(LATEST);
    }

    /**
     * Asks the CodePush service whether the configured app deployment has an update available
     * using deploymentKey already set in constructor.
     *
     * @return remote package info if there is an update, <code>null</code> otherwise.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    public AssetsRemotePackage checkForUpdate() throws AssetsNativeApiCallException {
        AssetsConfiguration nativeConfiguration = getNativeConfiguration();
        return checkForUpdate(nativeConfiguration.getDeploymentKey());
    }

    /**
     * Asks the Assets service whether the configured app deployment has an update available
     * using specified deployment key.
     *
     * @param deploymentKey deployment key to use.
     * @return remote package info if there is an update, <code>null</code> otherwise.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public AssetsRemotePackage checkForUpdate(String deploymentKey) throws AssetsNativeApiCallException {
        AssetsConfiguration config = getNativeConfiguration();
        try {
            config.setDeploymentKey(deploymentKey != null ? deploymentKey : config.getDeploymentKey());
        } catch (AssetsIllegalArgumentException e) {
            throw new AssetsNativeApiCallException(e);
        }
        AssetsLocalPackage localPackage;
        localPackage = getCurrentPackage();
        AssetsLocalPackage queryPackage;
        if (localPackage == null) {
            queryPackage = AssetsLocalPackage.createEmptyPackageForCheckForUpdateQuery(config.getAppVersion());
        } else {
            queryPackage = localPackage;
        }
        AssetsRemotePackage update;
        try {
            update = new AssetsAcquisitionManager(mUtilities.mUtils, mUtilities.mFileUtils)
                    .queryUpdateWithCurrentPackage(config, queryPackage);
        } catch (AssetsQueryUpdateException e) {
            throw new AssetsNativeApiCallException(e);
        }
        if (update == null || update.isUpdateAppVersion() ||
                localPackage != null && (update.getPackageHash().equals(localPackage.getPackageHash())) ||
                (localPackage == null || localPackage.isDebugOnly()) && config.getPackageHash().equals(update.getPackageHash())) {
            if (update != null && update.isUpdateAppVersion()) {
                AppCenterLog.info(LOG_TAG, "An update is available but it is not targeting the binary version of your app.");
                notifyAboutBinaryVersionMismatchChange(update);
            }
            return null;
        } else {
            if (deploymentKey != null) {
                update.setDeploymentKey(deploymentKey);
            }
            update.setFailedInstall(existsFailedUpdate(update.getPackageHash()));
            return update;
        }
    }

    /**
     * Synchronizes your app assets with the latest release to the configured deployment using default sync options.
     *
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    public void sync() throws AssetsNativeApiCallException {
        sync(new AssetsSyncOptions());
    }

    /**
     * Synchronizes your app assets with the latest release to the configured deployment.
     *
     * @param synchronizationOptions sync options.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    public void sync(AssetsSyncOptions synchronizationOptions) throws AssetsNativeApiCallException {
        if (mState.mSyncInProgress) {
            notifyAboutSyncStatusChange(SYNC_IN_PROGRESS);
            AppCenterLog.info(Assets.LOG_TAG, "Sync already in progress.");
            return;
        }
        final AssetsSyncOptions syncOptions = synchronizationOptions == null ? new AssetsSyncOptions(mDeploymentKey) : synchronizationOptions;
        if (isEmpty(syncOptions.getDeploymentKey())) {
            syncOptions.setDeploymentKey(mDeploymentKey);
        }
        if (syncOptions.getInstallMode() == null) {
            syncOptions.setInstallMode(ON_NEXT_RESTART);
        }
        if (syncOptions.getMandatoryInstallMode() == null) {
            syncOptions.setMandatoryInstallMode(IMMEDIATE);
        }

        /* minimumBackgroundDuration, ignoreFailedUpdates are primitives and always have default value */
        if (syncOptions.getCheckFrequency() == null) {
            syncOptions.setCheckFrequency(ON_APP_START);
        }
        final AssetsConfiguration configuration = getNativeConfiguration();
        if (syncOptions.getDeploymentKey() != null) {
            try {
                configuration.setDeploymentKey(syncOptions.getDeploymentKey());
            } catch (AssetsIllegalArgumentException e) {
                throw new AssetsNativeApiCallException(e);
            }
        }
        mState.mSyncInProgress = true;
        try {
            notifyAboutSyncStatusChange(CHECKING_FOR_UPDATE);
            final AssetsRemotePackage remotePackage = checkForUpdate(syncOptions.getDeploymentKey());
            final boolean updateShouldBeIgnored =
                    remotePackage != null && (remotePackage.isFailedInstall() && syncOptions.getIgnoreFailedUpdates());
            if (remotePackage == null || updateShouldBeIgnored) {
                if (updateShouldBeIgnored) {
                    AppCenterLog.info(Assets.LOG_TAG, "An update is available, but it is being ignored due to having been previously rolled back.");
                }
                AssetsLocalPackage currentPackage = getCurrentPackage();
                if (currentPackage != null && currentPackage.isPending()) {
                    notifyAboutSyncStatusChange(UPDATE_INSTALLED);
                } else {
                    notifyAboutSyncStatusChange(UP_TO_DATE);
                }
                mState.mSyncInProgress = false;
            } else if (syncOptions.getUpdateDialog() != null) {
                final AssetsUpdateDialog updateDialogOptions = syncOptions.getUpdateDialog();
                String message;
                final String acceptButtonText;
                final String declineButtonText = updateDialogOptions.getOptionalIgnoreButtonLabel();
                if (remotePackage.isMandatory()) {
                    message = updateDialogOptions.getMandatoryUpdateMessage();
                    acceptButtonText = updateDialogOptions.getMandatoryContinueButtonLabel();
                } else {
                    message = updateDialogOptions.getOptionalUpdateMessage();
                    acceptButtonText = updateDialogOptions.getOptionalInstallButtonLabel();
                }
                if (updateDialogOptions.getAppendReleaseDescription() && !isEmpty(remotePackage.getDescription())) {
                    message = updateDialogOptions.getDescriptionPrefix() + " " + remotePackage.getDescription();
                }

                /* Ask user whether he want to install update or ignore it. */
                notifyAboutSyncStatusChange(AWAITING_USER_ACTION);
                final String finalMessage = message;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mConfirmationDialog.shouldInstallUpdate(updateDialogOptions.getTitle(), finalMessage, acceptButtonText, declineButtonText, new AssetsConfirmationCallback() {

                            @Override
                            public void onResult(boolean userAcceptsProposal) {
                                if (userAcceptsProposal) {
                                    try {
                                        doDownloadAndInstall(remotePackage, syncOptions, configuration);
                                        mState.mSyncInProgress = false;
                                    } catch (Exception e) {
                                        notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                                        mState.mSyncInProgress = false;
                                        AppCenterLog.error(Assets.LOG_TAG, e.getMessage());
                                    }
                                } else {
                                    notifyAboutSyncStatusChange(UPDATE_IGNORED);
                                    mState.mSyncInProgress = false;
                                }
                            }

                            @Override
                            public void throwError(AssetsGeneralException e) {
                                notifyAboutSyncStatusChange(UNKNOWN_ERROR);
                                mState.mSyncInProgress = false;
                                AppCenterLog.error(Assets.LOG_TAG, e.getMessage());
                            }
                        });
                    }
                });
            } else {
                doDownloadAndInstall(remotePackage, syncOptions, configuration);
                mState.mSyncInProgress = false;
            }
        } catch (Exception e) {
            notifyAboutSyncStatusChange(UNKNOWN_ERROR);
            mState.mSyncInProgress = false;
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Notifies the runtime that a freshly installed update should be considered successful,
     * and therefore, an automatic client-side rollback isn't necessary.
     *
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public void notifyApplicationReady() throws AssetsNativeApiCallException {
        mManagers.mSettingsManager.removePendingUpdate();
        final AssetsDeploymentStatusReport statusReport = getNewStatusReport();
        if (statusReport != null) {
            tryReportStatus(statusReport);
        }
    }

    /**
     * Gets instance of {@link AssetsAcquisitionManager}.
     *
     * @return instance of {@link AssetsAcquisitionManager}.
     */
    public AssetsAcquisitionManager getAcquisitionSdk() {
        return mManagers.mAcquisitionManager;
    }

    /**
     * Logs custom message on device.
     *
     * @param message message to be logged.
     */
    public void log(String message) {
        AppCenterLog.info(LOG_TAG, message);
    }

    /**
     * Performs just the restart itself.
     *
     * @param onlyIfUpdateIsPending   restart only if update is pending or unconditionally.
     * @param assetsRestartListener listener to notify that the application has restarted.
     * @return <code>true</code> if restarted successfully.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public boolean restartInternal(AssetsRestartListener assetsRestartListener, boolean onlyIfUpdateIsPending) throws AssetsMalformedDataException {

        /* If this is an unconditional restart request, or there
        * is current pending update, then reload the app. */
        if (!onlyIfUpdateIsPending || mManagers.mSettingsManager.isPendingUpdate(null)) {
            loadApp(assetsRestartListener);
            return true;
        }

        return false;
    }

    /**
     * Attempts to restart the application unconditionally (whether there is pending update is ignored).
     */
    public void restartApp() throws AssetsNativeApiCallException {
        try {
            mManagers.mRestartManager.restartApp(false);
        } catch (AssetsMalformedDataException e) {
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Attempts to restart the application.
     *
     * @param onlyIfUpdateIsPending if <code>true</code>, restart is performed only if update is pending.
     */
    public boolean restartApp(boolean onlyIfUpdateIsPending) throws AssetsNativeApiCallException {
        try {
            return mManagers.mRestartManager.restartApp(onlyIfUpdateIsPending);
        } catch (AssetsMalformedDataException e) {
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Permits restarts.
     */
    public void disallowRestart() {
        mManagers.mRestartManager.disallowRestarts();
    }

    /**
     * Allows restarts.
     */
    public void allowRestart() throws AssetsNativeApiCallException {
        try {
            mManagers.mRestartManager.allowRestarts();
        } catch (AssetsMalformedDataException e) {
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Gets current app version.
     *
     * @return current app version.
     */
    public String getAppVersion() {
        return mAppVersion;
    }

    /**
     * Sets current app version.
     *
     * @param appVersion current app version.
     */
    public void setAppVersion(String appVersion) {
        this.mAppVersion = appVersion;
    }

    /**
     * Adds listener for binary version mismatch event.
     *
     * @param listener listener for binary version mismatch event.
     */
    public void addBinaryVersionMismatchListener(AssetsBinaryVersionMismatchListener listener) {
        mListeners.mBinaryVersionMismatchListeners.add(listener);
    }

    /**
     * Notifies listeners about changed sync status and log it.
     *
     * @param syncStatus sync status.
     */
    @SuppressWarnings("WeakerAccess")
    protected void notifyAboutSyncStatusChange(AssetsSyncStatus syncStatus) {
        for (AssetsSyncStatusListener syncStatusListener : mListeners.mSyncStatusListeners) {
            syncStatusListener.syncStatusChanged(syncStatus);
        }
        switch (syncStatus) {
            case CHECKING_FOR_UPDATE: {
                AppCenterLog.info(LOG_TAG, "Checking for update.");
                break;
            }
            case AWAITING_USER_ACTION: {
                AppCenterLog.info(LOG_TAG, "Awaiting user action.");
                break;
            }
            case DOWNLOADING_PACKAGE: {
                AppCenterLog.info(LOG_TAG, "Downloading package.");
                break;
            }
            case INSTALLING_UPDATE: {
                AppCenterLog.info(LOG_TAG, "Installing update.");
                break;
            }
            case UP_TO_DATE: {
                AppCenterLog.info(LOG_TAG, "App is up to date.");
                break;
            }
            case UPDATE_IGNORED: {
                AppCenterLog.info(LOG_TAG, "User cancelled the update.");
                break;
            }
            case UPDATE_INSTALLED: {
                if (mState.mCurrentInstallModeInProgress == ON_NEXT_RESTART) {
                    AppCenterLog.info(LOG_TAG, "Update is installed and will be run on the next app restart.");
                } else if (mState.mCurrentInstallModeInProgress == ON_NEXT_SUSPEND) {
                    AppCenterLog.info(LOG_TAG, "Update is installed and will be run after the app has been in the background for at least " + mState.mMinimumBackgroundDuration + " seconds.");
                } else if (mState.mCurrentInstallModeInProgress == IMMEDIATE) {
                    AppCenterLog.info(LOG_TAG, "Update is installed and will be run right now.");
                } else if (mState.mCurrentInstallModeInProgress == ON_NEXT_RESUME) {
                    AppCenterLog.info(LOG_TAG, "Update is installed and will be run when the app next resumes.");
                }
                break;
            }
            case UNKNOWN_ERROR: {
                AppCenterLog.info(LOG_TAG, "An unknown error occurred.");
                break;
            }
        }
    }

    /**
     * Notifies listeners about changed update download progress.
     *
     * @param receivedBytes received amount of bytes.
     * @param totalBytes    total amount of bytes.
     */
    protected void notifyAboutDownloadProgressChange(long receivedBytes, long totalBytes) {
        for (AssetsDownloadProgressListener downloadProgressListener : mListeners.mDownloadProgressListeners) {
            downloadProgressListener.downloadProgressChanged(receivedBytes, totalBytes);
        }
    }

    /**
     * Notifies listeners about binary version mismatch between local and remote packages.
     *
     * @param update remote package.
     */
    @SuppressWarnings("WeakerAccess")
    protected void notifyAboutBinaryVersionMismatchChange(AssetsRemotePackage update) {
        for (AssetsBinaryVersionMismatchListener listener : mListeners.mBinaryVersionMismatchListeners) {
            listener.binaryVersionMismatchChanged(update);
        }
    }

    /**
     * Initializes update after app restart.
     *
     * @throws AssetsGetPackageException    if error occurred during the getting current package.
     * @throws AssetsPlatformUtilsException if error occurred during usage of {@link AssetsPlatformUtils}.
     * @throws AssetsRollbackException      if error occurred during rolling back of package.
     */
    @SuppressWarnings("WeakerAccess")
    protected void initializeUpdateAfterRestart() throws AssetsGetPackageException, AssetsRollbackException, AssetsPlatformUtilsException, AssetsGeneralException, AssetsMalformedDataException {

        /* Reset the state which indicates that the app was just freshly updated. */
        mState.mDidUpdate = false;
        AssetsPendingUpdate pendingUpdate = mManagers.mSettingsManager.getPendingUpdate();
        if (pendingUpdate != null) {
            AssetsLocalPackage packageMetadata = mManagers.mUpdateManager.getCurrentPackage();
            if (packageMetadata == null || !mUtilities.mPlatformUtils.isPackageLatest(packageMetadata, mAppVersion, mContext) &&
                    !mAppVersion.equals(packageMetadata.getAppVersion())) {
                AppCenterLog.info(LOG_TAG, "Skipping initializeUpdateAfterRestart(), binary version is newer.");
                return;
            }
            boolean updateIsLoading = pendingUpdate.isPendingUpdateLoading();
            if (updateIsLoading) {

                /* Pending update was initialized, but notifyApplicationReady was not called.
                 * Therefore, deduce that it is a broken update and rollback. */
                AppCenterLog.info(LOG_TAG, "Update did not finish loading the last time, rolling back to a previous version.");
                mState.mNeedToReportRollback = true;
                rollbackPackage();
            } else {

                /* There is in fact a new update running for the first
                 * time, so update the local state to ensure the client knows. */
                mState.mDidUpdate = true;

                /* Mark that we tried to initialize the new update, so that if it crashes,
                 * we will know that we need to rollback when the app next starts. */
                mManagers.mSettingsManager.savePendingUpdate(pendingUpdate);
            }
        }
    }

    /**
     * Rolls back package.
     *
     * @throws AssetsGetPackageException if error occurred during getting current update.
     * @throws AssetsRollbackException   if error occurred during rolling back of package.
     */
    private void rollbackPackage() throws AssetsGetPackageException, AssetsRollbackException, AssetsMalformedDataException {
        AssetsLocalPackage failedPackage = mManagers.mUpdateManager.getCurrentPackage();
        mManagers.mSettingsManager.saveFailedUpdate(failedPackage);
        mManagers.mUpdateManager.rollbackPackage();
        mManagers.mSettingsManager.removePendingUpdate();
    }

    /**
     * Clears any saved updates on device.
     *
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void clearUpdates() throws IOException {
        mManagers.mUpdateManager.clearUpdates();
        mManagers.mSettingsManager.removePendingUpdate();
        mManagers.mSettingsManager.removeFailedUpdates();
    }

    /**
     * Checks whether an update with the following hash has failed.
     *
     * @param packageHash hash to check.
     * @return <code>true</code> if there is a failed update with provided hash, <code>false</code> otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean existsFailedUpdate(String packageHash) throws AssetsNativeApiCallException {
        try {
            return mManagers.mSettingsManager.existsFailedUpdate(packageHash);
        } catch (AssetsMalformedDataException e) {
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Indicates whether update with specified packageHash is running for the first time.
     *
     * @param packageHash package hash for check.
     * @return true, if application is running for the first time, false otherwise.
     * @throws AssetsNativeApiCallException if error occurred during the operation.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isFirstRun(String packageHash) throws AssetsNativeApiCallException {
        try {
            return mState.mDidUpdate
                    && !isEmpty(packageHash)
                    && packageHash.equals(mManagers.mUpdateManager.getCurrentPackageHash());
        } catch (IOException | AssetsMalformedDataException e) {
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Downloads and installs update.
     *
     * @param remotePackage update to use.
     * @param syncOptions   sync options.
     * @param configuration configuration to use.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    private void doDownloadAndInstall(final AssetsRemotePackage remotePackage, final AssetsSyncOptions syncOptions, final AssetsConfiguration configuration) throws AssetsNativeApiCallException {
        notifyAboutSyncStatusChange(DOWNLOADING_PACKAGE);
        AssetsLocalPackage localPackage = downloadUpdate(remotePackage);
        try {
            mManagers.mAcquisitionManager.reportStatusDownload(configuration, localPackage);
        } catch (AssetsReportStatusException e) {
            AppCenterLog.error(Assets.LOG_TAG, e.getMessage());
        }
        AssetsInstallMode resolvedInstallMode = localPackage.isMandatory() ? syncOptions.getMandatoryInstallMode() : syncOptions.getInstallMode();
        mState.mCurrentInstallModeInProgress = resolvedInstallMode;
        notifyAboutSyncStatusChange(AssetsSyncStatus.INSTALLING_UPDATE);
        installUpdate(localPackage, resolvedInstallMode, syncOptions.getMinimumBackgroundDuration(), syncOptions.shouldRestart());
        notifyAboutSyncStatusChange(UPDATE_INSTALLED);
        mState.mSyncInProgress = false;
        if (resolvedInstallMode == IMMEDIATE && syncOptions.shouldRestart()) {
            try {
                mManagers.mRestartManager.restartApp(false);
            } catch (AssetsMalformedDataException e) {
                throw new AssetsNativeApiCallException(e);
            }
        } else {
            mManagers.mRestartManager.clearPendingRestart();
        }
    }

    /**
     * Installs update.
     *
     * @param updatePackage             update to install.
     * @param installMode               installation mode.
     * @param minimumBackgroundDuration minimum background duration value (see {@link AssetsSyncOptions#minimumBackgroundDuration}).
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public void installUpdate(final AssetsLocalPackage updatePackage, final AssetsInstallMode installMode, final int minimumBackgroundDuration, boolean shouldSavePendingUpdate) throws AssetsNativeApiCallException {
        try {
            mManagers.mUpdateManager.installPackage(updatePackage.getPackageHash(), mManagers.mSettingsManager.isPendingUpdate(null));
        } catch (AssetsInstallException | AssetsMalformedDataException e) {
            throw new AssetsNativeApiCallException(e);
        }
        String pendingHash = updatePackage.getPackageHash();
        if (pendingHash == null) {
            throw new AssetsNativeApiCallException("Update package to be installed has no hash.");
        } else if (shouldSavePendingUpdate){
            AssetsPendingUpdate pendingUpdate = new AssetsPendingUpdate();
            pendingUpdate.setPendingUpdateHash(pendingHash);
            pendingUpdate.setPendingUpdateIsLoading(false);
            mManagers.mSettingsManager.savePendingUpdate(pendingUpdate);
        }
        if (installMode == ON_NEXT_RESUME ||

                /* We also add the resume listener if the installMode is IMMEDIATE, because
                 * if the current activity is backgrounded, we want to reload the bundle when
                 * it comes back into the foreground. */
                installMode == IMMEDIATE ||
                installMode == ON_NEXT_SUSPEND) {

            /* Store the minimum duration on the native module as an instance
             * variable instead of relying on a closure below, so that any
             * subsequent resume-based installs could override it. */
            mState.mMinimumBackgroundDuration = minimumBackgroundDuration;
            handleInstallModesForUpdateInstall(installMode);
        }
    }

    /**
     * Tries to send status report.
     *
     * @param statusReport report to send.
     */
    @SuppressWarnings("WeakerAccess")
    protected void tryReportStatus(final AssetsDeploymentStatusReport statusReport) throws AssetsNativeApiCallException {
        try {
            AssetsConfiguration configuration = getNativeConfiguration();
            if (!isEmpty(statusReport.getAppVersion())) {
                AppCenterLog.info(Assets.LOG_TAG, "Reporting binary update (" + statusReport.getAppVersion() + ")");
                mManagers.mAcquisitionManager.reportStatusDeploy(configuration, statusReport);
            } else {
                String label = statusReport.getPackage() != null ? statusReport.getPackage().getLabel() : statusReport.getLabel();
                if (statusReport.getStatus().equals(SUCCEEDED)) {
                    AppCenterLog.info(Assets.LOG_TAG, "Reporting update success (" + label + ")");
                } else {
                    AppCenterLog.info(Assets.LOG_TAG, "Reporting update rollback (" + label + ")");
                }
                configuration.setDeploymentKey(statusReport.getPackage() == null ? statusReport.getDeploymentKey() : statusReport.getPackage().getDeploymentKey());
                mManagers.mAcquisitionManager.reportStatusDeploy(configuration, statusReport);
            }
            saveReportedStatus(statusReport);
        } catch (AssetsReportStatusException | AssetsIllegalArgumentException e) {

            /* In order to do not lose original exception if another one will be thrown during the retry
             * we need to wrap it */
            AssetsNativeApiCallException exceptionToThrow = new AssetsNativeApiCallException(e);
            try {
                retrySendStatusReport(statusReport);
            } catch (AssetsNativeApiCallException retryException) {
                exceptionToThrow = new AssetsNativeApiCallException(exceptionToThrow);
            }
            throw exceptionToThrow;
        }

        /* If there was several attempts to send error reports */
        clearScheduledAttemptsToRetrySendStatusReport();
    }

    /**
     * Attempts to retry sending status report if there was sending error before.
     *
     * @param statusReport status report.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    private void retrySendStatusReport(AssetsDeploymentStatusReport statusReport) throws AssetsNativeApiCallException {

        /* Try again when the app resumes */
        AppCenterLog.info(Assets.LOG_TAG, "Report status failed: " + mUtilities.mUtils.convertObjectToJsonString(statusReport));
        saveStatusReportForRetry(statusReport);
        Callable<Void> sender = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final AssetsDeploymentStatusReport statusReport = getNewStatusReport();
                if (statusReport != null) {
                    tryReportStatus(statusReport);
                }
                return null;
            }
        };
        try {
            retrySendStatusReportOnAppResume(sender);
        } catch (Exception e) {
            throw new AssetsNativeApiCallException("Error retry sending status report. ", e);
        }
    }

    /**
     * Retrieves status report for sending.
     *
     * @return status report for sending.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public AssetsDeploymentStatusReport getNewStatusReport() throws AssetsNativeApiCallException {
        if (mState.mNeedToReportRollback) {
            mState.mNeedToReportRollback = false;
            try {
                ArrayList<AssetsPackage> failedUpdates = mManagers.mSettingsManager.getFailedUpdates();
                if (failedUpdates != null && failedUpdates.size() > 0) {
                    AssetsPackage lastFailedPackage = failedUpdates.get(failedUpdates.size() - 1);
                    AssetsDeploymentStatusReport failedStatusReport = mManagers.mTelemetryManager.buildRollbackReport(lastFailedPackage);
                    if (failedStatusReport != null) {
                        return failedStatusReport;
                    }
                }
            } catch (AssetsMalformedDataException e) {
                throw new AssetsNativeApiCallException(e);
            }
        } else if (mState.mDidUpdate) {
            AssetsLocalPackage currentPackage;
            try {
                currentPackage = mManagers.mUpdateManager.getCurrentPackage();
            } catch (AssetsGetPackageException e) {
                throw new AssetsNativeApiCallException(e);
            }
            if (currentPackage != null) {
                try {
                    AssetsDeploymentStatusReport newPackageStatusReport =
                            mManagers.mTelemetryManager.buildUpdateReport(currentPackage);
                    if (newPackageStatusReport != null) {
                        return newPackageStatusReport;
                    }
                } catch (AssetsIllegalArgumentException e) {
                    throw new AssetsNativeApiCallException(e);
                }
            }
        } else if (mState.mIsRunningBinaryVersion) {
            try {
                AssetsDeploymentStatusReport newAppVersionStatusReport = mManagers.mTelemetryManager.buildBinaryUpdateReport(mAppVersion);
                if (newAppVersionStatusReport != null) {
                    return newAppVersionStatusReport;
                }
            } catch (AssetsIllegalArgumentException e) {
                throw new AssetsNativeApiCallException(e);
            }
        } else {
            AssetsDeploymentStatusReport retryStatusReport;
            try {
                retryStatusReport = mManagers.mSettingsManager.getStatusReportSavedForRetry();
            } catch (JSONException e) {
                throw new AssetsNativeApiCallException(e);
            }
            if (retryStatusReport != null) {
                return retryStatusReport;
            }
        }
        return null;
    }

    /**
     * Saves already sent status report.
     *
     * @param statusReport report to save.
     */
    @SuppressWarnings("WeakerAccess")
    public void saveReportedStatus(AssetsDeploymentStatusReport statusReport) {
        mManagers.mTelemetryManager.saveReportedStatus(statusReport);
    }

    /**
     * Saves status report for further retry os it's sending.
     *
     * @param statusReport status report.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public void saveStatusReportForRetry(AssetsDeploymentStatusReport statusReport) throws AssetsNativeApiCallException {
        try {
            mManagers.mSettingsManager.saveStatusReportForRetry(statusReport);
        } catch (JSONException e) {
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Downloads update.
     *
     * @param updatePackage update to download.
     * @return resulted local package.
     * @throws AssetsNativeApiCallException if error occurred during the execution of operation.
     */
    @SuppressWarnings("WeakerAccess")
    public AssetsLocalPackage downloadUpdate(final AssetsRemotePackage updatePackage) throws AssetsNativeApiCallException {
        try {
            String binaryModifiedTime = "" + mUtilities.mPlatformUtils.getBinaryResourcesModifiedTime(mContext);
            String entryPoint = null;
            String downloadUrl = updatePackage.getDownloadUrl();
            File downloadFile = mManagers.mUpdateManager.getPackageDownloadFile();
            DownloadPackageTask downloadTask = new DownloadPackageTask(mUtilities.mFileUtils, downloadUrl, downloadFile, getDownloadProgressCallbackForUpdateDownload());
            ApiHttpRequest<AssetsDownloadPackageResult> downloadRequest = new ApiHttpRequest<>(downloadTask);
            AssetsDownloadPackageResult downloadPackageResult = mManagers.mUpdateManager.downloadPackage(updatePackage.getPackageHash(), downloadRequest);
            boolean isZip = downloadPackageResult.isZip();
            String newUpdateFolderPath = mManagers.mUpdateManager.getPackageFolderPath(updatePackage.getPackageHash());
            String newUpdateMetadataPath = mUtilities.mFileUtils.appendPathComponent(newUpdateFolderPath, PACKAGE_FILE_NAME);
            if (isZip) {
                mManagers.mUpdateManager.unzipPackage(downloadFile);
                entryPoint = mManagers.mUpdateManager.mergeDiff(newUpdateFolderPath, newUpdateMetadataPath, updatePackage.getPackageHash(), mPublicKey, mEntryPoint);
            } else {
                mUtilities.mFileUtils.moveFile(downloadFile, new File(newUpdateFolderPath), mEntryPoint);
            }
            AssetsLocalPackage newPackage = createLocalPackage(false, false, true, false, entryPoint, updatePackage);
            newPackage.setBinaryModifiedTime(binaryModifiedTime);
            mUtilities.mUtils.writeObjectToJsonFile(newPackage, newUpdateMetadataPath);
            return newPackage;
        } catch (IOException | AssetsDownloadPackageException | AssetsUnzipException | AssetsMergeException e) {
            try {
                mManagers.mSettingsManager.saveFailedUpdate(updatePackage);
            } catch (AssetsMalformedDataException ex) {
                throw new AssetsNativeApiCallException(ex);
            }
            throw new AssetsNativeApiCallException(e);
        }
    }

    /**
     * Gets current package update entry point.
     * @return path to update contents.
     */
    public String getCurrentUpdateEntryPoint() throws AssetsGetPackageException, IOException {
        return mManagers.mUpdateManager.getCurrentUpdatePath(mEntryPoint);
    }

    /**
     * Removes pending update.
     */
    public void removePendingUpdate() {
        mManagers.mSettingsManager.removePendingUpdate();
    }

    /**
     * Returns instance of {@link AssetsRestartManager}.
     *
     * @return instance of {@link AssetsRestartManager}.
     */
    public AssetsRestartManager getRestartManager() {
        return mManagers.mRestartManager;
    }

    /**
     * Returns whether application is running in debug mode.
     *
     * @return whether application is running in debug mode.
     */
    public boolean isDebugMode() {
        return mIsDebugMode;
    }

    /**
     * Gets {@link DownloadProgressCallback} for update downloading that could be used for platform-specific actions.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract DownloadProgressCallback getDownloadProgressCallbackForUpdateDownload();

    /**
     * Performs all work needed to be done on native side to support install modes but {@link AssetsInstallMode#ON_NEXT_RESTART}.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract void handleInstallModesForUpdateInstall(AssetsInstallMode installMode);

    /**
     * Removes pending updates information.
     * Retries to send status report on app resume using platform-specific way for it.
     * Use <code>sender.call()</code> to invoke sending of report.
     *
     * @param sender task that sends status report.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract void retrySendStatusReportOnAppResume(Callable<Void> sender);

    /**
     * Clears any scheduled attempts to retry send status report.
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract void clearScheduledAttemptsToRetrySendStatusReport();

    /**
     * Sets the actual confirmation dialog to use.
     *
     * @param dialog instance of {@link AssetsConfirmationDialog}.
     */
    protected abstract void setConfirmationDialog(AssetsConfirmationDialog dialog);

    /**
     * Loads application.
     *
     * @param assetsRestartListener listener to notify that the app is loaded.
     */
    protected abstract void loadApp(AssetsRestartListener assetsRestartListener);
}
