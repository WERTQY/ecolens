package com.example.ecolens;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerFragment extends Fragment {

    private static final String TAG = "ScannerFragment";
    private PreviewView previewView;
    private FloatingActionButton captureButton;
    private CardView resultCard;
    private TextView resultText;
    private TextView binInstructionText;
    private TextView confidenceText;
    private Button closeButton;

    private GarbageDetector garbageDetector;
    private OverlayView overlay;
    private ExecutorService cameraExecutor;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.scanner_preview_view);
        captureButton = view.findViewById(R.id.scanner_capture_btn);
        resultCard = view.findViewById(R.id.scanner_result_card);
        resultText = view.findViewById(R.id.scanner_result_text);
        binInstructionText = view.findViewById(R.id.scanner_bin_instruction);
        confidenceText = view.findViewById(R.id.scanner_confidence_text);
        closeButton = view.findViewById(R.id.scanner_btn_close);
        MaterialToolbar toolbar = view.findViewById(R.id.scan_toolbar);

        garbageDetector = new GarbageDetector(this.getContext());
        overlay = view.findViewById(R.id.overlay);
        cameraExecutor = Executors.newSingleThreadExecutor();

        //toolbar back to home
        View backButton = view.findViewById(R.id.btn_back_manual);
        backButton.setOnClickListener(v -> {
            Navigation.findNavController(view).navigateUp();
        });

        /*
        captureButton.setOnClickListener(v -> {

            showPopup("Plastic", "Orange", 99);
        });
        */

        /*
        closeButton.setOnClickListener(v -> {
            resultCard.setVisibility(View.INVISIBLE);
        });
        */

        // Camera Permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                //bind to Lifecycle
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);

                previewView.postDelayed(this::runObjectDetection, 1000);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // Dummy logic popup
    private void showPopup(String detectedItem, String binType, float confidence){
        resultText.setText("Detected: " + detectedItem);
        binInstructionText.setText("Throw in " + binType);
        confidenceText.setText(String.format("Confidence: %.1f%%", confidence));
        resultCard.setVisibility(View.VISIBLE);
    }

    private void runObjectDetection() {
        // 1. Capture the image exactly as it appears on screen
        android.graphics.Bitmap bitmap = previewView.getBitmap();

        if (bitmap != null) {
            // 2. Run Detection (We pass rotation = 0 because the view is already rotated!)
            List<Detection> results = garbageDetector.detect(bitmap);

            // 3. Draw the boxes
            overlay.setResults(results, bitmap.getHeight(), bitmap.getWidth());

            // Debugging Log: Prove it's working
            if (!results.isEmpty()) {
                android.util.Log.d("EcoLens", "Found: " + results.size() + " objects");
            }
        }

        // 4. Run again in 500ms (Loop)
        // Adjust this number: 100ms = fast / 1000ms = slow
        previewView.postDelayed(this::runObjectDetection, 500);
    }

}