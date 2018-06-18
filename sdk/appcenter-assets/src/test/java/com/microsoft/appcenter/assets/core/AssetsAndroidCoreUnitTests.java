package com.microsoft.appcenter.assets.core;

import android.text.TextUtils;

import com.microsoft.appcenter.assets.AssetsConfiguration;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsSyncOptions;
import com.microsoft.appcenter.assets.enums.AssetsUpdateState;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;
import com.microsoft.appcenter.assets.exceptions.AssetsQueryUpdateException;
import com.microsoft.appcenter.assets.managers.AssetsAcquisitionManager;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.CHECKING_FOR_UPDATE;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.SYNC_IN_PROGRESS;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UPDATE_INSTALLED;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UP_TO_DATE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

//@RunWith(PowerMockRunner.class)
@PrepareForTest({AssetsBaseCore.class, TextUtils.class})
public class AssetsAndroidCoreUnitTests {
    private AssetsBaseCore mAssetsBaseCore;
    final static String PACKAGE_HASH = "hash";
    final static boolean FAILED_INSTALL = true;
    final static boolean IS_FIRST_RUN = false;
    final static String DEPLOYMENT_KEY = "key";
    private CoreTestUtils mCoreTestUtils;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() {
        mAssetsBaseCore = Mockito.mock(AssetsBaseCore.class);
        mCoreTestUtils = new CoreTestUtils(mAssetsBaseCore);
    }

    //region sync

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

    //endregion sync

