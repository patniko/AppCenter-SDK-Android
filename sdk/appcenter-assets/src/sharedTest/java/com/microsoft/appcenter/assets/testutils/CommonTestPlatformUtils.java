package com.microsoft.appcenter.assets.testutils;

import android.content.Context;

import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.exceptions.AssetsGeneralException;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;

import java.io.IOException;

/**
 * Platform specific implementation of utils (only for testing).
 */
public class CommonTestPlatformUtils implements AssetsPlatformUtils {

    /**
     * Instance of the utils implementation (singleton).
     */
    private static CommonTestPlatformUtils INSTANCE;

    /**
     * Private constructor to prevent creating utils manually.
     */
    private CommonTestPlatformUtils() {
    }

    /**
     * Provides instance of the utils class.
     *
     * @return instance.
     */
    public static CommonTestPlatformUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CommonTestPlatformUtils();
        }
        return INSTANCE;
    }

    //TODO Implement test for this method
    @Override
    public boolean isPackageLatest(AssetsLocalPackage packageMetadata, String currentAppVersion, Context context) throws AssetsGeneralException {
        return false;
    }

    //TODO Implement test for this method
    @Override
    public long getBinaryResourcesModifiedTime(Context context) throws NumberFormatException {
        return 0;
    }

    //TODO Implement test for this method
    @Override
    public void clearDebugCache(Context context) throws IOException {

    }
}
