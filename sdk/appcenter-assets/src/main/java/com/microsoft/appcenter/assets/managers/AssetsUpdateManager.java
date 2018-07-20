package com.microsoft.appcenter.assets.managers;

import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsConfiguration;
import com.microsoft.appcenter.assets.AssetsConstants;
import com.microsoft.appcenter.assets.apirequests.ApiHttpRequest;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackageInfo;
import com.microsoft.appcenter.assets.exceptions.AssetsApiHttpRequestException;
import com.microsoft.appcenter.assets.exceptions.AssetsDownloadPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsGetPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsInstallException;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsMergeException;
import com.microsoft.appcenter.assets.exceptions.AssetsRollbackException;
import com.microsoft.appcenter.assets.exceptions.AssetsSignatureVerificationException;
import com.microsoft.appcenter.assets.exceptions.AssetsUnzipException;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;
import com.microsoft.appcenter.assets.utils.AssetsUpdateUtils;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

/**
 * Manager responsible for update read/write actions.
 */
public class AssetsUpdateManager {

    /**
     * Platform-specific utils implementation.
     */
    private AssetsPlatformUtils mPlatformUtils;

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    /**
     * Instance of {@link AssetsUpdateUtils} to work with.
     */
    private AssetsUpdateUtils mAssetsUpdateUtils;

    /**
     * Instance of {@link AssetsUtils} to work with.
     */
    private AssetsUtils mAssetsUtils;

    /**
     * Whether to use test configuration.
     */
    private static boolean sTestConfigurationFlag = false;

    /**
     * General path for storing files.
     */
    private String mDocumentsDirectory;

    /**
     * Assets configuration for instance.
     */
    private AssetsConfiguration mAssetsConfiguration;

    /**
     * Creates instance of AssetsUpdateManager.
     *
     * @param documentsDirectory  path for storing files.
     * @param platformUtils       instance of {@link AssetsPlatformUtils} to work with.
     * @param fileUtils           instance of {@link FileUtils} to work with.
     * @param assetsUtils       instance of {@link AssetsUtils} to work with.
     * @param assetsUpdateUtils instance of {@link AssetsUpdateUtils} to work with.
     * @param assetsConfiguration instance of {@link AssetsConfiguration} to work with.
     */
    public AssetsUpdateManager(String documentsDirectory, AssetsPlatformUtils platformUtils, FileUtils fileUtils, AssetsUtils assetsUtils, AssetsUpdateUtils assetsUpdateUtils, AssetsConfiguration assetsConfiguration) {
        mPlatformUtils = platformUtils;
        mFileUtils = fileUtils;
        mAssetsUpdateUtils = assetsUpdateUtils;
        mAssetsUtils = assetsUtils;
        mDocumentsDirectory = documentsDirectory;
        mAssetsConfiguration = assetsConfiguration;
    }

    /**
     * Sets flag to use test configuration.
     *
     * @param shouldUseTestConfiguration <code>true</code> to use test configuration.
     */
    public static void setUsingTestConfiguration(boolean shouldUseTestConfiguration) {
        sTestConfigurationFlag = shouldUseTestConfiguration;
    }

    /**
     * Gets path to unzip files to.
     *
     * @return path to unzip files to.
     */
    public String getUnzippedFolderPath() {
        return mFileUtils.appendPathComponent(getAssetsPath(), AssetsConstants.UNZIPPED_FOLDER_NAME);
    }

    /**
     * Gets general path for storing files.
     *
     * @return general path for storing files.
     */
    private String getDocumentsDirectory() {
        return mDocumentsDirectory;
    }

    /**
     * Gets application-specific folder.
     *
     * @return application-specific folder.
     */
    private String getAssetsPath() {
        String assetsPath = mFileUtils.appendPathComponent(getDocumentsDirectory(), mAssetsConfiguration.getAppName());
        if (sTestConfigurationFlag) {
            assetsPath = mFileUtils.appendPathComponent(assetsPath, "TestPackages");
        }
        return assetsPath;
    }

