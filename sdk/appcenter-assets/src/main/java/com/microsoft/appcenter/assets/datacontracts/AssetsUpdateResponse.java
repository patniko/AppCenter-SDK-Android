package com.microsoft.appcenter.assets.datacontracts;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.assets.exceptions.AssetsIllegalArgumentException;

/**
 * A response class containing info about the update.
 */
public class AssetsUpdateResponse {

    /**
     * Information about the existing update.
     */
    @SerializedName("updateInfo")
    private AssetsUpdateResponseUpdateInfo updateInfo;

    /**
     * Gets the information about the existing update and returns it.
     *
     * @return information about the existing update.
     */
    public AssetsUpdateResponseUpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    /**
     * Sets the information about the existing update.
     *
     * @param updateInfo information about the existing update.
     */
    public void setUpdateInfo(AssetsUpdateResponseUpdateInfo updateInfo) throws AssetsIllegalArgumentException {
        if (updateInfo != null) {
            this.updateInfo = updateInfo;
        } else {
            throw new AssetsIllegalArgumentException(this.getClass().getName(), "updateInfo");
        }
    }
}
