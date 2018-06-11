package com.microsoft.appcenter.assets;

/**
 * Common set of the Assets-specific constants.
 */
public final class AssetsConstants {
    
    /**
     * Key from <code>build.gradle</code> file for TimeStamp value.
     * TimeStamp represents the time when binary package has been build.
     */
    public static final String BINARY_MODIFIED_TIME_KEY = "binaryModifiedTime";

    /**
     * Root folder name inside each update.
     */
    public static final String ASSETS_FOLDER_PREFIX = "Assets";

    /**
     * Key for getting hash file for binary contents from assets folder.
     */
    public static final String ASSETS_HASH_FILE_NAME = "AssetsHash";

    /**
     * Key for getting Assets shared preferences from application context.
     */
    public static final String ASSETS_PREFERENCES = "Assets";

    /**
     * File name for diff manifest that distributes with CodePush updates.
     */
    public static final String DIFF_MANIFEST_FILE_NAME = "hotcodepush.json";

    /**
     * Buffer size for downloading updates.
     */
    public static final int DOWNLOAD_BUFFER_SIZE = 1024 * 256;

    /**
     * Default file name for downloading updates.
     */
    public static final String DOWNLOAD_FILE_NAME = "download.zip";

    /**
     * Event name for dispatching sync status to JavaScript.
     * See {@link com.microsoft.appcenter.assets.enums.AssetsSyncStatus} for details.
     */
    public static final String SYNC_STATUS_EVENT_NAME = "AssetsSyncStatus";

    /**
     * Event name for dispatching Assets download progress to JavaScript.
     */
    public static final String DOWNLOAD_PROGRESS_EVENT_NAME = "AssetsDownloadProgress";

    /**
     * Event name for dispatching to JavaScript Assets update package that targets to other binary version.
     */
    public static final String BINARY_VERSION_MISMATCH_EVENT_NAME = "AssetsBinaryVersionMismatch";

    /**
     * Key for download url property from update manifest.
     */
    public static final String DOWNLOAD_URL_KEY = "downloadUrl";

    /**
     * Package file name to store update metadata file.
     */
    public static final String PACKAGE_FILE_NAME = "app.json";

    /**
     * Package hash key for running an update.
     */
    public static final String PACKAGE_HASH_KEY = "packageHash";

    /**
     * Name of the file containing information about the available packages.
     */
    public static final String STATUS_FILE_NAME = "assets.json";

    /**
     * Folder name for unzipped update.
     */
    public static final String UNZIPPED_FOLDER_NAME = "unzipped";

    /**
     * Key for getting binary resources modified time from <code>build.gradle</code> file.
     */
    public static final String ASSETS_APK_BUILD_TIME_KEY = "ASSETS_APK_BUILD_TIME";

    /**
     * File name for jwt file of signed CodePush update.
     */
    public static final String BUNDLE_JWT_FILE_NAME = ".codepushrelease";
}
