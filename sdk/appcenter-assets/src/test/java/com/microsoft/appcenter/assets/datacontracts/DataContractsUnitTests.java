package com.microsoft.appcenter.assets.datacontracts;

import com.microsoft.appcenter.assets.AssetsStatusReportIdentifier;
import com.microsoft.appcenter.assets.enums.AssetsCheckFrequency;
import com.microsoft.appcenter.assets.enums.AssetsDeploymentStatus;
import com.microsoft.appcenter.assets.enums.AssetsInstallMode;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Tests all the data classes.
 */
public class DataContractsUnitTests {

    private final static String CLIENT_UNIQUE_ID = "YHFv65";
    private final static String DEPLOYMENT_KEY = "ABC123";
    private final static String LABEL = "awesome package";
    private final static boolean FAILED_INSTALL = false;
    private final static boolean IS_PENDING = true;
    private final static boolean IS_DEBUG_ONLY = false;
    private final static boolean IS_FIRST_RUN = false;
    private final static String PREVIOUS_DEPLOYMENT_KEY = "prevABC123";
    private final static String PREVIOUS_LABEL = "awesome package previous";
    private final static String APP_VERSION = "2.2.1";
    private final static String DESCRIPTION = "short description";
    private final static boolean IS_MANDATORY = true;
    private final static boolean UPDATE_APP_VERSION = true;
    private final static boolean IS_AVAILABLE = true;
    private final static boolean SHOULD_RUN_BINARY = false;
    private final static long PACKAGE_SIZE = 102546723;
    private final static String DOWNLOAD_URL = "https://url.com";
    private final static String PACKAGE_HASH = "HASH";
    private final static String ERROR = "An error has occurred";
    private final static String APP_ENTRY_POINT = "/www/index.html";

