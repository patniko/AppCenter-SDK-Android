package com.microsoft.appcenter.assets.managers;

import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsConfiguration;
import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.apirequests.CheckForUpdateTask;
import com.microsoft.appcenter.assets.apirequests.ReportStatusTask;
import com.microsoft.appcenter.assets.datacontracts.AssetsDeploymentStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsReportStatusResult;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateRequest;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateResponse;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateResponseUpdateInfo;
import com.microsoft.appcenter.assets.exceptions.AssetsApiHttpRequestException;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsQueryUpdateException;
import com.microsoft.appcenter.assets.exceptions.AssetsReportStatusException;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Locale;

import static com.microsoft.appcenter.assets.Assets.LOG_TAG;
import static com.microsoft.appcenter.assets.enums.ReportType.DEPLOY;
import static com.microsoft.appcenter.assets.enums.ReportType.DOWNLOAD;

public class AssetsAcquisitionManager {

    /**
     * Endpoint for sending {@link AssetsDownloadStatusReport}.
     */
    final private static String REPORT_DOWNLOAD_STATUS_ENDPOINT = "reportStatus/download";

    /**
     * Endpoint for sending {@link AssetsDeploymentStatusReport}.
     */
    final private static String REPORT_DEPLOYMENT_STATUS_ENDPOINT = "reportStatus/deploy";

    /**
     * Query updates string pattern.
     */
    final private static String UPDATE_CHECK_ENDPOINT = "updateCheck?%s";

    /**
     * Instance of {@link AssetsUtils} to work with.
     */
    private AssetsUtils mAssetsUtils;

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    /**
     * Creates an instance of {@link AssetsAcquisitionManager}.
     *
     * @param assetsUtils instance of {@link AssetsUtils} to work with.
     * @param fileUtils     instance of {@link FileUtils} to work with.
     */
    public AssetsAcquisitionManager(AssetsUtils assetsUtils, FileUtils fileUtils) {
        mAssetsUtils = assetsUtils;
        mFileUtils = fileUtils;
    }

    private String fixServerUrl(String serverUrl) {
        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        return serverUrl;
    }

    /**
     * Sends a request to server for updates of the current package.
     *
     * @param configuration  current application configuration.
     * @param currentPackage instance of {@link AssetsLocalPackage}.
     * @return {@link AssetsRemotePackage} or <code>null</code> if there is no update.
     * @throws AssetsQueryUpdateException exception occurred during querying for update.
     */
    public AssetsRemotePackage queryUpdateWithCurrentPackage(AssetsConfiguration configuration, AssetsLocalPackage currentPackage) throws AssetsQueryUpdateException {
        if (currentPackage == null || currentPackage.getAppVersion() == null || currentPackage.getAppVersion().isEmpty()) {
            throw new AssetsQueryUpdateException("Calling common acquisition SDK with incorrect package");
        }

        /* Extract parameters from configuration */
        String serverUrl = fixServerUrl(configuration.getServerUrl());
        String deploymentKey = configuration.getDeploymentKey();
        String clientUniqueId = configuration.getClientUniqueId();
        try {
            AssetsUpdateRequest updateRequest = AssetsUpdateRequest.createUpdateRequest(deploymentKey, currentPackage, clientUniqueId);
            final String requestUrl = serverUrl + String.format(Locale.getDefault(), UPDATE_CHECK_ENDPOINT, mAssetsUtils.getQueryStringFromObject(updateRequest, "UTF-8"));
            CheckForUpdateTask checkForUpdateTask = new CheckForUpdateTask(mFileUtils, mAssetsUtils, requestUrl);
            ApiHttpRequest<AssetsUpdateResponse> checkForUpdateRequest = new ApiHttpRequest<>(checkForUpdateTask);
            try {
                AssetsUpdateResponse AssetsUpdateResponse = checkForUpdateRequest.makeRequest();
                AssetsUpdateResponseUpdateInfo updateInfo = AssetsUpdateResponse.getUpdateInfo();
                if (updateInfo.isUpdateAppVersion()) {
                    return AssetsRemotePackage.createDefaultRemotePackage(updateInfo.getAppVersion(), updateInfo.isUpdateAppVersion());
                } else if (!updateInfo.isAvailable()) {
                    return null;
                }
                return AssetsRemotePackage.createRemotePackageFromUpdateInfo(deploymentKey, updateInfo);
            } catch (AssetsApiHttpRequestException e) {
                throw new AssetsQueryUpdateException(e, currentPackage.getPackageHash());
            }
        } catch (AssetsMalformedDataException | AssetsIllegalArgumentException e) {
            throw new AssetsQueryUpdateException(e, currentPackage.getPackageHash());
        }
    }

