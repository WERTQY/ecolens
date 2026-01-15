package com.example.ecolens;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import java.util.List;

public class GarbageAnalyzer implements ImageAnalysis.Analyzer {

    private final GarbageClassifier classifier;
    private final OnResultListener listener;
    private long lastTimeStamp = 0;
    private static final long INTERVAL = 500;
    private static final float CROP_SCALE = 1f;

    public interface OnResultListener {
        void onResult(String label, float score);
    }

    public GarbageAnalyzer(GarbageClassifier classifier, OnResultListener listener) {
        this.classifier = classifier;
        this.listener = listener;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(@NonNull ImageProxy image) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTimeStamp < INTERVAL) {
            image.close();
            return;
        }
        lastTimeStamp = currentTime;

        // Convert to Bitmap
        Bitmap bitmap = image.toBitmap();

        // Rotate Bitmap (Fix Camera Rotation)
        float rotationDegrees = image.getImageInfo().getRotationDegrees();
        Bitmap rotatedBitmap = rotateBitmap(bitmap, rotationDegrees);

        // CROP CENTER (Matching 280dp Box)
        int minSide = Math.min(rotatedBitmap.getWidth(), rotatedBitmap.getHeight());

        //calculate the crop size
        int cropDimension = (int) (minSide * CROP_SCALE);

        int xOffset = (rotatedBitmap.getWidth() - cropDimension) / 2;
        int yOffset = (rotatedBitmap.getHeight() - cropDimension) / 2;

        // Create the cropped square
        Bitmap croppedBitmap = Bitmap.createBitmap(
                rotatedBitmap, xOffset, yOffset, cropDimension, cropDimension
        );

        // Classify
        saveBitmapForDebugging(croppedBitmap);
        // The classifier will resize this crop down to 224x224 automatically
        List<GarbageClassifier.ClassificationResult> results = classifier.classify(croppedBitmap);

        // send result
        if (!results.isEmpty()) {
            GarbageClassifier.ClassificationResult top = results.get(0);
            listener.onResult(top.label, top.score);
        }

        image.close();
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        if (angle == 0) return source;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void saveBitmapForDebugging(Bitmap bitmap) {
        // Only save 1 image every 5 seconds to avoid flooding your gallery
        if (System.currentTimeMillis() % 5000 > 200) return;

        String filename = "DEBUG_" + System.currentTimeMillis() + ".jpg";
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.TITLE, filename);
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        try {
            android.net.Uri uri = classifier.context.getContentResolver().insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try (java.io.OutputStream out = classifier.context.getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                System.out.println("SAVED DEBUG IMAGE: " + uri.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}