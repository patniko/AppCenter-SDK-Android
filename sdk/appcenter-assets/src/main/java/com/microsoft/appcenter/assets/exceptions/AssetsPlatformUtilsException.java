package com.microsoft.appcenter.assets.exceptions;

import com.microsoft.appcenter.assets.interfaces.AssetsPlatformUtils;

/**
 * Exception class for handling {@link AssetsPlatformUtils} exceptions.
 */
public class AssetsPlatformUtilsException extends Exception {

    /**
     * Creates instance of {@link AssetsPlatformUtilsException}.
     *
     * @param detailMessage detailed message.
     * @param cause         cause of error.
     */
    public AssetsPlatformUtilsException(String detailMessage, Throwable cause) {
        super(detailMessage, cause);
    }
}
