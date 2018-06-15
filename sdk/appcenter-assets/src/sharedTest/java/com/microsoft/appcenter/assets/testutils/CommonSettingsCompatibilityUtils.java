package com.microsoft.appcenter.assets.testutils;

import android.content.Context;
import android.content.SharedPreferences;

import com.microsoft.appcenter.assets.AssetsConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utils to test settings manager compatibility with the old version of the sdk.
 * Contains simplified version of the old storing methods.
 */
public class CommonSettingsCompatibilityUtils {

    /**
     * Key for getting/storing pending  failed update.
     */
    private static final String FAILED_UPDATES_KEY = "ASSETS_FAILED_UPDATES";

    /**
     * Package hash key for pending  update.
     */
    private static final String PENDING_UPDATE_HASH_KEY = "hash";

    /**
     * Key for getting/storing pending  update that is loading.
     */
    private static final String PENDING_UPDATE_IS_LOADING_KEY = "isLoading";

    /**
     * Key for getting/storing pending  update.
     */
    private static final String PENDING_UPDATE_KEY = "ASSETS_PENDING_UPDATE";

    /**
     * Saves a failed update using old version of the method.
     *
     * @param failedPackage {@link JSONObject} containing failed package.
     * @param context       application context.
     */
    public static void saveFailedUpdate(JSONObject failedPackage, Context context) throws JSONException {
        SharedPreferences mSettings = context.getSharedPreferences(AssetsConstants.ASSETS_PREFERENCES, 0);
        String failedUpdatesString = mSettings.getString(FAILED_UPDATES_KEY, null);
        JSONArray failedUpdates;
        if (failedUpdatesString == null) {
            failedUpdates = new JSONArray();
        } else {
            failedUpdates = new JSONArray(failedUpdatesString);
        }
        failedUpdates.put(failedPackage);
        mSettings.edit().putString(FAILED_UPDATES_KEY, failedUpdates.toString()).commit();
    }

    /**
     * Saves a pending update using old version of the method.
     *
     * @param packageHash hash of the pending update.
     * @param isLoading   whether this update is loading.
     * @param context     application context.
     */
    public static void savePendingUpdate(String packageHash, boolean isLoading, Context context) throws JSONException {
        SharedPreferences mSettings = context.getSharedPreferences(AssetsConstants.ASSETS_PREFERENCES, 0);
        JSONObject pendingUpdate = new JSONObject();
        pendingUpdate.put(PENDING_UPDATE_HASH_KEY, packageHash);
        pendingUpdate.put(PENDING_UPDATE_IS_LOADING_KEY, isLoading);
        mSettings.edit().putString(PENDING_UPDATE_KEY, pendingUpdate.toString()).commit();
    }

    /**
     * Saves any string under the <code>PENDING_UPDATE_KEY</code>.
     *
     * @param fakeString string to be saved.
     * @param context    application context.
     */
    public static void saveStringToPending(String fakeString, Context context) {
        SharedPreferences mSettings = context.getSharedPreferences(AssetsConstants.ASSETS_PREFERENCES, 0);
        mSettings.edit().putString(PENDING_UPDATE_KEY, fakeString).commit();
    }

    /**
     * Saves any string under the <code>FAILED_UPDATE_KEY</code>.
     *
     * @param fakeString string to be saved.
     * @param context    application context.
     */
    public static void saveStringToFailed(String fakeString, Context context) {
        SharedPreferences mSettings = context.getSharedPreferences(AssetsConstants.ASSETS_PREFERENCES, 0);
        mSettings.edit().putString(FAILED_UPDATES_KEY, fakeString).commit();
    }
}
