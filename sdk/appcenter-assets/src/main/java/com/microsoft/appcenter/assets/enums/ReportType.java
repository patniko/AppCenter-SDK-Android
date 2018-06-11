package com.microsoft.appcenter.assets.enums;

import com.microsoft.appcenter.assets.datacontracts.AssetsDeploymentStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsDownloadStatusReport;

/**
 * Type of the sent report.
 */
public enum ReportType {

    /**
     * {@link AssetsDownloadStatusReport}.
     */
    DOWNLOAD("Error occurred during delivering download status report."),

    /**
     * {@link AssetsDeploymentStatusReport}.
     */
    DEPLOY("Error occurred during delivering deploy status report.");

    /**
     * Message describing the exception depending on the report type.
     */
    private final String message;

    /**
     * Creates instance of the enum using the provided message.
     *
     * @param message message describing the exception.
     */
    ReportType(String message) {
        this.message = message;
    }

    /**
     * Gets the message of the specified type.
     *
     * @return message.
     */
    public String getMessage() {
        return this.message;
    }
}
