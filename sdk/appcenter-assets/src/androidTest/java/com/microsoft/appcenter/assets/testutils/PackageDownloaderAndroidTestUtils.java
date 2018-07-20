package com.microsoft.appcenter.assets.testutils;

import android.os.Environment;

import com.microsoft.appcenter.assets.AssetsConstants;
import com.microsoft.appcenter.assets.apirequests.DownloadPackageTask;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.interfaces.DownloadProgressCallback;
import com.microsoft.appcenter.assets.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Utils to make {@link DownloadPackageTask} testing process easier and avoid code repetition.
 */
public class PackageDownloaderAndroidTestUtils {

    /**
     * Executes <code>doInBackground()</code> method of {@link DownloadPackageTask}.
     *
     * @param downloadPackageTask instance of package downloader.
     * @return download result.
     */
    private static AssetsDownloadPackageResult executeDoInBackground(DownloadPackageTask downloadPackageTask) throws Exception {
        Method method = downloadPackageTask.getClass().getMethod("doInBackground", Void[].class);
        return (AssetsDownloadPackageResult) method.invoke(downloadPackageTask, (Object[]) new Void[]{null});
    }

    /**
     * Executes <code>doInBackground()</code> method of {@link DownloadPackageTask} only and assert that it fails.
     *
     * @param downloadPackageTask instance of package downloader.
     */
    public static void checkDoInBackgroundFails(DownloadPackageTask downloadPackageTask) throws Exception {
        executeDoInBackground(downloadPackageTask);
        assertNotNull(downloadPackageTask.getInnerException());
    }

    /**
     * Executes <code>doInBackground()</code> method of {@link DownloadPackageTask} only and assert that it does not fail.
     *
     * @param downloadPackageTask instance of package downloader.
     */
    public static void checkDoInBackgroundNotFails(DownloadPackageTask downloadPackageTask) throws Exception {
        executeDoInBackground(downloadPackageTask);
        assertNull(downloadPackageTask.getInnerException());
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param url custom url.
     * @return package downloader instance that can be mocked.
     */
    public static DownloadPackageTask createDownloadTask(String url) {
        File assetsPath = new File(Environment.getExternalStorageDirectory(), AssetsConstants.ASSETS_FOLDER_PREFIX);
        File downloadFolder = new File(assetsPath.getPath());
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, AssetsConstants.DOWNLOAD_FILE_NAME);
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        DownloadPackageTask downloadPackageTask = new DownloadPackageTask(FileUtils.getInstance(), url, downloadFilePath, downloadProgressCallback);
        return spy(downloadPackageTask);
    }

    /**
     * Creates spied package downloader instance.
     *
     * @param downloadFilePath custom download path.
     * @param url              custom url.
     * @return package downloader instance that can be mocked.
     */
    public static DownloadPackageTask createDownloadTask(String url, File downloadFilePath) {
        DownloadProgressCallback downloadProgressCallback = mock(DownloadProgressCallback.class);
        DownloadPackageTask downloadPackageTask = new DownloadPackageTask(FileUtils.getInstance(), url, downloadFilePath, downloadProgressCallback);
        return spy(downloadPackageTask);
    }
}
