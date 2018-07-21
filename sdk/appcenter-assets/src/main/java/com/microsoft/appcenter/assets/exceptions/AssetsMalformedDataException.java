package com.microsoft.appcenter.assets.exceptions;

/**
 * Exception class for throwing malformed Assets data exceptions.
 * Malformed data could be json blob of Assets update manifest and other json blobs
 * saved locally, received from server and etc.
 */
public class AssetsMalformedDataException extends Exception {

    /**
     * Creates instance of the malformed Assets data exception using
     * <code>path</code> and <code>message</code> arguments.
     *
     * @param message the message with explanation of error.
     * @param cause   the cause why Assets data cannot be parsed.
     */
    public AssetsMalformedDataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates instance of the malformed Assets data exception using
     * <code>message</code> argument.
     *
     * @param message the message with explanation of error.
     */
    public AssetsMalformedDataException(String message) {
        super(message);
    }
}
