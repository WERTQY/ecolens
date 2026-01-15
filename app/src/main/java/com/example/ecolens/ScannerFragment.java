package com.example.ecolens;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerFragment extends Fragment {

    private static final String TAG = "ScannerFragment";
    private PreviewView previewView;
    private TextView resultText;
    private TextView binColourText;
    private TextView confidenceText;
    private View resultBackground;

    //Debouncing
    private int consecutiveTimes;
    private String lastLabel;
    private int consecutiveLowConfidence;


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
        resultText = view.findViewById(R.id.scanner_result_label);
        binColourText = view.findViewById(R.id.scanner_bin_colour);
        confidenceText = view.findViewById(R.id.scanner_confidence_label);
        resultBackground = view.findViewById(R.id.scanner_result_background);
        MaterialToolbar toolbar = view.findViewById(R.id.scan_toolbar);
        garbageClassifier = new GarbageClassifier(this.getContext(), 400);

        consecutiveTimes = 0;
        consecutiveLowConfidence = 0;
        String lastLabel = "";

        cameraExecutor = Executors.newSingleThreadExecutor();

        //toolbar back to home
        View backButton = view.findViewById(R.id.btn_back_map);
        backButton.setOnClickListener(v -> {
            Navigation.findNavController(view).navigateUp();
        });

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
                            public void onResult(String outputLabel, float score) {
                                //clean the label to fix issues.
                                String label = outputLabel.trim();

                                System.out.println("---------------------- AI DEBUG ----------------------");
                                System.out.println("Detected: " + label);
                                System.out.println("Confidence: " + score);
                                System.out.println("Last Detected: " + lastLabel);

                                if(consecutiveLowConfidence >= 2) {
                                    //FIX THE ISSUES
                                    if (getActivity() != null) {
                                        // SWITCH TO MAIN THREAD
                                        getActivity().runOnUiThread(() -> {
                                            // NOW it is safe to touch the UI
                                            resultBackground.setVisibility(View.INVISIBLE);
                                            showResult("", "", -1, "#FFFFFF");
                                        });
                                    }
                                }

                                if(score < 0.5){
                                    consecutiveLowConfidence++;
                                    return;
                                }

                                if(!label.equals("plastic") && !label.equals("paper") && !label.equals("glass") && !label.equals("metal")) {
                                    if(label.equals("battery") || label.equals("biological") || label.equals("shoes")) {
                                        getActivity().runOnUiThread(() -> {
                                            // NOW it is safe to touch the UI
                                            resultBackground.setVisibility(View.VISIBLE);
                                            showResult("Non-Recyclable", "", score * 100, "#FFFFFF");
                                        });
                                        return;
                                    }
                                    consecutiveLowConfidence++;
                                    return;
                                }

                                consecutiveLowConfidence = 0;

                                if(consecutiveTimes == 0) {
                                    lastLabel = label;
                                }
                                if(label.equals(lastLabel)) consecutiveTimes++;
                                else consecutiveTimes = 0;

                                lastLabel = label;

                                if(consecutiveTimes >= 3) {
                                    String binType = "";
                                    String colourCode = "#FFFFFF";
                                    switch (label) {
                                        case "plastic":
                                        case "metal":
                                            binType = "Orange";
                                            colourCode = "#F58220";
                                            break;
                                        case "paper":
                                            binType = "Blue";
                                            colourCode = "#005696";
                                            break;
                                        case "glass":
                                            binType = "Brown";
                                            colourCode = "#8B4513";
                                            break;
                                    }
                                    //FIX THE ISSUES
                                    if (getActivity() != null) {
                                        // SWITCH TO MAIN THREAD
                                        String finalBinType = binType;
                                        String finalColourCode = colourCode;
                                        getActivity().runOnUiThread(() -> {
                                            // NOW it is safe to touch the UI
                                            resultBackground.setVisibility(View.VISIBLE);
                                            showResult(label, finalBinType, score * 100, finalColourCode);
                                        });
                                    }
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
    private void showResult(String detectedItem, String binType, float confidence, String colourCode){
        if(detectedItem.isEmpty())
            resultText.setText("");
        else
            resultText.setText("Detected: " + detectedItem);

        if(binType.isEmpty())
            binColourText.setText("");
        else {
            binColourText.setTextColor(Color.parseColor(colourCode));
            binColourText.setText("Throw in " + binType);
        }
        if(confidence == -1)
            confidenceText.setText("");
        else
            confidenceText.setText(String.format("Confidence: %.1f%%", confidence));

    }
}