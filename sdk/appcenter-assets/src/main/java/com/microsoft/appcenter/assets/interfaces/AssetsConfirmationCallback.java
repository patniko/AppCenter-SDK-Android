package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.exceptions.AssetsGeneralException;

/**
 * Callback for delivering results of the confirmation callback proposal.
 */
public interface AssetsConfirmationCallback {

    /**
     * Called when user pressed some button.
     *
     * @param accept <code>true</code> if user accepted proposal, <code>false</code> otherwise.
     */
    void onResult(boolean accept);

    /**
     * Called on some error.
     *
     * @param e exception that has occurred.
     */
    void throwError(AssetsGeneralException e);
}
