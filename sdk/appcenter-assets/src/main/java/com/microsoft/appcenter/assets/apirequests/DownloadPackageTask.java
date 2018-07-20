package com.microsoft.appcenter.assets.apirequests;

import com.microsoft.appcenter.assets.AssetsConstants;
import com.microsoft.appcenter.assets.DownloadProgress;
import com.microsoft.appcenter.assets.exceptions.AssetsDownloadPackageException;
import com.microsoft.appcenter.assets.exceptions.AssetsFinalizeException;
import com.microsoft.appcenter.assets.interfaces.DownloadProgressCallback;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadPackageResult;
import com.microsoft.appcenter.assets.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Downloads an update.
 */
public class DownloadPackageTask extends BaseHttpTask<AssetsDownloadPackageResult> {

    /**
     * Header in the beginning of every zip file.
     */
    private final static Integer ZIP_HEADER = 0x504b0304;

    /**
     * Path to download file to.
     */
    private File mDownloadFile;

    /**
     * Callback for download process.
     */
    private DownloadProgressCallback mDownloadProgressCallback;

    /**
     * Creates instance of {@link DownloadPackageTask}.
     *
     * @param fileUtils                instance of {@link FileUtils} to work with.
     * @param requestUrl               url for downloading an update.
     * @param downloadFile             path to download file to.
     * @param downloadProgressCallback callback for download progress.
     */
    public DownloadPackageTask(FileUtils fileUtils, String requestUrl, File downloadFile, DownloadProgressCallback downloadProgressCallback) {
        mFileUtils = fileUtils;
        mRequestUrl = requestUrl;
        mDownloadFile = downloadFile;
        mDownloadProgressCallback = downloadProgressCallback;
    }

    @Override
    protected AssetsDownloadPackageResult doInBackground(Void... params) {
        HttpURLConnection connection;
        BufferedInputStream bufferedInputStream = null;
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            connection = createConnection(mRequestUrl);
        } catch (IOException e) {

            /* We can't throw custom errors from this function, so any error will be passed to the result. */
            mExecutionException = new AssetsDownloadPackageException(mRequestUrl, e);
            return null;
        }
        try {
            bufferedInputStream = new BufferedInputStream(connection.getInputStream());
            long totalBytes = connection.getContentLength();
            long receivedBytes = 0;
            fileOutputStream = new FileOutputStream(mDownloadFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream, AssetsConstants.DOWNLOAD_BUFFER_SIZE);
            byte[] data = new byte[AssetsConstants.DOWNLOAD_BUFFER_SIZE];

            /* Header allows us to check whether this is a zip-stream. */
            byte[] header = new byte[4];
            int numBytesRead;
            while ((numBytesRead = bufferedInputStream.read(data, 0, AssetsConstants.DOWNLOAD_BUFFER_SIZE)) > 0) {
                if (receivedBytes < 4) {
                    for (int i = 0; i < numBytesRead; i++) {
                        int headerOffset = (int) (receivedBytes) + i;
                        if (headerOffset >= 4) {
                            break;
                        }
                        header[headerOffset] = data[i];
                    }
                }
                receivedBytes += numBytesRead;
                bufferedOutputStream.write(data, 0, numBytesRead);
                if (mDownloadProgressCallback != null) {
                    mDownloadProgressCallback.call(new DownloadProgress(totalBytes, receivedBytes));
                }
            }
            if (totalBytes >= 0 && totalBytes != receivedBytes) {
                mExecutionException = new AssetsDownloadPackageException(receivedBytes, totalBytes);
                return null;
            }
            boolean isZip = ByteBuffer.wrap(header).getInt() == ZIP_HEADER;
            return new AssetsDownloadPackageResult(mDownloadFile, isZip);
        } catch (IOException e) {
            mExecutionException = new AssetsDownloadPackageException(e);
            return null;
        } finally {
            Exception e = mFileUtils.finalizeResources(
                    Arrays.asList(bufferedOutputStream, fileOutputStream, bufferedInputStream),
                    null);
            if (e != null) {
                mFinalizeException = new AssetsFinalizeException(e);
            }
        }
    }
}
