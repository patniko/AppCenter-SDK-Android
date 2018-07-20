package com.microsoft.appcenter.assets.exceptions;

import com.microsoft.appcenter.assets.core.AssetsBaseCore;

/**
 * Class for all exceptions that is coming from {@link AssetsBaseCore} public methods.
 */
public class AssetsNativeApiCallException extends Exception {

    /**
     * Creates instance of {@link AssetsNativeApiCallException}.
     *
     * @param detailMessage detailed message.
     */
    public AssetsNativeApiCallException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Creates instance of {@link AssetsNativeApiCallException}.
     *
     * @param cause cause of error.
     */
    public AssetsNativeApiCallException(Throwable cause) {
        super(cause);
    }


    /**
     * Creates instance of {@link AssetsNativeApiCallException}.
     *
     * @param detailMessage detailed message.
     * @param cause         cause of error.
     */
    public AssetsNativeApiCallException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
