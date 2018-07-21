package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.exceptions.AssetsInvalidPublicKeyException;

/**
 * Represents interface for provider of public key.
 */
public interface AssetsPublicKeyProvider {

    /**
     * Gets public key.
     *
     * @return public key.
     */
    String getPublicKey() throws AssetsInvalidPublicKeyException;
}
