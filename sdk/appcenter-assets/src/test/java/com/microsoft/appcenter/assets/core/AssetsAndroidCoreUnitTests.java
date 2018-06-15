package com.microsoft.appcenter.assets.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.microsoft.appcenter.assets.AssetsAndroidEntryPointProvider;
import com.microsoft.appcenter.assets.AssetsAndroidPublicKeyProvider;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;
import com.microsoft.appcenter.assets.enums.AssetsUpdateState;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;
import com.microsoft.appcenter.assets.utils.AndroidUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static com.microsoft.appcenter.assets.core.CoreTestUtils.injectManagersInCore;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AssetsAndroidCore.class)
public class AssetsAndroidCoreUnitTests {
    private AssetsAndroidCore mAssetsAndroidCore;

    private AndroidUtils mAndroidUtils;

    private final static String DEPLOYMENT_KEY = "fake-deployment-key";

    private final static String SERVER_URL = "fake-server-url";

    private final static boolean IS_DEBUG_MODE = false;

    private final static String UPDATE_ENTRY_POINT = "fake-update-entry-point-path";

    private final static String PACKAGE_HASH = "hash";
    private final static boolean FAILED_INSTALL = true;
    private final static boolean IS_FIRST_RUN = false;

    @Before
    public void setUp() throws Exception {
        mAndroidUtils = AndroidUtils.getInstance();

        Context applicationContext = mock(Context.class);
        when(applicationContext.getPackageName()).thenReturn("package-name");

        File filesDir = mock(File.class);
        when(filesDir.getAbsolutePath()).thenReturn("path");
        when(applicationContext.getFilesDir()).thenReturn(filesDir);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionName = "version-name";

        PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.getPackageInfo(any(String.class), any(int.class))).thenReturn(packageInfo);
        when(applicationContext.getPackageManager()).thenReturn(packageManager);

        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(applicationContext);

        MemberModifier
                .stub(MemberMatcher.method(AssetsAndroidCore.class,
                        "initializeUpdateAfterRestart")).toReturn(null);

