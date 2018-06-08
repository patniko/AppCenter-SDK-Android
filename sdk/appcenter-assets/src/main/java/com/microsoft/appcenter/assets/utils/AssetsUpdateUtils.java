package com.microsoft.appcenter.assets.utils;

import android.content.Context;
import android.util.Base64;

import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsConstants;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.exceptions.AssetsSignatureVerificationException;
import com.microsoft.appcenter.assets.exceptions.AssetsSignatureVerificationException.SignatureExceptionType;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HashUtils;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Utils class for CodePush updates.
 */
public class AssetsUpdateUtils {

    /**
     * Instance of {@link FileUtils} to work with.
     */
    private FileUtils mFileUtils;

    /**
     * Instance of {@link AssetsUtils} to work with.
     */
    private AssetsUtils mAssetsUtils;

    /**
     * Instance of the class (singleton).
     */
    private static AssetsUpdateUtils INSTANCE;

    /**
     * Gets and instance of {@link AssetsUpdateUtils}.
     *
     * @param fileUtils     instance of {@link FileUtils} to work with.
     * @param assetsUtils instance of {@link AssetsUtils} to work with.
     * @return instance of the class.
     */
    public static AssetsUpdateUtils getInstance(FileUtils fileUtils, AssetsUtils assetsUtils) {
        if (INSTANCE == null) {
            INSTANCE = new AssetsUpdateUtils();
        }
        INSTANCE.mFileUtils = fileUtils;
        INSTANCE.mAssetsUtils = assetsUtils;
        return INSTANCE;
    }

    /**
     * Private constructor to prevent direct creating the instance of the class.
     */
    private AssetsUpdateUtils() {
    }

    /**
     * A string constant defining the operating system-specific new line marker.
     */
    @SuppressWarnings("WeakerAccess")
    public final String NEW_LINE = System.getProperty("line.separator");

