package com.microsoft.appcenter.assets.exceptions;

/**
 * An exception occurred during making HTTP request to CodePush server.
 */
public class AssetsApiHttpRequestException extends Exception {

    /**
     * Creates instance of {@link AssetsApiHttpRequestException}.
     *
     * @param throwable the cause why request failed.
     */
    public AssetsApiHttpRequestException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Creates instance of {@link AssetsApiHttpRequestException}.
     *
     * @param detailMessage the detailed message of why request failed.
     */
    public AssetsApiHttpRequestException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Creates instance of {@link AssetsApiHttpRequestException}.
     *
     * @param detailMessage the cause why request failed.
     * @param throwable     the detailed message of why request failed.
     */
    public AssetsApiHttpRequestException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
