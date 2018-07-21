package com.microsoft.appcenter.assets.exceptions;

/**
 * An exception occurred during rollback.
 */
public class AssetsRollbackException extends Exception {

    /**
     * Default error message.
     */
    private static String MESSAGE = "Error occurred during the rollback.";

    /**
     * Creates an instance of the exception with default detail message and specified cause.
     *
     * @param cause cause of exception.
     */
    public AssetsRollbackException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
