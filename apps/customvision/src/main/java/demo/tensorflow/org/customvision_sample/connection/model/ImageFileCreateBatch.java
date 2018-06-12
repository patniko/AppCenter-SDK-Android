package demo.tensorflow.org.customvision_sample.connection.model;

/**
 * Custom vision images create batch (from file).
 */
public class ImageFileCreateBatch {

    private String[] tagIds;

    private ImageFileCreateEntry[] images;

    public ImageFileCreateBatch(String[] tagIds, ImageFileCreateEntry[] images) {
        this.tagIds = tagIds;
        this.images = images;
    }
}
