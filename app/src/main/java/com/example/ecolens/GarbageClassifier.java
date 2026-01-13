package com.example.ecolens;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GarbageClassifier {
    public final Context context;
    private ImageClassifier imageClassifier;
    private final int inputSize;

    // Constructor
    public GarbageClassifier(Context context, int inputSize) {
        this.context = context;
        this.inputSize = inputSize; //128
        setupClassifier();
    }

    private void setupClassifier() {
        ImageClassifier.ImageClassifierOptions options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3) // get top 3 results
                .build();

        try {
            // load model
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                    context, "model.tflite", options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ClassificationResult> classify(Bitmap bitmap) {
        if (imageClassifier == null) {
            setupClassifier();
        }

        // 1. Preprocess the image (Resize to model input size)
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                .build();

        TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // 2. Run Inference
        List<Classifications> results = imageClassifier.classify(tensorImage);

        // 3. Map results to our custom Java class
        List<ClassificationResult> mappedResults = new ArrayList<>();

        if (!results.isEmpty() && !results.get(0).getCategories().isEmpty()) {
            for (org.tensorflow.lite.support.label.Category category : results.get(0).getCategories()) {
                mappedResults.add(new ClassificationResult(category.getLabel(), category.getScore()));
            }
        }

        // Sort by score (descending)
        Collections.sort(mappedResults, new Comparator<ClassificationResult>() {
            @Override
            public int compare(ClassificationResult o1, ClassificationResult o2) {
                return Float.compare(o2.score, o1.score);
            }
        });

        return mappedResults;
    }

    // Inner class to hold data
    public static class ClassificationResult {
        public String label;
        public float score;

        public ClassificationResult(String label, float score) {
            this.label = label;
            this.score = score;
        }
    }
}
