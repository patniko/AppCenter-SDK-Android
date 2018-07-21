package com.microsoft.appcenter.assets;

import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;
import com.microsoft.appcenter.assets.interfaces.AssetsEntryPointProvider;

/**
 * Android-specific implementation of {@link AssetsEntryPointProvider}.
 */
public class AssetsAndroidEntryPointProvider implements AssetsEntryPointProvider {

    /**
     * Path to the update entry point.
     */
    private String mEntryPoint;

    /**
     * Creates an instance of {@link AssetsAndroidEntryPointProvider}.
     *
     * @param entryPoint path to the update entry point.
     */
    public AssetsAndroidEntryPointProvider(String entryPoint) {
        mEntryPoint = entryPoint;
    }

    @Override
    public String getEntryPoint() throws AssetsNativeApiCallException {
        if (mEntryPoint != null) {
            return mEntryPoint;
        }
        return "";
    }
}
