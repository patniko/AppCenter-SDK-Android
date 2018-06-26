package com.microsoft.appcenter.assets.managers;

import android.os.Environment;

import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.exceptions.AssetsDownloadPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsSignatureVerificationException;
import com.microsoft.appcenter.assets.exceptions.AssetsUnzipException;
import com.microsoft.appcenter.assets.testutils.CommonTestPlatformUtils;
import com.microsoft.appcenter.assets.utils.AssetsUpdateUtils;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * This class is for testing those {@link AssetsUpdateManager} test cases that depend on {@link FileUtils} methods failure.
 */
public class UpdateManagerAndroidFileTests {

    /**
     * Test package hash.
     */
    private final static String PACKAGE_HASH = "FHJDKF648723f";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Instance of {@link CommonTestPlatformUtils} to work with.
     */
    private CommonTestPlatformUtils mPlatformUtils;

    /**
     * Instance of testable {@link AssetsUpdateManager}.
     */
    private AssetsUpdateManager assetsUpdateManager;

    @Before
    public void setUp() {
        mPlatformUtils = CommonTestPlatformUtils.getInstance();
        FileUtils fileUtils = FileUtils.getInstance();
        AssetsUtils assetsUtils = AssetsUtils.getInstance(fileUtils);
        AssetsUpdateUtils assetsUpdateUtils = AssetsUpdateUtils.getInstance(fileUtils, assetsUtils);
        recreateUpdateManager(fileUtils, assetsUtils, assetsUpdateUtils);
    }

    /**
     * Recreates {@link AssetsUpdateManager} with the new mocks of utils.
     *
     * @param fileUtils           mocked instance of {@link FileUtils}.
     * @param assetsUtils       mocked instance of {@link AssetsUtils}.
     * @param assetsUpdateUtils mocked instance of {@link AssetsUpdateUtils}.
     */
    private void recreateUpdateManager(FileUtils fileUtils, AssetsUtils assetsUtils, AssetsUpdateUtils assetsUpdateUtils) {
        assetsUpdateManager = new AssetsUpdateManager(new File(Environment.getExternalStorageDirectory(), "/Test").getPath(), mPlatformUtils, fileUtils, assetsUtils, assetsUpdateUtils);
    }

    /**
     * Download package should throw a {@link AssetsDownloadPackageException}
     * if an {@link IOException} is thrown during {@link FileUtils#deleteDirectoryAtPath(String)}.
     * If deleting file at path where a new update should be located fails, the whole method should fail.
     */
    @Test(expected = AssetsDownloadPackageException.class)
    public void downloadFailsIfDeleteNewUpdateFolderPathFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        fileUtils = spy(fileUtils);
        doThrow(new IOException()).when(fileUtils).deleteDirectoryAtPath(anyString());
        doReturn(true).when(fileUtils).fileAtPathExists(anyString());
        AssetsUtils assetsUtils = AssetsUtils.getInstance(fileUtils);
        AssetsUpdateUtils assetsUpdateUtils = AssetsUpdateUtils.getInstance(fileUtils, assetsUtils);
        recreateUpdateManager(fileUtils, assetsUtils, assetsUpdateUtils);
        assetsUpdateManager.downloadPackage("", mock(ApiHttpRequest.class));
    }

    /**
     * Unzip should throw a {@link AssetsUnzipException}
     * if an {@link IOException} is thrown during {@link FileUtils#unzipFile(File, File)}.
     */
    @Test(expected = AssetsUnzipException.class)
    public void unzipFailsIfUnzipFileFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        fileUtils = spy(fileUtils);
        doThrow(new IOException()).when(fileUtils).unzipFile(any(File.class), any(File.class));
        AssetsUtils assetsUtils = AssetsUtils.getInstance(fileUtils);
        AssetsUpdateUtils assetsUpdateUtils = AssetsUpdateUtils.getInstance(fileUtils, assetsUtils);
        recreateUpdateManager(fileUtils, assetsUtils, assetsUpdateUtils);
        assetsUpdateManager = spy(assetsUpdateManager);
        doReturn("").when(assetsUpdateManager).getUnzippedFolderPath();
        assetsUpdateManager.unzipPackage(mock(File.class));
    }

    /**
     * Verifying signature should throw a {@link AssetsSignatureVerificationException}
     * if {@link AssetsUpdateUtils#verifyFolderHash(String, String)} throws an {@link IOException}.
     */
    @Test(expected = AssetsSignatureVerificationException.class)
    public void verifyFailsIfVerifyFolderHashFails() throws Exception {
        FileUtils fileUtils = FileUtils.getInstance();
        AssetsUtils assetsUtils = AssetsUtils.getInstance(fileUtils);
        AssetsUpdateUtils assetsUpdateUtils = AssetsUpdateUtils.getInstance(fileUtils, assetsUtils);
        assetsUpdateUtils = spy(assetsUpdateUtils);
        doThrow(new IOException()).when(assetsUpdateUtils).verifyFolderHash(anyString(), anyString());
        recreateUpdateManager(fileUtils, assetsUtils, assetsUpdateUtils);
        String newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(PACKAGE_HASH);
        assetsUpdateManager.verifySignature(newUpdateFolderPath, null, PACKAGE_HASH, true);
    }
}
