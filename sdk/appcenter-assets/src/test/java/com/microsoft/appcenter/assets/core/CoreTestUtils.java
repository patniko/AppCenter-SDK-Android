package com.microsoft.appcenter.assets.core;

import android.text.TextUtils;

import com.microsoft.appcenter.assets.AssetsConfiguration;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.enums.AssetsUpdateState;
import com.microsoft.appcenter.assets.managers.AssetsAcquisitionManager;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;

import static com.microsoft.appcenter.assets.core.AssetsAndroidCoreUnitTests.DEPLOYMENT_KEY;
import static com.microsoft.appcenter.assets.core.AssetsAndroidCoreUnitTests.FAILED_INSTALL;
import static com.microsoft.appcenter.assets.core.AssetsAndroidCoreUnitTests.IS_FIRST_RUN;
import static com.microsoft.appcenter.assets.core.AssetsAndroidCoreUnitTests.PACKAGE_HASH;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Contains util methods for testing {@link AssetsAndroidCore}.
 */
public class CoreTestUtils {

    /**
     * Instance of {@link AssetsBaseCore} to use mocks and stubs on.
     */
    private AssetsBaseCore mAssetsBaseCore;

    /**
     * Creates instance of utils.
     *
     * @param assetsBaseCore instance of {@link AssetsBaseCore} to use mocks and stubs on.
     */
    CoreTestUtils(AssetsBaseCore assetsBaseCore) {
        mAssetsBaseCore = assetsBaseCore;
    }

