package com.microsoft.appcenter.assets.core;

import android.text.TextUtils;

import com.microsoft.appcenter.assets.AssetsConfiguration;
import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.apirequests.DownloadPackageTask;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPendingUpdate;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsSyncOptions;
import com.microsoft.appcenter.assets.enums.AssetsInstallMode;
import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;
import com.microsoft.appcenter.assets.enums.AssetsUpdateState;
import com.microsoft.appcenter.assets.exceptions.AssetsDownloadPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;
import com.microsoft.appcenter.assets.exceptions.AssetsInstallException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsMergeException;
import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;
import com.microsoft.appcenter.assets.exceptions.AssetsQueryUpdateException;
import com.microsoft.appcenter.assets.exceptions.AssetsUnzipException;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;
import com.microsoft.appcenter.assets.managers.AssetsAcquisitionManager;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.io.IOException;

import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.CHECKING_FOR_UPDATE;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.SYNC_IN_PROGRESS;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UNKNOWN_ERROR;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UPDATE_INSTALLED;
import static com.microsoft.appcenter.assets.enums.AssetsSyncStatus.UP_TO_DATE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

//@RunWith(PowerMockRunner.class)
@PrepareForTest({AssetsBaseCore.class, TextUtils.class, AppCenterLog.class})
public class AssetsAndroidCoreUnitTests {
    private AssetsBaseCore mAssetsBaseCore;
    final static String PACKAGE_HASH = "hash";
    final static boolean FAILED_INSTALL = true;
    final static boolean IS_FIRST_RUN = false;
    final static String DEPLOYMENT_KEY = "key";
    private final static String ENTRY_POINT = "entryPoint";
    private final static String NEW_UPDATE_FOLDER = "newUpdateFolder";
    private final static String NEW_UPDATE_METADATA = "metadataPath";
    final static String DOWNLOAD_URL = "url";
    private final static boolean IS_ZIP = true;
    private final static long BINARY_MODIFIED = 0L;
    private final static File DOWNLOAD_FILE = new File("");
    private AssetsUpdateManager mAssetsUpdateManager;
    private SettingsManager mSettingsManager;
    private AssetsConfiguration mConfiguration;
    private CoreTestUtils mCoreTestUtils;
    private FileUtils mFileUtils;
    private AssetsUtils mAssetsUtils;
    private AssetsState mAssetsState;

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() throws  Exception{
        mAssetsBaseCore = Mockito.mock(AssetsBaseCore.class);
        mCoreTestUtils = new CoreTestUtils(mAssetsBaseCore);

        mCoreTestUtils.mockContext();
        mAssetsState = mCoreTestUtils.mockState(false, false);
        mCoreTestUtils.mockEntryPoint(ENTRY_POINT);
        mCoreTestUtils.mockDoDownloadAndInstall();

        mAssetsUpdateManager = mock(AssetsUpdateManager.class);
        mConfiguration = mock(AssetsConfiguration.class);
        mSettingsManager = mock(SettingsManager.class);
        mAssetsUtils = mock(AssetsUtils.class);
        mFileUtils = mock(FileUtils.class);
    }

    //region sync

