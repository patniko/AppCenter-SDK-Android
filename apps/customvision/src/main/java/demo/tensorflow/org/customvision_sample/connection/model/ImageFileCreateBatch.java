package demo.tensorflow.org.customvision_sample.connection.model;

/**
 * Custom vision images create batch (from file).
 */
public class ImageFileCreateBatch extends ImageCreateBatch {

    public ImageFileCreateBatch(String[] tagIds, ImageFileCreateEntry[] images) {
        super(tagIds);
        this.images = images;
    }

    private ImageFileCreateEntry[] images;
}