    /**
     * Gets path to json file containing information about the available packages.
     *
     * @return path to json file containing information about the available packages.
     */
    private String getStatusFilePath() {
        return mFileUtils.appendPathComponent(getAssetsPath(), AssetsConstants.STATUS_FILE_NAME);
    }

    /**
     * Gets metadata about the current update.
     *
     * @return metadata about the current update.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public AssetsPackageInfo getCurrentPackageInfo() throws AssetsMalformedDataException, IOException {
        String statusFilePath = getStatusFilePath();
        if (!mFileUtils.fileAtPathExists(statusFilePath)) {
            return new AssetsPackageInfo();
        }
        return mAssetsUtils.getObjectFromJsonFile(statusFilePath, AssetsPackageInfo.class);

    }

    /**
     * Updates file containing information about the available packages.
     *
     * @param packageInfo new information.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void updateCurrentPackageInfo(AssetsPackageInfo packageInfo) throws IOException {
        try {
            mAssetsUtils.writeObjectToJsonFile(packageInfo, getStatusFilePath());
        } catch (IOException e) {
            throw new IOException("Error updating current package info", e);
        }
    }

    /**
     * Gets folder for storing current package files.
     *
     * @return folder for storing current package files.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public String getCurrentPackageFolderPath() throws AssetsMalformedDataException, IOException {
        String packageHash = getCurrentPackageHash();
        if (packageHash == null) {
            return null;
        }
        return getPackageFolderPath(packageHash);
    }

    /**
     * Gets folder for the package by the package hash.
     *
     * @param packageHash current package identifier (hash).
     * @return path to package folder.
     */
    public String getPackageFolderPath(String packageHash) {
        return mFileUtils.appendPathComponent(getAssetsPath(), packageHash);
    }

    /**
     * Gets file for package download.
     *
     * @return file for package download.
     * @throws IOException if read/write error occurred while accessing the file system.
     */
    public File getPackageDownloadFile() throws IOException {
        File downloadFolder = new File(getAssetsPath());
        if (!downloadFolder.exists()) {
            if (!downloadFolder.mkdirs()) {
                throw new IOException("Couldn't create directory" + downloadFolder.getAbsolutePath() + " for downloading file");
            }
        }
        return new File(downloadFolder, AssetsConstants.DOWNLOAD_FILE_NAME);
    }

    /**
     * Gets entry path to the application.
     *
     * @param entryFileName file name of the entry file.
     * @return entry path to the application.
     * @throws IOException                 read/write error occurred while accessing the file system.
     * @throws AssetsGetPackageException exception occurred when obtaining a package.
     */
    public String getCurrentPackageEntryPath(String entryFileName) throws AssetsGetPackageException, IOException {
        String packageFolder;
        try {
            packageFolder = getCurrentPackageFolderPath();
        } catch (AssetsMalformedDataException e) {
            throw new AssetsGetPackageException(e);
        }
        if (packageFolder == null) {
            return null;
        }
        AssetsLocalPackage currentPackage = getCurrentPackage();
        if (currentPackage == null) {
            return null;
        }
        String relativeEntryPath = currentPackage.getEntryPoint();
        if (relativeEntryPath == null) {
            return mFileUtils.appendPathComponent(packageFolder, entryFileName);
        } else {
            return mFileUtils.appendPathComponent(packageFolder, relativeEntryPath);
        }
    }

    /**
     * Gets the path to current update contents.
     * @param updateEntryPath path to the update in package contents, if provided.
     * @return path to current update contents.
     */
    public String getCurrentUpdatePath(String updateEntryPath) throws AssetsGetPackageException, IOException {
        String packageFolder;
        try {
            packageFolder = getCurrentPackageFolderPath();
        } catch (AssetsMalformedDataException e) {
            throw new AssetsGetPackageException(e);
        }
        if (packageFolder == null) {
            return null;
        }
        AssetsLocalPackage currentPackage = getCurrentPackage();
        if (currentPackage == null) {
            return null;
        }
        return mFileUtils.appendPathComponent(packageFolder, updateEntryPath);
    }

