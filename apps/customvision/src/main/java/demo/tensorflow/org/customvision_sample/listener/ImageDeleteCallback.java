package demo.tensorflow.org.customvision_sample.listener;

public interface ImageDeleteCallback {
    void onImagesDeleted();
    void onDeleteFailure(int responseCode);
}
