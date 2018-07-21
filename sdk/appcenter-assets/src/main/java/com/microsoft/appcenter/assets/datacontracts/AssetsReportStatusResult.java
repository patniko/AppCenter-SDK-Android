package com.microsoft.appcenter.assets.datacontracts;

/**
 * Represents the result of sending status report to server.
 */
public class AssetsReportStatusResult {

    /**
     * The result string from server.
     */
    private String result;

    /**
     * Creates an instance of the {@link AssetsReportStatusResult} that has been successful.
     *
     * @param result result string from server.
     * @return instance of the class.
     */
    public static AssetsReportStatusResult createSuccessful(String result) {
        AssetsReportStatusResult assetsReportStatusResult = new AssetsReportStatusResult();
        assetsReportStatusResult.setResult(result);
        return assetsReportStatusResult;
    }

    /**
     * Gets the result string from server.
     *
     * @return result string from server.
     */
    public String getResult() {
        return result;
    }

    /**
     * Sets the result string from server.
     *
     * @param result result string from server.
     */
    public void setResult(String result) {
        this.result = result;
    }
}
