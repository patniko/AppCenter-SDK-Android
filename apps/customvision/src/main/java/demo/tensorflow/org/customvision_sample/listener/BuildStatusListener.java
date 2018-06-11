package demo.tensorflow.org.customvision_sample.listener;

import demo.tensorflow.org.customvision_sample.connection.model.VstsBuild;

public interface BuildStatusListener {
    void onBuildStatusChanged(VstsBuild build);
}
