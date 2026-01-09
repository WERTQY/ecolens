package com.example.ecolens;
import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GarbageDetector {
    private final Context context;
    private ObjectDetector objectDetector;

    public GarbageDetector(Context context) {
        this.context = context;
        setupDetector();
    }

    private void setupDetector() {
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(5)          // detect up to 5 objects at once
                .setScoreThreshold(0.5f)   // only show if 50% sure
                .build();

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(
                    context, "model.tflite", options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Detection> detect(Bitmap bitmap, int rotation) {
        if (objectDetector == null) {
            setupDetector();
        }

        // prepare the image
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new Rot90Op(-rotation / 90)) // handle rotation
                .build();

        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // run detection
        return objectDetector.detect(tensorImage);
    }
}
