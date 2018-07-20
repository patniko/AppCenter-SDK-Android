package com.microsoft.appcenter.analytics;

import android.support.annotation.NonNull;

import java.util.Map;

/**
 * Target for advanced transmission target usage.
 */
public class AnalyticsTransmissionTarget {

    private String mTransmissionTargetToken;

    /**
     * Common event properties for this target. Inherited by children.
     */
    private final Map<String, String> mEventProperties = new HashMap<>();

    /**
     * Create a new instance.
     *
     * @param transmissionTargetToken The token for this transmission target.
     */
    AnalyticsTransmissionTarget(@NonNull String transmissionTargetToken) {
        mTransmissionTargetToken = transmissionTargetToken;
    }

    /**
     * Track a custom event with name.
     *
     * @param name An event name.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public void trackEvent(String name) {
        trackEvent(name, null);
    }

    /**
     * Track a custom event with name and optional properties.
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    @SuppressWarnings("WeakerAccess")
    public void trackEvent(String name, Map<String, String> properties) {

        /* Merge common properties. More specific target wins conflicts. */
        Map<String, String> mergedProperties = new HashMap<>();
        for (AnalyticsTransmissionTarget target = this; target != null; target = target.mParentTarget) {
            target.mergeEventProperties(mergedProperties);
        }

        /* Override with parameter. */
        if (properties != null) {
            mergedProperties.putAll(properties);
        }

        /*
         * If we passed null as parameter and no common properties set,
         * keep null for consistency with Analytics class regarding null vs empty.
         */
        else if (mergedProperties.isEmpty()) {
            mergedProperties = null;
        }

        /* Track event with merged properties. */
        Analytics.trackEvent(name, mergedProperties, this);
    }

    /**
     * Extracted method to synchronize on each level at once while reading properties.
     * Nesting synchronize between parent/child could lead to deadlocks.
     */
    private synchronized void mergeEventProperties(Map<String, String> mergedProperties) {
        for (Map.Entry<String, String> property : mEventProperties.entrySet()) {
            String key = property.getKey();
            if (!mergedProperties.containsKey(key)) {
                mergedProperties.put(key, property.getValue());
            }
        }
    }

    /**
     * Add or overwrite the given key for the common event properties. Properties will be inherited
     * by children of this transmission target.
     *
     * @param key   The property key.
     * @param value The property value.
     */
    @SuppressWarnings("WeakerAccess")
    public synchronized void setEventProperty(String key, String value) {
        mEventProperties.put(key, value);
    }

    /**
     * Removes the given key from the common event properties.
     *
     * @param key The property key to be removed.
     */
    @SuppressWarnings("WeakerAccess")
    public synchronized void removeEventProperty(String key) {
        mEventProperties.remove(key);
    }

    /**
     * Getter for transmission target token.
     *
     * @return the transmission target token.
     */
    String getTransmissionTargetToken() {
        return mTransmissionTargetToken;
    }

}