    /**
     * Injects the provided instance of {@link AssetsUpdateManager} into {@link AssetsAndroidCore}.
     *
     * @param assetsUpdateManager fake assets update manager.
     */
    public void injectManagersInCore(AssetsUpdateManager assetsUpdateManager) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mUpdateManager").set(assetsManagers, assetsUpdateManager);
        MemberModifier.field(AssetsBaseCore.class, "mManagers").set(mAssetsBaseCore, assetsManagers);
    }

    /**
     * Injects the provided instance of {@link AssetsUpdateManager} into {@link AssetsAndroidCore}.
     *
     * @param assetsUpdateManager fake assets update manager.
     * @param settingsManager     fake settings manager.
     */
    public void injectManagersInCore(AssetsUpdateManager assetsUpdateManager, SettingsManager settingsManager) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mUpdateManager").set(assetsManagers, assetsUpdateManager);
        MemberModifier.field(AssetsManagers.class, "mSettingsManager").set(assetsManagers, settingsManager);
        MemberModifier.field(AssetsBaseCore.class, "mManagers").set(mAssetsBaseCore, assetsManagers);
    }

    /**
     * Injects the provided instance of {@link AssetsAcquisitionManager} into {@link AssetsAndroidCore}.
     *
     * @param assetsAcquisitionManager fake assets acquisition manager.
     */
    public void injectManagersInCore(AssetsAcquisitionManager assetsAcquisitionManager) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mAcquisitionManager").set(assetsManagers, assetsAcquisitionManager);
        MemberModifier.field(AssetsBaseCore.class, "mManagers").set(mAssetsBaseCore, assetsManagers);
    }

    /**
     * Retrieves and checks the specified update package.
     *
     * @param isPending whether a {@link AssetsUpdateState#PENDING} package should be retrieved.
     * @return update package.
     */
    public AssetsLocalPackage getUpdatePackage(boolean isPending) throws Exception {
        AssetsLocalPackage returnedPackage = callGetUpdateMetadata(isPending ? AssetsUpdateState.PENDING : AssetsUpdateState.RUNNING);
        assertEquals(returnedPackage.getPackageHash(), PACKAGE_HASH);
        assertEquals(returnedPackage.isPending(), isPending);
        assertEquals(returnedPackage.isFailedInstall(), FAILED_INSTALL);
        assertEquals(returnedPackage.isFirstRun(), IS_FIRST_RUN);
        return returnedPackage;
    }

    /**
     * Prepares classes for testing {@link AssetsAndroidCore#getUpdateMetadata()} workflow
     * which should return an update.
     */
    public void prepareGetUpdateWorkflow(boolean isPending) throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = AssetsLocalPackage.createEmptyPackageForCheckForUpdateQuery("1.0");
        currentPackage.setPackageHash(PACKAGE_HASH);
        currentPackage.setDebugOnly(false);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(isPending);

        injectManagersInCore(assetsUpdateManager, settingsManager);
        when(mAssetsBaseCore.existsFailedUpdate(eq(PACKAGE_HASH))).thenReturn(FAILED_INSTALL);
        when(mAssetsBaseCore.isFirstRun(eq(PACKAGE_HASH))).thenReturn(IS_FIRST_RUN);
    }

    /**
     * Mocks {@link AssetsBaseCore#getCurrentPackage()} to return fake {@link AssetsLocalPackage}.
     *
     * @param packageHash hash to be set to fake package.
     * @return instance of fake {@link AssetsLocalPackage}.
     */
    public AssetsLocalPackage mockLocalPackage(String packageHash) throws Exception {
        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(packageHash);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(currentPackage);
        return currentPackage;
    }

    /**
     * Mocks {@link AssetsBaseCore#getCurrentPackage()} to return fake {@link AssetsLocalPackage}.
     *
     * @return instance of fake {@link AssetsLocalPackage}.
     */
    public AssetsLocalPackage mockLocalPackage() throws Exception {
        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(currentPackage);
        return currentPackage;
    }

    /**
     * Mocks {@link AssetsUpdateManager#getCurrentPackage()} to return fake {@link AssetsLocalPackage}.
     *
     * @param packageHash hash to be set to fake package.
     */
    public void mockLocalPackageIntoUpdateManager(String packageHash, AssetsUpdateManager assetsUpdateManager) throws Exception {
        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(packageHash);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
    }

    /**
     * Creates and injects into {@link AssetsBaseCore} a fake instance of {@link AssetsAcquisitionManager}.
     *
     * @param configuration       instance of {@link AssetsConfiguration} which should be passed to
     *                            {@link AssetsAcquisitionManager#queryUpdateWithCurrentPackage(AssetsConfiguration, AssetsLocalPackage)}
     *                            in order to return the specified {@link AssetsRemotePackage}.
     * @param currentPackage      instance of {@link AssetsLocalPackage} which should be passed to
     *                            {@link AssetsAcquisitionManager#queryUpdateWithCurrentPackage(AssetsConfiguration, AssetsLocalPackage)}
     *                            in order to return the specified {@link AssetsRemotePackage}.
     * @param assetsRemotePackage {@link AssetsRemotePackage} which should be returned by
     *                            {@link AssetsAcquisitionManager#queryUpdateWithCurrentPackage(AssetsConfiguration, AssetsLocalPackage)}
     *                            if the conditions match.
     */
    public void mockAcquisitionManager(AssetsConfiguration configuration, AssetsLocalPackage currentPackage, AssetsRemotePackage assetsRemotePackage) throws Exception {
        AssetsAcquisitionManager assetsAcquisitionManager = mock(AssetsAcquisitionManager.class);
        when(assetsAcquisitionManager.queryUpdateWithCurrentPackage(eq(configuration), currentPackage == null ? any(AssetsLocalPackage.class) : eq(currentPackage))).thenReturn(assetsRemotePackage);
        injectManagersInCore(assetsAcquisitionManager);
    }

    /**
     * Builds common checkForUpdate workflow.
     */
    public void prepareCheckForUpdate() throws Exception {
        AssetsConfiguration configuration = mockConfiguration(null);
        AssetsLocalPackage currentPackage = mockLocalPackage("");
        AssetsRemotePackage assetsRemotePackage = createFakeRemotePackage();
        mockAcquisitionManager(configuration, currentPackage, assetsRemotePackage);

        when(mAssetsBaseCore.existsFailedUpdate(eq(PACKAGE_HASH))).thenReturn(FAILED_INSTALL);
    }

    /**
     * Calls {@link AssetsBaseCore#checkForUpdate()}
     *
     * @param deploymentKey deployment key parameter for the function.
     * @return returned instance of {@link AssetsRemotePackage}.
     */
    public AssetsRemotePackage callCheckForUpdate(String deploymentKey) throws Exception {
        doCallRealMethod().when(mAssetsBaseCore).checkForUpdate(any(String.class));
        return mAssetsBaseCore.checkForUpdate(deploymentKey);
    }

    /**
     * Calls {@link AssetsBaseCore#checkForUpdate()} with the standard deployment key.
     *
     * @return returned instance of {@link AssetsRemotePackage}.
     */
    public AssetsRemotePackage callCheckForUpdate() throws Exception {
        doCallRealMethod().when(mAssetsBaseCore).checkForUpdate(any(String.class));
        return mAssetsBaseCore.checkForUpdate(DEPLOYMENT_KEY);
    }

    /**
     * Ensures that {@link AssetsBaseCore#checkForUpdate()} returns <code>null</code>.
     */
    public void assertCheckForUpdateReturnsNull() throws Exception {
        assertNull(callCheckForUpdate());
    }

    /**
     * Mocks {@link AssetsBaseCore#getNativeConfiguration()} to return fake {@link AssetsConfiguration}.
     *
     * @param packageHash package hash that should be set on configuration.
     *                    if passed <code>null</code>, not set.
     * @return instance of fake configuration.
     */
    public AssetsConfiguration mockConfiguration(String packageHash) throws Exception {
        AssetsConfiguration configuration = mock(AssetsConfiguration.class);
        when(configuration.setDeploymentKey(eq(DEPLOYMENT_KEY))).thenReturn(null);
        when(mAssetsBaseCore.getNativeConfiguration()).thenReturn(configuration);
        if (packageHash != null) {
            when(configuration.getPackageHash()).thenReturn(packageHash);
        }
        return configuration;
    }

    /**
     * Creates a fake instance of {@link AssetsRemotePackage} with <code>updateAppVersion</code> equal to <code>false</code>.
     *
     * @return fake instance of {@link AssetsRemotePackage}.
     */
    public AssetsRemotePackage createFakeRemotePackage() {
        return createFakeRemotePackage(false);
    }

    /**
     * Creates a fake instance of {@link AssetsRemotePackage}.
     *
     * @param updateAppVersion value of updateAppVersion parameter.
     * @return fake instance of {@link AssetsRemotePackage}.
     */
    private AssetsRemotePackage createFakeRemotePackage(boolean updateAppVersion) {
        AssetsPackage assetsPackage = new AssetsPackage();
        assetsPackage.setPackageHash(PACKAGE_HASH);
        return AssetsRemotePackage.createRemotePackage(false, 1000, "", updateAppVersion, assetsPackage);
    }

    /**
     * Creates a fake instance of {@link AssetsRemotePackage} with <code>updateAppVersion</code> equal to <code>true</code>.
     *
     * @return fake instance of {@link AssetsRemotePackage}.
     */
    public AssetsRemotePackage createFakeRemotePackageUpdateAppVersion() {
        return createFakeRemotePackage(true);
    }

    /**
     * Calls {@link AssetsBaseCore#getUpdateMetadata()}.
     *
     * @return returned instance of {@link AssetsLocalPackage}.
     */
    public AssetsLocalPackage callGetUpdateMetadata() throws Exception {
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata();
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));
        return mAssetsBaseCore.getUpdateMetadata();
    }

    /**
     * Calls {@link AssetsBaseCore#getUpdateMetadata()}.
     *
     * @param assetsUpdateState {@link AssetsUpdateState} to call the method with.
     * @return returned instance of {@link AssetsLocalPackage}.
     */
    public AssetsLocalPackage callGetUpdateMetadata(AssetsUpdateState assetsUpdateState) throws Exception {
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata();
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));
        return mAssetsBaseCore.getUpdateMetadata(assetsUpdateState);
    }

    /**
     * Ensures that {@link AssetsBaseCore#getUpdateMetadata()} returns <code>null</code>.
     *
     * @param assetsUpdateState {@link AssetsUpdateState} to call the method with.
     */
    public void assertGetUpdateMetadataReturnsNull(AssetsUpdateState assetsUpdateState) throws Exception {
        assertNull(callGetUpdateMetadata(assetsUpdateState));
    }

    /**
     * Ensures that {@link AssetsBaseCore#getUpdateMetadata()} returns <code>null</code>.
     */
    public void assertGetUpdateMetadataReturnsNull() throws Exception {
        assertNull(callGetUpdateMetadata());
    }


    public void mockTextUtils() {
        PowerMockito.mockStatic(TextUtils.class);

        // An android class TextUtils does not work correctly in unit test section.
        // We need to mock its methods if we want different behavior.
        when(TextUtils.isEmpty(any(String.class))).thenReturn(true);
    }
}
