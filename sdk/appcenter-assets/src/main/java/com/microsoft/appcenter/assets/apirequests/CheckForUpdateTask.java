package com.microsoft.appcenter.assets.apirequests;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateResponse;
import com.microsoft.appcenter.assets.exceptions.AssetsFinalizeException;
import com.microsoft.appcenter.assets.exceptions.AssetsQueryUpdateException;
import com.microsoft.appcenter.assets.utils.AssetsUtils;
import com.microsoft.appcenter.assets.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.appcenter.assets.Assets.LOG_TAG;

/**
 * Performs sending status reports to server.
 */
public class CheckForUpdateTask extends BaseHttpTask<AssetsUpdateResponse> {

    /**
     * Creates an instance of {@link CheckForUpdateTask}.
     *
     * @param fileUtils     instance of {@link FileUtils} to work with.
     * @param assetsUtils instance of {@link AssetsUtils} to work with.
     * @param requestUrl    url to query update against.
     */
    public CheckForUpdateTask(FileUtils fileUtils, AssetsUtils assetsUtils, String requestUrl) {
        mFileUtils = fileUtils;
        mAssetsUtils = assetsUtils;
        mRequestUrl = requestUrl;
    }

    @Override
    protected AssetsUpdateResponse doInBackground(Void... voids) {
        InputStream stream = null;
        Scanner scanner = null;
        HttpURLConnection connection;
        try {
            connection = createConnection(mRequestUrl);
        } catch (IOException e) {

            /* We can't throw custom errors from this function, so any error will be passed to the result. */
            mExecutionException = new AssetsQueryUpdateException(e);
            return null;
        }
        try {
            boolean failed;
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                stream = connection.getInputStream();
                failed = false;
            } else {
                stream = connection.getErrorStream();
                failed = true;
            }
            scanner = new Scanner(stream).useDelimiter("\\A");
            String result = scanner.hasNext() ? scanner.next() : "";
            if (failed) {
                AppCenterLog.info(LOG_TAG, result);
                mExecutionException = new AssetsQueryUpdateException(result);
                return null;
            } else {
                return mAssetsUtils.convertStringToObject(result, AssetsUpdateResponse.class);
            }
        } catch (IOException e) {
            mExecutionException = new AssetsQueryUpdateException(e);
            return null;
        } finally {
            Exception e = mFileUtils.finalizeResources(
                    Arrays.asList(stream, scanner),
                    null);
            if (e != null) {
                mFinalizeException = new AssetsFinalizeException(e);
            }
        }
    }
}