    /**
     * Gets the identifier of the current package (hash).
     *
     * @return the identifier of the current package.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public String getCurrentPackageHash() throws IOException, AssetsMalformedDataException {
        AssetsPackageInfo info = getCurrentPackageInfo();
        return info.getCurrentPackage();
    }

    /**
     * Gets the identifier of the previous installed package (hash).
     *
     * @return the identifier of the previous installed package.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     **/
    public String getPreviousPackageHash() throws IOException, AssetsMalformedDataException {
        AssetsPackageInfo info = getCurrentPackageInfo();
        return info.getPreviousPackage();
    }

    /**
     * Gets current package json object.
     *
     * @return current package json object.
     * @throws AssetsGetPackageException exception occurred when obtaining a package.
     */
    public AssetsLocalPackage getCurrentPackage() throws AssetsGetPackageException {
        String packageHash;
        try {
            packageHash = getCurrentPackageHash();
        } catch (IOException | AssetsMalformedDataException e) {
            throw new AssetsGetPackageException(e);
        }
        if (packageHash == null) {
            return null;
        }
        return getPackage(packageHash);
    }

    /**
     * Gets previous installed package json object.
     *
     * @return previous installed package json object.
     * @throws AssetsGetPackageException exception occurred when obtaining a package.
     */
    public AssetsLocalPackage getPreviousPackage() throws AssetsGetPackageException {
        String packageHash;
        try {
            packageHash = getPreviousPackageHash();
        } catch (IOException | AssetsMalformedDataException e) {
            throw new AssetsGetPackageException(e);
        }
        if (packageHash == null) {
            return null;
        }
        return getPackage(packageHash);
    }

    /**
     * Gets package object by its hash.
     *
     * @param packageHash package identifier (hash).
     * @return package object.
     * @throws AssetsGetPackageException exception occurred when obtaining a package.
     */
    public AssetsLocalPackage getPackage(String packageHash) throws AssetsGetPackageException {
        String folderPath = getPackageFolderPath(packageHash);
        String packageFilePath = mFileUtils.appendPathComponent(folderPath, AssetsConstants.PACKAGE_FILE_NAME);
        try {
            return mAssetsUtils.getObjectFromJsonFile(packageFilePath, AssetsLocalPackage.class);
        } catch (AssetsMalformedDataException e) {
            throw new AssetsGetPackageException(e);
        }
    }

    /**
     * Deletes the current package and installs the previous one.
     *
     * @throws AssetsRollbackException exception occurred during package rollback.
     */
    public void rollbackPackage() throws AssetsRollbackException {
        try {
            AssetsPackageInfo info = getCurrentPackageInfo();
            String currentPackageFolderPath = getCurrentPackageFolderPath();
            mFileUtils.deleteDirectoryAtPath(currentPackageFolderPath);
            info.setCurrentPackage(info.getPreviousPackage());
            info.setPreviousPackage(null);
            updateCurrentPackageInfo(info);
        } catch (IOException | AssetsMalformedDataException e) {
            throw new AssetsRollbackException(e);
        }
    }

    /**
     * Installs the new package.
     *
     * @param packageHash         package hash to install.
     * @param removePendingUpdate whether to remove pending updates data.
     * @throws AssetsInstallException exception occurred during package installation.
     */
    public void installPackage(String packageHash, boolean removePendingUpdate) throws AssetsInstallException {
        try {
            AssetsPackageInfo info = getCurrentPackageInfo();
            String currentPackageHash = getCurrentPackageHash();
            if (packageHash != null && packageHash.equals(currentPackageHash)) {

                /* The current package is already the one being installed, so we should no-op. */
                return;
            }
            if (removePendingUpdate) {
                String currentPackageFolderPath = getCurrentPackageFolderPath();
                if (currentPackageFolderPath != null) {
                    mFileUtils.deleteDirectoryAtPath(currentPackageFolderPath);
                }
            } else {
                String previousPackageHash = getPreviousPackageHash();
                if (previousPackageHash != null && !previousPackageHash.equals(packageHash)) {
                    mFileUtils.deleteDirectoryAtPath(getPackageFolderPath(previousPackageHash));
                }
                info.setPreviousPackage(info.getCurrentPackage());
            }
            info.setCurrentPackage(packageHash);
            updateCurrentPackageInfo(info);
        } catch (IOException | AssetsMalformedDataException e) {
            throw new AssetsInstallException(e);
        }
    }

