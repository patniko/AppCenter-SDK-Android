package com.microsoft.appcenter.assets.core;

import com.microsoft.appcenter.assets.utils.AssetsUpdateUtils;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;

/**
 * Encapsulates utilities that {@link AssetsBaseCore} is using.
 */
@SuppressWarnings("WeakerAccess")
public class AssetsUtilities {

    /**
     * Instance of {@link AssetsUtils}.
     */
    public AssetsUtils mUtils;

    /**
     * Instance of {@link FileUtils}.
     */
    public FileUtils mFileUtils;

    /**
     * Instance of {@link AssetsUpdateUtils}.
     */
    public AssetsUpdateUtils mUpdateUtils;

    /**
     * Instance of {@link AssetsPlatformUtils}.
     */
    public AssetsPlatformUtils mPlatformUtils;

    /**
     * Create instance of AssetsUtilities.
     *
     * @param utils         instance of {@link AssetsUtils}.
     * @param fileUtils     instance of {@link FileUtils}.
     * @param updateUtils   instance of {@link AssetsUpdateUtils}.
     * @param platformUtils instance of {@link AssetsPlatformUtils}.
     */
    public AssetsUtilities(
            AssetsUtils utils,
            FileUtils fileUtils,
            AssetsUpdateUtils updateUtils,
            AssetsPlatformUtils platformUtils) {
        this.mUtils = utils;
        this.mFileUtils = fileUtils;
        this.mUpdateUtils = updateUtils;
        this.mPlatformUtils = platformUtils;
    }
}
