package com.microsoft.appcenter.assets.exceptions;

/**
 * Class to handle exception occurred when obtaining public key.
 */
public class AssetsInvalidPublicKeyException extends Exception {

    /**
     * Creates an instance of {@link AssetsInvalidPublicKeyException} with custom message and cause.
     *
     * @param message custom message.
     * @param cause   exception-cause.
     */
    public AssetsInvalidPublicKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an instance of {@link AssetsInvalidPublicKeyException} with custom message.
     *
     * @param message custom message.
     */
    public AssetsInvalidPublicKeyException(String message) {
        super(message);
    }
}