package com.microsoft.appcenter.assets.managers;

import android.os.Environment;

import com.microsoft.appcenter.assets.AssetsConstants;
import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.apirequests.DownloadPackageTask;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackageInfo;
import com.microsoft.appcenter.assets.exceptions.AssetsDownloadPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsInstallException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsMergeException;
import com.microsoft.appcenter.assets.exceptions.AssetsRollbackException;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;
import com.microsoft.appcenter.assets.testutils.CommonTestPlatformUtils;
import com.microsoft.appcenter.assets.utils.AssetsUpdateUtils;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static com.microsoft.appcenter.assets.AssetsConstants.ASSETS_FOLDER_PREFIX;
import static com.microsoft.appcenter.assets.AssetsConstants.PACKAGE_FILE_NAME;
import static com.microsoft.appcenter.assets.testutils.UpdateManagerAndroidTestUtils.executeDownload;
import static com.microsoft.appcenter.assets.testutils.UpdateManagerAndroidTestUtils.executeFullWorkflow;
import static com.microsoft.appcenter.assets.testutils.UpdateManagerAndroidTestUtils.executeWorkflow;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * This class tests all the {@link AssetsUpdateManager} scenarios.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UpdateManagerAndroidTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /**
     * Url to download a package that represents a full update.
     */
    private final static String FULL_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/6CjTRZUgaYrHlhH3mKy2JsQVIJtsa0021bd2-9be1-4904-b4c6-16ce9c797779";

    /**
     * Url to download a package that represents a diff update.
     */
    private final static String DIFF_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/8wuI2wwTlf4RioIb1cLRtyQyzRW80840428d-683e-4d30-a120-c592a355a594";

    /**
     * Url to download a package that represents a signed package.
     */
    private final static String SIGNED_PACKAGE_URL = "https://codepush.blob.core.windows.net/storagev2/OWIRaqwJQUbNeiX60nDnijj9HxMza0021bd2-9be1-4904-b4c6-16ce9c797779";

    /**
     * Hash of the package that represents a full update.
     */
    private final static String FULL_PACKAGE_HASH = "a1d28a073a1fa45745a8b1952ccc5c2bd4753e533e7b9e48459a6c186ecd32af";

    /**
     * Hash of the package that represents a diff update.
     */
    private final static String DIFF_PACKAGE_HASH = "ff46674f196ae852ccb67e49346a11cb9d8c0243ba24003e11b83dd7469b5dd4";

    /**
     * Hash of the signed package.
     */
    private final static String SIGNED_PACKAGE_HASH = "ce9148e0d0422dc7ffefba3a82f527a0e75f51c449f34a5f7dabab6f36251aaf";

    /**
     * Public key used to sign a test package.
     */
    private final static String SIGNED_PACKAGE_PUBLIC_KEY = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAM4bfGAHAEx+IVl5/qaRHisPvpGfCY47O7EkW8XhZVer+bo1k6VT3s8hPBMQfcFw/ZQotWwLkvStelvrQptJFiUCAwEAAQ";

    /**
     * Instance of {@link AssetsUpdateManager} to work with.
     */
    private AssetsUpdateManager assetsUpdateManager;

    /**
     * Instance of {@link AssetsUtils} to work with.
     */
    private AssetsUtils mAssetsUtils;

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    /**
     * Instance of package json object.
     */
    private AssetsLocalPackage packageObject;

    @Before
    public void setUp() throws Exception {
        AssetsPlatformUtils platformUtils = CommonTestPlatformUtils.getInstance();
        mFileUtils = FileUtils.getInstance();
        mAssetsUtils = AssetsUtils.getInstance(mFileUtils);
        AssetsUpdateUtils assetsUpdateUtils = AssetsUpdateUtils.getInstance(mFileUtils, mAssetsUtils);
        assetsUpdateManager = new AssetsUpdateManager(Environment.getExternalStorageDirectory().getPath(), platformUtils, mFileUtils, mAssetsUtils, assetsUpdateUtils);
        AssetsPackage assetsPackage = new AssetsPackage();
        assetsPackage.setAppVersion("1.2");
        assetsPackage.setPackageHash(FULL_PACKAGE_HASH);
        assetsPackage.setLabel("fdfds");
        assetsPackage.setDeploymentKey("FDSFD");
        assetsPackage.setDescription("description");
        assetsPackage.setFailedInstall(false);
        assetsPackage.setMandatory(false);
        packageObject = AssetsLocalPackage.createLocalPackage(false, false, false, false, "/www/index.html", assetsPackage);
        File assetsFolder = new File(Environment.getExternalStorageDirectory(), AssetsConstants.ASSETS_FOLDER_PREFIX);
        assetsFolder.mkdirs();
    }

    /**
     * This tests a full update workflow. Download -> unzip -> merge install several packages.
     */
    @Test
    public void fullWorkflowTest() throws Exception {
        assetsUpdateManager.clearUpdates();
        executeFullWorkflow(assetsUpdateManager, FULL_PACKAGE_HASH, FULL_PACKAGE_URL);
        String newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(FULL_PACKAGE_HASH);
        String newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, AssetsConstants.PACKAGE_FILE_NAME);
        packageObject.setPackageHash(FULL_PACKAGE_HASH);
        mAssetsUtils.writeObjectToJsonFile(packageObject, newUpdateMetadataPath);
        executeFullWorkflow(assetsUpdateManager, DIFF_PACKAGE_HASH, DIFF_PACKAGE_URL);
        newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, AssetsConstants.PACKAGE_FILE_NAME);
        packageObject.setPackageHash(DIFF_PACKAGE_HASH);
        mAssetsUtils.writeObjectToJsonFile(packageObject, newUpdateMetadataPath);
        AssetsLocalPackage assetsPreviousPackage = assetsUpdateManager.getPreviousPackage();
        AssetsLocalPackage assetsCurrentPackage = assetsUpdateManager.getCurrentPackage();
        AssetsLocalPackage assetsPackage = assetsUpdateManager.getPackage(DIFF_PACKAGE_HASH);
        assertEquals(FULL_PACKAGE_HASH, assetsPreviousPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, assetsPackage.getPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, assetsCurrentPackage.getPackageHash());
        assertTrue(mFileUtils.fileAtPathExists(assetsUpdateManager.getCurrentPackageEntryPath("index.html")));
    }

    /**
     * This tests the case when relative entry path is <code>null</code>.
     */
    @Test
    public void relativeEntryPathNullTest() throws Exception {
        packageObject.setEntryPoint(null);
        packageObject.setPackageHash(DIFF_PACKAGE_HASH);
        assetsUpdateManager = spy(assetsUpdateManager);
        doReturn(packageObject).when(assetsUpdateManager).getCurrentPackage();
        doReturn("").when(assetsUpdateManager).getCurrentPackageFolderPath();
        assertFalse(mFileUtils.fileAtPathExists(assetsUpdateManager.getCurrentPackageEntryPath("index.html")));
    }

    /**
     * {@link AssetsUpdateManager#getCurrentPackageEntryPath(String)} should return <code>null</code>
     * if {@link AssetsUpdateManager#getCurrentPackage()} returns <code>null</code>.
     */
    @Test
    public void entryPathIsNullWhenPackageIsNull() throws Exception {
        assetsUpdateManager = spy(assetsUpdateManager);
        doReturn(null).when(assetsUpdateManager).getCurrentPackage();
        doReturn("").when(assetsUpdateManager).getCurrentPackageFolderPath();
        assertNull(assetsUpdateManager.getCurrentPackageEntryPath("index.html"));
    }

    /**
     * This tests {@link AssetsUpdateManager#verifySignature(String, String, String, boolean)} method.
     * It downloads signed package and tests case when it is verified and when no public key passed to signed package.
     */
    @Test
    public void verifyTest() throws Exception {

        executeWorkflow(assetsUpdateManager, SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_URL);

        String newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(SIGNED_PACKAGE_HASH);
        String newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, PACKAGE_FILE_NAME);
        assetsUpdateManager.mergeDiff(newUpdateFolderPath, newUpdateMetadataPath, SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_PUBLIC_KEY, "index.html");
        executeWorkflow(assetsUpdateManager, SIGNED_PACKAGE_HASH, SIGNED_PACKAGE_URL);

        newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(SIGNED_PACKAGE_HASH);
        newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, PACKAGE_FILE_NAME);
        assetsUpdateManager.mergeDiff(newUpdateFolderPath, newUpdateMetadataPath, SIGNED_PACKAGE_HASH, null, "index.html");
    }

    /**
     * This tests that clearing updates works properly.
     */
    @Test
    public void updateManagerClearTest() throws Exception {
        assetsUpdateManager.clearUpdates();
        assertNull(assetsUpdateManager.getCurrentPackageEntryPath(""));
        assertFalse(mFileUtils.fileAtPathExists(new File(Environment.getExternalStorageDirectory(), AssetsConstants.ASSETS_FOLDER_PREFIX).getPath()));
        AssetsLocalPackage assetsLocalPackage = assetsUpdateManager.getCurrentPackage();
        assertNull(assetsLocalPackage);
        assertNull(assetsUpdateManager.getCurrentPackageEntryPath(""));
    }

    /**
     * This tests installation with the test configuration set.
     */
    @Test
    public void installTestTestConfig() throws Exception {
        AssetsUpdateManager.setUsingTestConfiguration(true);
        File one = new File(Environment.getExternalStorageDirectory(), AssetsConstants.ASSETS_FOLDER_PREFIX);
        new File(one, "TestPackages").mkdirs();
        assetsUpdateManager.installPackage(packageObject.getPackageHash(), true);
        AssetsUpdateManager.setUsingTestConfiguration(false);
    }

    /**
     * Tests installing the package with the same hash.
     */
    @Test
    public void installTheSamePackage() throws Exception {
        /* Install the same package. */
        assetsUpdateManager.installPackage("dfd", true);
        assetsUpdateManager.installPackage("dfd", true);

        /* Install some new package. */
        assetsUpdateManager.installPackage("ffffff", false);

        /* Install the same as previous. */
        assetsUpdateManager.installPackage("dfd", false);
        assetsUpdateManager = spy(assetsUpdateManager);

        /* Both current and passed package hashes are null and therefore equal. */
        doReturn(null).when(assetsUpdateManager).getCurrentPackageHash();
        assetsUpdateManager.installPackage(null, true);
    }

    /**
     * Tests download package with null callback.
     */
    @Test
    public void nullDownloadProgressCallBack() throws Exception {
        File downloadFolder = new File(Environment.getExternalStorageDirectory(), ASSETS_FOLDER_PREFIX);
        downloadFolder.mkdirs();
        File downloadFilePath = new File(downloadFolder, AssetsConstants.DOWNLOAD_FILE_NAME);
        DownloadPackageTask downloadPackageTask = new DownloadPackageTask(mFileUtils, FULL_PACKAGE_URL, downloadFilePath, null);
        ApiHttpRequest<AssetsDownloadPackageResult> apiHttpRequest = new ApiHttpRequest<>(downloadPackageTask);
        assetsUpdateManager.downloadPackage(FULL_PACKAGE_HASH, apiHttpRequest);
    }

    /**
     * Tests rollback workflow. Install -> install -> rollback.
     */
    @Test
    public void installTestRollback() throws Exception {
        assetsUpdateManager.installPackage(packageObject.getPackageHash(), false);
        assetsUpdateManager.installPackage(DIFF_PACKAGE_HASH, false);
        AssetsPackageInfo assetsPackageInfo = assetsUpdateManager.getCurrentPackageInfo();
        assertNotSame(FULL_PACKAGE_HASH, assetsPackageInfo.getCurrentPackage());
        assertEquals(FULL_PACKAGE_HASH, assetsUpdateManager.getPreviousPackageHash());
        assertEquals(DIFF_PACKAGE_HASH, assetsPackageInfo.getCurrentPackage());
        assetsUpdateManager.rollbackPackage();
        assetsPackageInfo = assetsUpdateManager.getCurrentPackageInfo();
        assertEquals(FULL_PACKAGE_HASH, assetsPackageInfo.getCurrentPackage());
    }

    /**
     * Current package folder path should be deleted before installation.
     */
    @Test
    public void packageFolderIsDeleted() throws Exception {
        assetsUpdateManager = spy(assetsUpdateManager);
        doReturn(new File(Environment.getExternalStorageDirectory(), "/Test").getPath()).when(assetsUpdateManager).getCurrentPackageFolderPath();
        assetsUpdateManager.installPackage(packageObject.getPackageHash(), true);
        assertFalse(mFileUtils.fileAtPathExists(new File(Environment.getExternalStorageDirectory(), "/Test").getPath()));
    }

    /**
     * Previous package folder path should be deleted before installation.
     */
    @Test
    public void previousPackageFolderIsDeleted() throws Exception {
        assetsUpdateManager.installPackage("ddd", false);
        assetsUpdateManager.installPackage("fdsf", false);
        assetsUpdateManager.installPackage("fds", false);
    }

    /**
     * {@link AssetsUpdateManager#getPreviousPackage()} should throw a {@link AssetsGetPackageException}
     * if {@link AssetsUtils#getJsonObjectFromFile(String)} throws a {@link AssetsMalformedDataException}.
     */
    @Test(expected = AssetsGetPackageException.class)
    public void getPreviousPackageFailsIfGetJsonFails() throws Exception {
        String newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        String newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, AssetsConstants.PACKAGE_FILE_NAME);
        new File(newUpdateMetadataPath).delete();
        assetsUpdateManager.getPackage(DIFF_PACKAGE_HASH);
    }

    /**
     * {@link AssetsUpdateManager#getPreviousPackage()} should throw a {@link AssetsGetPackageException}
     * if {@link AssetsUpdateManager#getPreviousPackageHash()} throws an {@link IOException}.
     */
    @Test(expected = AssetsGetPackageException.class)
    public void getPreviousPackageFailsIfGetPreviousPackageHashFails() throws Exception {
        assetsUpdateManager = spy(assetsUpdateManager);
        doThrow(new IOException()).when(assetsUpdateManager).getPreviousPackageHash();
        assetsUpdateManager.getPreviousPackage();
    }

    /**
     * {@link AssetsUpdateManager#getPreviousPackage()} should return <code>null</code>
     * if {@link AssetsUpdateManager#getPreviousPackageHash()} returns <code>null</code>.
     */
    @Test
    public void getPreviousPackageNullIfGetPreviousPackageHashNull() throws Exception {
        assetsUpdateManager = spy(assetsUpdateManager);
        doReturn(null).when(assetsUpdateManager).getPreviousPackageHash();
        assertNull(assetsUpdateManager.getPreviousPackage());
    }

    /**
     * Merge should throw a {@link AssetsMergeException}
     * if a public key passed but the package contains no signature.
     */
    @Test(expected = AssetsMergeException.class)
    public void mergeFailsIfNoSignatureWhereShouldBe() throws Exception {
        assetsUpdateManager.clearUpdates();
        executeWorkflow(assetsUpdateManager, DIFF_PACKAGE_HASH, DIFF_PACKAGE_URL);
        String newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(DIFF_PACKAGE_HASH);
        String newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, PACKAGE_FILE_NAME);
        assetsUpdateManager.mergeDiff(newUpdateFolderPath, newUpdateMetadataPath, DIFF_PACKAGE_HASH, "", "index.html");
    }

    /**
     * Installing a package should throw a {@link AssetsInstallException}
     * if {@link AssetsUpdateManager#updateCurrentPackageInfo(AssetsPackageInfo)} )} throws an {@link IOException} due to {@link java.io.FileNotFoundException}.
     */
    @Test(expected = AssetsInstallException.class)
    public void installTestFail() throws Exception {
        mFileUtils.deleteDirectoryAtPath(new File(Environment.getExternalStorageDirectory(), AssetsConstants.ASSETS_FOLDER_PREFIX).getPath());
        assetsUpdateManager.installPackage(packageObject.getPackageHash(), false);
    }

    /**
     * {@link AssetsUpdateManager#getCurrentPackageEntryPath(String)} should throw a {@link AssetsGetPackageException}
     * if {@link AssetsUpdateManager#getCurrentPackageFolderPath()} throws a {@link AssetsMalformedDataException}.
     */
    @Test(expected = AssetsGetPackageException.class)
    public void getCurrentPackageEntryPathFailsIfGetFolderPathFails() throws Exception {
        AssetsUpdateManager spiedUpdateManager = Mockito.spy(assetsUpdateManager);
        Mockito.doThrow(mock(AssetsMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
        spiedUpdateManager.getCurrentPackageEntryPath("");
    }

    /**
     * Merge should throw a {@link AssetsMergeException}
     * if {@link AssetsUpdateManager#getCurrentPackageFolderPath()} throws a {@link AssetsMalformedDataException}.
     */
     @Test(expected = AssetsMergeException.class)
    public void mergeFailsIfGetFolderPathFails() throws Exception {
        AssetsUpdateManager spiedUpdateManager = Mockito.spy(assetsUpdateManager);
        Mockito.doThrow(mock(AssetsMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageFolderPath();
         String newUpdateFolderPath = assetsUpdateManager.getPackageFolderPath(FULL_PACKAGE_HASH);
         String newUpdateMetadataPath = mFileUtils.appendPathComponent(newUpdateFolderPath, PACKAGE_FILE_NAME);
        spiedUpdateManager.mergeDiff(newUpdateFolderPath, newUpdateMetadataPath, FULL_PACKAGE_HASH, null, "");
    }

    /**
     * Get current package should throw a {@link AssetsGetPackageException}
     * if {@link AssetsUpdateManager#getCurrentPackageHash()} throws a {@link AssetsMalformedDataException}.
     */
    @Test(expected = AssetsGetPackageException.class)
    public void getCurrentPackageFailsIfGetPackageHashFails() throws Exception {
        AssetsUpdateManager spiedUpdateManager = Mockito.spy(assetsUpdateManager);
        Mockito.doThrow(mock(AssetsMalformedDataException.class)).when(spiedUpdateManager).getCurrentPackageHash();
        spiedUpdateManager.getCurrentPackage();
    }

    /**
     * {@link AssetsUpdateManager#getCurrentPackageEntryPath(String)} should return <code>null</code>
     * if {@link AssetsUpdateManager#getCurrentPackage()} returns <code>null</code>.
     */
    @Test
    public void returnNullOnGetCurrentPackageEntryPathWhenPackageIsNull() throws Exception {
        assetsUpdateManager.clearUpdates();
        AssetsUpdateManager spiedUpdateManager = Mockito.spy(assetsUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        assertNull(spiedUpdateManager.getCurrentPackageEntryPath(""));
    }

    /**
     * Rollback should throw a {@link AssetsRollbackException}
     * if {@link AssetsUpdateManager#getCurrentPackage()} returns <code>null</code>.
     */
    @Test(expected = AssetsRollbackException.class)
    public void rollbackFailsIfGetCurrentPackageFails() throws Exception {
        assetsUpdateManager.clearUpdates();
        AssetsUpdateManager spiedUpdateManager = Mockito.spy(assetsUpdateManager);
        Mockito.doReturn(null).when(spiedUpdateManager).getCurrentPackage();
        spiedUpdateManager.rollbackPackage();
    }

    /**
     * Downloading files should throw a {@link AssetsDownloadPackageException}
     * if a {@link java.net.MalformedURLException} is thrown when attempting to download.
     */
    @Test(expected = AssetsDownloadPackageException.class)
    public void downloadPackageFailsIfPackageDownloaderFails() throws Exception {
        executeDownload(assetsUpdateManager, "fff", false, "/");
    }

    /**
     * If status file does not exist, package info should be empty.
     */
    @Test
    public void noPackageInfoTest() throws Exception {
        File assets = new File(Environment.getExternalStorageDirectory(), AssetsConstants.ASSETS_FOLDER_PREFIX);
        new File(assets, AssetsConstants.STATUS_FILE_NAME).delete();
        AssetsPackageInfo assetsPackageInfo = assetsUpdateManager.getCurrentPackageInfo();
        assertNull(assetsPackageInfo.getCurrentPackage());
    }

    /**
     * Getting current package info should throw a {@link AssetsMalformedDataException}
     * if the status file where the info is located is corrupt or contains wrong data.
     */
    @Test(expected = AssetsMalformedDataException.class)
    public void invalidPackageTest() throws Exception {
        File assets = new File(Environment.getExternalStorageDirectory(), AssetsConstants.ASSETS_FOLDER_PREFIX);
        new File(assets, AssetsConstants.STATUS_FILE_NAME).delete();
        new File(assets, AssetsConstants.STATUS_FILE_NAME).createNewFile();
        assetsUpdateManager.getCurrentPackageInfo();
    }
}
