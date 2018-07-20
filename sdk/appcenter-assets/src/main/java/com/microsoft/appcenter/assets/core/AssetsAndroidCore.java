package com.microsoft.appcenter.assets.core;

import android.content.Context;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.enums.AssetsInstallMode;
import com.microsoft.appcenter.assets.exceptions.AssetsInitializeException;
import com.microsoft.appcenter.assets.interfaces.AssetsConfirmationDialog;
import com.microsoft.appcenter.assets.interfaces.AssetsEntryPointProvider;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;
import com.microsoft.appcenter.assets.interfaces.AssetsPublicKeyProvider;
import com.microsoft.appcenter.assets.interfaces.AssetsRestartListener;
import com.microsoft.appcenter.assets.interfaces.DownloadProgressCallback;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.concurrent.Callable;

/**
 * Android-specific instance of {@link AssetsBaseCore}.
 */
public class AssetsAndroidCore extends AssetsBaseCore {

    /**
     * Creates instance of the {@link AssetsAndroidCore}. Default constructor.
     *
     * @param deploymentKey      application deployment key.
     * @param context            application context.
     * @param isDebugMode        indicates whether application is running in debug mode.
     * @param serverUrl          CodePush server url.
     * @param publicKeyProvider  instance of {@link AssetsPublicKeyProvider}.
     * @param entryPointProvider instance of {@link AssetsEntryPointProvider}.
     * @param platformUtils      instance of {@link AssetsPlatformUtils}.
     * @param appName            application name.
     * @param appVersion         application version to be overridden.
     * @param baseDirectory      directory to be set as base instead of files dir, or <code>null</code>.
     * @throws AssetsInitializeException error occurred during the initialization.
     */
    public AssetsAndroidCore(
            @NonNull String deploymentKey,
            @NonNull Context context,
            boolean isDebugMode,
            String serverUrl,
            AssetsPublicKeyProvider publicKeyProvider,
            AssetsEntryPointProvider entryPointProvider,
            AssetsPlatformUtils platformUtils,
            String appVersion,
            String appName,
            String baseDirectory
    ) throws AssetsInitializeException {
        super(deploymentKey, context, isDebugMode, serverUrl, publicKeyProvider, entryPointProvider, platformUtils, appVersion, appName, baseDirectory);
    }

    @Override protected DownloadProgressCallback getDownloadProgressCallbackForUpdateDownload() {
        return null;
    }

    @Override protected void handleInstallModesForUpdateInstall(AssetsInstallMode installMode) {

    }

    @Override protected void retrySendStatusReportOnAppResume(Callable<Void> sender) {

    }

    @Override protected void clearScheduledAttemptsToRetrySendStatusReport() {

    }

    @Override protected void setConfirmationDialog(AssetsConfirmationDialog dialog) {
        mConfirmationDialog = dialog;
    }

    @Override protected void loadApp(AssetsRestartListener assetsRestartListener) {
        if (assetsRestartListener != null) {
            try {
                assetsRestartListener.onRestartFinished();
            } catch (Exception e) {
                AppCenterLog.error(Assets.LOG_TAG, e.getMessage());
            }
        }
    }
}
