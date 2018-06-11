package com.microsoft.appcenter.assets.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonSyntaxException;
import com.microsoft.appcenter.assets.AssetsConstants;
import com.microsoft.appcenter.assets.AssetsStatusReportIdentifier;
import com.microsoft.appcenter.assets.datacontracts.AssetsDeploymentStatusReport;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPendingUpdate;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.exceptions.AssetsMalformedDataException;
import com.microsoft.appcenter.assets.utils.AssetsUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manager responsible for saving and retrieving settings in local repository.
 */
public class SettingsManager {

    /**
     * Instance of {@link AssetsUtils} to work with.
     */
    private AssetsUtils mAssetsUtils;

    /**
     * Key for getting/storing info about failed Assets updates.
     */
    private final String FAILED_UPDATES_KEY = "ASSETS_FAILED_UPDATES";

    /**
     * Key for getting/storing info about pending update.
     */
    private final String PENDING_UPDATE_KEY = "ASSETS_PENDING_UPDATE";

    /**
     * Key for storing last deployment report identifier.
     */
    private final String LAST_DEPLOYMENT_REPORT_KEY = "ASSETS_LAST_DEPLOYMENT_REPORT";

    /**
     * Key for storing last retry deployment report identifier.
     */
    private final String RETRY_DEPLOYMENT_REPORT_KEY = "ASSETS_RETRY_DEPLOYMENT_REPORT";

    /**
     * Instance of {@link SharedPreferences}.
     */
    private SharedPreferences mSettings;

    /**
     * Creates an instance of {@link SettingsManager} with {@link Context} provided.
     *
     * @param applicationContext current application context.
     * @param assetsUtils      instance of {@link AssetsUtils} to work with.
     */
    public SettingsManager(Context applicationContext, AssetsUtils assetsUtils) {
        mSettings = applicationContext.getSharedPreferences(AssetsConstants.ASSETS_PREFERENCES, 0);
        mAssetsUtils = assetsUtils;
    }