    /**
     * Sends deployment status to server.
     *
     * @param configuration          current application configuration.
     * @param deploymentStatusReport instance of {@link AssetsDeploymentStatusReport}.
     * @throws AssetsReportStatusException exception occurred when sending the status.
     */
    public void reportStatusDeploy(AssetsConfiguration configuration, AssetsDeploymentStatusReport deploymentStatusReport) throws AssetsReportStatusException {

        /* Extract parameters from configuration */
        String appVersion = configuration.getAppVersion();
        String serverUrl = fixServerUrl(configuration.getServerUrl());
        String deploymentKey = configuration.getDeploymentKey();
        String clientUniqueId = configuration.getClientUniqueId();

        try {
            deploymentStatusReport.setClientUniqueId(clientUniqueId);
            deploymentStatusReport.setDeploymentKey(deploymentKey);
            deploymentStatusReport.setAppVersion(deploymentStatusReport.getPackage() != null ? deploymentStatusReport.getPackage().getAppVersion() : appVersion);
            deploymentStatusReport.setLabel(deploymentStatusReport.getPackage() != null ? deploymentStatusReport.getPackage().getLabel() : deploymentStatusReport.getLabel());
        } catch (AssetsIllegalArgumentException e) {
            AppCenterLog.error(Assets.LOG_TAG, new AssetsReportStatusException(e, DEPLOY).getMessage());
        }
        final String requestUrl = serverUrl + REPORT_DEPLOYMENT_STATUS_ENDPOINT;
        switch (deploymentStatusReport.getStatus()) {
            case SUCCEEDED:
            case FAILED:
                break;
            default: {
                if (deploymentStatusReport.getStatus() == null) {
                    AppCenterLog.error(Assets.LOG_TAG, new AssetsReportStatusException("Missing status argument.", DEPLOY).getMessage());
                } else {
                    AppCenterLog.error(Assets.LOG_TAG, new AssetsReportStatusException("Unrecognized status \"" + deploymentStatusReport.getStatus().getValue() + "\".", DEPLOY).getMessage());
                }
            }
        }
        deploymentStatusReport.setPackage(deploymentStatusReport.getPackage());
        final String deploymentStatusReportJsonString = mAssetsUtils.convertObjectToJsonString(deploymentStatusReport);
        ReportStatusTask reportStatusDeployTask = new ReportStatusTask(mFileUtils, requestUrl, deploymentStatusReportJsonString, DEPLOY);
        ApiHttpRequest<AssetsReportStatusResult> reportStatusDeployRequest = new ApiHttpRequest<>(reportStatusDeployTask);
        try {
            AssetsReportStatusResult assetsReportStatusResult = reportStatusDeployRequest.makeRequest();
            AppCenterLog.info(LOG_TAG, "Report status deploy: " + assetsReportStatusResult.getResult());
        } catch (AssetsApiHttpRequestException e) {
            AppCenterLog.error(Assets.LOG_TAG, new AssetsReportStatusException(e, DEPLOY).getMessage());
        }
    }

    /**
     * Sends download status to server.
     *
     * @param configuration     current application configuration.
     * @param downloadedPackage instance of {@link AssetsLocalPackage} that has been downloaded.
     * @throws AssetsReportStatusException exception occurred when sending the status.
     */
    public void reportStatusDownload(AssetsConfiguration configuration, AssetsLocalPackage downloadedPackage) throws AssetsReportStatusException {

        /* Extract parameters from configuration */
        String serverUrl = fixServerUrl(configuration.getServerUrl());
        String deploymentKey = configuration.getDeploymentKey();
        String clientUniqueId = configuration.getClientUniqueId();
        final String requestUrl = serverUrl + REPORT_DOWNLOAD_STATUS_ENDPOINT;
        try {
            final AssetsDownloadStatusReport downloadStatusReport = AssetsDownloadStatusReport.createReport(clientUniqueId, deploymentKey, downloadedPackage.getLabel());
            final String downloadStatusReportJsonString = mAssetsUtils.convertObjectToJsonString(downloadStatusReport);
            ReportStatusTask reportStatusDownloadTask = new ReportStatusTask(mFileUtils, requestUrl, downloadStatusReportJsonString, DOWNLOAD);
            ApiHttpRequest<AssetsReportStatusResult> reportStatusDownloadRequest = new ApiHttpRequest<>(reportStatusDownloadTask);
            AssetsReportStatusResult assetsReportStatusResult = reportStatusDownloadRequest.makeRequest();
            AppCenterLog.info(LOG_TAG, "Report status download: " + assetsReportStatusResult.getResult());
        } catch (AssetsApiHttpRequestException | AssetsIllegalArgumentException e) {
            throw new AssetsReportStatusException(e, DOWNLOAD);
        }
    }
}
