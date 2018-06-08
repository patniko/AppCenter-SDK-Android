package demo.tensorflow.org.customvision_sample.connection;

import android.util.Log;

import com.google.gson.Gson;

import demo.tensorflow.org.customvision_sample.listener.BuildStatusListener;
import demo.tensorflow.org.customvision_sample.connection.model.VstsBuild;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VstsService {

    private final String VSTS_ENDPOINT = "https://msmobilecenter.visualstudio.com/";
    private String mBaseEndpoint;
    private String BUILD_ENDPOINT = "build/builds";
    private String API_VERSION = "?api-version=4.1";
    private String mAccessToken;
    private boolean successFlag = true;
    private int lastResponseCode;

    public VstsService(String PAT, String project) {
        mAccessToken = PAT;
        mBaseEndpoint = VSTS_ENDPOINT + project + "/_apis/";
    }

    public String kickBuild(String buildDefinitionId) {
        try {
            successFlag = true;
            String responseBody = makeRequest(mBaseEndpoint + BUILD_ENDPOINT + API_VERSION, "{\"definition\":{\"id\": \"" + buildDefinitionId + "\"}}");
            if (!successFlag && (lastResponseCode == 203 || lastResponseCode == 404)) {
                return String.valueOf(lastResponseCode);
            }
            VstsBuild build = new Gson().fromJson(responseBody, VstsBuild.class);
            return build.getId();
        } catch (Exception e) {
            Log.e("VSTS", e.getMessage());
        }
        return "";
    }

    public VstsBuild getBuild(String buildId, BuildStatusListener buildStatusListener) {
        try {
            successFlag = true;
            String responseBody = makeRequest(mBaseEndpoint + BUILD_ENDPOINT + API_VERSION + "&buildId=" + buildId, null);
            if (successFlag) {
                VstsBuild build = new Gson().fromJson(responseBody, VstsBuild.class);
                if (buildStatusListener != null) {
                    buildStatusListener.onBuildStatusChanged(build);
                }
                return build;
            }
        } catch (Exception e) {
            Log.e("VSTS", e.getMessage());
        }
        return null;
    }

    private String makeRequest(String endpoint, String bodyOrNull) {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(endpoint).newBuilder();
            String url = urlBuilder.build().toString();
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url);

            if (bodyOrNull != null) {
                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), bodyOrNull);
                requestBuilder
                        .post(requestBody)
                        .addHeader("Content-Type", "application/json");
            }
            String PAT = android.util.Base64.encodeToString((":" + mAccessToken).getBytes(), android.util.Base64.DEFAULT);
            Request request = requestBuilder
                    .addHeader("Authorization", "Basic " + PAT.trim())
                    .build();
            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            lastResponseCode = response.code();
            if (!response.isSuccessful() || lastResponseCode != 200) {
                Log.e("VSTS", responseBody);
                successFlag = false;

            }
            return responseBody;
        } catch (Exception e) {
            Log.e("VSTS", e.getMessage());
        }
        return "";
    }
}