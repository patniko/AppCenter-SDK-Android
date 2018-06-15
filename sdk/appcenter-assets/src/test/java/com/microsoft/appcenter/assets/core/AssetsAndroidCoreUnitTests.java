package com.microsoft.appcenter.assets.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.microsoft.appcenter.assets.AssetsAndroidEntryPointProvider;
import com.microsoft.appcenter.assets.AssetsAndroidPublicKeyProvider;
import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
}
