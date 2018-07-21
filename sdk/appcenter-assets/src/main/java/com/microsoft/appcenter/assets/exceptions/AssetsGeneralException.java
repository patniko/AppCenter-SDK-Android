package com.microsoft.appcenter.assets.exceptions;

/**
 * General code push exception that has no specified sub type or occasion.
 */
public class AssetsGeneralException extends Exception {

    /**
     * Creates an instance of {@link AssetsGeneralException} with custom message and exception provided.
     *
     * @param detailMessage custom message.
     * @param throwable     cause of the exception.
     */
    public AssetsGeneralException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Creates an instance of {@link AssetsGeneralException} with custom message provided.
     *
     * @param detailMessage custom message.
     */
    public AssetsGeneralException(String detailMessage) {
        super(detailMessage);
    }
}
