package com.microsoft.appcenter.assets.managers;

import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.assets.AssetsStatusReportIdentifier;
import com.microsoft.appcenter.assets.datacontracts.AssetsDeploymentStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPendingUpdate;
import com.microsoft.appcenter.assets.enums.AssetsDeploymentStatus;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.testutils.CommonSettingsCompatibilityUtils;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class SettingManagerAndroidTests {

    private final static String DEPLOYMENT_KEY = "ABC123";
    private final static String LABEL = "awesome package";
    private final static boolean FAILED_INSTALL = false;
    private final static String APP_VERSION = "2.2.1";
    private final static String DESCRIPTION = "short description";
    private final static boolean IS_MANDATORY = true;
    private final static String PACKAGE_HASH = "HASH";

    /**
     * Instance of {@link AssetsUtils} to work with.
     */
    private AssetsUtils mAssetsUtils;

    /**
     * Instance of {@link SettingsManager} to work with.
     */
    private SettingsManager mSettingsManager;

    @Before
    public void setUp() throws Exception {
        mAssetsUtils = AssetsUtils.getInstance(FileUtils.getInstance());
        mSettingsManager = new SettingsManager(InstrumentationRegistry.getContext(), mAssetsUtils);
    }

    /**
     * Checks that saving -> retrieving pending update returns valid data.
     */
    @Test
    public void pendingUpdateTest() throws Exception {
        AssetsPendingUpdate assetsPendingUpdate = new AssetsPendingUpdate();
        assetsPendingUpdate.setPendingUpdateHash(PACKAGE_HASH);
        assetsPendingUpdate.setPendingUpdateIsLoading(false);
        mSettingsManager.savePendingUpdate(assetsPendingUpdate);
        assetsPendingUpdate = mSettingsManager.getPendingUpdate();
        assertEquals(assetsPendingUpdate.getPendingUpdateHash(), PACKAGE_HASH);
        assertEquals(assetsPendingUpdate.isPendingUpdateLoading(), false);
        assertTrue(mSettingsManager.isPendingUpdate(PACKAGE_HASH));
    }

    /**
     * {@link SettingsManager#isPendingUpdate(String)} should return <code>false</code> if pending update is <code>null</code>.
     */
    @Test
    public void pendingUpdateNullTest() throws Exception {
        mSettingsManager.removePendingUpdate();
        assertFalse(mSettingsManager.isPendingUpdate(PACKAGE_HASH));
        assertFalse(mSettingsManager.isPendingUpdate(null));
    }

    /**
     * {@link SettingsManager#getPendingUpdate()} should throw {@link AssetsMalformedDataException}
     * if a json string representing pending update could not be parsed.
     */
    @Test(expected = AssetsMalformedDataException.class)
    public void pendingUpdateFailsIfJsonParseError() throws Exception {
        CommonSettingsCompatibilityUtils.saveStringToPending("123", InstrumentationRegistry.getContext());
        mSettingsManager.getPendingUpdate();
    }

    /**
     * {@link SettingsManager#getPendingUpdate()} should return <code>null</code> if there is no info about the pending update.
     */
    @Test
    public void pendingUpdateIsNull() throws Exception {
        mSettingsManager.removePendingUpdate();
        AssetsPendingUpdate assetsPendingUpdate = mSettingsManager.getPendingUpdate();
        assertNull(assetsPendingUpdate);
    }

    /**
     * Creates instance of {@link AssetsLocalPackage} for testing.
     *
     * @param packageHash hash to create package with.
     * @return instance of {@link AssetsLocalPackage}.
     */
    private AssetsLocalPackage createLocalPackage(String packageHash) {
        AssetsPackage assetsPackage = createPackage(packageHash);
        return AssetsLocalPackage.createLocalPackage(false, false, false, false, "", assetsPackage);
    }

    private AssetsPackage createPackage(String packageHash) {
        AssetsPackage assetsPackage = new AssetsPackage();
        assetsPackage.setAppVersion(APP_VERSION);
        assetsPackage.setDeploymentKey(DEPLOYMENT_KEY);
        assetsPackage.setDescription(DESCRIPTION);
        assetsPackage.setFailedInstall(FAILED_INSTALL);
        assetsPackage.setLabel(LABEL);
        assetsPackage.setMandatory(IS_MANDATORY);
        assetsPackage.setPackageHash(packageHash);
        return assetsPackage;
    }

    /**
     * Checks that saving -> retrieving failed updates returns valid data.
     */
    @Test
    public void failedUpdateTest() throws Exception {
        mSettingsManager.removeFailedUpdates();
        AssetsPackage assetsPackage = createPackage("newHash");
        mSettingsManager.saveFailedUpdate(assetsPackage);
        assetsPackage = createLocalPackage(PACKAGE_HASH);
        mSettingsManager.saveFailedUpdate(assetsPackage);
        List<AssetsPackage> assetsPackages = mSettingsManager.getFailedUpdates();
        assetsPackage = assetsPackages.get(0);
        assertEquals(assetsPackage.getDeploymentKey(), DEPLOYMENT_KEY);
        assertTrue(mSettingsManager.existsFailedUpdate(PACKAGE_HASH));
        assertFalse(mSettingsManager.existsFailedUpdate(null));
    }

    /**
     * {@link SettingsManager#existsFailedUpdate(String)} should return <code>false</code> if failed update info is empty.
     */
    @Test
    public void failedUpdateNullTest() throws Exception {
        mSettingsManager.removeFailedUpdates();
        assertFalse(mSettingsManager.existsFailedUpdate(PACKAGE_HASH));
    }

    /**
     * {@link SettingsManager#getFailedUpdates()} should return empty array if there is no info about the failed updates.
     */
    @Test
    public void failedUpdateIsNull() throws Exception {
        mSettingsManager.removeFailedUpdates();
        List<AssetsPackage> assetsPackages = mSettingsManager.getFailedUpdates();
        assertEquals(0, assetsPackages.size());
    }

    /**
     * Tests workflow save identifier -> get identifier.
     */
    @Test
    public void identifierTest() throws Exception {
        mSettingsManager.saveIdentifierOfReportedStatus(new AssetsStatusReportIdentifier("123", "1.2"));
        AssetsStatusReportIdentifier assetsStatusReportIdentifier = mSettingsManager.getPreviousStatusReportIdentifier();
        assertEquals(assetsStatusReportIdentifier.getDeploymentKey(), "123");
    }

    /**
     * Tests workflow save status report -> get status report -> remove report -> get report and assert that it is <code>null</code>.
     */
    @Test
    public void statusReportTest() throws Exception {
        AssetsDeploymentStatusReport assetsDeploymentStatusReport = new AssetsDeploymentStatusReport();
        assetsDeploymentStatusReport.setAppVersion("1.2");
        assetsDeploymentStatusReport.setStatus(AssetsDeploymentStatus.SUCCEEDED);
        assetsDeploymentStatusReport.setPreviousDeploymentKey("123");
        assetsDeploymentStatusReport.setPreviousLabelOrAppVersion("1.2");
        assetsDeploymentStatusReport.setClientUniqueId("111");
        mSettingsManager.saveStatusReportForRetry(assetsDeploymentStatusReport);
        assetsDeploymentStatusReport = mSettingsManager.getStatusReportSavedForRetry();
        assertEquals(assetsDeploymentStatusReport.getAppVersion(), "1.2");
        mSettingsManager.removeStatusReportSavedForRetry();
        assetsDeploymentStatusReport = mSettingsManager.getStatusReportSavedForRetry();
        assertNull(assetsDeploymentStatusReport);
    }
}
