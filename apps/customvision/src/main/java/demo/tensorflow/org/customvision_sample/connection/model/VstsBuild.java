package demo.tensorflow.org.customvision_sample.connection.model;

/**
 * Vsts Build model.
 */
public class VstsBuild {

    private String id;

    private BuildStatus status;

    private BuildResult result;

    public VstsBuild(String id, BuildStatus status) {
        this.id = id;
        this.status = status;
    }

    public BuildStatus getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public BuildResult getResult() {
        return result;
    }
}
