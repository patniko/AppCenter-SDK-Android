package com.microsoft.appcenter.assets;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.assets.datacontracts.AssetsDeploymentStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateRequest;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateResponse;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;
import com.microsoft.appcenter.assets.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * This class tests cases where an error happens and should be logged via {@link AppCenterLog#error(String, String)}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(AppCenterLog.class)
public class LoggingUnitTests {

    private final static String CLIENT_UNIQUE_ID = "YHFfdsfdv65";
    private final static String DEPLOYMENT_KEY = "ABC123";
    private final static String LABEL = "awesome package";
    private final static boolean FAILED_INSTALL = false;
    private final static boolean IS_PENDING = true;
    private final static boolean IS_DEBUG_ONLY = false;
    private final static boolean IS_FIRST_RUN = false;
    private final static String APP_ENTRY_POINT = "/www/index.html";

    private FileUtils mFileUtils;

    @Before
    public void setUp() {
        this.mFileUtils = FileUtils.getInstance();
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testUpdateRequestAppVersionNull() throws Exception {
        AssetsPackage assetsPackage = new AssetsPackage();
        AssetsLocalPackage assetsLocalPackage = AssetsLocalPackage.createLocalPackage(FAILED_INSTALL, IS_FIRST_RUN, IS_PENDING, IS_DEBUG_ONLY, APP_ENTRY_POINT, assetsPackage);
        AssetsUpdateRequest assetsUpdateRequest = AssetsUpdateRequest.createUpdateRequest(DEPLOYMENT_KEY, assetsLocalPackage, CLIENT_UNIQUE_ID);
        assetsUpdateRequest.setAppVersion(null);
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testUpdateRequestDeploymentKeyNull() throws Exception {
        AssetsPackage assetsPackage = new AssetsPackage();
        assetsPackage.setAppVersion("");
        AssetsLocalPackage assetsLocalPackage = AssetsLocalPackage.createLocalPackage(FAILED_INSTALL, IS_FIRST_RUN, IS_PENDING, IS_DEBUG_ONLY, APP_ENTRY_POINT, assetsPackage);
        AssetsUpdateRequest assetsUpdateRequest = AssetsUpdateRequest.createUpdateRequest(DEPLOYMENT_KEY, assetsLocalPackage, CLIENT_UNIQUE_ID);
        assetsUpdateRequest.setDeploymentKey(null);
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testDownloadReportLabelNull() throws Exception {
        AssetsDownloadStatusReport assetsDownloadStatusReport = AssetsDownloadStatusReport.createReport(CLIENT_UNIQUE_ID, DEPLOYMENT_KEY, LABEL);
        assetsDownloadStatusReport.setLabel(null);
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testDownloadReportClientIdNull() throws Exception {
        AssetsDownloadStatusReport assetsDownloadStatusReport = AssetsDownloadStatusReport.createReport(CLIENT_UNIQUE_ID, DEPLOYMENT_KEY, LABEL);
        assetsDownloadStatusReport.setClientUniqueId(null);
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testDownloadReportDeploymentKeyNull() throws Exception {
        AssetsDownloadStatusReport assetsDownloadStatusReport = AssetsDownloadStatusReport.createReport(CLIENT_UNIQUE_ID, DEPLOYMENT_KEY, LABEL);
        assetsDownloadStatusReport.setDeploymentKey(null);
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testDeploymentReportAppVersionNull() throws Exception {
        AssetsDeploymentStatusReport assetsDeploymentStatusReport = new AssetsDeploymentStatusReport();
        assetsDeploymentStatusReport.setAppVersion(null);
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testDeploymentReportPreviousKeyNull() throws Exception {
        AssetsDeploymentStatusReport assetsDeploymentStatusReport = new AssetsDeploymentStatusReport();
        assetsDeploymentStatusReport.setPreviousDeploymentKey(null);
    }

    @Test(expected = AssetsIllegalArgumentException.class)
    public void testUpdateResponseUpdateInfoNull() throws Exception {
        AssetsUpdateResponse assetsUpdateResponse = new AssetsUpdateResponse();
        assetsUpdateResponse.setUpdateInfo(null);
    }

    /**
     * Checks {@link FileUtils#finalizeResources} logs custom error message.
     */
    @Test
    public void testFinalizeResourcesLogging() {
        mockStatic(AppCenterLog.class);
        Closeable brokenResource = new Closeable() {
            @Override
            public void close() throws IOException {
                throw new IOException();
            }
        };
        mFileUtils.finalizeResources(Arrays.asList(brokenResource), "log me");
        verifyStatic(VerificationModeFactory.times(1));
    }
}