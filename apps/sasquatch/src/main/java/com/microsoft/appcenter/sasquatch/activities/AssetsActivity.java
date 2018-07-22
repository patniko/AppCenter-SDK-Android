package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsBuilder;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.Locale;
import java.util.Map;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_APP_NAME_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_APP_VERSION_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_PUBLIC_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_SERVER_URL;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.DEPLOYMENT_KEY_KEY;

public class AssetsActivity extends AppCompatActivity {

    private Assets.AssetsDeploymentInstance assets;

    private TextView mPackageInfoView;
    private LinearLayout mProgressLbl;
    private Button mCheckForUpdateBtn;
    private Button mInstallBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets);

        mPackageInfoView = findViewById(R.id.package_info);
        updateInfoView(true, null);

        mCheckForUpdateBtn = findViewById(R.id.check_for_update_btn);
        mInstallBtn = findViewById(R.id.install_btn);
        mProgressLbl = findViewById(R.id.progress_lbl);

        mCheckForUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                checkForUpdate();
            }
        });
        mInstallBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Intent intent = new Intent(AssetsActivity.this, AssetsActivitySync.class);
                startActivity(intent);
            }
        });

        setupAssets();
    }

    private void setupAssets() {
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
                assets.getUpdateMetadata().thenAccept(new AppCenterConsumer<AssetsLocalPackage>() {
                    @Override public void accept(AssetsLocalPackage assetsLocalPackage) {
                        updateInfoView(true, assetsLocalPackage);
                    }
                });
                // assets.addSyncStatusListener(AssetsActivity.this);
            }
        });
    }

    private void updateInfoView(final boolean isCurrent, final AssetsPackage packageInfo) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                mPackageInfoView.setText(constructPackageInfoString(isCurrent, packageInfo));
                if (packageInfo != null) {
                    mInstallBtn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private Spanned constructPackageInfoString(boolean isCurrent, AssetsPackage packageInfo) {
        String text = getString(isCurrent ? R.string.package_info_format : R.string.rm_package_info_format);
        int highlightColorRes = ContextCompat.getColor(getApplicationContext(), android.R.color.black);
        String highlightColorString = "#" + Integer.toHexString(highlightColorRes).substring(2);
        String packageString = isCurrent ? getString(R.string.assets_no_package) : getString(R.string.assets_no_rm_package);
        if (packageInfo != null) {
            Gson gson = new Gson();
            JsonElement json = gson.toJsonTree(packageInfo);
            StringBuilder packageStringBuilder = new StringBuilder();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                packageStringBuilder.append("<br>").append("<b>").append(entry.getKey()).append("</b> : ").append(entry.getValue());
            }
            packageString = packageStringBuilder.toString();
        }
        String html = String.format(Locale.getDefault(), text, "<b><font color=" + highlightColorString + ">", "</b></font><br>", packageString.toString());
        return Html.fromHtml(html);
    }

    public void checkForUpdate() {
        mProgressLbl.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override public void run() {
                assets.checkForUpdate().thenAccept(new AppCenterConsumer<AssetsRemotePackage>() {
                    @Override public void accept(AssetsRemotePackage assetsRemotePackage) {
                        mProgressLbl.setVisibility(View.GONE);
                        updateInfoView(false, assetsRemotePackage);
                    }
                });
            }
        }).start();
    }
}
