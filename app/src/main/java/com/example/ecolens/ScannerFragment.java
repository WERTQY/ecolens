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

    private GarbageClassifier garbageClassifier;
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
        garbageClassifier = new GarbageClassifier(this.getContext(), 224);

        cameraExecutor = Executors.newSingleThreadExecutor();

        //toolbar back to home
        View backButton = view.findViewById(R.id.btn_back_map);
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

                // 2. Image Analysis
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(640, 480)) // Keep low for speed
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor,
                        new GarbageAnalyzer(garbageClassifier, new GarbageAnalyzer.OnResultListener() {
                            @Override
                            public void onResult(String label, float score) {
                                String confidence = String.format("%.1f", score * 100);
                                if(!label.equals("plastic") && !label.equals("paper") && !label.equals("glass")) {

                                }else {
                                    if(score >= 0.6) {
                                        System.out.println(confidence);
                                        System.out.println(label);
                                    }
                                    //showPopup(label, "Orange", Float.parseFloat(confidence));
                                }
                            }
                        })
                );


                //bind to Lifecycle
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // Logic popup
    private void showPopup(String detectedItem, String binType, float confidence){
        resultText.setText("Detected: " + detectedItem);
        binInstructionText.setText("Throw in " + binType);
        confidenceText.setText(String.format("Confidence: %.1f%%", confidence));
        resultCard.setVisibility(View.VISIBLE);
    }
}