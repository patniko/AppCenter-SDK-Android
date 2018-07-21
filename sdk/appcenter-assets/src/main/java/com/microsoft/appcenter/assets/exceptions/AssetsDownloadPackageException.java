package com.microsoft.appcenter.assets.exceptions;

/**
 * An exception occurred during downloading the package.
 */
public class AssetsDownloadPackageException extends AssetsApiHttpRequestException {

    /**
     * The default error message.
     */
    private static String MESSAGE = "Error occurred during package downloading.";

    /**
     * Creates an instance of the exception provided the size mismatch.
     *
     * @param received the number of bytes actually received.
     * @param total    the number of bytes that had to be received.
     */
    public AssetsDownloadPackageException(long received, long total) {
        super(MESSAGE + "Received " + received + " bytes, expected " + total);
    }

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public AssetsDownloadPackageException(Throwable cause) {
        super(MESSAGE, cause);
    }

    /**
     * Creates an instance of the exception with specified download url and cause.
     *
     * @param downloadUrl the url an update was attempted to be downloaded from.
     * @param cause       cause of exception.
     */
    public AssetsDownloadPackageException(String downloadUrl, Throwable cause) {
        super(MESSAGE + " Download url is " + downloadUrl, cause);
    }
}
