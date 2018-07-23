package com.microsoft.appcenter.assets.interfaces;

import com.microsoft.appcenter.assets.exceptions.AssetsNativeApiCallException;

/**
 * Interface for providing information about update entry point.
 */
public interface AssetsEntryPointProvider {

    /**
     * Gets location of update entry point.
     *
     * @return location of update entry point.
     */
    String getEntryPoint() throws AssetsNativeApiCallException;
}
