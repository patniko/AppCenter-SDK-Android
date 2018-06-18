package com.microsoft.appcenter.assets.core;

import com.microsoft.appcenter.assets.managers.AssetsAcquisitionManager;
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
     * @param assetsBaseCore   instance of {@link AssetsAndroidCore}.
     */
    public static void injectManagersInCore(AssetsUpdateManager assetsUpdateManager, AssetsBaseCore assetsBaseCore) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mUpdateManager").set(assetsManagers, assetsUpdateManager);
        MemberModifier.field(AssetsBaseCore.class, "mManagers").set(assetsBaseCore, assetsManagers);
    }

    /**
     * Injects the provided instance of {@link AssetsUpdateManager} into {@link AssetsAndroidCore}.
     *
     * @param assetsUpdateManager fake assets update manager.
     * @param settingsManager     fake settings manager.
     * @param assetsAndroidCore   instance of {@link AssetsAndroidCore}.
     */
    public static void injectManagersInCore(AssetsUpdateManager assetsUpdateManager, SettingsManager settingsManager, AssetsBaseCore assetsAndroidCore) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mUpdateManager").set(assetsManagers, assetsUpdateManager);
        MemberModifier.field(AssetsManagers.class, "mSettingsManager").set(assetsManagers, settingsManager);
        MemberModifier.field(AssetsBaseCore.class, "mManagers").set(assetsAndroidCore, assetsManagers);
    }

    public static void injectManagersInCore(AssetsAcquisitionManager assetsAcquisitionManager, AssetsBaseCore assetsBaseCore) throws Exception {
        AssetsManagers assetsManagers = mock(AssetsManagers.class);

        MemberModifier.field(AssetsManagers.class, "mAcquisitionManager").set(assetsManagers, assetsAcquisitionManager);
        MemberModifier.field(AssetsBaseCore.class, "mManagers").set(assetsBaseCore, assetsManagers);
    }
}