    /**
     * Clears all the updates data.
     *
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public void clearUpdates() throws IOException {
        mFileUtils.deleteDirectoryAtPath(getAssetsPath());
    }

    /**
     * Downloads the update package.
     *
     * @param packageHash            update package hash.
     * @param downloadPackageRequest instance of {@link ApiHttpRequest} to download the update.
     * @return downloaded package.
     * @throws AssetsDownloadPackageException an exception occurred during package downloading.
     */
    public AssetsDownloadPackageResult downloadPackage(String packageHash, ApiHttpRequest<AssetsDownloadPackageResult> downloadPackageRequest) throws AssetsDownloadPackageException {
        String newUpdateFolderPath = getPackageFolderPath(packageHash);
        if (mFileUtils.fileAtPathExists(newUpdateFolderPath)) {

            /* This removes any stale data in <code>newPackageFolderPath</code> that could have been left
             * uncleared due to a crash or error during the download or install process. */
            try {
                mFileUtils.deleteDirectoryAtPath(newUpdateFolderPath);
            } catch (IOException e) {
                throw new AssetsDownloadPackageException(e);
            }
        }

        /* Download the file while checking if it is a zip and notifying client of progress. */
        AssetsDownloadPackageResult downloadPackageResult;
        try {
            downloadPackageResult = downloadPackageRequest.makeRequest();
        } catch (AssetsApiHttpRequestException e) {
            throw new AssetsDownloadPackageException(e);
        }
        return downloadPackageResult;
    }

    /**
     * Unzips the following package file.
     *
     * @param downloadFile package file.
     * @throws AssetsUnzipException an exception occurred during unzipping.
     */
    public void unzipPackage(File downloadFile) throws AssetsUnzipException {
        String unzippedFolderPath = getUnzippedFolderPath();
        try {
            File unzippedFolder = new File(unzippedFolderPath);
            mFileUtils.unzipFile(downloadFile, unzippedFolder);
            mFileUtils.deleteFileOrFolderSilently(downloadFile);
            // Rename app package directory to match configured app name
            for (File file : unzippedFolder.listFiles()) {
                if (file.isDirectory()) {
                    if (!file.renameTo(new File(unzippedFolder, mAssetsConfiguration.getAppName()))) {
                        throw new IOException("Unable to rename package file.");
                    }
                    return;
                }
            }
            mFileUtils.deleteFileOrFolderSilently(downloadFile);
        } catch (IOException e) {
            throw new AssetsUnzipException(e);
        }
    }

