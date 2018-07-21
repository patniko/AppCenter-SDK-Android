package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsBuilder;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_APP_NAME_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_APP_VERSION_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_PUBLIC_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_SERVER_URL;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.DEPLOYMENT_KEY_KEY;

public class AssetsActivity extends AppCompatActivity {

    private Assets.AssetsDeploymentInstance assets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets);

        String deploymentKey = MainActivity.sSharedPreferences.getString(DEPLOYMENT_KEY_KEY, getString(R.string.deployment_key));
        Assets.getBuilder(deploymentKey).thenAccept(new AppCenterConsumer<AssetsBuilder>() {
            @Override public void accept(AssetsBuilder assetsBuilder) {
                String appName = MainActivity.sSharedPreferences.getString(ASSETS_APP_NAME_KEY, null);
                String appVersion = MainActivity.sSharedPreferences.getString(ASSETS_APP_VERSION_KEY, null);
                String publicKey = MainActivity.sSharedPreferences.getString(ASSETS_PUBLIC_KEY, null);
                String serverUrl = MainActivity.sSharedPreferences.getString(ASSETS_SERVER_URL, null);
                if (appName != null) {
                    assetsBuilder.setAppName(appName);
                }
                if (appVersion != null) {
                    assetsBuilder.setAppVersion(appVersion);
                }
                if (publicKey != null) {
                    assetsBuilder.setPublicKey(publicKey);
                }
                if (serverUrl != null) {
                    assetsBuilder.setServerUrl(serverUrl);
                }
                assets = assetsBuilder.build();
               // assets.addSyncStatusListener(AssetsActivity.this);
            }
        });
    }
}
