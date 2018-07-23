package com.microsoft.appcenter.sasquatch.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsBuilder;
import com.microsoft.appcenter.assets.datacontracts.AssetsLocalPackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsSyncOptions;
import com.microsoft.appcenter.assets.datacontracts.AssetsUpdateDialog;
import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;
import com.microsoft.appcenter.assets.interfaces.AssetsDownloadProgressListener;
import com.microsoft.appcenter.assets.interfaces.AssetsSyncStatusListener;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.io.File;
import java.util.Locale;

import static android.view.View.GONE;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_APP_NAME_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_APP_VERSION_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_ENTRY_FILE;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_PUBLIC_KEY;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.ASSETS_SERVER_URL;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.DEPLOYMENT_KEY_KEY;

public class AssetsActivitySync extends AppCompatActivity implements AssetsSyncStatusListener, AssetsDownloadProgressListener {

    private Assets.AssetsDeploymentInstance assets;

    private TextView mDownloadProgressLbl;
    private TextView mSyncStatusLbl;
    private TextView mNoContentLbl;
    private Button mSyncBtn;
    private Button mClearUpdatesBtn;
    private ImageView mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assets_sync);

        mDownloadProgressLbl = findViewById(R.id.download_progress_lbl);
        mSyncStatusLbl = findViewById(R.id.sync_status_lbl);
        mNoContentLbl = findViewById(R.id.no_content_lbl);
        mSyncBtn = findViewById(R.id.sync_btn);
        mClearUpdatesBtn = findViewById(R.id.clear_updates_btn);
        mContentView = findViewById(R.id.content_view);

        mSyncBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                sync();
            }
        });

        mClearUpdatesBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                assets.clearUpdates();
                updateCurrentPackageInfo();
            }
        });

        setupAssets();
    }

    private void setupAssets() {
        String deploymentKey = MainActivity.sSharedPreferences.getString(DEPLOYMENT_KEY_KEY, getString(R.string.deployment_key));
        Assets.getBuilder(deploymentKey, this).thenAccept(new AppCenterConsumer<AssetsBuilder>() {
            @Override public void accept(AssetsBuilder assetsBuilder) {
                String appName = MainActivity.sSharedPreferences.getString(ASSETS_APP_NAME_KEY, null);
                String appVersion = MainActivity.sSharedPreferences.getString(ASSETS_APP_VERSION_KEY, null);
                String publicKey = MainActivity.sSharedPreferences.getString(ASSETS_PUBLIC_KEY, null);
                String serverUrl = MainActivity.sSharedPreferences.getString(ASSETS_SERVER_URL, null);
                String entryFile = MainActivity.sSharedPreferences.getString(ASSETS_ENTRY_FILE, getString(R.string.assets_entry_file));
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
                if (entryFile != null) {
                    assetsBuilder.setUpdateSubFolder(entryFile);
                }
                assets = assetsBuilder.build();
                updateCurrentPackageInfo();
                assets.addSyncStatusListener(AssetsActivitySync.this);
                assets.addDownloadProgressListener(AssetsActivitySync.this);
            }
        });
    }

    public void sync() {
        new Thread(new Runnable() {
            @Override public void run() {
                AssetsSyncOptions assetsSyncOptions = new AssetsSyncOptions();
                assetsSyncOptions.setUpdateDialog(new AssetsUpdateDialog());
                assets.sync(assetsSyncOptions);
            }
        }).start();
    }

    @Override public void syncStatusChanged(final AssetsSyncStatus syncStatus) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                String syncStatusText = "";
                switch (syncStatus) {
                    case UP_TO_DATE:
                        syncStatusText = getString(R.string.assets_sync_up_to_date);
                        break;
                    case UNKNOWN_ERROR:
                        syncStatusText = getString(R.string.assets_sync_err);
                        break;
                    case UPDATE_IGNORED:
                        syncStatusText = getString(R.string.assets_sync_ignored);
                        break;
                    case SYNC_IN_PROGRESS:
                        syncStatusText = getString(R.string.assets_sync_progress);
                        break;
                    case UPDATE_INSTALLED: {
                        syncStatusText = getString(R.string.assets_sync_installed);
                        updateCurrentPackageInfo();
                        assets.notifyApplicationReady();
                    }
                    break;
                    case INSTALLING_UPDATE:
                        syncStatusText = getString(R.string.assets_sync_installing);
                        break;
                    case CHECKING_FOR_UPDATE:
                        syncStatusText = getString(R.string.assets_sync_checking);
                        break;
                    case AWAITING_USER_ACTION:
                        syncStatusText = getString(R.string.assets_sync_awaiting);
                        break;
                    case DOWNLOADING_PACKAGE:
                        syncStatusText = getString(R.string.assets_sync_downloading);
                        break;
                    default:
                        break;
                }
                mSyncStatusLbl.setText(syncStatusText);
            }
        });
    }

    @Override public void downloadProgressChanged(final long receivedBytes, final long totalBytes) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                String downloadProgressText = "";
                int percentage = (int) (((receivedBytes * 1.0) / totalBytes) * 100);
                if (percentage == 100) {
                    downloadProgressText = "Package downloaded";
                } else {
                    downloadProgressText = String.format(Locale.getDefault(), "%d", percentage);
                }
                mDownloadProgressLbl.setText(downloadProgressText);
            }
        });
    }

    private void updateCurrentPackageInfo() {
        assets.getUpdateMetadata().thenAccept(new AppCenterConsumer<AssetsLocalPackage>() {
            @Override public void accept(AssetsLocalPackage assetsLocalPackage) {
                //TODO: display path
                boolean imageExists = true;
                mContentView.setVisibility(assetsLocalPackage == null ? GONE : View.VISIBLE);
                mNoContentLbl.setVisibility(assetsLocalPackage == null ? View.VISIBLE : GONE);
                if (assetsLocalPackage != null) {
                    String entry = assets.getCurrentUpdateEntryPoint();
                    if (entry == null) {
                        imageExists = false;
                    } else {
                        File entryFile = new File(entry);
                        if (entryFile.exists()) {
                            Drawable drawable = Drawable.createFromPath(entry);
                            if (drawable == null) {
                                imageExists = false;
                            } else {
                                mContentView.setImageDrawable(Drawable.createFromPath(entry));
                            }
                        } else {
                            imageExists = false;
                        }
                    }
                    if (!imageExists) {
                        mContentView.setVisibility(GONE);
                        mNoContentLbl.setVisibility(View.VISIBLE);
                        mNoContentLbl.setText(String.format(Locale.getDefault(), getString(R.string.assets_no_image_format), MainActivity.sSharedPreferences.getString(ASSETS_ENTRY_FILE, getString(R.string.assets_entry_file))));
                    }
                } else {
                    mNoContentLbl.setText(R.string.assets_no_update_content);
                }
            }
        });
    }
}