    @Test
    public void dataContractsTest() throws Exception {

        /* Check package. */
        AssetsPackage assetsPackage = new AssetsPackage();
        assetsPackage.setAppVersion(APP_VERSION);
        assetsPackage.setDeploymentKey(DEPLOYMENT_KEY);
        assetsPackage.setDescription(DESCRIPTION);
        assetsPackage.setFailedInstall(FAILED_INSTALL);
        assetsPackage.setLabel(LABEL);
        assetsPackage.setMandatory(IS_MANDATORY);
        assetsPackage.setPackageHash(PACKAGE_HASH);
        checkPackage(assetsPackage);

        /* Check local package. */
        AssetsLocalPackage assetsLocalPackage = AssetsLocalPackage.createLocalPackage(FAILED_INSTALL, IS_FIRST_RUN, IS_PENDING, IS_DEBUG_ONLY, APP_ENTRY_POINT, assetsPackage);
        checkLocalPackage(assetsLocalPackage);
        AssetsLocalPackage assetsLocalPackageForQuery = AssetsLocalPackage.createEmptyPackageForCheckForUpdateQuery(APP_VERSION);
        String dateString = new Date().toString();
        assetsLocalPackageForQuery.setBinaryModifiedTime(dateString);
        assertEquals(assetsLocalPackageForQuery.getBinaryModifiedTime(), dateString);

        /* Check download report. */
        AssetsDownloadStatusReport assetsDownloadStatusReport = AssetsDownloadStatusReport.createReport(CLIENT_UNIQUE_ID, DEPLOYMENT_KEY, LABEL);
        checkDownloadReport(assetsDownloadStatusReport);

        /* Check deployment report. */
        AssetsDeploymentStatusReport assetsDeploymentStatusReport = new AssetsDeploymentStatusReport();
        assetsDeploymentStatusReport.setClientUniqueId(CLIENT_UNIQUE_ID);
        assetsDeploymentStatusReport.setDeploymentKey(DEPLOYMENT_KEY);
        assetsDeploymentStatusReport.setLabel(LABEL);
        assetsDeploymentStatusReport.setAppVersion(APP_VERSION);
        assetsDeploymentStatusReport.setPreviousDeploymentKey(PREVIOUS_DEPLOYMENT_KEY);
        assetsDeploymentStatusReport.setPreviousLabelOrAppVersion(PREVIOUS_LABEL);
        assetsDeploymentStatusReport.setStatus(AssetsDeploymentStatus.SUCCEEDED);
        // TODO there should be AssetsPackage instance instead!!!
        assetsDeploymentStatusReport.setPackage(assetsLocalPackage);
        assertEquals(assetsLocalPackage, assetsDeploymentStatusReport.getPackage());
        checkDeploymentReport(assetsDeploymentStatusReport);

        /* Check update response info. */
        AssetsUpdateResponseUpdateInfo assetsUpdateResponseUpdateInfo = new AssetsUpdateResponseUpdateInfo();
        assetsUpdateResponseUpdateInfo.setAppVersion(APP_VERSION);
        assetsUpdateResponseUpdateInfo.setAvailable(IS_AVAILABLE);
        assetsUpdateResponseUpdateInfo.setDescription(DESCRIPTION);
        assetsUpdateResponseUpdateInfo.setDownloadUrl(DOWNLOAD_URL);
        assetsUpdateResponseUpdateInfo.setLabel(LABEL);
        assetsUpdateResponseUpdateInfo.setMandatory(IS_MANDATORY);
        assetsUpdateResponseUpdateInfo.setPackageHash(PACKAGE_HASH);
        assetsUpdateResponseUpdateInfo.setPackageSize(PACKAGE_SIZE);
        assetsUpdateResponseUpdateInfo.setShouldRunBinaryVersion(SHOULD_RUN_BINARY);
        assetsUpdateResponseUpdateInfo.setUpdateAppVersion(UPDATE_APP_VERSION);
        checkUpdateResponse(assetsUpdateResponseUpdateInfo);

        /* Check update response. */
        AssetsUpdateResponse assetsUpdateResponse = new AssetsUpdateResponse();
        assetsUpdateResponse.setUpdateInfo(assetsUpdateResponseUpdateInfo);
        assertEquals(assetsUpdateResponseUpdateInfo, assetsUpdateResponse.getUpdateInfo());

        /* Check remote package. */
        AssetsRemotePackage assetsDefaultRemotePackage = AssetsRemotePackage.createDefaultRemotePackage(APP_VERSION, UPDATE_APP_VERSION);
        assertEquals(APP_VERSION, assetsDefaultRemotePackage.getAppVersion());
        assertEquals(UPDATE_APP_VERSION, assetsDefaultRemotePackage.isUpdateAppVersion());
        AssetsRemotePackage assetsRemotePackage = AssetsRemotePackage.createRemotePackage(FAILED_INSTALL, PACKAGE_SIZE, DOWNLOAD_URL, UPDATE_APP_VERSION, assetsPackage);
        checkRemotePackage(assetsRemotePackage);
        AssetsRemotePackage assetsUpdateRemotePackage = AssetsRemotePackage.createRemotePackageFromUpdateInfo(DEPLOYMENT_KEY, assetsUpdateResponseUpdateInfo);
        checkRemotePackage(assetsUpdateRemotePackage);

        /* Check update request. */
        AssetsUpdateRequest assetsUpdateRequest = AssetsUpdateRequest.createUpdateRequest(DEPLOYMENT_KEY, assetsLocalPackage, CLIENT_UNIQUE_ID);
        assetsUpdateRequest.setCompanion(false);
        assertEquals(DEPLOYMENT_KEY, assetsUpdateRequest.getDeploymentKey());
        assertEquals(CLIENT_UNIQUE_ID, assetsUpdateRequest.getClientUniqueId());
        assertEquals(assetsLocalPackage.getAppVersion(), assetsUpdateRequest.getAppVersion());
        assertEquals(assetsLocalPackage.getLabel(), assetsUpdateRequest.getLabel());
        assertEquals(assetsLocalPackage.getPackageHash(), assetsUpdateRequest.getPackageHash());
        assertEquals(false, assetsUpdateRequest.isCompanion());

        /* Check update dialog. */
        AssetsUpdateDialog assetsUpdateDialog = new AssetsUpdateDialog();
        assertEquals("An update is available that must be installed.", assetsUpdateDialog.getMandatoryUpdateMessage());
        assertEquals("An update is available. Would you like to install it?", assetsUpdateDialog.getOptionalUpdateMessage());
        assertEquals("Description: ", assetsUpdateDialog.getDescriptionPrefix());
        assertEquals("Continue", assetsUpdateDialog.getMandatoryContinueButtonLabel());
        assertEquals("Ignore", assetsUpdateDialog.getOptionalIgnoreButtonLabel());
        assertEquals("Install", assetsUpdateDialog.getOptionalInstallButtonLabel());
        assertEquals("Update available", assetsUpdateDialog.getTitle());
        assertEquals(false, assetsUpdateDialog.getAppendReleaseDescription());

        /* Check sync options. */
        AssetsSyncOptions assetsSyncOptionsEmpty = new AssetsSyncOptions();
        assertNull(assetsSyncOptionsEmpty.getDeploymentKey());

        AssetsSyncOptions assetsSyncOptions = new AssetsSyncOptions(DEPLOYMENT_KEY);
        assetsSyncOptions.setUpdateDialog(assetsUpdateDialog);
        assertEquals(DEPLOYMENT_KEY, assetsSyncOptions.getDeploymentKey());
        assertEquals(0, assetsSyncOptions.getMinimumBackgroundDuration());
        assertEquals(AssetsInstallMode.ON_NEXT_RESTART, assetsSyncOptions.getInstallMode());
        assertEquals(AssetsInstallMode.IMMEDIATE, assetsSyncOptions.getMandatoryInstallMode());
        assertEquals(true, assetsSyncOptions.getIgnoreFailedUpdates());
        assertEquals(assetsUpdateDialog, assetsSyncOptions.getUpdateDialog());
        assertEquals(AssetsCheckFrequency.ON_APP_START, assetsSyncOptions.getCheckFrequency());
        assertEquals(true, assetsSyncOptions.shouldRestart());
        AssetsReportStatusResult assetsReportStatusResult = AssetsReportStatusResult.createSuccessful("123");
        assertEquals("123", assetsReportStatusResult.getResult());

        /* Check status report identifier. */
        AssetsStatusReportIdentifier assetsStatusReportIdentifier = new AssetsStatusReportIdentifier(LABEL);
        assertFalse(assetsStatusReportIdentifier.hasDeploymentKey());
        assertEquals(assetsStatusReportIdentifier.getVersionLabel(), LABEL);
        assertEquals(assetsStatusReportIdentifier.getVersionLabelOrEmpty(), LABEL);
        assertEquals(assetsStatusReportIdentifier.toString(), LABEL);
        assetsStatusReportIdentifier = new AssetsStatusReportIdentifier(null);
        assertEquals(assetsStatusReportIdentifier.getVersionLabelOrEmpty(), "");
        assertEquals(assetsStatusReportIdentifier.toString(), null);
        assetsStatusReportIdentifier = AssetsStatusReportIdentifier.fromString(LABEL);
        assertEquals(LABEL, assetsStatusReportIdentifier.getVersionLabel());
        assetsStatusReportIdentifier = AssetsStatusReportIdentifier.fromString("1:2:3");
        assertNull(assetsStatusReportIdentifier);
    }