    /**
     * Gets an array with containing failed updates info arranged by time of the failure ascending.
     * Each item represents an instance of {@link AssetsPackage} that has failed to update.
     *
     * @return an array of failed updates.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public ArrayList<AssetsPackage> getFailedUpdates() throws AssetsMalformedDataException {
        String failedUpdatesString = mSettings.getString(FAILED_UPDATES_KEY, null);
        if (failedUpdatesString == null) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(Arrays.asList(mAssetsUtils.convertStringToObject(failedUpdatesString, AssetsPackage[].class)));
        } catch (JsonSyntaxException e) {

            /* Unrecognized data format, clear and replace with expected format. */
            List<AssetsLocalPackage> emptyArray = new ArrayList<>();
            mSettings.edit().putString(FAILED_UPDATES_KEY, mAssetsUtils.convertObjectToJsonString(emptyArray)).apply();
            throw new AssetsMalformedDataException("Unable to parse failed updates metadata " + failedUpdatesString + " stored in SharedPreferences", e);
        }
    }

    /**
     * Gets object with pending update info.
     *
     * @return object with pending update info.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public AssetsPendingUpdate getPendingUpdate() throws AssetsMalformedDataException {
        String pendingUpdateString = mSettings.getString(PENDING_UPDATE_KEY, null);
        if (pendingUpdateString == null) {
            return null;
        }
        try {
            return mAssetsUtils.convertStringToObject(pendingUpdateString, AssetsPendingUpdate.class);
        } catch (JsonSyntaxException e) {
            throw new AssetsMalformedDataException("Unable to parse pending update metadata " + pendingUpdateString + " stored in SharedPreferences", e);
        }
    }

    /**
     * Checks whether an update with the following hash has failed.
     *
     * @param packageHash hash to check.
     * @return <code>true</code> if there is a failed update with provided hash, <code>false</code> otherwise.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public boolean existsFailedUpdate(String packageHash) throws AssetsMalformedDataException {
        List<AssetsPackage> failedUpdates = getFailedUpdates();
        if (packageHash != null) {
            for (AssetsPackage failedPackage : failedUpdates) {
                if (packageHash.equals(failedPackage.getPackageHash())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether there is a pending update with the provided hash.
     * Pass <code>null</code> to check if there is any pending update.
     *
     * @param packageHash expected package hash of the pending update.
     * @return <code>true</code> if there is a pending update with the provided hash.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public boolean isPendingUpdate(String packageHash) throws AssetsMalformedDataException {
        AssetsPendingUpdate pendingUpdate = getPendingUpdate();
        return pendingUpdate != null && !pendingUpdate.isPendingUpdateLoading() &&
                (packageHash == null || pendingUpdate.getPendingUpdateHash().equals(packageHash));
    }

    /**
     * Removes information about failed updates.
     */
    public void removeFailedUpdates() {
        mSettings.edit().remove(FAILED_UPDATES_KEY).apply();
    }

    /**
     * Removes information about the pending update.
     */
    public void removePendingUpdate() {
        mSettings.edit().remove(PENDING_UPDATE_KEY).apply();
    }

    /**
     * Adds another failed update info to the list of failed updates.
     *
     * @param failedPackage instance of failed {@link AssetsRemotePackage}.
     * @throws AssetsMalformedDataException error thrown when actual data is broken (i .e. different from the expected).
     */
    public void saveFailedUpdate(AssetsPackage failedPackage) throws AssetsMalformedDataException {
        ArrayList<AssetsPackage> failedUpdates = getFailedUpdates();
        failedUpdates.add(failedPackage);
        String failedUpdatesString = mAssetsUtils.convertObjectToJsonString(failedUpdates);
        mSettings.edit().putString(FAILED_UPDATES_KEY, failedUpdatesString).apply();
    }

    /**
     * Saves information about the pending update.
     *
     * @param pendingUpdate instance of the {@link AssetsPendingUpdate}.
     */
    public void savePendingUpdate(AssetsPendingUpdate pendingUpdate) {
        mSettings.edit().putString(PENDING_UPDATE_KEY, mAssetsUtils.convertObjectToJsonString(pendingUpdate)).apply();
    }

    /**
     * Gets status report already saved for retry it's sending.
     *
     * @return report saved for retry sending.
     * @throws JSONException if there was error of deserialization of report from json document.
     */
    public AssetsDeploymentStatusReport getStatusReportSavedForRetry() throws JSONException {
        String retryStatusReportString = mSettings.getString(RETRY_DEPLOYMENT_REPORT_KEY, null);
        if (retryStatusReportString != null) {
            JSONObject retryStatusReport = new JSONObject(retryStatusReportString);
            return mAssetsUtils.convertJsonObjectToObject(retryStatusReport, AssetsDeploymentStatusReport.class);
        }
        return null;
    }

    /**
     * Saves status report for further retry os it's sending.
     *
     * @param statusReport status report.
     * @throws JSONException if there was an error during report serialization into json document.
     */
    public void saveStatusReportForRetry(AssetsDeploymentStatusReport statusReport) throws JSONException {
        JSONObject statusReportJSON = mAssetsUtils.convertObjectToJsonObject(statusReport);
        mSettings.edit().putString(RETRY_DEPLOYMENT_REPORT_KEY, statusReportJSON.toString()).apply();
    }

    /**
     * Remove status report that was saved for retry of it's sending.
     */
    public void removeStatusReportSavedForRetry() {
        mSettings.edit().remove(RETRY_DEPLOYMENT_REPORT_KEY).apply();
    }

    /**
     * Gets previously saved status report identifier.
     *
     * @return previously saved status report identifier.
     */
    public AssetsStatusReportIdentifier getPreviousStatusReportIdentifier() {
        String identifierString = mSettings.getString(LAST_DEPLOYMENT_REPORT_KEY, null);
        if (identifierString != null) {
            return AssetsStatusReportIdentifier.fromString(identifierString);
        }
        return null;
    }

    /**
     * Saves identifier of already sent status report.
     *
     * @param identifier identifier of already sent status report.
     */
    public void saveIdentifierOfReportedStatus(AssetsStatusReportIdentifier identifier) {
        mSettings.edit().putString(LAST_DEPLOYMENT_REPORT_KEY, identifier.toString()).apply();
    }

}