        mAssetsAndroidCore = new AssetsAndroidCore(
                DEPLOYMENT_KEY,
                context,
                IS_DEBUG_MODE,
                SERVER_URL,
                new AssetsAndroidPublicKeyProvider(null, context),
                new AssetsAndroidEntryPointProvider(UPDATE_ENTRY_POINT),
                mAndroidUtils);
    }

    @Captor
    ArgumentCaptor<AssetsSyncStatus> captor;
    @Test
    public void syncInProgressTest() throws Exception {
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsState assetsState = new AssetsState();
        assetsState.mSyncInProgress = true;

        MemberModifier
                .field(AssetsAndroidCore.class, "mState").set(mAssetsAndroidCore, assetsState);

        mAssetsAndroidCore.sync();

        PowerMockito.verifyPrivate(mAssetsAndroidCore, times(1)).invoke("notifyAboutSyncStatusChange", captor.capture());
        PowerMockito.verifyPrivate(mAssetsAndroidCore, times(0)).invoke("getNativeConfiguration");
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw {@link AssetsNativeApiCallException}
     * if a {@link AssetsGetPackageException} has occurred when trying to get package via
     * {@link AssetsUpdateManager#getCurrentPackage()}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIfGetCurrentPackageFails() throws Exception {
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        when(assetsUpdateManager.getCurrentPackage()).thenThrow(AssetsGetPackageException.class);

        injectManagersInCore(assetsUpdateManager, mAssetsAndroidCore);
        mAssetsAndroidCore.getUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateManager#getCurrentPackage()} returns <code>null</code>.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfCurrentPackageNull() throws Exception {
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(null);

        injectManagersInCore(assetsUpdateManager, mAssetsAndroidCore);
        assertNull(mAssetsAndroidCore.getUpdateMetadata());
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return <code>null</code>
     * if {@link AssetsUpdateState#PENDING} was requested and current package is not pending.
     */
    @Test
    public void getUpdateMetadataReturnsNullIfRequestedPendingAndItsNull() throws Exception {
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(settingsManager.isPendingUpdate(matches(PACKAGE_HASH))).thenReturn(false);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsAndroidCore);

        assertNull(mAssetsAndroidCore.getUpdateMetadata(AssetsUpdateState.PENDING));
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw a {@link AssetsNativeApiCallException}
     * if {@link SettingsManager#isPendingUpdate(String)} throws {@link AssetsMalformedDataException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIfSettingsManagerThrows() throws Exception {
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(settingsManager.isPendingUpdate(matches(PACKAGE_HASH))).thenThrow(AssetsMalformedDataException.class);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsAndroidCore);

        mAssetsAndroidCore.getUpdateMetadata();
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the previous package
     * if {@link AssetsUpdateState#RUNNING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPreviousPackageIfCurrentUpdateIsPending() throws Exception {
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        AssetsLocalPackage previousPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(assetsUpdateManager.getPreviousPackage()).thenReturn(previousPackage);
        when(settingsManager.isPendingUpdate(matches(PACKAGE_HASH))).thenReturn(true);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsAndroidCore);

        assertEquals(mAssetsAndroidCore.getUpdateMetadata(null), previousPackage);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should throw a {@link AssetsNativeApiCallException}
     * if {@link AssetsUpdateState#RUNNING} was requested, current package is pending,
     * and {@link AssetsUpdateManager#getPreviousPackage()} throws {@link AssetsGetPackageException}.
     */
    @Test(expected = AssetsNativeApiCallException.class)
    public void getUpdateMetadataFailsIFGetPreviousPackageFails() throws Exception {
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = mock(AssetsLocalPackage.class);
        when(currentPackage.getPackageHash()).thenReturn(PACKAGE_HASH);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(assetsUpdateManager.getPreviousPackage()).thenThrow(AssetsGetPackageException.class);
        when(settingsManager.isPendingUpdate(matches(PACKAGE_HASH))).thenReturn(true);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsAndroidCore);

        mAssetsAndroidCore.getUpdateMetadata(null);
    }

    /**
     * {@link AssetsBaseCore#getUpdateMetadata()} should return the valid pending package
     * if {@link AssetsUpdateState#PENDING} was requested and current package is pending.
     */
    @Test
    public void getUpdateMetadataReturnsPendingIfRequested() throws Exception {
        prepareGetPendingUpdateWorkflow();
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
        MemberModifier.field(AssetsAndroidCore.class, "mState").set(mAssetsAndroidCore, assetsState);

        AssetsLocalPackage returnedPackage = getPendingUpdatePackage();
        assertEquals(returnedPackage.isDebugOnly(), true);
    }

    /**
     * Retrieves and checks pending update package.
     *
     * @return pending update package.
     */
    private AssetsLocalPackage getPendingUpdatePackage() throws Exception {
        AssetsLocalPackage returnedPackage = mAssetsAndroidCore.getUpdateMetadata(AssetsUpdateState.PENDING);
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
        mAssetsAndroidCore = spy(mAssetsAndroidCore);

        AssetsUpdateManager assetsUpdateManager = mock(AssetsUpdateManager.class);
        SettingsManager settingsManager = mock(SettingsManager.class);

        AssetsLocalPackage currentPackage = AssetsLocalPackage.createEmptyPackageForCheckForUpdateQuery("1.0");
        currentPackage.setPackageHash(PACKAGE_HASH);
        currentPackage.setDebugOnly(false);
        when(assetsUpdateManager.getCurrentPackage()).thenReturn(currentPackage);
        when(settingsManager.isPendingUpdate(matches(PACKAGE_HASH))).thenReturn(true);

        injectManagersInCore(assetsUpdateManager, settingsManager, mAssetsAndroidCore);

        MemberModifier.stub(MemberMatcher.method(AssetsAndroidCore.class,
                "existsFailedUpdate", String.class)).toReturn(FAILED_INSTALL);
        MemberModifier.stub(MemberMatcher.method(AssetsAndroidCore.class,
                "isFirstRun", String.class)).toReturn(IS_FIRST_RUN);
    }
}
