package com.example.quizapledu.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.TestListResponse;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OMRScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private Button btnCapture;
    private TextView tvInstruction, tvStatus;
    private LinearLayout layoutProcessing;

    private int selectedTestId = -1;
    private int selectedStudentId = -1;
    private String selectedTestName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_omr_scan);

        previewView    = findViewById(R.id.camera_preview);
        btnCapture     = findViewById(R.id.btn_capture);
        tvInstruction  = findViewById(R.id.tv_instruction);
        tvStatus       = findViewById(R.id.tv_status);
        layoutProcessing = findViewById(R.id.layout_processing);

        if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);

        tvInstruction.setText("Fotografiaza foaia cu raspunsuri marcate (X sau cerc).");
        if (tvStatus != null) tvStatus.setText("Selecteaza testul si studentul mai intai");

        // Selecteaza test mai intai
        selectTest();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        btnCapture.setEnabled(false);
        btnCapture.setOnClickListener(v -> captureAndScan());
    }

    private void selectTest() {
        RetrofitClient.getApiService().getTests()
                .enqueue(new Callback<TestListResponse>() {
                    @Override
                    public void onResponse(Call<TestListResponse> call, Response<TestListResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().tests == null) {
                            Toast.makeText(OMRScanActivity.this,
                                    "Nu s-au putut incarca testele", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        List<TestListResponse.Test> tests = response.body().tests;
                        String[] testNames = new String[tests.size()];
                        int[] testIds = new int[tests.size()];
                        for (int i = 0; i < tests.size(); i++) {
                            testNames[i] = tests.get(i).test_name;
                            testIds[i] = tests.get(i).id;
                        }

                        new AlertDialog.Builder(OMRScanActivity.this)
                                .setTitle("Selecteaza testul")
                                .setItems(testNames, (dialog, which) -> {
                                    selectedTestId = testIds[which];
                                    selectedTestName = testNames[which];
                                    selectStudent();
                                })
                                .setNegativeButton("Anuleaza", (d, w) -> finish())
                                .setCancelable(false)
                                .show();
                    }

                    @Override
                    public void onFailure(Call<TestListResponse> call, Throwable t) {
                        Toast.makeText(OMRScanActivity.this,
                                "Eroare conexiune", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void selectStudent() {
        android.widget.EditText etStudentId = new android.widget.EditText(this);
        etStudentId.setHint("ID student (ex: 2)");
        etStudentId.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etStudentId.setTextColor(0xFFFAFAFA);
        etStudentId.setHintTextColor(0xFF52525B);
        etStudentId.setPadding(32, 16, 32, 16);

        new AlertDialog.Builder(this)
                .setTitle("ID student")
                .setMessage("Test: " + selectedTestName)
                .setView(etStudentId)
                .setPositiveButton("Confirma", (dialog, which) -> {
                    String input = etStudentId.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(this, "Introdu ID-ul studentului",
                                Toast.LENGTH_SHORT).show();
                        selectStudent();
                        return;
                    }
                    selectedStudentId = Integer.parseInt(input);
                    if (tvStatus != null) {
                        tvStatus.setText("Test: " + selectedTestName
                                + " | Student ID: " + selectedStudentId);
                    }
                    btnCapture.setEnabled(true);
                    Toast.makeText(this, "Gata! Fotografiaza foaia.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Anuleaza", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Permisiunea camerei e necesara", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Eroare camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureAndScan() {
        if (imageCapture == null || selectedTestId == -1 || selectedStudentId == -1) return;

        btnCapture.setEnabled(false);
        btnCapture.setText("Se proceseaza...");
        tvInstruction.setText("Se analizeaza grila...");
        if (layoutProcessing != null) layoutProcessing.setVisibility(View.VISIBLE);

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull androidx.camera.core.ImageProxy image) {
                        Bitmap bitmap = imageProxyToBitmap(image);
                        image.close();
                        if (bitmap != null) uploadOMR(bitmap);
                        else resetUI();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        resetUI();
                        Toast.makeText(OMRScanActivity.this,
                                "Eroare captura: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Bitmap imageProxyToBitmap(androidx.camera.core.ImageProxy image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private void uploadOMR(Bitmap bitmap) {
        try {
            // Resize la 1600px max
            int maxW = 1600;
            if (bitmap.getWidth() > maxW) {
                float ratio = (float) maxW / bitmap.getWidth();
                bitmap = Bitmap.createScaledBitmap(bitmap, maxW,
                        (int)(bitmap.getHeight()*ratio), true);
            }

            File file = new File(getCacheDir(), "omr_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            RequestBody imageBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image", file.getName(), imageBody);
            RequestBody testIdBody = RequestBody.create(
                    MediaType.parse("text/plain"), String.valueOf(selectedTestId));
            RequestBody studentIdBody = RequestBody.create(
                    MediaType.parse("text/plain"), String.valueOf(selectedStudentId));

            RetrofitClient.getApiService()
                    .scanOMR(imagePart, testIdBody, studentIdBody)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            resetUI();
                            if (!response.isSuccessful() || response.body() == null) {
                                showError("Eroare server");
                                return;
                            }
                            try {
                                String raw = response.body().string();
                                JSONObject json = new JSONObject(raw);
                                if (json.optBoolean("success", false)) {
                                    showOMRResults(json);
                                } else {
                                    showError(json.optString("error", "Eroare necunoscuta"));
                                }
                            } catch (Exception e) { showError(e.getMessage()); }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            resetUI();
                            showError("Eroare conexiune: " + t.getMessage());
                        }
                    });

        } catch (Exception e) {
            resetUI();
            showError(e.getMessage());
        }
    }

    private void showOMRResults(JSONObject json) {
        double totalScore = json.optDouble("total_score", 0);
        String scoreDetails = json.optString("score_details", "");
        JSONArray detectedAnswers = json.optJSONArray("detected_answers");
        JSONArray results = json.optJSONArray("results");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Scor final: %.1f%%\n", totalScore));
        sb.append("Puncte: ").append(scoreDetails).append("\n\n");

        if (results != null) {
            int correct = 0;
            for (int i = 0; i < results.length(); i++) {
                try {
                    JSONObject r = results.getJSONObject(i);
                    boolean isCorrect = r.optBoolean("is_correct", false);
                    if (isCorrect) correct++;
                    sb.append(String.format("Q%d: %s → %s %s\n",
                            i + 1,
                            r.optString("detected_answer", "?"),
                            r.optString("correct_answer", "?"),
                            isCorrect ? "✓" : "✗"));
                } catch (Exception e) { /* skip */ }
            }
            sb.append(String.format("\nCorecte: %d/%d", correct, results.length()));
        }

        new AlertDialog.Builder(this)
                .setTitle("Grila corectata!")
                .setMessage(sb.toString())
                .setPositiveButton("Alta foaie", (d, w) -> {
                    selectStudent();
                })
                .setNegativeButton("Gata", (d, w) -> finish())
                .show();
    }

    private void showError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Eroare")
                .setMessage(msg + "\n\nSfaturi:\n• Asigura-te ca marcajele X/cerculete sunt clare\n• Lumina uniforma, fara umbre\n• Foaia centrata in cadru")
                .setPositiveButton("Rescaneaza", null)
                .show();
    }

    private void resetUI() {
        btnCapture.setEnabled(true);
        btnCapture.setText("Fotografiaza");
        tvInstruction.setText("Fotografiaza foaia cu raspunsuri marcate (X sau cerc).");
        if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
    }
}