    /**
     * Merges contents with the current update based on the manifest.
     *
     * @param newUpdateFolderPath        directory for new update.
     * @param newUpdateMetadataPath      path to update metadata file for new update.
     * @param newUpdateHash              hash of the new update package.
     * @param stringPublicKey            public key used to verify signature.
     *                                   Can be <code>null</code> if code signing is not enabled.
     * @param expectedEntryPointFileName file name of the update entry point.
     * @return actual new update entry point.
     * @throws AssetsMergeException an exception occurred during merging.
     */
    public String mergeDiff(String newUpdateFolderPath, String newUpdateMetadataPath, String newUpdateHash, String stringPublicKey, String expectedEntryPointFileName) throws AssetsMergeException {
        String unzippedFolderPath = getUnzippedFolderPath();
        String diffManifestFilePath = mFileUtils.appendPathComponent(unzippedFolderPath, AssetsConstants.DIFF_MANIFEST_FILE_NAME);

        /* If this is a diff, not full update, copy the new files to the package directory. */
        boolean isDiffUpdate = mFileUtils.fileAtPathExists(diffManifestFilePath);
        try {
            if (isDiffUpdate) {
                String currentPackageFolderPath = getCurrentPackageFolderPath();
                if (currentPackageFolderPath != null) {
                    mAssetsUpdateUtils.copyNecessaryFilesFromCurrentPackage(diffManifestFilePath, currentPackageFolderPath, newUpdateFolderPath);
                }
                File diffManifestFile = new File(diffManifestFilePath);
                if (!diffManifestFile.delete()) {
                    throw new AssetsMergeException("Couldn't delete diff manifest file " + diffManifestFilePath);
                }
            }
            mFileUtils.copyDirectoryContents(new File(unzippedFolderPath), new File(newUpdateFolderPath));
            mFileUtils.deleteDirectoryAtPath(unzippedFolderPath);
        } catch (IOException | AssetsMalformedDataException | JSONException e) {
            throw new AssetsMergeException(e);
        }
        String entryPoint = mAssetsUpdateUtils.findEntryPointInUpdateContents(newUpdateFolderPath, expectedEntryPointFileName);
        if (mFileUtils.fileAtPathExists(newUpdateMetadataPath)) {
            File metadataFileFromOldUpdate = new File(newUpdateMetadataPath);
            if (!metadataFileFromOldUpdate.delete()) {
                throw new AssetsMergeException("Couldn't delete metadata file from old update " + newUpdateMetadataPath);
            }
        }
        if (isDiffUpdate) {
            AppCenterLog.info(Assets.LOG_TAG, "Applying diff update.");
        } else {
            AppCenterLog.info(Assets.LOG_TAG, "Applying full update.");
        }
        try {
            verifySignature(newUpdateFolderPath, stringPublicKey, newUpdateHash, isDiffUpdate);
        } catch (AssetsSignatureVerificationException e) {
            throw new AssetsMergeException(e);
        }

        return entryPoint;
    }

    /**
     * Verifies package signature if code signing is enabled.
     *
     * @param newUpdateFolderPath path to the current update.
     * @param stringPublicKey public key used to verify signature.
     *                        Can be <code>null</code> if code signing is not enabled.
     * @param newUpdateHash   hash of the update package.
     * @param isDiffUpdate    <code>true</code> if this is a diff update, <code>false</code> if this is a full update.
     * @throws AssetsSignatureVerificationException an exception during verifying package signature.
     */
    public void verifySignature(String newUpdateFolderPath, String stringPublicKey, String newUpdateHash, boolean isDiffUpdate) throws AssetsSignatureVerificationException {
        try {
            boolean isSignatureVerificationEnabled = (stringPublicKey != null);
            String signaturePath = mAssetsUpdateUtils.getJWTFilePath(newUpdateFolderPath);
            boolean isSignatureAppearedInApp = mFileUtils.fileAtPathExists(signaturePath);
            if (isSignatureVerificationEnabled) {
                if (isSignatureAppearedInApp) {
                    mAssetsUpdateUtils.verifyFolderHash(newUpdateFolderPath, newUpdateHash);
                    mAssetsUpdateUtils.verifyUpdateSignature(newUpdateFolderPath, newUpdateHash, stringPublicKey);
                } else {
                    throw new AssetsSignatureVerificationException(AssetsSignatureVerificationException.SignatureExceptionType.NO_SIGNATURE);
                }
            } else {
                if (isSignatureAppearedInApp) {
                    AppCenterLog.info(Assets.LOG_TAG,
                            "Warning! JWT signature exists in codepush update but code integrity check couldn't be performed because there is no public key configured. "
                                    + "Please ensure that public key is properly configured within your application."
                    );
                    mAssetsUpdateUtils.verifyFolderHash(newUpdateFolderPath, newUpdateHash);
                } else {
                    if (isDiffUpdate) {
                        mAssetsUpdateUtils.verifyFolderHash(newUpdateFolderPath, newUpdateHash);
                    }
                }
            }
        } catch (IOException e) {
            throw new AssetsSignatureVerificationException(e);
        }
    }
}
