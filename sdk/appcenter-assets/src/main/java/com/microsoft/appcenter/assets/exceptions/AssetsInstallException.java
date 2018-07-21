package com.microsoft.appcenter.assets.exceptions;

/**
 * An exception occurred during installing the package.
 */
public class AssetsInstallException extends Exception {

    /**
     * Default error message.
     */
    private static String MESSAGE = "Error occurred during installing the package.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public AssetsInstallException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
