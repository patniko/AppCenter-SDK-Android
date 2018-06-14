package com.microsoft.appcenter.assets.testutils;

import android.os.Environment;

import com.microsoft.appcenter.assets.AssetsConstants;
import com.microsoft.appcenter.assets.DownloadProgress;
import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.apirequests.DownloadPackageTask;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.interfaces.DownloadProgressCallback;
import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.utils.FileUtils;

import java.io.File;

import static com.microsoft.appcenter.assets.AssetsConstants.ASSETS_FOLDER_PREFIX;
import static com.microsoft.appcenter.assets.AssetsConstants.PACKAGE_FILE_NAME;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Utils to make {@link AssetsUpdateManager} testing process easier and avoid code repetition.
 */
public class UpdateManagerAndroidTestUtils {

    /**
     * Executes "download" workflow.
     *
     * @param assetsUpdateManager instance of code push update manager.
     * @param packageHash           package hash to use.
     * @param verify                whether verify that callback is called.
     * @param url                   url for downloading.
     * @return result of the download.
     */
    public static AssetsDownloadPackageResult executeDownload(AssetsUpdateManager assetsUpdateManager, String packageHash, boolean verify, String url) throws Exception {
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        File downloadFolder = new File(Environment.getExternalStorageDirectory(), ASSETS_FOLDER_PREFIX);
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, AssetsConstants.DOWNLOAD_FILE_NAME);
        DownloadPackageTask downloadPackageTask = new DownloadPackageTask(FileUtils.getInstance(), url, downloadFilePath, downloadProgressCallback);
        ApiHttpRequest<AssetsDownloadPackageResult> apiHttpRequest = new ApiHttpRequest<>(downloadPackageTask);
        AssetsDownloadPackageResult assetsDownloadPackageResult = assetsUpdateManager.downloadPackage(packageHash, apiHttpRequest);
        if (verify) {
            verify(downloadProgressCallback, timeout(5000).atLeast(1)).call(any(DownloadProgress.class));
        }
        return assetsDownloadPackageResult;
    }

    /**
     * Performs very common workflow: download -> unzip.
     *
     * @param assetsUpdateManager instance of update manager.
     * @param packageHash           package hash to use.
     * @param url                   url for downloading.
     */
    public static void executeWorkflow(AssetsUpdateManager assetsUpdateManager, String packageHash, String url) throws Exception {
        AssetsDownloadPackageResult downloadPackageResult = executeDownload(assetsUpdateManager, packageHash, true, url);
        File downloadFile = downloadPackageResult.getDownloadFile();
        assertEquals(true, downloadPackageResult.isZip());
        assetsUpdateManager.unzipPackage(downloadFile);
    }

    /**
     * Performs full testing workflow: download -> unzip -> install -> write metadata.
     *
     * @param packageHash           package hash to use.
     * @param assetsUpdateManager instance of update manager.
     * @param packageUrl            package url to use.
     */
    public static void executeFullWorkflow(AssetsUpdateManager assetsUpdateManager, String packageHash, String packageUrl) throws Exception {
        executeWorkflow(assetsUpdateManager, packageHash, packageUrl);
        String newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(packageHash);
        String newUpdateMetadataPath = new File(newUpdateFolderPath, PACKAGE_FILE_NAME).getPath();
        String entryPoint = assetsUpdateManager.mergeDiff(newUpdateFolderPath, newUpdateMetadataPath, packageHash, null, "index.html");
        assertEquals("/www/index.html", entryPoint);
        assetsUpdateManager.installPackage(packageHash, false);
    }
}
