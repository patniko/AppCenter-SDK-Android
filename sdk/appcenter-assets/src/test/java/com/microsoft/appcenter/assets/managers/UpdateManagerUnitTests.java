package com.microsoft.appcenter.assets.managers;

import android.os.Environment;

import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.apirequests.DownloadPackageTask;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.exceptions.AssetsDownloadPackageException;
import com.microsoft.appcenter.assets.testutils.CommonTestPlatformUtils;
import com.microsoft.appcenter.assets.utils.AssetsUpdateUtils;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;

import org.junit.Test;

import java.io.File;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * This class contains {@link AssetsUpdateManager} tests, that for some reasons can't be executed in instrumental module.
 */
public class UpdateManagerUnitTests {

    /**
     * Download package should throw a {@link AssetsDownloadPackageException} if an {@link InterruptedException} is thrown during {@link DownloadPackageTask#get()}.
     * If executing an {@link android.os.AsyncTask} fails, downloading package should fail, too.
     */
    @Test(expected = AssetsDownloadPackageException.class)
    public void downloadFailsIfPackageDownloaderFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        AssetsUtils assetsUtils = AssetsUtils.getInstance(fileUtils);
        AssetsUpdateUtils assetsUpdateUtils = AssetsUpdateUtils.getInstance(fileUtils, assetsUtils);
        AssetsUpdateManager assetsUpdateManager = new AssetsUpdateManager(new File(Environment.getExternalStorageDirectory(), "/Test").getPath(),
                CommonTestPlatformUtils.getInstance(),
                fileUtils, assetsUtils, assetsUpdateUtils);
        assetsUpdateManager = spy(assetsUpdateManager);
        doReturn(new File(Environment.getExternalStorageDirectory(), "/Test/HASH").getPath()).when(assetsUpdateManager).getPackageFolderPath(anyString());
        DownloadPackageTask packageDownloader = mock(DownloadPackageTask.class, CALLS_REAL_METHODS);
        when(packageDownloader.get()).thenThrow(new InterruptedException());
        ApiHttpRequest<AssetsDownloadPackageResult> apiHttpRequest = new ApiHttpRequest<>(packageDownloader);
        assetsUpdateManager.downloadPackage("", apiHttpRequest);
    }
}
