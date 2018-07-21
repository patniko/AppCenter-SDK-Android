package demo.tensorflow.org.customvision_sample.listener;

public interface ImageUploadCallback {
    void onUploadSuccess();
    void onUploadFailure(int responseCode, boolean duplicates);
}
