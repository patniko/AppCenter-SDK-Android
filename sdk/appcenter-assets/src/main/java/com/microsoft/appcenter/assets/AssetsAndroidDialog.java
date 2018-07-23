package com.microsoft.appcenter.assets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.microsoft.appcenter.assets.exceptions.AssetsGeneralException;
import com.microsoft.appcenter.assets.interfaces.AssetsConfirmationCallback;
import com.microsoft.appcenter.assets.interfaces.AssetsConfirmationDialog;

import java.util.Arrays;

/**
 * Represents a react native dialog.
 */
public class AssetsAndroidDialog implements AssetsConfirmationDialog {

    private Context mContext;

    public AssetsAndroidDialog(Context context) {
        mContext = context;
    }

    /**
     * Internal method for actually showing the dialog.
     *
     * @param title              title of the dialog.
     * @param message            message to be displayed.
     * @param positiveButtonText text on the positive action button.
     * @param negativeButtonText test on the negative action button.
     * @param successCallback    callback to handle "OK" events.
     */
    public void showDialog(final String title, final String message, final String positiveButtonText,
                                    final String negativeButtonText, final Callback successCallback, final Callback errorCallback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        successCallback.invoke(true);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        successCallback.invoke(false);
                        break;
                    default:
                        errorCallback.invoke("Unknown button ID pressed.");
                }
            }
        };
        if (title != null) {
            builder.setTitle(title);
        }
        if (message != null) {
            builder.setMessage(message);
        }
        if (positiveButtonText != null) {
            builder.setPositiveButton(positiveButtonText, clickListener);
        }
        if (negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText, clickListener);
        }
        try {
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            errorCallback.invoke(e.getMessage(), Log.getStackTraceString(e));
        }
    }

    @Override
    public void shouldInstallUpdate(String title, String message, String acceptText, String declineText, final AssetsConfirmationCallback assetsConfirmationCallback) {
        final Callback successCallback = new Callback() {
            @Override
            public void invoke(Object... args) {
                boolean result = (boolean)args[0];
                assetsConfirmationCallback.onResult(result);
            }
        };
        final Callback errorCallback = new Callback() {
            @Override
            public void invoke(Object... args) {
                assetsConfirmationCallback.throwError(new AssetsGeneralException("Exception occurred when showing a dialog. Args: " + Arrays.toString(args)));
            }
        };
        showDialog(title, message, acceptText, declineText, successCallback, errorCallback);
    }

    interface Callback {
        void invoke(Object... args);
    }
}
