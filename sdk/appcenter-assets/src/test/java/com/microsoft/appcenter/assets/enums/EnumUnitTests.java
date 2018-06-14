package com.microsoft.appcenter.assets.enums;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests all the enum classes.
 */
public class EnumUnitTests {

    @Test
    public void enumsTest() throws Exception {
        AssetsCheckFrequency assetsCheckFrequency = AssetsCheckFrequency.MANUAL;
        int checkFrequencyValue = assetsCheckFrequency.getValue();
        assertEquals(2, checkFrequencyValue);
        AssetsDeploymentStatus assetsDeploymentStatus = AssetsDeploymentStatus.SUCCEEDED;
        String deploymentStatusValue = assetsDeploymentStatus.getValue();
        assertEquals("DeploymentSucceeded", deploymentStatusValue);
        AssetsInstallMode assetsInstallMode = AssetsInstallMode.IMMEDIATE;
        int installModeValue = assetsInstallMode.getValue();
        assertEquals(0, installModeValue);
        AssetsSyncStatus assetsSyncStatus = AssetsSyncStatus.AWAITING_USER_ACTION;
        int syncStatusValue = assetsSyncStatus.getValue();
        assertEquals(6, syncStatusValue);
        AssetsUpdateState assetsUpdateState = AssetsUpdateState.LATEST;
        int updateStateValue = assetsUpdateState.getValue();
        assertEquals(2, updateStateValue);
        ReportType reportType = ReportType.DEPLOY;
        String message = reportType.getMessage();
        assertEquals("Error occurred during delivering deploy status report.", message);

        /* Test <code>valueOf()</code> and <code>values()</code>. */
        assertEquals(3, AssetsCheckFrequency.values().length);
        assertEquals(2, AssetsDeploymentStatus.values().length);
        assertEquals(2, ReportType.values().length);
        assertEquals(4, AssetsInstallMode.values().length);
        assertEquals(9, AssetsSyncStatus.values().length);
        assertEquals(3, AssetsUpdateState.values().length);
        assertEquals(AssetsUpdateState.RUNNING, AssetsUpdateState.valueOf("RUNNING"));
        assertEquals(AssetsDeploymentStatus.FAILED, AssetsDeploymentStatus.valueOf("FAILED"));
        assertEquals(AssetsInstallMode.IMMEDIATE, AssetsInstallMode.valueOf("IMMEDIATE"));
        assertEquals(AssetsSyncStatus.AWAITING_USER_ACTION, AssetsSyncStatus.valueOf("AWAITING_USER_ACTION"));
        assertEquals(AssetsCheckFrequency.MANUAL, AssetsCheckFrequency.valueOf("MANUAL"));
        assertEquals(ReportType.DEPLOY, ReportType.valueOf("DEPLOY"));
    }
}