    private void checkDeploymentReport(AssetsDeploymentStatusReport assetsDeploymentStatusReport) {
        assertEquals(APP_VERSION, assetsDeploymentStatusReport.getAppVersion());
        assertEquals(PREVIOUS_DEPLOYMENT_KEY, assetsDeploymentStatusReport.getPreviousDeploymentKey());
        assertEquals(PREVIOUS_LABEL, assetsDeploymentStatusReport.getPreviousLabelOrAppVersion());
        assertEquals(AssetsDeploymentStatus.SUCCEEDED, assetsDeploymentStatusReport.getStatus());
        checkDownloadReport(assetsDeploymentStatusReport);
    }

    private void checkDownloadReport(AssetsDownloadStatusReport assetsDownloadStatusReport) {
        assertEquals(CLIENT_UNIQUE_ID, assetsDownloadStatusReport.getClientUniqueId());
        assertEquals(DEPLOYMENT_KEY, assetsDownloadStatusReport.getDeploymentKey());
        assertEquals(LABEL, assetsDownloadStatusReport.getLabel());
    }

    private void checkLocalPackage(AssetsLocalPackage assetsLocalPackage) {
        assertEquals(IS_FIRST_RUN, assetsLocalPackage.isFirstRun());
        assertEquals(IS_PENDING, assetsLocalPackage.isPending());
        assertEquals(IS_DEBUG_ONLY, assetsLocalPackage.isDebugOnly());
        checkPackage(assetsLocalPackage);
    }

    private void checkRemotePackage(AssetsRemotePackage assetsRemotePackage) {
        assertEquals(UPDATE_APP_VERSION, assetsRemotePackage.isUpdateAppVersion());
        assertEquals(PACKAGE_SIZE, assetsRemotePackage.getPackageSize());
        assertEquals(DOWNLOAD_URL, assetsRemotePackage.getDownloadUrl());
        checkPackage(assetsRemotePackage);
    }

    private void checkPackage(AssetsPackage assetsPackage) {
        assertEquals(APP_VERSION, assetsPackage.getAppVersion());
        assertEquals(DEPLOYMENT_KEY, assetsPackage.getDeploymentKey());
        assertEquals(DESCRIPTION, assetsPackage.getDescription());
        assertEquals(FAILED_INSTALL, assetsPackage.isFailedInstall());
        assertEquals(LABEL, assetsPackage.getLabel());
        assertEquals(IS_MANDATORY, assetsPackage.isMandatory());
        assertEquals(PACKAGE_HASH, assetsPackage.getPackageHash());
    }

    private void checkUpdateResponse(AssetsUpdateResponseUpdateInfo assetsUpdateResponseUpdateInfo) {
        assertEquals(APP_VERSION, assetsUpdateResponseUpdateInfo.getAppVersion());
        assertEquals(IS_AVAILABLE, assetsUpdateResponseUpdateInfo.isAvailable());
        assertEquals(DESCRIPTION, assetsUpdateResponseUpdateInfo.getDescription());
        assertEquals(DOWNLOAD_URL, assetsUpdateResponseUpdateInfo.getDownloadUrl());
        assertEquals(LABEL, assetsUpdateResponseUpdateInfo.getLabel());
        assertEquals(IS_MANDATORY, assetsUpdateResponseUpdateInfo.isMandatory());
        assertEquals(PACKAGE_HASH, assetsUpdateResponseUpdateInfo.getPackageHash());
        assertEquals(PACKAGE_SIZE, assetsUpdateResponseUpdateInfo.getPackageSize());
        assertEquals(SHOULD_RUN_BINARY, assetsUpdateResponseUpdateInfo.isShouldRunBinaryVersion());
        assertEquals(UPDATE_APP_VERSION, assetsUpdateResponseUpdateInfo.isUpdateAppVersion());
    }
}