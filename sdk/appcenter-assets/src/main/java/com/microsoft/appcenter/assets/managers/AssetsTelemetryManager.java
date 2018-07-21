package com.microsoft.appcenter.assets.managers;

import com.microsoft.appcenter.assets.AssetsStatusReportIdentifier;
import com.microsoft.appcenter.assets.datacontracts.AssetsDeploymentStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.enums.AssetsDeploymentStatus;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;

import org.json.JSONException;

import static android.text.TextUtils.isEmpty;
import static com.microsoft.appcenter.assets.enums.AssetsDeploymentStatus.FAILED;
import static com.microsoft.appcenter.assets.enums.AssetsDeploymentStatus.SUCCEEDED;

/**
 * Manager responsible for get/update telemetry reports on device.
 */
public class AssetsTelemetryManager {

    /**
     * Instance of {@link SettingsManager} to work with.
     */
    private SettingsManager mSettingsManager;

    /**
     * Creates an instance of {@link AssetsTelemetryManager}.
     *
     * @param settingsManager instance of {@link SettingsManager} to work with.
     */
    public AssetsTelemetryManager(SettingsManager settingsManager) {
        mSettingsManager = settingsManager;
    }

    /**
     * Builds binary update report using current app version.
     *
     * @param appVersion current app version.
     * @return new binary update report.
     */
    public AssetsDeploymentStatusReport buildBinaryUpdateReport(String appVersion) throws AssetsIllegalArgumentException {
        AssetsStatusReportIdentifier previousStatusReportIdentifier = mSettingsManager.getPreviousStatusReportIdentifier();
        AssetsDeploymentStatusReport report = null;
        if (previousStatusReportIdentifier == null) {

            /* There was no previous status report */
            mSettingsManager.removeStatusReportSavedForRetry();
            report = new AssetsDeploymentStatusReport();
            report.setAppVersion(appVersion);
            report.setLabel("");
            report.setStatus(AssetsDeploymentStatus.SUCCEEDED);
        } else {
            boolean identifierHasDeploymentKey = previousStatusReportIdentifier.hasDeploymentKey();
            String identifierLabel = previousStatusReportIdentifier.getVersionLabelOrEmpty();
            if (identifierHasDeploymentKey || !identifierLabel.equals(appVersion)) {
                mSettingsManager.removeStatusReportSavedForRetry();
                report = new AssetsDeploymentStatusReport();
                if (identifierHasDeploymentKey) {
                    String previousDeploymentKey = previousStatusReportIdentifier.getDeploymentKey();
                    String previousLabel = previousStatusReportIdentifier.getVersionLabel();
                    report = new AssetsDeploymentStatusReport();
                    report.setAppVersion(appVersion);
                    report.setPreviousDeploymentKey(previousDeploymentKey);
                    report.setPreviousLabelOrAppVersion(previousLabel);
                } else {

                    /* Previous status report was with a binary app version. */
                    report.setAppVersion(appVersion);
                    report.setPreviousLabelOrAppVersion(previousStatusReportIdentifier.getVersionLabel());
                }
            }
        }
        return report;
    }

    /**
     * Builds update report using current local package information.
     *
     * @param currentPackage current local package information.
     * @return new update report.
     */
    public AssetsDeploymentStatusReport buildUpdateReport(AssetsLocalPackage currentPackage) throws AssetsIllegalArgumentException {
        AssetsStatusReportIdentifier currentPackageIdentifier = buildPackageStatusReportIdentifier(currentPackage);
        AssetsStatusReportIdentifier previousStatusReportIdentifier = mSettingsManager.getPreviousStatusReportIdentifier();
        AssetsDeploymentStatusReport report = null;
        if (currentPackageIdentifier != null) {
            if (previousStatusReportIdentifier == null) {
                mSettingsManager.removeStatusReportSavedForRetry();
                report = new AssetsDeploymentStatusReport();
                report.setPackage(currentPackage);
                report.setStatus(SUCCEEDED);
            } else {

                /* Compare identifiers as strings for simplicity */
                if (!previousStatusReportIdentifier.toString().equals(currentPackageIdentifier.toString())) {
                    mSettingsManager.removeStatusReportSavedForRetry();
                    report = new AssetsDeploymentStatusReport();
                    if (previousStatusReportIdentifier.hasDeploymentKey()) {
                        String previousDeploymentKey = previousStatusReportIdentifier.getDeploymentKey();
                        String previousLabel = previousStatusReportIdentifier.getVersionLabel();
                        report.setPackage(currentPackage);
                        report.setStatus(SUCCEEDED);
                        report.setPreviousDeploymentKey(previousDeploymentKey);
                        report.setPreviousLabelOrAppVersion(previousLabel);
                    } else {

                        /* Previous status report was with a binary app version. */
                        report.setPackage(currentPackage);
                        report.setStatus(SUCCEEDED);
                        report.setPreviousLabelOrAppVersion(previousStatusReportIdentifier.getVersionLabel());
                    }
                }
            }
        }
        return report;
    }

    /**
     * Builds rollback report using current local package information.
     *
     * @param lastFailedPackage current local package information.
     * @return new rollback report.
     */
    public AssetsDeploymentStatusReport buildRollbackReport(AssetsPackage lastFailedPackage) {
        AssetsDeploymentStatusReport report = new AssetsDeploymentStatusReport();
        report.setPackage(lastFailedPackage);
        report.setStatus(FAILED);
        return report;
    }

    /**
     * Saves already sent status report.
     *
     * @param statusReport report to save.
     */
    public void saveReportedStatus(AssetsDeploymentStatusReport statusReport) {

        /* We don't need to record rollback reports, so exit early if that's what was specified. */
        if (statusReport.getStatus() != null && statusReport.getStatus() == FAILED) {
            return;
        }
        if (!isEmpty(statusReport.getAppVersion())) {
            AssetsStatusReportIdentifier statusIdentifier = new AssetsStatusReportIdentifier(statusReport.getAppVersion());
            mSettingsManager.saveIdentifierOfReportedStatus(statusIdentifier);
        } else if (statusReport.getPackage() != null) {
            AssetsStatusReportIdentifier packageIdentifier = buildPackageStatusReportIdentifier(statusReport.getPackage());
            mSettingsManager.saveIdentifierOfReportedStatus(packageIdentifier);
        }
    }

    /**
     * Builds status report identifier using local package.
     *
     * @param updatePackage local package.
     * @return status report identifier.
     */
    private AssetsStatusReportIdentifier buildPackageStatusReportIdentifier(AssetsPackage updatePackage) {

        /* Because deploymentKeys can be dynamically switched, we use a
           combination of the deploymentKey and label as the packageIdentifier. */
        String deploymentKey = updatePackage.getDeploymentKey();
        String label = updatePackage.getLabel();
        if (deploymentKey != null && label != null) {
            return new AssetsStatusReportIdentifier(deploymentKey, label);
        } else {
            return null;
        }
    }

    /**
     * Gets status report already saved for retry it's sending.
     *
     * @return report saved for retry sending.
     * @throws JSONException if there was error of deserialization of report from json document.
     */
    public AssetsDeploymentStatusReport getStatusReportSavedForRetry() throws JSONException {
        AssetsDeploymentStatusReport report = mSettingsManager.getStatusReportSavedForRetry();
        if (report != null) {
            mSettingsManager.removeStatusReportSavedForRetry();
        }
        return report;
    }
}
