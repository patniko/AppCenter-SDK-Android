package com.microsoft.appcenter.assets.exceptions;

import com.microsoft.appcenter.assets.core.AssetsBaseCore;

/**
 * Exception class for handling {@link AssetsBaseCore} creating exceptions.
 */
public class AssetsInitializeException extends RuntimeException {

    /**
     * Creates instance of {@link AssetsInitializeException}.
     *
     * @param cause cause of error.
     */
    public AssetsInitializeException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates instance of {@link AssetsInitializeException}.
     *
     * @param detailMessage detailed message.
     * @param cause         cause of error.
     */
    public AssetsInitializeException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
