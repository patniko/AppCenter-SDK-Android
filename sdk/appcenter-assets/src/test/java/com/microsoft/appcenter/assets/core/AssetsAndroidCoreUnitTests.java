package com.microsoft.appcenter.assets.core;

import android.text.TextUtils;

import com.microsoft.appcenter.assets.AssetsConfiguration;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsSyncOptions;
import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;
import com.microsoft.appcenter.assets.enums.AssetsUpdateState;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;
import com.microsoft.appcenter.assets.managers.AssetsAcquisitionManager;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static com.microsoft.appcenter.assets.core.CoreTestUtils.injectManagersInCore;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.CHECKING_FOR_UPDATE;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.SYNC_IN_PROGRESS;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UPDATE_INSTALLED;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UP_TO_DATE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

//@RunWith(PowerMockRunner.class)
@PrepareForTest({AssetsBaseCore.class, TextUtils.class})
public class AssetsAndroidCoreUnitTests {
    private AssetsBaseCore mAssetsBaseCore;
    private final static String PACKAGE_HASH = "hash";
    private final static boolean FAILED_INSTALL = true;
    private final static boolean IS_FIRST_RUN = false;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() {
        mAssetsBaseCore = Mockito.mock(AssetsBaseCore.class);
    }

    @Test
    public void syncInProgressTest() throws Exception {
        AssetsState assetsState = new AssetsState();
        assetsState.mSyncInProgress = true;

        MemberModifier
                .field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        doCallRealMethod().when(mAssetsBaseCore).sync();
        doCallRealMethod().when(mAssetsBaseCore).sync(any(AssetsSyncOptions.class));
        mAssetsBaseCore.sync();

        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutSyncStatusChange", SYNC_IN_PROGRESS);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(0)).invoke("getNativeConfiguration");
    }

    @Test(expected = AssetsNativeApiCallException.class)
    public void syncOptionsNotDefinedTest() throws Exception {
        AssetsSyncOptions assetsSyncOptions = mock(AssetsSyncOptions.class);
        when(assetsSyncOptions.getDeploymentKey()).thenReturn(null, "fake-deployment-key", null);
        when(assetsSyncOptions.getCheckFrequency()).thenReturn(null);
        when(assetsSyncOptions.getInstallMode()).thenReturn(null);
        when(assetsSyncOptions.getMandatoryInstallMode()).thenReturn(null);

        when(mAssetsBaseCore.getNativeConfiguration()).thenReturn(new AssetsConfiguration());

        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenReturn(true);

        AssetsState assetsState = new AssetsState();
        MemberModifier
                .field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        doCallRealMethod().when(mAssetsBaseCore).sync(assetsSyncOptions);
        mAssetsBaseCore.sync(assetsSyncOptions);
    }

