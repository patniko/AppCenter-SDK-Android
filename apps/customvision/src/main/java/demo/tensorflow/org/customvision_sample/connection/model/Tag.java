package demo.tensorflow.org.customvision_sample.connection.model;

/**
 * Custom Vision image tag.
 */
public class Tag {

    public Tag(String id, String name, String description, int imageCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageCount = imageCount;
    }

    private String id;
    private String name;
    private String description;
    private int imageCount;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
