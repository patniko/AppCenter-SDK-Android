package com.microsoft.appcenter.assets.exceptions;

/**
 * An exception occurred during merging the contents of the package.
 */
public class AssetsMergeException extends Exception {

    /**
     * Default error message.
     */
    private static String MESSAGE = "Error occurred during package contents merging.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public AssetsMergeException(Throwable cause) {
        super(MESSAGE, cause);
    }

    /**
     * Creates an instance of the exception with specified message.
     *
     * @param message custom message.
     */
    public AssetsMergeException(String message) {
        super(MESSAGE + message);
    }

}