    @Test
    public void syncUpToDateTest() throws Exception {
        AssetsState assetsState = new AssetsState();
        MemberModifier
                .field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        when(mAssetsBaseCore.checkForUpdate()).thenReturn(null);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(null);

        doCallRealMethod().when(mAssetsBaseCore).sync();
        doCallRealMethod().when(mAssetsBaseCore).sync(any(AssetsSyncOptions.class));
        mAssetsBaseCore.sync();

        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutSyncStatusChange", CHECKING_FOR_UPDATE);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutSyncStatusChange", UP_TO_DATE);
        assertFalse(assetsState.mSyncInProgress);
    }

    @Test
    public void syncUpdateInstalledTest() throws Exception {
        AssetsState assetsState = new AssetsState();
        MemberModifier
                .field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        when(mAssetsBaseCore.checkForUpdate()).thenReturn(null);

        AssetsLocalPackage assetsLocalPackage = mock(AssetsLocalPackage.class);
        when(assetsLocalPackage.isPending()).thenReturn(true);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(assetsLocalPackage);

        doCallRealMethod().when(mAssetsBaseCore).sync();
        doCallRealMethod().when(mAssetsBaseCore).sync(any(AssetsSyncOptions.class));
        mAssetsBaseCore.sync();

        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutSyncStatusChange", CHECKING_FOR_UPDATE);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutSyncStatusChange", UPDATE_INSTALLED);
        assertFalse(assetsState.mSyncInProgress);
    }

    @Test
    public void syncDoDownloadAndInstallTest() throws Exception {
        mAssetsBaseCore = spy(mAssetsBaseCore);

        AssetsState assetsState = new AssetsState();
        MemberModifier
                .field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        AssetsRemotePackage assetsRemotePackage = new AssetsRemotePackage();
        when(mAssetsBaseCore.checkForUpdate(any(String.class))).thenReturn(assetsRemotePackage);

        MemberModifier
                .stub(MemberMatcher.method(AssetsBaseCore.class,
                        "doDownloadAndInstall"))
                .toReturn(null);

        doCallRealMethod().when(mAssetsBaseCore).sync();
        doCallRealMethod().when(mAssetsBaseCore).sync(any(AssetsSyncOptions.class));
        mAssetsBaseCore.sync();

        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutSyncStatusChange", CHECKING_FOR_UPDATE);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("doDownloadAndInstall", assetsRemotePackage, any(AssetsSyncOptions.class), null);
        assertFalse(assetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw {@link AssetsNativeApiCallException}
     * if a {@link AssetsGetPackageException} has occurred when trying to get package via
     * {@link AssetsUpdateManager#getCurrentPackage()}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIfGetCurrentPackageFails() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        when(assetsUpdateManager.getCurrentPackage()).thenThrow(AssetsGetPackageException.class);

        injectManagersInCore(assetsUpdateManager, mAssetsBaseCore);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata();
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));
        mAssetsBaseCore.getUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateManager#getCurrentPackage()} returns <code>null </code>.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfCurrentPackageNull() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(null);

        injectManagersInCore(assetsUpdateManager, mAssetsBaseCore);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata();
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));

        assertNull(mAssetsBaseCore.getUpdateMetadata());
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateState#PENDING} was requested and current package is not pending.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfRequestedPendingAndItsNull() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(false);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsBaseCore);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));

        assertNull(mAssetsBaseCore.getUpdateMetadata(AssetsUpdateState.PENDING));
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw a {@link AssetsNativeApiCallException}
     * if {@link SettingsManager#isPendingUpdate(String)} throws {@link AssetsMalformedDataException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIfSettingsManagerThrows() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenThrow(AssetsMalformedDataException.class);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsBaseCore);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata();
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));

        mAssetsBaseCore.getUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the previous package
     * if {@link AssetsUpdateState#RUNNING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPreviousPackageIfCurrentUpdateIsPending() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        AssetsLocalPackage previousPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(assetsUpdateManager.getPreviousPackage()).thenReturn(previousPackage);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(true);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsBaseCore);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));

        assertEquals(mAssetsBaseCore.getUpdateMetadata(null), previousPackage);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw a {@link AssetsNativeApiCallException}
     * if {@link AssetsUpdateState#RUNNING} was requested, current package is pending,
     * and {@link AssetsUpdateManager#getPreviousPackage()} throws {@link AssetsGetPackageException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIFGetPreviousPackageFails() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(assetsUpdateManager.getPreviousPackage()).thenThrow(AssetsGetPackageException.class);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(true);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsBaseCore);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));

        mAssetsBaseCore.getUpdateMetadata(null);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the valid pending package
     * if {@link AssetsUpdateState#PENDING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPendingIfRequested() throws Exception {
        prepareGetPendingUpdateWorkflow();

        AssetsState assetsState = new AssetsState();
        assetsState.mIsRunningBinaryVersion = false;
        MemberModifier.field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata();

        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));
        AssetsLocalPackage returnedPackage = getPendingUpdatePackage();
        assertEquals(returnedPackage.isDebugOnly(), false);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the valid pending package
     * if {@link AssetsUpdateState#PENDING} was requested and current package is pending.
     * {@link AssetsLocalPackage#isDebugOnly} should equal to <code>true</code> if
     * {@link AssetsState#mIsRunningBinaryVersion} equals to <code>true</code>.
     */
    @Test
    public void getUpdateMetadataReturnsPendingIfRequestedRunningBinaryVersion() throws Exception {
        prepareGetPendingUpdateWorkflow();

        AssetsState assetsState = new AssetsState();
        assetsState.mIsRunningBinaryVersion = true;
        MemberModifier.field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        AssetsLocalPackage returnedPackage = getPendingUpdatePackage();
        assertEquals(returnedPackage.isDebugOnly(), true);
    }

    /**
     * Retrieves and checks pending update package.
     *
     * @return pending update package.
     */
    private AssetsLocalPackage getPendingUpdatePackage() throws Exception {
        AssetsLocalPackage returnedPackage = mAssetsBaseCore.getUpdateMetadata(AssetsUpdateState.PENDING);
        assertEquals(returnedPackage.getPackageHash(), PACKAGE_HASH);
        assertEquals(returnedPackage.isPending(), true);
        assertEquals(returnedPackage.isFailedInstall(), FAILED_INSTALL);
        assertEquals(returnedPackage.isFirstRun(), IS_FIRST_RUN);
        return returnedPackage;
    }

    /**
     * Prepares classes for testing {@link AssetsAndroidCore#getUpdateMetadata()} workflow
     * which should return a pending update.
     */
    private void prepareGetPendingUpdateWorkflow() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = AssetsLocalPackage.createEmptyPackageForCheckForUpdateQuery("1.0");
        currentPackage.setPackageHash(PACKAGE_HASH);
        currentPackage.setDebugOnly(false);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(true);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsBaseCore);
        when(mAssetsBaseCore.existsFailedUpdate(eq(PACKAGE_HASH))).thenReturn(FAILED_INSTALL);
        when(mAssetsBaseCore.isFirstRun(eq(PACKAGE_HASH))).thenReturn(IS_FIRST_RUN);
        doCallRealMethod().when(mAssetsBaseCore).getUpdateMetadata(any(AssetsUpdateState.class));
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should throw a {@link AssetsNativeApiCallException}
     * if setting configuration deployemnt key has thrown {@link AssetsIllegalArgumentException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void checkForUpdateFailsIfSetDepKeyThrows() throws Exception {
        AssetsConfiguration configuration = mock(AssetsConfiguration.class);
        String deploymentKey = "key";
        when(configuration.setDeploymentKey(deploymentKey)).thenThrow(AssetsIllegalArgumentException.class);
        when(mAssetsBaseCore.getNativeConfiguration()).thenReturn(configuration);
        doCallRealMethod().when(mAssetsBaseCore).checkForUpdate(any(String.class));
        mAssetsBaseCore.checkForUpdate(deploymentKey);
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return a valid {@link AssetsRemotePackage}.
     */
    @Test
    public void checkForUpdateReturnPackage() throws Exception {
        AssetsConfiguration configuration = mock(AssetsConfiguration.class);
        String deploymentKey = "key";
        when(mAssetsBaseCore.getNativeConfiguration()).thenReturn(configuration);
        when(configuration.setDeploymentKey(eq(deploymentKey))).thenReturn(null);
        doCallRealMethod().when(mAssetsBaseCore).checkForUpdate(any(String.class));

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn("");
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(currentPackage);
        AssetsAcquisitionManager assetsAcquisitionManager = mock(AssetsAcquisitionManager.class);

        AssetsPackage assetsPackage = new AssetsPackage();
        assetsPackage.setPackageHash(PACKAGE_HASH);
        AssetsRemotePackage assetsRemotePackage = AssetsRemotePackage.createRemotePackage(false, 1000, "", false, assetsPackage);

        when(assetsAcquisitionManager.queryUpdateWithCurrentPackage(eq(configuration), eq(currentPackage))).thenReturn(assetsRemotePackage);
        injectManagersInCore(assetsAcquisitionManager, mAssetsBaseCore);
        when(mAssetsBaseCore.existsFailedUpdate(eq(PACKAGE_HASH))).thenReturn(FAILED_INSTALL);
        AssetsRemotePackage returnedPackage = mAssetsBaseCore.checkForUpdate(deploymentKey);

        assertEquals(returnedPackage.getDeploymentKey(), deploymentKey);
        assertEquals(returnedPackage.isFailedInstall(), FAILED_INSTALL);
    }
}