    /**
     * {@link AssetsBaseCore#sync()} should call {@link AssetsSyncStatus#SYNC_IN_PROGRESS},
     * if {@link AssetsState#mSyncInProgress} already equals <code>true</code>.
     */
    @Test
    public void syncInProgressTest() throws Exception {
        mAssetsState = mCoreTestUtils.mockState(false, true);
        mCoreTestUtils.callSync();

        mCoreTestUtils.verifyNotifySyncStatusCalled(SYNC_IN_PROGRESS);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(0)).invoke("getNativeConfiguration");
    }

    /**
     * {@link AssetsBaseCore#sync()} should throws a {@link AssetsNativeApiCallException}
     * if an attempt to set <code>null</code> deploymentKey was made.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void syncOptionsNotDefinedTest() throws Exception {
        AssetsSyncOptions assetsSyncOptions = mCoreTestUtils.mockSyncOptions(null, false);
        when(assetsSyncOptions.getDeploymentKey()).thenReturn(null, DEPLOYMENT_KEY, null);

        when(mAssetsBaseCore.getNativeConfiguration()).thenReturn(new AssetsConfiguration());

        mCoreTestUtils.mockTextUtils();

        mCoreTestUtils.callSync(assetsSyncOptions);
    }

    /**
     * {@link AssetsBaseCore#sync()} should call {@link AssetsSyncStatus#CHECKING_FOR_UPDATE},
     * then {@link AssetsSyncStatus#UP_TO_DATE} if {@link AssetsRemotePackage} equals <code>null</code>
     * and {@link AssetsLocalPackage} equals <code>null</code>.
     */
    @Test
    public void syncUpToDateTest() throws Exception {
        when(mAssetsBaseCore.checkForUpdate()).thenReturn(null);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(null);

        mCoreTestUtils.callSync();

        mCoreTestUtils.verifyNotifySyncStatusCalled(CHECKING_FOR_UPDATE);
        mCoreTestUtils.verifyNotifySyncStatusCalled(UP_TO_DATE);
        assertFalse(mAssetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#sync()} should call {@link AssetsSyncStatus#CHECKING_FOR_UPDATE},
     * then {@link AssetsSyncStatus#UP_TO_DATE} if {@link AssetsRemotePackage} equals <code>null</code>
     * and {@link AssetsLocalPackage#isPending} equals <code>false</code>.
     */
    @Test
    public void syncUpToDateTest2() throws Exception {
        when(mAssetsBaseCore.checkForUpdate()).thenReturn(null);
        AssetsLocalPackage assetsLocalPackage = mock(AssetsLocalPackage.class);
        when(assetsLocalPackage.isPending()).thenReturn(false);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(assetsLocalPackage);

        mCoreTestUtils.callSync();

        mCoreTestUtils.verifyNotifySyncStatusCalled(CHECKING_FOR_UPDATE);
        mCoreTestUtils.verifyNotifySyncStatusCalled(UP_TO_DATE);
        assertFalse(mAssetsState.mSyncInProgress);
    }


    /**
     * {@link AssetsBaseCore#sync()} should call {@link AssetsSyncStatus#CHECKING_FOR_UPDATE},
     * then {@link AssetsSyncStatus#UNKNOWN_ERROR} in case of any error.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void syncCheckForUpdateFails() throws Exception {
        when(mAssetsBaseCore.checkForUpdate(anyString())).thenThrow(AssetsNativeApiCallException.class);
        AssetsLocalPackage assetsLocalPackage = mock(AssetsLocalPackage.class);
        when(assetsLocalPackage.isPending()).thenReturn(false);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(assetsLocalPackage);

        mCoreTestUtils.callSync();

        mCoreTestUtils.verifyNotifySyncStatusCalled(CHECKING_FOR_UPDATE);
        mCoreTestUtils.verifyNotifySyncStatusCalled(UNKNOWN_ERROR);
        assertFalse(mAssetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#sync()} should call {@link AssetsSyncStatus#CHECKING_FOR_UPDATE},
     * then {@link AssetsSyncStatus#UPDATE_INSTALLED}.
     */
    @Test
    public void syncUpdateInstalledTest() throws Exception {
        when(mAssetsBaseCore.checkForUpdate()).thenReturn(null);

        AssetsLocalPackage assetsLocalPackage = mock(AssetsLocalPackage.class);
        when(assetsLocalPackage.isPending()).thenReturn(true);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(assetsLocalPackage);
        mCoreTestUtils.callSync();
        mCoreTestUtils.verifyNotifySyncStatusCalled(CHECKING_FOR_UPDATE);
        mCoreTestUtils.verifyNotifySyncStatusCalled(UPDATE_INSTALLED);
        assertFalse(mAssetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#sync()} should ignore failed updates if {@link AssetsRemotePackage#failedInstall}
     * equals <code>true</code> and {@link AssetsSyncOptions#ignoreFailedUpdates} equals <code>true</code>.
     */
    @Test
    public void syncIgnoreFailedUpdates() throws Exception {
        mCoreTestUtils.mockConfiguration(PACKAGE_HASH);
        AssetsRemotePackage assetsRemotePackage = mock(AssetsRemotePackage.class);
        when(assetsRemotePackage.isFailedInstall()).thenReturn(true);

        when(mAssetsBaseCore.checkForUpdate(eq(DEPLOYMENT_KEY))).thenReturn(assetsRemotePackage);

        AssetsLocalPackage assetsLocalPackage = mock(AssetsLocalPackage.class);
        when(assetsLocalPackage.isPending()).thenReturn(true);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(assetsLocalPackage);

        AssetsSyncOptions assetsSyncOptions = mCoreTestUtils.mockSyncOptions(DEPLOYMENT_KEY,true);

        mCoreTestUtils.callSync(assetsSyncOptions);

        mCoreTestUtils.verifyNotifySyncStatusCalled(CHECKING_FOR_UPDATE);
        mCoreTestUtils.verifyNotifySyncStatusCalled(UPDATE_INSTALLED);
        assertFalse(mAssetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#sync()} should not ignore failed updates if {@link AssetsRemotePackage#failedInstall}
     * equals <code>false</code>.
     */
    @Test
    public void syncNotIgnoreFailedUpdatesNoFailedInstalls() throws Exception {
        mCoreTestUtils.mockConfiguration(PACKAGE_HASH);
        AssetsRemotePackage assetsRemotePackage = mock(AssetsRemotePackage.class);
        when(assetsRemotePackage.isFailedInstall()).thenReturn(false);

        when(mAssetsBaseCore.checkForUpdate(eq(DEPLOYMENT_KEY))).thenReturn(assetsRemotePackage);

        AssetsLocalPackage assetsLocalPackage = mock(AssetsLocalPackage.class);
        when(assetsLocalPackage.isPending()).thenReturn(true);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(assetsLocalPackage);

        AssetsSyncOptions assetsSyncOptions = mCoreTestUtils.mockSyncOptions(DEPLOYMENT_KEY,true);

        mCoreTestUtils.callSync(assetsSyncOptions);

        mCoreTestUtils.verifyNotifySyncStatusCalled(CHECKING_FOR_UPDATE);
        assertFalse(mAssetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#sync()} should not ignore failed updates if {@link AssetsRemotePackage#failedInstall}
     * equals <code>true</code> and {@link AssetsSyncOptions#ignoreFailedUpdates} equals <code>false</code>.
     */
    @Test
    public void syncNotIgnoreFailedUpdates() throws Exception {
        mCoreTestUtils.mockConfiguration(PACKAGE_HASH);
        AssetsRemotePackage assetsRemotePackage = mock(AssetsRemotePackage.class);
        when(assetsRemotePackage.isFailedInstall()).thenReturn(true);
        when(mAssetsBaseCore.checkForUpdate(anyString())).thenReturn(assetsRemotePackage);

        AssetsLocalPackage assetsLocalPackage = mock(AssetsLocalPackage.class);
        when(assetsLocalPackage.isPending()).thenReturn(true);
        when(mAssetsBaseCore.getCurrentPackage()).thenReturn(assetsLocalPackage);

        AssetsSyncOptions assetsSyncOptions = mCoreTestUtils.mockSyncOptions(DEPLOYMENT_KEY,false);

        mCoreTestUtils.callSync(assetsSyncOptions);
        mCoreTestUtils.verifyNotifySyncStatusCalled(CHECKING_FOR_UPDATE);
        assertFalse(mAssetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#sync()} should call {@link AssetsBaseCore#doDownloadAndInstall(AssetsRemotePackage, AssetsSyncOptions, AssetsConfiguration)}
     * if {@link AssetsSyncOptions#updateDialog} equals <code>null</code>.
     */
    @Test
    public void syncDoDownloadAndInstallTest() throws Exception {
        AssetsRemotePackage assetsRemotePackage = new AssetsRemotePackage();
        when(mAssetsBaseCore.checkForUpdate(any(String.class))).thenReturn(assetsRemotePackage);
        mCoreTestUtils.callSync();

        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("notifyAboutSyncStatusChange", CHECKING_FOR_UPDATE);
        assertFalse(mAssetsState.mSyncInProgress);
    }

    /**
     * {@link AssetsBaseCore#sync()} should create new {@link AssetsSyncOptions} if passed parameter
     * equals <code>null</code>.
     */
    @Test
    public void syncNullSyncOptions() throws Exception {
        MemberModifier.field(AssetsBaseCore.class, "mDeploymentKey").set(mAssetsBaseCore, DEPLOYMENT_KEY);

        AssetsRemotePackage assetsRemotePackage = new AssetsRemotePackage();
        when(mAssetsBaseCore.checkForUpdate(eq(DEPLOYMENT_KEY))).thenReturn(assetsRemotePackage);

        mCoreTestUtils.mockConfiguration(PACKAGE_HASH);

        mCoreTestUtils.mockTextUtils();

        AssetsSyncOptions assetsSyncOptions = mCoreTestUtils.mockSyncOptions(DEPLOYMENT_KEY,false);

        PowerMockito.whenNew(AssetsSyncOptions.class).withArguments(DEPLOYMENT_KEY).thenReturn(assetsSyncOptions);
        mCoreTestUtils.callSync(null);

        PowerMockito.verifyNew(AssetsSyncOptions.class).withArguments(DEPLOYMENT_KEY);
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
        when(mAssetsUpdateManager.getCurrentPackage()).thenThrow(AssetsGetPackageException.class);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager);
        mCoreTestUtils.callGetUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateManager#getCurrentPackage()} returns <code>null </code>.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfCurrentPackageNull() throws Exception {
        when(mAssetsUpdateManager.getCurrentPackage()).thenReturn(null);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager);
        mCoreTestUtils.assertGetUpdateMetadataReturnsNull();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateState#PENDING} was requested and current package is not pending.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfRequestedPendingAndItsNull() throws Exception {

        mCoreTestUtils.mockTextUtils();

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(null, mAssetsUpdateManager);
        when(mSettingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(false);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager, mSettingsManager);
        mCoreTestUtils.assertGetUpdateMetadataReturnsNull(AssetsUpdateState.PENDING);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw a {@link AssetsNativeApiCallException}
     * if {@link SettingsManager#isPendingUpdate(String)} throws {@link AssetsMalformedDataException}.
     */
    @SuppressWarnings("unchecked")
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIfSettingsManagerThrows() throws Exception {

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(PACKAGE_HASH, mAssetsUpdateManager);

        when(mSettingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenThrow(AssetsMalformedDataException.class);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager, mSettingsManager);
        mCoreTestUtils.callGetUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the previous package
     * if {@link AssetsUpdateState#RUNNING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPreviousPackageIfCurrentUpdateIsPending() throws Exception {

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(PACKAGE_HASH, mAssetsUpdateManager);

        AssetsLocalPackage previousPackage = mock(AssetsLocalPackage.class);
        when(mAssetsUpdateManager.getPreviousPackage()).thenReturn(previousPackage);
        when(mSettingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(true);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager, mSettingsManager);

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

        mCoreTestUtils.mockLocalPackageIntoUpdateManager(PACKAGE_HASH, mAssetsUpdateManager);

        when(mAssetsUpdateManager.getPreviousPackage()).thenThrow(AssetsGetPackageException.class);
        when(mSettingsManager.isPendingUpdate(eq(PACKAGE_HASH))).thenReturn(true);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager, mSettingsManager);
        mCoreTestUtils.callGetUpdateMetadata(null);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the valid pending package
     * if {@link AssetsUpdateState#PENDING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPendingIfRequested() throws Exception {
        mCoreTestUtils.prepareGetUpdateWorkflow(true);

        AssetsLocalPackage returnedPackage = mCoreTestUtils.getAndCheckUpdatePackage(true);
        assertEquals(returnedPackage.isDebugOnly(), false);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the valid running package
     * if {@link AssetsUpdateState#RUNNING} was requested and current package is not pending.
     */
    @Test
    public void getUpdateMetadataReturnsRunningIfRequested() throws Exception {
        mCoreTestUtils.prepareGetUpdateWorkflow(false);

        AssetsLocalPackage returnedPackage = mCoreTestUtils.getAndCheckUpdatePackage(false);
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

        mAssetsState = mCoreTestUtils.mockState(true, true);
        AssetsLocalPackage returnedPackage = mCoreTestUtils.getAndCheckUpdatePackage(true);
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
        when(mConfiguration.setDeploymentKey(DEPLOYMENT_KEY)).thenThrow(AssetsIllegalArgumentException.class);
        when(mAssetsBaseCore.getNativeConfiguration()).thenReturn(mConfiguration);
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

    //region installUpdate

    /**
     * {@link AssetsBaseCore#installUpdate(AssetsLocalPackage, AssetsInstallMode, int)} should throw
     * a {@link AssetsNativeApiCallException} if {@link AssetsUpdateManager#installPackage(String, boolean)}
     * throws a {@link AssetsInstallException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void installFails() throws Exception {
        doThrow(AssetsInstallException.class).when(mAssetsUpdateManager).installPackage(eq(PACKAGE_HASH), eq(true));

        when(mSettingsManager.isPendingUpdate(isNull(String.class))).thenReturn(true);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager, mSettingsManager);
        AssetsLocalPackage assetsLocalPackage = mCoreTestUtils.mockLocalPackage(PACKAGE_HASH);
        mCoreTestUtils.callInstallUpdate(assetsLocalPackage, AssetsInstallMode.IMMEDIATE, 0);
    }

    /**
     * {@link AssetsBaseCore#installUpdate(AssetsLocalPackage, AssetsInstallMode, int)} should throw
     * a {@link AssetsNativeApiCallException} if package hash equals <code>null</code>.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void installFailsPendingHashNull() throws Exception {
        doNothing().when(mAssetsUpdateManager).installPackage(isNull(String.class), eq(true));

        when(mSettingsManager.isPendingUpdate(isNull(String.class))).thenReturn(true);

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager, mSettingsManager);
        AssetsLocalPackage assetsLocalPackage = mCoreTestUtils.mockLocalPackage(null);
        mCoreTestUtils.callInstallUpdate(assetsLocalPackage, AssetsInstallMode.IMMEDIATE, 0);
    }

    /**
     * {@link AssetsBaseCore#installUpdate(AssetsLocalPackage, AssetsInstallMode, int)}
     * should be executed on {@link AssetsInstallMode#IMMEDIATE}.
     */
    @Test
    public void installImmediate() throws Exception {
        executeInstallWorkflow(AssetsInstallMode.IMMEDIATE);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("handleInstallModesForUpdateInstall", AssetsInstallMode.IMMEDIATE);
    }

    /**
     * {@link AssetsBaseCore#installUpdate(AssetsLocalPackage, AssetsInstallMode, int)}
     * should be executed on {@link AssetsInstallMode#ON_NEXT_RESTART}.
     */
    @Test
    public void installOnNextRestart() throws Exception {
        executeInstallWorkflow(AssetsInstallMode.ON_NEXT_RESTART);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(0)).invoke("handleInstallModesForUpdateInstall", AssetsInstallMode.ON_NEXT_RESTART);
    }

    /**
     * {@link AssetsBaseCore#installUpdate(AssetsLocalPackage, AssetsInstallMode, int)}
     * should be executed on {@link AssetsInstallMode#ON_NEXT_RESUME}.
     */
    @Test
    public void installOnNextResume() throws Exception {
        executeInstallWorkflow(AssetsInstallMode.ON_NEXT_RESUME);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("handleInstallModesForUpdateInstall", AssetsInstallMode.ON_NEXT_RESUME);
    }

    /**
     * {@link AssetsBaseCore#installUpdate(AssetsLocalPackage, AssetsInstallMode, int)}
     * should be executed on {@link AssetsInstallMode#ON_NEXT_SUSPEND}.
     */
    @Test
    public void installOnNextSuspend() throws Exception {
        executeInstallWorkflow(AssetsInstallMode.ON_NEXT_SUSPEND);
        PowerMockito.verifyPrivate(mAssetsBaseCore, times(1)).invoke("handleInstallModesForUpdateInstall", AssetsInstallMode.ON_NEXT_SUSPEND);
    }

    /**
     * Executes {@link AssetsBaseCore#installUpdate(AssetsLocalPackage, AssetsInstallMode, int)} workflow on
     * different install modes.
     *
     * @param assetsInstallMode install mode to execute the method with.
     */
    private void executeInstallWorkflow(AssetsInstallMode assetsInstallMode) throws Exception {
        doNothing().when(mAssetsUpdateManager).installPackage(isNull(String.class), eq(true));

        when(mSettingsManager.isPendingUpdate(isNull(String.class))).thenReturn(true);
        doNothing().when(mSettingsManager).savePendingUpdate(any(AssetsPendingUpdate.class));

        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager, mSettingsManager);
        AssetsLocalPackage assetsLocalPackage = mCoreTestUtils.mockLocalPackage(PACKAGE_HASH);
        AssetsState assetsState = new AssetsState();
        assetsState.mMinimumBackgroundDuration = 0;

        MemberModifier
                .field(AssetsBaseCore.class, "mState").set(mAssetsBaseCore, assetsState);
        mCoreTestUtils.callInstallUpdate(assetsLocalPackage, assetsInstallMode, 0);
        Mockito.verify(mSettingsManager).savePendingUpdate(any(AssetsPendingUpdate.class));
    }

    //endregion

    //region downloadUpdate

    /**
     * {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} should download an update.
     */
    @Test
    public void downloadUpdate() throws Exception {
        AssetsPlatformUtils assetsPlatformUtils = mCoreTestUtils.mockBinaryResourcesModifiedTime(BINARY_MODIFIED);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();

        when(mAssetsUpdateManager.getPackageDownloadFile()).thenReturn(DOWNLOAD_FILE);

        mCoreTestUtils.mockCreatingRequest(DOWNLOAD_URL, DOWNLOAD_FILE);

        AssetsDownloadPackageResult assetsDownloadPackageResult = mock(AssetsDownloadPackageResult.class);
        when(assetsDownloadPackageResult.isZip()).thenReturn(IS_ZIP);
        when(mAssetsUpdateManager.downloadPackage(eq(PACKAGE_HASH), any(ApiHttpRequest.class))).thenReturn(assetsDownloadPackageResult);
        when(mAssetsUpdateManager.getPackageFolderPath(eq(PACKAGE_HASH))).thenReturn(NEW_UPDATE_FOLDER);
        when(mFileUtils.appendPathComponent(any(String.class), any(String.class))).thenReturn(NEW_UPDATE_METADATA);
        doNothing().when(mFileUtils).moveFile(eq(DOWNLOAD_FILE), any(File.class), eq(ENTRY_POINT));
        doNothing().when(mAssetsUtils).writeObjectToJsonFile(any(AssetsLocalPackage.class), eq(NEW_UPDATE_METADATA));

        mCoreTestUtils.injectUtilitiesInCore(assetsPlatformUtils, mFileUtils, mAssetsUtils);
        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager);

        AssetsLocalPackage assetsLocalPackage = mCoreTestUtils.callDownloadUpdate(assetsRemotePackage);

        assertEquals(assetsLocalPackage.getBinaryModifiedTime(), "" + BINARY_MODIFIED);
        PowerMockito.verifyNew(DownloadPackageTask.class).withArguments(mFileUtils, DOWNLOAD_URL, DOWNLOAD_FILE, null);
        PowerMockito.verifyNew(ApiHttpRequest.class).withArguments(any(DownloadPackageTask.class));
    }

    /**
     * {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} should download and unzip zip update.
     */
    @Test
    public void downloadZipUpdate() throws Exception {
        AssetsPlatformUtils assetsPlatformUtils = mCoreTestUtils.mockBinaryResourcesModifiedTime(BINARY_MODIFIED);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();

        when(mAssetsUpdateManager.getPackageDownloadFile()).thenReturn(DOWNLOAD_FILE);

        mCoreTestUtils.mockCreatingRequest(DOWNLOAD_URL, DOWNLOAD_FILE);

        AssetsDownloadPackageResult assetsDownloadPackageResult = mock(AssetsDownloadPackageResult.class);
        when(assetsDownloadPackageResult.isZip()).thenReturn(IS_ZIP);
        when(mAssetsUpdateManager.downloadPackage(eq(PACKAGE_HASH), any(ApiHttpRequest.class))).thenReturn(assetsDownloadPackageResult);
        when(mAssetsUpdateManager.getPackageFolderPath(eq(PACKAGE_HASH))).thenReturn(NEW_UPDATE_FOLDER);
        when(mFileUtils.appendPathComponent(any(String.class), any(String.class))).thenReturn(NEW_UPDATE_METADATA);
        doNothing().when(mAssetsUpdateManager).unzipPackage(eq(DOWNLOAD_FILE));
        doReturn("").when(mAssetsUpdateManager).mergeDiff(eq(NEW_UPDATE_FOLDER), eq(NEW_UPDATE_METADATA), eq(PACKAGE_HASH), any(String.class), eq(ENTRY_POINT));
        doNothing().when(mAssetsUtils).writeObjectToJsonFile(any(AssetsLocalPackage.class), eq(NEW_UPDATE_METADATA));

        mCoreTestUtils.injectUtilitiesInCore(assetsPlatformUtils, mFileUtils, mAssetsUtils);
        mCoreTestUtils.injectManagersInCore(mAssetsUpdateManager);

        AssetsLocalPackage assetsLocalPackage = mCoreTestUtils.callDownloadUpdate(assetsRemotePackage);

        assertEquals(assetsLocalPackage.getBinaryModifiedTime(), "" + BINARY_MODIFIED);
        PowerMockito.verifyNew(DownloadPackageTask.class).withArguments(mFileUtils, DOWNLOAD_URL, DOWNLOAD_FILE, null);
        PowerMockito.verifyNew(ApiHttpRequest.class).withArguments(any(DownloadPackageTask.class));
        Mockito.verify(mAssetsUpdateManager).unzipPackage(eq(DOWNLOAD_FILE));
        Mockito.verify(mAssetsUpdateManager).mergeDiff(eq(NEW_UPDATE_FOLDER), eq(NEW_UPDATE_METADATA), eq(PACKAGE_HASH), any(String.class), eq(ENTRY_POINT));
        Mockito.verify(mAssetsUtils).writeObjectToJsonFile(any(AssetsLocalPackage.class), eq(NEW_UPDATE_METADATA));
    }

    /**
     * {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} should throw a
     * {@link AssetsNativeApiCallException} if {@link IOException} was thrown.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void downloadZipUpdateFailsIfIOException() throws Exception {
        AssetsPlatformUtils assetsPlatformUtils = mCoreTestUtils.mockBinaryResourcesModifiedTime(BINARY_MODIFIED);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        when(mAssetsUpdateManager.getPackageDownloadFile()).thenThrow(IOException.class);

        ensureDownloadFails(mAssetsUpdateManager, assetsPlatformUtils, null, assetsRemotePackage);
    }

    /**
     * {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} should throw a
     * {@link AssetsNativeApiCallException} if {@link AssetsUpdateManager#downloadPackage(String, ApiHttpRequest)}
     * throws {@link AssetsDownloadPackageException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void downloadZipUpdateFailsIfDownloadThrows() throws Exception {
        AssetsPlatformUtils assetsPlatformUtils = mCoreTestUtils.mockBinaryResourcesModifiedTime(BINARY_MODIFIED);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        when(mAssetsUpdateManager.getPackageDownloadFile()).thenReturn(DOWNLOAD_FILE);
        mCoreTestUtils.mockCreatingRequest(DOWNLOAD_URL, DOWNLOAD_FILE);
        when(mAssetsUpdateManager.downloadPackage(eq(PACKAGE_HASH), any(ApiHttpRequest.class))).thenThrow(AssetsDownloadPackageException.class);

        ensureDownloadFails(mAssetsUpdateManager, assetsPlatformUtils, null, assetsRemotePackage);
    }

    /**
     * {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} should throw a
     * {@link AssetsNativeApiCallException} if {@link AssetsUpdateManager#unzipPackage(File)}
     * throws {@link AssetsUnzipException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void downloadZipUpdateFailsIfUnzipThrows() throws Exception {
        AssetsDownloadPackageResult assetsDownloadPackageResult = mock(AssetsDownloadPackageResult.class);

        AssetsPlatformUtils assetsPlatformUtils = mCoreTestUtils.mockBinaryResourcesModifiedTime(BINARY_MODIFIED);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        when(mAssetsUpdateManager.getPackageDownloadFile()).thenReturn(DOWNLOAD_FILE);

        mCoreTestUtils.mockCreatingRequest(DOWNLOAD_URL, DOWNLOAD_FILE);

        when(assetsDownloadPackageResult.isZip()).thenReturn(IS_ZIP);
        when(mAssetsUpdateManager.downloadPackage(eq(PACKAGE_HASH), any(ApiHttpRequest.class))).thenReturn(assetsDownloadPackageResult);
        when(mAssetsUpdateManager.getPackageFolderPath(eq(PACKAGE_HASH))).thenReturn(NEW_UPDATE_FOLDER);
        when(mFileUtils.appendPathComponent(any(String.class), any(String.class))).thenReturn(NEW_UPDATE_METADATA);
        doThrow(AssetsUnzipException.class).when(mAssetsUpdateManager).unzipPackage(eq(DOWNLOAD_FILE));

        ensureDownloadFails(mAssetsUpdateManager, assetsPlatformUtils, mFileUtils, assetsRemotePackage);
    }

    /**
     * {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} should throw a
     * {@link AssetsNativeApiCallException} if {@link AssetsUpdateManager#mergeDiff(String, String, String, String, String)}
     * throws {@link AssetsMergeException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void downloadZipUpdateFailsIfMergeThrows() throws Exception {
        AssetsDownloadPackageResult assetsDownloadPackageResult = mock(AssetsDownloadPackageResult.class);

        AssetsPlatformUtils assetsPlatformUtils = mCoreTestUtils.mockBinaryResourcesModifiedTime(BINARY_MODIFIED);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        when(mAssetsUpdateManager.getPackageDownloadFile()).thenReturn(DOWNLOAD_FILE);

        mCoreTestUtils.mockCreatingRequest(DOWNLOAD_URL, DOWNLOAD_FILE);

        when(assetsDownloadPackageResult.isZip()).thenReturn(IS_ZIP);
        when(mAssetsUpdateManager.downloadPackage(eq(PACKAGE_HASH), any(ApiHttpRequest.class))).thenReturn(assetsDownloadPackageResult);
        when(mAssetsUpdateManager.getPackageFolderPath(eq(PACKAGE_HASH))).thenReturn(NEW_UPDATE_FOLDER);
        when(mFileUtils.appendPathComponent(any(String.class), any(String.class))).thenReturn(NEW_UPDATE_METADATA);
        doNothing().when(mAssetsUpdateManager).unzipPackage(eq(DOWNLOAD_FILE));
        doThrow(AssetsMergeException.class).when(mAssetsUpdateManager).mergeDiff(eq(NEW_UPDATE_FOLDER), eq(NEW_UPDATE_METADATA), eq(PACKAGE_HASH), any(String.class), eq(ENTRY_POINT));

        ensureDownloadFails(mAssetsUpdateManager, assetsPlatformUtils, mFileUtils, assetsRemotePackage);
    }

    /**
     * {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} should rethrow a
     * {@link AssetsNativeApiCallException} if {@link SettingsManager#saveFailedUpdate(AssetsPackage)}
     * throws {@link AssetsMalformedDataException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void downloadZipUpdateFailsIfIOExceptionFailsAgain() throws Exception {
        AssetsPlatformUtils assetsPlatformUtils = mCoreTestUtils.mockBinaryResourcesModifiedTime(BINARY_MODIFIED);
        AssetsRemotePackage assetsRemotePackage = mCoreTestUtils.createFakeRemotePackage();
        when(mAssetsUpdateManager.getPackageDownloadFile()).thenThrow(IOException.class);
        doThrow(AssetsMalformedDataException.class).when(mSettingsManager).saveFailedUpdate(any(AssetsRemotePackage.class));

        ensureDownloadFails(mAssetsUpdateManager, assetsPlatformUtils, null, assetsRemotePackage);
    }

    /**
     * Verifies that {@link AssetsBaseCore#downloadUpdate(AssetsRemotePackage)} fails to execute and calls
     * {@link SettingsManager#saveFailedUpdate(AssetsPackage)}.
     * @param assetsUpdateManager update manager to inject.
     * @param assetsPlatformUtils platform utils to inject.
     * @param fileUtils file utils to inject.
     * @param assetsRemotePackage remote package to call with.
     */
    private void ensureDownloadFails(AssetsUpdateManager assetsUpdateManager, AssetsPlatformUtils assetsPlatformUtils, FileUtils fileUtils, AssetsRemotePackage assetsRemotePackage) throws  Exception {
        SettingsManager settingsManager = mCoreTestUtils.mockSaveFailedUpdates();
        mCoreTestUtils.injectManagersInCore(assetsUpdateManager, settingsManager);
        mCoreTestUtils.injectUtilitiesInCore(assetsPlatformUtils, fileUtils, null);
        mCoreTestUtils.callDownloadUpdate(assetsRemotePackage);
        Mockito.verify(settingsManager).saveFailedUpdate(assetsRemotePackage);
    }
    //endregion
}
