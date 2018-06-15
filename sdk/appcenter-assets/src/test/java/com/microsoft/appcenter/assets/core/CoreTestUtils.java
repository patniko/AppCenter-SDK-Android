package com.microsoft.appcenter.assets.core;

import com.microsoft.appcenter.assets.managers.AssetsUpdateManager;
import com.microsoft.appcenter.assets.managers.SettingsManager;

import org.powermock.api.support.membermodification.MemberModifier;

import static org.mockito.Mockito.mock;

/**
 * Contains util methods for testing {@link AssetsAndroidCore}.
 */
public class CoreTestUtils {

    /**
     * Injects the provided instance of {@link AssetsUpdateManager} into {@link AssetsAndroidCore}.
     *
     * @param assetsUpdateManager fake assets update manager.
     * @param assetsAndroidCore   instance of {@link AssetsAndroidCore}.
     */
    public static void injectManagersInCore(AssetsUpdateManager assetsUpdateManager, AssetsAndroidCore assetsAndroidCore) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mUpdateManager").set(assetsManagers, assetsUpdateManager);
        MemberModifier.field(AssetsAndroidCore.class, "mManagers").set(assetsAndroidCore, assetsManagers);
    }

    /**
     * Injects the provided instance of {@link AssetsUpdateManager} into {@link AssetsAndroidCore}.
     *
     * @param assetsUpdateManager fake assets update manager.
     * @param settingsManager     fake settings manager.
     * @param assetsAndroidCore   instance of {@link AssetsAndroidCore}.
     */
    public static void injectManagersInCore(AssetsUpdateManager assetsUpdateManager, SettingsManager settingsManager, AssetsAndroidCore assetsAndroidCore) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mUpdateManager").set(assetsManagers, assetsUpdateManager);
        MemberModifier.field(AssetsManagers.class, "mSettingsManager").set(assetsManagers, settingsManager);
        MemberModifier.field(AssetsAndroidCore.class, "mManagers").set(assetsAndroidCore, assetsManagers);
    }
}
