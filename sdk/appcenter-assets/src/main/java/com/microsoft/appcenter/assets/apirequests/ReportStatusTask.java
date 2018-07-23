package com.microsoft.appcenter.assets.apirequests;

import com.microsoft.appcenter.assets.datacontracts.AssetsReportStatusResult;
import com.microsoft.appcenter.assets.exceptions.AssetsFinalizeException;
import com.microsoft.appcenter.assets.exceptions.AssetsReportStatusException;
import com.microsoft.appcenter.assets.enums.ReportType;
import com.microsoft.appcenter.assets.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

/**
 * Performs sending status reports to server.
 */
public class ReportStatusTask extends BaseHttpTask<AssetsReportStatusResult> {

    /**
     * Report as json string.
     */
    private String mJson;

    /**
     * Type of the report as listen in {@link ReportType}.
     */
    private ReportType mReportType;

    /**
     * Creates an instance of {@link ReportStatusTask}.
     *
     * @param fileUtils  instance of {@link FileUtils} to work with.
     * @param requestUrl url to send report to.
     * @param json       report as json string.
     * @param reportType type of the report as listed in {@link ReportType}.
     */
    public ReportStatusTask(FileUtils fileUtils, String requestUrl, String json, ReportType reportType) {
        mFileUtils = fileUtils;
        mRequestUrl = requestUrl;
        mJson = json;
        mReportType = reportType;
    }

    @Override
    protected AssetsReportStatusResult doInBackground(Void... voids) {
        OutputStream outputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        InputStream stream = null;
        Scanner scanner = null;
        HttpURLConnection connection;
        try {
            connection = createConnection(mRequestUrl);
        } catch (IOException e) {

            /* We can't throw custom errors from this function, so any error will be passed to the result. */
            mExecutionException = new AssetsReportStatusException(e, mReportType);
            return null;
        }
        try {
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            outputStream = connection.getOutputStream();
            outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
            outputStreamWriter.write(mJson);
            outputStreamWriter.flush();
            outputStreamWriter.close();
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
                mExecutionException = new AssetsReportStatusException(result, mReportType);
                return null;
            } else {
                return AssetsReportStatusResult.createSuccessful(result);
            }
        } catch (IOException e) {
            mExecutionException = new AssetsReportStatusException(e, mReportType);
            return null;
        } finally {
            Exception e = mFileUtils.finalizeResources(
                    Arrays.asList(outputStream, stream, scanner),
                    null);
            if (e != null) {
                Exception wrappedException = new AssetsReportStatusException(e, mReportType);
                mFinalizeException = new AssetsFinalizeException(wrappedException);
            }
        }
    }
}
