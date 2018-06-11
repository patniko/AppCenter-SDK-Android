/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package demo.tensorflow.org.customvision_sample;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.assets.Assets;
import com.microsoft.appcenter.assets.AssetsBuilder;
import com.microsoft.appcenter.assets.datacontracts.AssetsRemotePackage;
import com.microsoft.appcenter.assets.datacontracts.AssetsSyncOptions;
import com.microsoft.appcenter.assets.enums.AssetsInstallMode;
import com.microsoft.appcenter.assets.enums.AssetsSyncStatus;
import com.microsoft.appcenter.assets.interfaces.AssetsSyncStatusListener;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import demo.tensorflow.org.customvision_sample.OverlayView.DrawCallback;
import demo.tensorflow.org.customvision_sample.connection.CustomVisionService;
import demo.tensorflow.org.customvision_sample.connection.VstsService;
import demo.tensorflow.org.customvision_sample.connection.model.Tag;
import demo.tensorflow.org.customvision_sample.connection.model.VstsBuild;
import demo.tensorflow.org.customvision_sample.env.BorderedText;
import demo.tensorflow.org.customvision_sample.env.Logger;
import demo.tensorflow.org.customvision_sample.listener.BuildStatusListener;
import demo.tensorflow.org.customvision_sample.listener.ImageDeleteCallback;
import demo.tensorflow.org.customvision_sample.listener.ImageUploadCallback;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener, ImageUploadCallback, ImageDeleteCallback, BuildStatusListener, AssetsSyncStatusListener {

    private static final Logger LOGGER = new Logger();

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;
    private MSCognitiveServicesClassifier classifier;
    private CustomVisionService customVisionService;
    private VstsService vstsService;
    private BorderedText borderedText;

    private ImageButton addButton;
    private ImageButton deleteButton;
    private ImageButton kickBuildButton;
    private ImageButton syncButton;
    private ProgressBar progressBar;
    private ProgressBar progressBar2;
    private ProgressBar progressBar3;
    private ProgressDialog progressDialog;

    private Timer timer;

    private Tag[] mTags;
    private String mLabelId;
    private String mLabel;

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        initViews();
        setListeners();
        startServices();
    }

    private void setListeners() {
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialogInterface) {
                if (timer != null) {
                    timer.cancel();
                }
                mTrainingInProgress = false;
                restart();
            }
        });

        kickBuildButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onKick();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onAdd();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onDelete();
            }
        });

        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onSync();
            }
        });
    }

    private void initViews() {
        progressBar2 = findViewById(R.id.progress_bar2);
        progressBar3 = findViewById(R.id.progress_bar3);
        addButton = findViewById(R.id.add_button);
        deleteButton = findViewById(R.id.delete_button);
        kickBuildButton = findViewById(R.id.kick_button);
        syncButton = findViewById(R.id.sync_button);
        progressBar = findViewById(R.id.progress_bar);
        progressDialog = new ProgressDialog(this);
        progressDialog.create();
    }

    private void startServices() {
        customVisionService = new CustomVisionService(getString(R.string.CUSTOM_VISION_TRAINING_KEY), getString(R.string.CUSTOM_VISION_PROJECT_ID), getApplicationContext());
        vstsService = new VstsService(getString(R.string.VSTS_PAT), getString(R.string.VSTS_PROJECT_NAME));

        AppCenter.start(getApplication(), "", Assets.class);

        Assets.getBuilder(getString(R.string.CODE_PUSH_DEPLOYMENT_KEY)).thenAccept(new AppCenterConsumer<AssetsBuilder>() {
            @Override public void accept(AssetsBuilder assetsBuilder) {
                assets = assetsBuilder.setUpdateSubFolder("assets").build();
                assets.addSyncStatusListener(ClassifierActivity.this);
                sync();
                checkAssets();
            }
        });
        getTagsInBackground();
    }

    private void getTagsInBackground() {
        new Thread(new Runnable() {
            @Override public void run() {
                mTags = customVisionService.getTags();
            }
        }).start();
    }

    private void checkAssets() {
        try {
            if (assets.getCurrentUpdateEntryPoint() != null) {
                String assetsModelPath = assets.getCurrentUpdateEntryPoint()
                        + File.separator
                        + getString(R.string.MODEL_FILE);
                String assetsLabelPath = assets.getCurrentUpdateEntryPoint()
                        + File.separator
                        + getString(R.string.LABELS_FILE);
                if (new File(assetsModelPath).exists()) {
                    MSCognitiveServicesClassifier.setModelFile(assetsModelPath);
                }
                if (new File(assetsLabelPath).exists()) {
                    MSCognitiveServicesClassifier.setLabelFile(assetsLabelPath);
                }
            }
        } catch (Exception e) {
            LOGGER.e("MODELS", e.getMessage());
        }
        boolean noModel = MSCognitiveServicesClassifier.checkModel(getAssets());
        boolean noLabel = MSCognitiveServicesClassifier.checkLabel(getAssets());
        noModelOrLabel = noModel || noLabel;
        TextView warningLabel = findViewById(R.id.no_model_label);
        warningLabel.setVisibility(noModelOrLabel ? View.VISIBLE : View.GONE);
        if (noLabel) {
            warningLabel.setText(R.string.no_label_file);
        }
        if (noModel) {
            warningLabel.setText(R.string.no_model_file);
        }
        if (noModel && noLabel) {
            warningLabel.setText(R.string.no_model_label_file);
        }
    }

    private void sync() {
        new Thread(new Runnable() {
            @Override public void run() {
                assets.checkForUpdate().thenAccept(new AppCenterConsumer<AssetsRemotePackage>() {
                    @Override public void accept(AssetsRemotePackage AssetsRemotePackage) {
                        if (AssetsRemotePackage != null) {
                            updateSyncButton(true);
                        }
                    }
                });
            }
        }).start();
    }

    private void updateSyncButton(final boolean updateAvailable) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Drawable drawable = getApplicationContext().getResources().getDrawable(updateAvailable ? R.drawable.arrow_down_white : R.drawable.sync_white);
                syncButton.setImageDrawable(drawable);
            }
        });
    }

    public void onSync() {
        if (assets != null) {
            syncInProgress = true;
            final AssetsSyncOptions assetsSyncOptions = new AssetsSyncOptions();
            assetsSyncOptions.setInstallMode(AssetsInstallMode.IMMEDIATE);
            runInBackground(new Runnable() {
                @Override public void run() {
                    assets.sync(assetsSyncOptions);
                }
            });
        }
    }

    public void syncStatusChanged(final AssetsSyncStatus syncStatus) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                switch (syncStatus) {
                    case INSTALLING_UPDATE:
                    case CHECKING_FOR_UPDATE:
                    case DOWNLOADING_PACKAGE:
                    case SYNC_IN_PROGRESS: {
                        syncButton.setVisibility(View.GONE);
                        progressBar.animate();
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    break;
                    case UNKNOWN_ERROR: {
                        syncButton.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        Drawable drawable = getApplicationContext().getResources().getDrawable(R.drawable.close_red);
                        syncButton.setImageDrawable(drawable);
                        Handler uiHandler = new Handler(Looper.getMainLooper());
                        uiHandler.postDelayed(new Runnable() {
                            @Override public void run() {
                                syncInProgress = false;
                                updateSyncButton(false);
                                restart();
                            }
                        }, 1000);
                    }
                    break;
                    default: {
                        syncButton.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        checkAssets();
                        syncInProgress = false;
                        updateSyncButton(false);
                        restart();
                        break;
                    }
                    case UPDATE_INSTALLED:
                        syncButton.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        checkAssets();
                        Drawable drawable = getApplicationContext().getResources().getDrawable(R.drawable.check_green);
                        syncButton.setImageDrawable(drawable);
                        Handler uiHandler = new Handler(Looper.getMainLooper());
                        uiHandler.postDelayed(new Runnable() {
                            @Override public void run() {
                                syncInProgress = false;
                                updateSyncButton(false);
                                restart();
                            }
                        }, 1000);
                        android.app.AlertDialog.Builder dlgAlert = new android.app.AlertDialog.Builder(ClassifierActivity.this);
                        dlgAlert.setMessage("New model has been successfully installed");
                        dlgAlert.setPositiveButton("OK", null);
                        dlgAlert.setCancelable(true);
                        dlgAlert.create().show();
                }
            }
        });
    }

    private void onKick() {
        mTrainingInProgress = true;
        progressDialog.show();
        progressDialog.setMessage(getString(R.string.build_status_start));
        runInBackground(new Runnable() {
            @Override public void run() {
                final String buildId = vstsService.kickBuild(getString(R.string.VSTS_BUILD_DEFINITION_ID));
                if (buildId.equals("203")) {
                    showUnauthorized();
                    progressDialog.dismiss();
                } else if (buildId.equals("404")) {
                    showError(String.format(Locale.getDefault(), getString(R.string.error_code_404_vsts), getString(R.string.VSTS_BUILD_DEFINITION_ID), getString(R.string.VSTS_PROJECT_NAME)));
                    progressDialog.dismiss();
                } else if (buildId.length() == 0) {
                    showError(getString(R.string.error_code));
                    progressDialog.dismiss();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            postponeCheckBuild(buildId);
                        }
                    });
                }
            }
        });
    }

    @SuppressLint("InflateParams")
    private void onAdd() {
        final View dialogView = getLayoutInflater().inflate(R.layout.edit_text, null);
        final EditText editText = dialogView.findViewById(R.id.edit_text);
        final AlertDialog editDialog = new AlertDialog.Builder(ClassifierActivity.this)
                .setTitle(R.string.enter_label_msg)
                .setView(dialogView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        mTrainingInProgress = true;
                        mLabel = editText.getText().toString();
                        mLabelId = tryFindTagWithName(mLabel);
                        addPerson();
                    }
                })
                .create();
        new AlertDialog.Builder(ClassifierActivity.this)
                .setTitle(R.string.select_label)
                .setItems(getChoices(true), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, final int i) {
                        if (i == 0) {
                            editDialog.show();
                        } else {
                            mTrainingInProgress = true;
                            mLabelId = getTagId(i - 1);
                            mLabel = getTag(i - 1);
                            addPerson();
                        }
                    }
                })
                .show();
    }

    private void onDelete() {
        new AlertDialog.Builder(ClassifierActivity.this)
                .setTitle(getString(R.string.select_label))
                .setItems(getChoices(false), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, final int i) {
                        dialogInterface.dismiss();
                        final String tagId = getTagId(i);
                        deleteButton.setVisibility(View.GONE);
                        progressBar3.setVisibility(View.VISIBLE);
                        progressBar3.animate();
                        runInBackground(new Runnable() {
                            @Override public void run() {
                                customVisionService.deleteTagAndItsImages(tagId, ClassifierActivity.this);
                                getTagsInBackground();
                            }
                        });
                    }
                })
                .show();
    }

    private String[] getChoices(boolean withAddChoice) {
        int length = mTags == null ? withAddChoice ? 1 : 0 : withAddChoice ? mTags.length + 1 : mTags.length;
        String[] choices = new String[length];
        if (withAddChoice) {
            choices[0] = getString(R.string.add_new_label_msg);
        }
        int i = withAddChoice ? 1 : 0;
        if (mTags != null) {
            for (Tag tag : mTags) {
                choices[i] = tag.getName();
                i++;
            }
        }

        return choices;
    }

    private String getTagId(int idx) {
        if (idx >= 0 && mTags != null) {
            return mTags[idx].getId();
        } else {
            return "";
        }
    }

    private String tryFindTagWithName(String name) {
        if (mTags != null) {
            for (Tag tag : mTags) {
                if (tag.getName().equals(name)) {
                    return tag.getId();
                }
            }
        }
        return "";
    }

    private String getTag(int idx) {
        if (idx >= 0 && mTags != null) {
            return mTags[idx].getName();
        } else {
            return "";
        }
    }

    private void addPerson() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(intent, 123);
    }

    @Override public void onUploadSuccess() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                showCompletion(addButton, progressBar2, R.drawable.check_green, android.R.color.holo_green_dark);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ClassifierActivity.this);
                dialogBuilder.setMessage(R.string.upload_success);
                dialogBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                onKick();
                            }
                        });
                    }
                });
                dialogBuilder.setNegativeButton("NO", null);
                AlertDialog dialog = dialogBuilder.create();
                dialog.show();
            }
        });
    }

    @Override public void onUploadFailure(final int responseCode, final boolean duplicates) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                showCompletion(addButton, progressBar2, R.drawable.close_red, android.R.color.holo_red_dark);
                if (responseCode == 401) {
                    showUnauthorized();
                } else if (responseCode == 400) {
                    showBadRequest();
                } else if (duplicates) {
                    showError(getString(R.string.error_code_duplicate));
                }
            }
        });
    }

    @Override public void onImagesDeleted() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                showCompletion(deleteButton, progressBar3, R.drawable.check_green, android.R.color.holo_green_dark);
            }
        });
    }

    @Override public void onDeleteFailure(final int responseCode) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                showCompletion(deleteButton, progressBar3, R.drawable.close_red, android.R.color.holo_red_dark);
                if (responseCode == 401) {
                    showUnauthorized();
                } else if (responseCode == 400) {
                    showBadRequest();
                }
            }
        });
    }

    private void showUnauthorized() {
        showError(getString(R.string.error_code_401));
    }

    private void showBadRequest() {
        showError(getString(R.string.error_code_400));
    }

    private void showError(String message) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ClassifierActivity.this);
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void showCompletion(final ImageButton button, final ProgressBar progressToHide, final int completionDrawable, final int color) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Drawable drawable = getApplicationContext().getResources().getDrawable(completionDrawable);
                button.setVisibility(View.VISIBLE);
                progressToHide.setVisibility(View.GONE);
                final Drawable originalDrawable = button.getDrawable();
                drawable.setTint(ContextCompat.getColor(getApplicationContext(), color));
                button.setImageDrawable(drawable);
                Handler uiHandler = new Handler(Looper.getMainLooper());
                uiHandler.postDelayed(new Runnable() {
                    @Override public void run() {
                        button.setImageDrawable(originalDrawable);
                        mTrainingInProgress = false;
                        restart();
                    }
                }, 1000);
            }
        });
    }

    @Override public void onBuildStatusChanged(final VstsBuild build) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (progressDialog != null) {
                    String resultString = "";
                    if (build.getResult() != null) {
                        switch (build.getResult()) {
                            case failed:
                                resultString = getString(R.string.build_result_failed);
                                break;
                            case cancelled:
                                resultString = getString(R.string.build_result_cancelled);
                                break;
                            case succeeded:
                                resultString = getString(R.string.build_result_succeeded);
                                break;
                            case partiallySucceeded:
                                resultString = getString(R.string.build_result_partially);
                                break;
                        }
                    }
                    if (build.getStatus() != null) {
                        switch (build.getStatus()) {
                            case all:
                            case none:
                            case notStarted:
                                progressDialog.setMessage(getString(R.string.build_status_start));
                                break;
                            case postponed: {
                                progressDialog.setMessage(getString(R.string.build_status_postpone));
                                dismissDialogIn5Sec();
                            }
                            break;
                            case cancelling:
                                progressDialog.setMessage(getString(R.string.build_status_cancel));
                                break;
                            case inProgress:
                                progressDialog.setMessage(getString(R.string.build_status_progress));
                                break;
                            case completed: {
                                progressDialog.setMessage(getString(R.string.build_status_completed) + resultString);
                                dismissDialogIn5Sec();
                            }
                            break;
                        }
                    }
                }
            }
        });
    }

    private void dismissDialogIn5Sec() {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.postDelayed(new Runnable() {
            @Override public void run() {
                progressDialog.dismiss();
            }
        }, 5000);
    }

    private void postponeCheckBuild(final String buildId) {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                runInBackground(new Runnable() {
                    @Override public void run() {
                        vstsService.getBuild(buildId, ClassifierActivity.this);
                    }
                });
            }
        }, 0, 5000);
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == 123) {
            addButton.setVisibility(View.GONE);
            progressBar2.animate();
            progressBar2.setVisibility(View.VISIBLE);

            ClipData clipData = data.getClipData();
            final ArrayList<Uri> uris = new ArrayList<>();

            if (clipData == null) {
                uris.add(data.getData());
            } else {
                for (int i = 0; i < clipData.getItemCount(); i++)
                    uris.add(clipData.getItemAt(i).getUri());
            }

            mUploadRunnable = new Runnable() {
                @Override public void run() {
                    try {
                        updateData(mLabelId, mLabel, getContentResolver(), uris);
                    } catch (Exception e) {
                        LOGGER.e(e, "Exception!");
                    }
                }
            };
        }
    }

    void updateData(String labelId, String label, ContentResolver contentResolver, ArrayList<Uri> uris) {
        synchronized (this) {
            customVisionService.createImagesFromFiles(contentResolver, labelId, label, uris, this);
            getTagsInBackground();
        }
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        classifier = new MSCognitiveServicesClassifier(ClassifierActivity.this);

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        final Display display = getWindowManager().getDefaultDisplay();
        final int screenOrientation = display.getRotation();

        LOGGER.i("Sensor orientation: %d, Screen orientation: %d", rotation, screenOrientation);

        sensorOrientation = rotation + screenOrientation;

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        yuvBytes = new byte[3][];

        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });
    }

    protected void processImageRGBbytes(int[] rgbBytes) {
        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        Recognition r = classifier.classifyImage(rgbFrameBitmap, sensorOrientation);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        final List<Recognition> results = new ArrayList<>();

                        if (r.getConfidence() > 0.7) {
                            results.add(r);
                        }

                        LOGGER.i("Detect: %s", results);
                        if (resultsView == null) {
                            resultsView = findViewById(R.id.results);
                        }
                        resultsView.setResults(results);
                        requestRender();
                        computing = false;
                        if (postInferenceCallback != null) {
                            postInferenceCallback.run();
                        }
                    }
                });

    }

    @Override
    public void onSetDebug(boolean debug) {
    }

    private void renderDebug(final Canvas canvas) {
        if (!isDebug()) {
            return;
        }

        final Vector<String> lines = new Vector<>();
        lines.add("Inference time: " + lastProcessingTimeMs + "ms");
        borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
    }
}
