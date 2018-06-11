package demo.tensorflow.org.customvision_sample.connection.model;

/**
 * Custom Vision image model.
 */
public class Image {

    private String id;
    private String imageUri;

    public Image(String id, String imageUri) {
        this.id = id;
        this.imageUri = imageUri;
    }

    public String getId() {
        return id;
    }
}