    //region getUpdateMetadata

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw {@link AssetsNativeApiCallException}
     * if a {@link AssetsGetPackageException} has occurred when trying to get package via
     * {@link AssetsUpdateManager#getCurrentPackage()}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIfGetCurrentPackageFails() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        when(assetsUpdateManager.getCurrentPackage()).thenThrow(AssetsGetPackageException.class);

        mCoreTestUtils.injectManagersInCore(assetsUpdateManager);
        mCoreTestUtils.callGetUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateManager#getCurrentPackage()} returns <code>null </code>.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfCurrentPackageNull() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(null);

        mCoreTestUtils.injectManagersInCore(assetsUpdateManager);
        mCoreTestUtils.assertGetUpdateMetadataReturnsNull();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateState#PENDING} was requested and current package is not pending.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfRequestedPendingAndItsNull() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        PowerMockito.mockStatic(TextUtils.class);

        // An android class TextUtils does not work correctly in unit test section.
        // We need to mock its methods if we want different behavior.
        when(TextUtils.isEmpty(any(String.class))).thenReturn(true);

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(null, assetsUpdateManager);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(false);

        mCoreTestUtils.injectManagersInCore(assetsUpdateManager, settingsManager);
        mCoreTestUtils.assertGetUpdateMetadataReturnsNull(AssetsUpdateState.PENDING);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw a {@link AssetsNativeApiCallException}
     * if {@link SettingsManager#isPendingUpdate(String)} throws {@link AssetsMalformedDataException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIfSettingsManagerThrows() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(PACKAGE_HASH, assetsUpdateManager);

        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenThrow(AssetsMalformedDataException.class);

        mCoreTestUtils.injectManagersInCore(assetsUpdateManager, settingsManager);
        mCoreTestUtils.callGetUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the previous package
     * if {@link AssetsUpdateState#RUNNING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPreviousPackageIfCurrentUpdateIsPending() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(PACKAGE_HASH, assetsUpdateManager);

        AssetsLocalPackage previousPackage = mock(AssetsLocalPackage.class);
        when(assetsUpdateManager.getPreviousPackage()).thenReturn(previousPackage);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(true);

        mCoreTestUtils.injectManagersInCore(assetsUpdateManager, settingsManager);

        assertEquals(mCoreTestUtils.callGetUpdateMetadata(null), previousPackage);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw a {@link AssetsNativeApiCallException}
     * if {@link AssetsUpdateState#RUNNING} was requested, current package is pending,
     * and {@link AssetsUpdateManager#getPreviousPackage()} throws {@link AssetsGetPackageException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIFGetPreviousPackageFails() throws Exception {
        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(PACKAGE_HASH, assetsUpdateManager);

        when(assetsUpdateManager.getPreviousPackage()).thenThrow(AssetsGetPackageException.class);
        when(settingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(true);

        mCoreTestUtils.injectManagersInCore(assetsUpdateManager, settingsManager);
        mCoreTestUtils.callGetUpdateMetadata(null);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the valid pending package
     * if {@link AssetsUpdateState#PENDING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPendingIfRequested() throws Exception {
        mCoreTestUtils.prepareGetUpdateWorkflow(true);

        AssetsState assetsState = new AssetsState();
        assetsState.mIsRunningBinaryVersion = false;
        MemberModifier.field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        AssetsLocalPackage returnedPackage = mCoreTestUtils.getUpdatePackage(true);
        assertEquals(returnedPackage.isDebugOnly(), false);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the valid running package
     * if {@link AssetsUpdateState#RUNNING} was requested and current package is not pending.
     */
    @Test
    public void getUpdateMetadataReturnsRunningIfRequested() throws Exception {
        mCoreTestUtils.prepareGetUpdateWorkflow(false);

        AssetsState assetsState = new AssetsState();
        assetsState.mIsRunningBinaryVersion = false;
        MemberModifier.field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        AssetsLocalPackage returnedPackage = mCoreTestUtils.getUpdatePackage(false);
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
        mCoreTestUtils.prepareGetUpdateWorkflow(true);

        AssetsState assetsState = new AssetsState();
        assetsState.mIsRunningBinaryVersion = true;
        MemberModifier.field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);

        AssetsLocalPackage returnedPackage = mCoreTestUtils.getUpdatePackage(true);
        assertEquals(returnedPackage.isDebugOnly(), true);
    }
    //endregion

    //region checkForUpdate

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should throw a {@link AssetsNativeApiCallException}
     * if setting configuration deployment key has thrown {@link AssetsIllegalArgumentException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = AssetsNativeApiCallException.class)
    public void checkForUpdateFailsIfSetDepKeyThrows() throws Exception {
        AssetsConfiguration configuration = mock(AssetsConfiguration.class);
        when(configuration.setDeploymentKey(DEPLOYMENT_KEY)).thenThrow(AssetsIllegalArgumentException.class);
        when(mAssetsBaseCore.getNativeConfiguration()).thenReturn(configuration);
        mCoreTestUtils.callCheckForUpdate();
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return a valid {@link AssetsRemotePackage}.
     */
    @Test
    public void checkForUpdateReturnPackage() throws Exception {
        mCoreTestUtils.prepareCheckForUpdate();
        AssetsRemotePackage returnedPackage = mCoreTestUtils.callCheckForUpdate();
        assertEquals(returnedPackage.getDeploymentKey(), DEPLOYMENT_KEY);
        assertEquals(returnedPackage.isFailedInstall(), FAILED_INSTALL);
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return a valid {@link AssetsRemotePackage}
     * if passed <code>deploymentKey</code> equals <code>null</code>.
     */
    @Test
    public void checkForUpdateReturnPackageDeploymentKeyNull() throws Exception {
        mCoreTestUtils.prepareCheckForUpdate();
        AssetsRemotePackage returnedPackage = mCoreTestUtils.callCheckForUpdate(null);
        assertNull(returnedPackage.getDeploymentKey());
        assertEquals(returnedPackage.isFailedInstall(), FAILED_INSTALL);
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return <code>null</code>
     * if {@link AssetsAcquisitionManager#queryUpdateWithCurrentPackage(AssetsConfiguration, AssetsLocalPackage)}
     * returns <code>null</code>
     */
    @Test
    public void checkForUpdateReturnsNullOnNullPackage() throws Exception {
        AssetsConfiguration configuration = mCoreTestUtils.mockConfiguration(null);
        AssetsLocalPackage currentPackage = mCoreTestUtils.mockLocalPackage();

        mCoreTestUtils.mockAcquisitionManager(configuration, currentPackage, null);
        mCoreTestUtils.assertCheckForUpdateReturnsNull();
    }

    @Captor
    private ArgumentCaptor<AssetsRemotePackage> assetsRemotePackageArgumentCaptor;

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return <code>null</code>
     * and call {@link AssetsBaseCore#notifyAboutBinaryVersionMismatchChange(AssetsRemotePackage)}
     * if {@link AssetsAcquisitionManager#queryUpdateWithCurrentPackage(AssetsConfiguration, AssetsLocalPackage)}
     * returns a {@link AssetsRemotePackage} where <code>isUpdateAppVersion</code> equals <code>true</code>.
     */
    @Test
    public void checkForUpdateReturnsNullIfUpdateAppVersion() throws Exception {
        AssetsConfiguration configuration = mCoreTestUtils.mockConfiguration(null);
        AssetsLocalPackage currentPackage = mCoreTestUtils.mockLocalPackage("");
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackageUpdateAppVersion();
        mCoreTestUtils.mockAcquisitionManager(configuration, currentPackage, assetsRemotePackage);

        mCoreTestUtils.assertCheckForUpdateReturnsNull();

        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutBinaryVersionMismatchChange", assetsRemotePackageArgumentCaptor.capture());
        assertEquals(assetsRemotePackageArgumentCaptor.getValue(), assetsRemotePackage);
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return <code>null</code>
     * if {@link AssetsLocalPackage#packageHash} equals {@link AssetsRemotePackage#packageHash}.
     */
    @Test
    public void checkForUpdateReturnsNullIfHashesEqual() throws Exception {
        AssetsConfiguration configuration = mCoreTestUtils.mockConfiguration(null);
        AssetsLocalPackage currentPackage = mCoreTestUtils.mockLocalPackage(PACKAGE_HASH);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        mCoreTestUtils.mockAcquisitionManager(configuration, currentPackage, assetsRemotePackage);

        mCoreTestUtils.assertCheckForUpdateReturnsNull();
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return <code>null</code>
     * if {@link AssetsLocalPackage} equals <code>null</code> and
     * {@link AssetsConfiguration#packageHash} equals {@link AssetsRemotePackage#packageHash}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void checkForUpdateReturnsNullIfHashEqualAndNoLocal() throws Exception {
        AssetsConfiguration configuration = mCoreTestUtils.mockConfiguration(PACKAGE_HASH);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(null);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        mCoreTestUtils.mockAcquisitionManager(configuration, null, assetsRemotePackage);

        mCoreTestUtils.assertCheckForUpdateReturnsNull();
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return valid package
     * if {@link AssetsLocalPackage} equals <code>null</code> and
     * {@link AssetsConfiguration#packageHash} not equals {@link AssetsRemotePackage#packageHash}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void checkForUpdateReturnsWithNoLocal() throws Exception {
        AssetsConfiguration configuration = mCoreTestUtils.mockConfiguration("");
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(null);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        mCoreTestUtils.mockAcquisitionManager(configuration, null, assetsRemotePackage);

        AssetsRemotePackage returnedPackage = mCoreTestUtils.callCheckForUpdate();
        assertEquals(returnedPackage.getPackageHash(), PACKAGE_HASH);
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should return <code>null</code>
     * if {@link AssetsLocalPackage#isDebugOnly} equals <code>true</code> and
     * {@link AssetsConfiguration#packageHash} equals {@link AssetsRemotePackage#packageHash}.
     */
    @Test
    public void checkForUpdateReturnsNullLocalIsDebugAndHashesEqual() throws Exception {
        AssetsConfiguration configuration = mCoreTestUtils.mockConfiguration(PACKAGE_HASH);
        AssetsLocalPackage currentPackage = mCoreTestUtils.mockLocalPackage(PACKAGE_HASH);
        when(currentPackage.isDebugOnly()).thenReturn(true);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        mCoreTestUtils.mockAcquisitionManager(configuration, null, assetsRemotePackage);

        mCoreTestUtils.assertCheckForUpdateReturnsNull();
    }

    /**
     * {@link AssetsBaseCore#checkForUpdate()} should throw a {@link AssetsNativeApiCallException}
     * if {@link AssetsAcquisitionManager#queryUpdateWithCurrentPackage(AssetsConfiguration, AssetsLocalPackage)}
     * throws a {@link AssetsQueryUpdateException}.
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    @Test(expected = AssetsNativeApiCallException.class)
    public void checkForUpdateFailsIfQueryThrows() throws Exception {
        AssetsConfiguration configuration = mCoreTestUtils.mockConfiguration(null);
        when(configuration.getAppVersion()).thenReturn("1.0");
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(null);
        AssetsAcquisitionManager assetsAcquisitionManager = mock(AssetsAcquisitionManager.class);

        when(assetsAcquisitionManager.queryUpdateWithCurrentPackage(eq(configuration), any(AssetsLocalPackage.class))).thenThrow(AssetsQueryUpdateException.class);
        mCoreTestUtils.injectManagersInCore(assetsAcquisitionManager);
        mCoreTestUtils.callCheckForUpdate(DEPLOYMENT_KEY);
    }

    //endregion
}
