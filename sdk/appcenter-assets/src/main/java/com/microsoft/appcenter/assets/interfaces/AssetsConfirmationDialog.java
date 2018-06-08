package com.microsoft.appcenter.assets.interfaces;

/**
 * Represents interface for update install confirmation dialog.
 */
public interface AssetsConfirmationDialog {

    /**
     * Proposes user to install update.
     *
     * @param title                        title for dialog.
     * @param message                      message to show.
     * @param acceptText                   text for accept button.
     * @param declineText                  text for decline button.
     * @param assetsConfirmationCallback callback for delivering results of the proposal.
     */
    void shouldInstallUpdate(String title, String message, String acceptText, String declineText, AssetsConfirmationCallback assetsConfirmationCallback);
}
