package com.microsoft.appcenter.assets.utils;

import android.content.Context;

import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.exceptions.AssetsGeneralException;
import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;

import java.io.IOException;

import static com.microsoft.appcenter.assets.AssetsConstants.ASSETS_APK_BUILD_TIME_KEY;

/**
 * Android-specific instance of {@link AssetsPlatformUtils}.
 */
public class AndroidUtils implements AssetsPlatformUtils {

    /**
     * Instance of {@link AndroidUtils}. Singleton.
     */
    private static AndroidUtils INSTANCE;

    /**
     * Private constructor to prevent creating utils manually.
     */
    private AndroidUtils() {
    }

    /**
     * Provides instance of the utils class.
     *
     * @return instance.
     */
    public static AndroidUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AndroidUtils();
        }
        return INSTANCE;
    }

    @Override
    public boolean isPackageLatest(AssetsLocalPackage packageMetadata, String currentAppVersion, Context context) throws AssetsGeneralException {
        try {
            Long binaryModifiedDateDuringPackageInstall = null;
            String binaryModifiedDateDuringPackageInstallString = packageMetadata.getBinaryModifiedTime();
            if (binaryModifiedDateDuringPackageInstallString != null) {
                binaryModifiedDateDuringPackageInstall = Long.parseLong(binaryModifiedDateDuringPackageInstallString);
            }
            String packageAppVersion = packageMetadata.getAppVersion();
            long binaryResourcesModifiedTime = getBinaryResourcesModifiedTime(context);
            return binaryModifiedDateDuringPackageInstall != null &&
                    binaryModifiedDateDuringPackageInstall == binaryResourcesModifiedTime &&
                    currentAppVersion.equals(packageAppVersion);
        } catch (NumberFormatException e) {
            throw new AssetsGeneralException("Error in reading binary modified date from package metadata", e);
        }
    }

    @Override
    public long getBinaryResourcesModifiedTime(Context context) throws NumberFormatException {
        String packageName = context.getPackageName();
        int assetsApkBuildTimeId = context.getResources().getIdentifier(ASSETS_APK_BUILD_TIME_KEY, "string", packageName);

        /* Double quotes replacement is needed for correct restoration of long values from strings.xml.
         * See https://github.com/Microsoft/cordova-plugin-code-push/issues/264 */
        String assetsApkBuildTime = context.getResources().getString(assetsApkBuildTimeId).replaceAll("\"", "");
        return Long.parseLong(assetsApkBuildTime);
    }

    @Override
    public void clearDebugCache(Context context) throws IOException {

    }
}
