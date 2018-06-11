package demo.tensorflow.org.customvision_sample.connection;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import demo.tensorflow.org.customvision_sample.R;
import demo.tensorflow.org.customvision_sample.connection.model.Image;
import demo.tensorflow.org.customvision_sample.connection.model.ImageFileCreateBatch;
import demo.tensorflow.org.customvision_sample.connection.model.ImageFileCreateEntry;
import demo.tensorflow.org.customvision_sample.connection.model.Tag;
import demo.tensorflow.org.customvision_sample.listener.ImageDeleteCallback;
import demo.tensorflow.org.customvision_sample.listener.ImageUploadCallback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomVisionService {

    private final String CUSTOM_VISION_ENDPOINT = "https://southcentralus.api.cognitive.microsoft.com/customvision/v2.0/";
    private final String CREATE_IMAGES_FILES_ENDPOINT = "/images/files";
    private final String CREATE_IMAGES_URL_ENDPOINT = "/images/urls";
    private final String GET_TAGGED_IMAGES_ENDPOINT = "/images/tagged";
    private final String IMAGES_ENDPOINT = "/images";
    private final String TAG_ENDPOINT = "/tags";

    private int MAX_IMAGE_SIZE = 512;
    private String mBaseEndpoint;
    private String mTrainingKey;
    private String mProjectId;
    private boolean successFlag;
    private int lastResponseCode;
    private boolean duplicates;
    private Gson mGson;
    private Context mContext;

    public CustomVisionService(String trainingKey, String projectId, Context context) {
        mProjectId = projectId;
        mTrainingKey = trainingKey;
        mBaseEndpoint = CUSTOM_VISION_ENDPOINT + "Training/projects/" + mProjectId;
        mGson = new Gson();
        String maxImageSize = context.getString(R.string.MAX_IMAGE_SIZE);
        if (maxImageSize.length() > 0) {
            MAX_IMAGE_SIZE = Integer.parseInt(maxImageSize);
        }
        mContext = context;
    }

    public Tag[] getTags() {
        try {
            String endpoint = mBaseEndpoint + TAG_ENDPOINT;
            String body = makeRequest(endpoint, null, false);
            return mGson.fromJson(body, Tag[].class);
        } catch (IOException | JsonSyntaxException e) {
            Log.e("CUSTOM_VISION", e.getMessage());
        }
        return null;
    }

    public String createTag(String tagName) {
        String tagId = "";
        try {
            String endpoint = mBaseEndpoint + TAG_ENDPOINT + "?name=" + tagName;
            String body = makeRequest(endpoint, "", false);
            Tag tag = mGson.fromJson(body, Tag.class);
            tagId = tag.getId();
        } catch (IOException e) {
            Log.e("CUSTOM_VISION", e.getMessage());
        }
        return tagId;
    }

    public Image[] getImagesByTagId(String tagId) {
        try {
            String endpoint = mBaseEndpoint + GET_TAGGED_IMAGES_ENDPOINT + "?tagIds=" + "[\"" + tagId + "\"]";
            String body = makeRequest(endpoint, null, false);
            return mGson.fromJson(body, Image[].class);
        } catch (IOException e) {
            Log.e("CUSTOM_VISION", e.getMessage());
        }
        return null;
    }

    public void deleteImages(Image[] images) {
        String endpoint = mBaseEndpoint + IMAGES_ENDPOINT + "?";
        try {
            for (Image image : images) {
                endpoint += "imageIds=" + image.getId() + "&";
            }
            endpoint = endpoint.substring(0, endpoint.length() - 1);
            makeRequest(endpoint, null, true);
        } catch (IOException e) {
            Log.e("CUSTOM_VISION", e.getMessage());
        }
    }

    public void deleteTag(String tagId) {
        try {
            String endpoint = mBaseEndpoint + TAG_ENDPOINT + "/" + tagId;
            makeRequest(endpoint, null, true);
        } catch (IOException e) {
            Log.e("CUSTOM_VISION", e.getMessage());
        }
    }

    public void deleteTagAndItsImages(String tagId, ImageDeleteCallback imageDeleteCallback) {
        successFlag = true;
        Image[] taggedImages = getImagesByTagId(tagId);

        if (taggedImages != null && taggedImages.length > 0) {
            deleteImages(taggedImages);
        }
        deleteTag(tagId);
        if (imageDeleteCallback != null) {
            if (successFlag) {
                imageDeleteCallback.onImagesDeleted();
            } else {
                imageDeleteCallback.onDeleteFailure(lastResponseCode);
            }
        }
    }

    public void createImagesFromFiles(ContentResolver contentResolver, String tagId, String tag, ArrayList<Uri> uris, ImageUploadCallback imageUploadCallback) {
        if (tagId.length() == 0) {
            tagId = createTag(tag);
        }
        ImageFileCreateBatch imageFileCreateBatch;
        String[] tags = new String[1];
        tags[0] = tagId;
        ImageFileCreateEntry[] images = new ImageFileCreateEntry[uris.size()];
        int i = 0;

        try {
            for (Uri uri : uris) {
                Bitmap bitmap = getBitmapFromUri(contentResolver, uri);
                String imageContents = this.convertBitmapToB64String(bitmap);
                ImageFileCreateEntry imageFileCreateEntry = new ImageFileCreateEntry(tag + "" + i + ".png", imageContents);
                images[i] = imageFileCreateEntry;
                i++;
            }

            imageFileCreateBatch = new ImageFileCreateBatch(tags, images);
            String endpoint = mBaseEndpoint + CREATE_IMAGES_FILES_ENDPOINT;
            successFlag = true;
            makeRequest(endpoint, mGson.toJson(imageFileCreateBatch), false);
            if (imageUploadCallback != null) {
                if (successFlag) {
                    imageUploadCallback.onUploadSuccess();
                } else {
                    imageUploadCallback.onUploadFailure(lastResponseCode, duplicates);
                }
            }
        } catch (Exception e) {
            Log.e("CUSTOM_VISION", e.getMessage());
        }
    }

    private String makeRequest(String endpoint, String bodyOrNull, boolean delete) throws IOException, NullPointerException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(endpoint).newBuilder();
        String url = urlBuilder.build().toString();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        if (delete) {
            requestBuilder
                    .delete()
                    .addHeader("Content-Type", "application/json");
        } else if (bodyOrNull != null) {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), bodyOrNull);
            requestBuilder
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json");
        }
        Request request = requestBuilder
                .addHeader("projectId", mProjectId)
                .addHeader("Training-Key", mTrainingKey)
                .build();
        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        lastResponseCode = response.code();
        duplicates = false;
        if (!response.isSuccessful() || (lastResponseCode != 200 && lastResponseCode != 204 && lastResponseCode != 404) || responseBody.contains("isBatchSuccessful\":false")) {
            Log.e("CUSTOM_VISION", responseBody);
            if (responseBody.contains("status\":\"OKDuplicate")) {
                duplicates = true;
            }
            successFlag = false;
        }
        return responseBody;
    }
    private Bitmap getBitmapFromUri(ContentResolver contentResolver, Uri uri) throws Exception {
        ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return bitmap;
    }

    private String convertBitmapToB64String(Bitmap image) {
        try {
            File testFile = new File(mContext.getCacheDir(), "TEST.PNG");
            FileOutputStream fileOutputStream = new FileOutputStream(testFile);
            image.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for PNG*/, fileOutputStream);
            fileOutputStream.close();
            long sizeInKB = testFile.length() / 1024;
            testFile.delete();
            if (sizeInKB > MAX_IMAGE_SIZE) {
                double scaleCoef = (MAX_IMAGE_SIZE * 1.0) / sizeInKB;
                int scaledQuality = (int) (100 * scaleCoef);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG, scaledQuality /*ignored for PNG*/, byteArrayOutputStream);
                byte[] bitmapData = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
                return android.util.Base64.encodeToString(bitmapData, android.util.Base64.DEFAULT);
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, byteArrayOutputStream);
            byte[] bitmapData = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return android.util.Base64.encodeToString(bitmapData, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }
}