    /**
     * Whether hashing file or directory should be ignored or not.
     *
     * @param relativeFilePath file path to check.
     * @return <code>true</code> if file path should be ignored during the hashing, <code>false</code> otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isHashIgnored(String relativeFilePath) {

        /* Note: The hashing logic here must mirror the hashing logic in other native SDK's, as well
         * as in the CLI. Ensure that any changes here are propagated to these other locations. */
        final String __MACOSX = "__MACOSX/";
        final String DS_STORE = ".DS_Store";
        final String ASSETS_METADATA = ".codepushrelease";
        return relativeFilePath.startsWith(__MACOSX)
                || relativeFilePath.equals(DS_STORE)
                || relativeFilePath.endsWith("/" + DS_STORE)
                || relativeFilePath.equals(ASSETS_METADATA)
                || relativeFilePath.endsWith("/" + ASSETS_METADATA);
    }

    /**
     * Method recursively walks through the directory, computes hash for each file within it and adds
     * respective computed entries <code>path:pathHash</code> to manifest object.
     *
     * @param folderPath root directory for walking.
     * @param pathPrefix prefix for each path which will be added in manifest to avoid using absolute paths.
     * @param manifest   reference to manifest object.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    private void addContentsOfFolderToManifest(String folderPath, String pathPrefix, ArrayList<String> manifest) throws IOException {
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();
        if (folderFiles == null) {
            throw new IOException("Pathname " + folderPath + " doesn't denote a directory.");
        }
        for (File file : folderFiles) {
            String fileName = file.getName();
            String fullFilePath = file.getAbsolutePath();
            String relativePath = (pathPrefix.isEmpty() ? "" : (pathPrefix + "/")) + fileName;
            if (isHashIgnored(relativePath)) {
                continue;
            }
            if (file.isDirectory()) {
                addContentsOfFolderToManifest(fullFilePath, relativePath, manifest);
            } else {
                try {
                    String fileData = mFileUtils.readFileToString(file.getAbsolutePath());
                    manifest.add(relativePath + ":" + computeHash(fileData));
                } catch (IOException e) {
                    throw new IOException("Unable to compute hash of update contents.", e);
                }
            }
        }
    }

    /**
     * Computes hash for string.
     *
     * @param data input data string.
     * @return computed hash.
     */
    private String computeHash(String data) {
        return HashUtils.sha256(data);
    }

    /**
     * Fills new package directory with files following diff manifest rules:
     * <ul>
     * <li>copy current installed package files to destination directory;</li>
     * <li>delete files from destination directory specified in `deletedFiles` of diff manifest.</li>
     * </ul>
     *
     * @param diffManifestFilePath     path to diff manifest file.
     * @param currentPackageFolderPath path to current package directory.
     * @param newPackageFolderPath     path to new package directory.
     * @throws IOException                    read/write error occurred while accessing the file system.
     * @throws JSONException                  error occurred during parsing a json object.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public void copyNecessaryFilesFromCurrentPackage(
            String diffManifestFilePath,
            String currentPackageFolderPath,
            String newPackageFolderPath
    ) throws IOException, JSONException, AssetsMalformedDataException {
        mFileUtils.copyDirectoryContents(new File(currentPackageFolderPath), new File(newPackageFolderPath));
        JSONObject diffManifest = mAssetsUtils.getJsonObjectFromFile(diffManifestFilePath);
        JSONArray deletedFiles = diffManifest.getJSONArray("deletedFiles");
        for (int i = 0; i < deletedFiles.length(); i++) {
            String fileNameToDelete = deletedFiles.getString(i);
            File fileToDelete = new File(newPackageFolderPath, fileNameToDelete);
            if (fileToDelete.exists()) {
                if (!fileToDelete.delete()) {
                    throw new IOException("Unable to delete file " + fileToDelete.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Locates hash computed on bundle file that was generated during the app build.
     *
     * @param context     application context.
     * @param isDebugMode is application running in debug mode.
     * @return hash value.
     */
    public String getHashForBinaryContents(Context context, boolean isDebugMode) throws AssetsMalformedDataException {
        try {
            return mAssetsUtils.getStringFromInputStream(context.getAssets().open(AssetsConstants.ASSETS_HASH_FILE_NAME));
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Computes hash of a directory and compares it with expected one.
     * If verification fails exception will be thrown.<br>
     * <p>
     * Hashing algorithm:
     * <ul>
     * <li>1. Recursively generate a sorted array of format &lt;relativeFilePath&gt;: &lt;sha256FileHash&gt;</li>
     * <li>2. JSON stringify the array</li>
     * <li>3. SHA256-hash the result</li>
     * </ul>
     *
     * @param folderPath   path to directory.
     * @param expectedHash expected hash value.
     * @return <code>true</code>, if verification succeeded, <code>false</code> otherwise.
     * @throws IOException read/write error occurred while accessing the file system.
     */
    public boolean verifyFolderHash(String folderPath, String expectedHash) throws IOException {
        AppCenterLog.info(Assets.LOG_TAG, "Verifying hash for folder path: " + folderPath);
        ArrayList<String> updateContentsManifest = new ArrayList<>();
        try {
            addContentsOfFolderToManifest(folderPath, "", updateContentsManifest);
        } catch (IOException e) {
            throw new IOException("Unable to build local manifest file.", e);
        }

        /* Sort manifest strings to make sure, that they are completely equal with manifest strings has been generated in cli! */
        Collections.sort(updateContentsManifest);
        JSONArray updateContentsJSONArray = new JSONArray();
        for (String manifestEntry : updateContentsManifest) {
            updateContentsJSONArray.put(manifestEntry);
        }

        /* The JSON serialization turns path separators into "\/", e.g. "Assets\/assets\/image.png". */
        String updateContentsManifestString = updateContentsJSONArray.toString().replace("\\/", "/");
        AppCenterLog.info(Assets.LOG_TAG, "Manifest string: " + updateContentsManifestString);
        String updateContentsManifestHash = computeHash(updateContentsManifestString);
        AppCenterLog.info(Assets.LOG_TAG, "Expected hash: " + expectedHash + ", actual hash: " + updateContentsManifestHash);
        return expectedHash.equals(updateContentsManifestHash);
    }

    /**
     * Verifies and decodes JWT.
     *
     * @param jwt       JWT string.
     * @param publicKey public key for verification.
     * @return <i>claims</i> value of decoded payload or null if error occurred.
     * @throws AssetsSignatureVerificationException if error occurred during JWT decoding or verification.
     */
    @SuppressWarnings("WeakerAccess")
    public Map<String, Object> verifyAndDecodeJWT(String jwt, PublicKey publicKey) throws AssetsSignatureVerificationException {
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
            if (signedJWT.verify(verifier)) {
                Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();
                AppCenterLog.info(Assets.LOG_TAG, "JWT verification succeeded, payload content: " + claims.toString());
                return claims;
            }
        } catch (JOSEException | ParseException e) {
            throw new AssetsSignatureVerificationException(e);
        }
        throw new AssetsSignatureVerificationException(SignatureExceptionType.NOT_SIGNED);
    }

    /**
     * Parses public key from string into {@link PublicKey} class instance.
     *
     * @param stringPublicKey input public key value.
     * @return parsed {@link PublicKey} class instance.
     * @throws AssetsSignatureVerificationException error during public key parsing.
     */
    @SuppressWarnings("WeakerAccess")
    public PublicKey parsePublicKey(String stringPublicKey) throws AssetsSignatureVerificationException {

        /* Remove unnecessary "begin/end public key" entries from string. */
        stringPublicKey = stringPublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace(NEW_LINE, "");
        byte[] byteKey = Base64.decode(stringPublicKey.getBytes(), Base64.DEFAULT);
        X509EncodedKeySpec X509Key = new X509EncodedKeySpec(byteKey);
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(X509Key);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssetsSignatureVerificationException(SignatureExceptionType.PUBLIC_KEY_NOT_PARSED, e);
        }
    }

    /**
     * Returns JWT file path of local update.
     *
     * @param updateFolderPath local update directory path.
     * @return JWT file path of update.
     */
    @SuppressWarnings("WeakerAccess")
    public String getJWTFilePath(String updateFolderPath) {
        return mFileUtils.appendPathComponent(updateFolderPath, AssetsConstants.BUNDLE_JWT_FILE_NAME);
    }

    /**
     * Returns JWT content of local update.
     *
     * @param folderPath local update directory path.
     * @return JWT content of update.
     * @throws AssetsSignatureVerificationException error during signature verification.
     */
    @SuppressWarnings("WeakerAccess")
    public String getJWT(String folderPath) throws AssetsSignatureVerificationException {
        final String signatureFilePath = getJWTFilePath(folderPath);
        try {
            return mFileUtils.readFileToString(signatureFilePath);
        } catch (IOException e) {
            throw new AssetsSignatureVerificationException(SignatureExceptionType.READ_SIGNATURE_FILE_ERROR, e);
        }
    }

    /**
     * Verifies signature of local update.
     *
     * @param folderPath      directory of local update.
     * @param packageHash     remote package hash.
     * @param stringPublicKey public key value.
     * @return <code>true</code> if signature valid, <code>false</code> otherwise.
     * @throws AssetsSignatureVerificationException error during signature verification.
     */
    public boolean verifyUpdateSignature(String folderPath, String packageHash, String stringPublicKey) throws AssetsSignatureVerificationException {
        AppCenterLog.info(Assets.LOG_TAG, "Verifying signature for folder path: " + folderPath);
        final PublicKey publicKey = parsePublicKey(stringPublicKey);
        final String jwt = getJWT(folderPath);
        final Map<String, Object> claims = verifyAndDecodeJWT(jwt, publicKey);
        final String contentHash = (String) claims.get("contentHash");
        if (contentHash == null) {
            throw new AssetsSignatureVerificationException(SignatureExceptionType.NO_CONTENT_HASH);
        }
        return contentHash.equals(packageHash);
    }

    /**
     * Recursively searches for the specified entry point in update files.
     *
     * @param folderPath       path to folder containing update files (search location).
     * @param expectedFileName expected file name of the entry point.
     * @return full path to entry point.
     */
    public String findEntryPointInUpdateContents(String folderPath, String expectedFileName) {
        File folder = new File(folderPath);
        File[] folderFiles = folder.listFiles();
        for (File file : folderFiles) {
            String fullFilePath = mFileUtils.appendPathComponent(folderPath, file.getName());
            if (file.isDirectory()) {
                String mainBundlePathInSubFolder = findEntryPointInUpdateContents(fullFilePath, expectedFileName);
                if (mainBundlePathInSubFolder != null) {
                    return mFileUtils.appendPathComponent(file.getName(), mainBundlePathInSubFolder);
                }
            } else {
                String fileName = file.getName();
                if (fileName.equals(expectedFileName)) {
                    return fileName;
                }
            }
        }
        return null;
    }

}
