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
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.utils.Constants;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private Button btnCapture;
    private TextView tvInstruction, tvScanMode, tvProcessing;
    private LinearLayout layoutProcessing;

    // Parametri primiti
    private int testId;
    private int questionId;
    private int studentId;
    private String scanMode; // "full_page" sau "single_question"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Citeste parametri
        testId     = getIntent().getIntExtra("test_id", 0);
        questionId = getIntent().getIntExtra("question_id", 0);
        scanMode   = getIntent().getStringExtra("scan_mode");
        if (scanMode == null) scanMode = "single_question";

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        studentId = prefs.getInt(Constants.KEY_USER_ID, 0);

        // Views
        previewView    = findViewById(R.id.camera_preview);
        btnCapture     = findViewById(R.id.btn_capture);
        tvInstruction  = findViewById(R.id.tv_instruction);
        tvScanMode     = findViewById(R.id.tv_scan_mode);
        tvProcessing   = findViewById(R.id.tv_processing);
        layoutProcessing = findViewById(R.id.layout_processing);

        if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);

        // Titlu in functie de mod
        if ("full_page".equals(scanMode)) {
            if (tvScanMode != null)
                tvScanMode.setText("Scanare foaie completa");
            tvInstruction.setText("Fotografiaza intreaga foaie de raspunsuri.\nAsigura-te ca tot textul e vizibil si luminat uniform.");
        } else {
            if (tvScanMode != null)
                tvScanMode.setText("Scanare raspuns");
            tvInstruction.setText("Fotografiaza raspunsul scris.\nScris clar, fundal alb, lumina buna.");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        btnCapture.setOnClickListener(v -> captureImage());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permisiunea camerei e necesara", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Calitate maxima pentru OCR mai bun
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Eroare camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureImage() {
        if (imageCapture == null) return;

        btnCapture.setEnabled(false);
        btnCapture.setText("Se proceseaza...");
        tvInstruction.setText("Se extrage textul din imagine...");
        if (layoutProcessing != null) layoutProcessing.setVisibility(View.VISIBLE);

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap bitmap = imageProxyToBitmap(image);
                        image.close();
                        if (bitmap != null) {
                            uploadImage(bitmap);
                        } else {
                            resetUI();
                            Toast.makeText(ScanActivity.this,
                                    "Eroare la capturare imagine", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        resetUI();
                        Toast.makeText(ScanActivity.this,
                                "Eroare captura: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void uploadImage(Bitmap bitmap) {
        try {
            // Rezolutie mai buna pentru OCR: max 1600px latime (era 1024)
            int maxWidth = 1600;
            if (bitmap.getWidth() > maxWidth) {
                float ratio = (float) maxWidth / bitmap.getWidth();
                int newHeight = (int) (bitmap.getHeight() * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true);
            }

            // Calitate JPEG mai mare: 90 (era 70) — mai putin loss = OCR mai precis
            File file = new File(getCacheDir(), "scan_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            RequestBody imageBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image", file.getName(), imageBody);

            if ("full_page".equals(scanMode)) {
                // Mod foaie completa: trimite la scan-full-page
                // Profesorul selecteaza testul si studentul din dialog
                showSelectTestDialog(imagePart, file);
            } else {
                // Mod raspuns individual
                RequestBody testIdBody     = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(testId));
                RequestBody studentIdBody  = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(studentId));
                RequestBody questionIdBody = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(questionId));

                RetrofitClient.getApiService()
                        .scanQuestion(imagePart, testIdBody, studentIdBody, questionIdBody)
                        .enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> r) {
                                resetUI();
                                if (r.isSuccessful() && r.body() != null) {
                                    try { handleSingleScanResult(r.body().string()); }
                                    catch (Exception e) { e.printStackTrace(); }
                                } else {
                                    Toast.makeText(ScanActivity.this,
                                            "Eroare server", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                resetUI();
                                Toast.makeText(ScanActivity.this,
                                        "Eroare conexiune: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

        } catch (Exception e) {
            resetUI();
            Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Pentru full_page: selectam testul si studentul inainte de upload
     */
    private void showSelectTestDialog(MultipartBody.Part imagePart, File imageFile) {
        // Incarca lista de teste
        RetrofitClient.getApiService().getTests().enqueue(
                new retrofit2.Callback<com.example.quizapledu.api.models.TestListResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.quizapledu.api.models.TestListResponse> call,
                                           retrofit2.Response<com.example.quizapledu.api.models.TestListResponse> response) {
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().tests == null) {
                            resetUI();
                            Toast.makeText(ScanActivity.this,
                                    "Nu s-au putut incarca testele", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        java.util.List<com.example.quizapledu.api.models.TestListResponse.Test> tests
                                = response.body().tests;
                        String[] testNames = new String[tests.size()];
                        int[] testIds = new int[tests.size()];

                        for (int i = 0; i < tests.size(); i++) {
                            testNames[i] = tests.get(i).test_name;
                            testIds[i] = tests.get(i).id;
                        }

                        new AlertDialog.Builder(ScanActivity.this)
                                .setTitle("Selecteaza testul")
                                .setItems(testNames, (dialog, which) -> {
                                    int selectedTestId = testIds[which];
                                    askForStudentId(imagePart, imageFile, selectedTestId);
                                })
                                .setNegativeButton("Anuleaza", (d, w) -> resetUI())
                                .show();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.quizapledu.api.models.TestListResponse> call,
                                          Throwable t) {
                        resetUI();
                        Toast.makeText(ScanActivity.this,
                                "Eroare la incarcare teste", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void askForStudentId(MultipartBody.Part imagePart, File imageFile, int selectedTestId) {
        android.widget.EditText etStudentId = new android.widget.EditText(this);
        etStudentId.setHint("ID student (ex: 2)");
        etStudentId.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etStudentId.setTextColor(0xFFFAFAFA);
        etStudentId.setHintTextColor(0xFF52525B);

        new AlertDialog.Builder(this)
                .setTitle("ID student")
                .setMessage("Introdu ID-ul studentului a carui foaie o scanezi:")
                .setView(etStudentId)
                .setPositiveButton("Scaneaza", (dialog, which) -> {
                    String input = etStudentId.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(this, "Introdu ID-ul studentului",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int sId = Integer.parseInt(input);
                    sendFullPageScan(imagePart, imageFile, selectedTestId, sId);
                })
                .setNegativeButton("Anuleaza", (d, w) -> resetUI())
                .show();
    }

    private void sendFullPageScan(MultipartBody.Part imagePart, File imageFile,
                                  int selectedTestId, int selectedStudentId) {
        tvInstruction.setText("Se analizeaza foaia... Poate dura 20-40 secunde.");

        // Re-creeaza imagePart din file (cel vechi e consumat)
        RequestBody imageBody = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part freshImagePart = MultipartBody.Part.createFormData(
                "image", imageFile.getName(), imageBody);

        RequestBody testIdBody    = RequestBody.create(
                MediaType.parse("text/plain"), String.valueOf(selectedTestId));
        RequestBody studentIdBody = RequestBody.create(
                MediaType.parse("text/plain"), String.valueOf(selectedStudentId));

        RetrofitClient.getApiService()
                .scanFullPage(freshImagePart, testIdBody, studentIdBody)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> r) {
                        resetUI();
                        if (r.isSuccessful() && r.body() != null) {
                            try { handleFullPageResult(r.body().string()); }
                            catch (Exception e) { e.printStackTrace(); }
                        } else {
                            Toast.makeText(ScanActivity.this,
                                    "Eroare server la scanare", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        resetUI();
                        Toast.makeText(ScanActivity.this,
                                "Eroare conexiune: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleSingleScanResult(String jsonResult) {
        try {
            JSONObject json = new JSONObject(jsonResult);
            if (json.optBoolean("success", false)) {
                String extractedText = json.optString("extracted_text", "");
                double score = json.optDouble("score", 0);
                double maxScore = json.optDouble("max_score", 10);
                String feedback = json.optString("feedback", "");

                String message = "Text extras:\n\"" + extractedText + "\"\n\n"
                        + "Scor: " + String.format("%.1f", score) + "/" + String.format("%.0f", maxScore)
                        + "\n\nFeedback: " + feedback;

                new AlertDialog.Builder(this)
                        .setTitle("Rezultat scanare")
                        .setMessage(message)
                        .setPositiveButton("Foloseste raspunsul", (d, w) -> {
                            Intent result = new Intent();
                            result.putExtra("extracted_text", extractedText);
                            setResult(RESULT_OK, result);
                            finish();
                        })
                        .setNegativeButton("Rescaneaza", (d, w) -> { })
                        .show();
            } else {
                String error = json.optString("error", "Eroare necunoscuta");
                showScanError(error);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Eroare procesare: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFullPageResult(String jsonResult) {
        try {
            JSONObject json = new JSONObject(jsonResult);
            if (json.optBoolean("success", false)) {
                double totalScore = json.optDouble("total_score", 0);
                String scoreDetails = json.optString("score_details", "");
                JSONArray answers = json.optJSONArray("answers");

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Scor final: %.1f%%\n", totalScore));
                sb.append("Detalii: ").append(scoreDetails).append("\n\n");

                if (answers != null) {
                    sb.append("Raspunsuri per intrebare:\n");
                    for (int i = 0; i < Math.min(answers.length(), 5); i++) {
                        JSONObject ans = answers.getJSONObject(i);
                        sb.append(String.format("Q%d: %.1f/%d — %s\n",
                                i + 1,
                                ans.optDouble("score", 0),
                                ans.optInt("max_points", 10),
                                ans.optString("feedback", "").length() > 40
                                        ? ans.optString("feedback").substring(0, 40) + "..."
                                        : ans.optString("feedback", "")));
                    }
                    if (answers.length() > 5) {
                        sb.append("... si ").append(answers.length() - 5)
                                .append(" intrebari in plus");
                    }
                }

                new AlertDialog.Builder(this)
                        .setTitle("Foaie corectata!")
                        .setMessage(sb.toString())
                        .setPositiveButton("Gata", (d, w) -> finish())
                        .setNeutralButton("Alta foaie", (d, w) -> { })
                        .show();
            } else {
                showScanError(json.optString("error", "Eroare scanare"));
            }
        } catch (Exception e) {
            Toast.makeText(this, "Eroare procesare: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showScanError(String error) {
        new AlertDialog.Builder(this)
                .setTitle("Scanare esuata")
                .setMessage("Eroare: " + error
                        + "\n\nSfaturi:\n• Scrie clar cu majuscule sau tipar\n"
                        + "• Fundal alb, marker negru\n"
                        + "• Lumina uniforma, fara umbre\n"
                        + "• Telefonul stabil, paralel cu foaia")
                .setPositiveButton("Incearca din nou", null)
                .show();
    }

    private void resetUI() {
        btnCapture.setEnabled(true);
        btnCapture.setText("Fotografiaza");
        if ("full_page".equals(scanMode)) {
            tvInstruction.setText("Fotografiaza intreaga foaie de raspunsuri.");
        } else {
            tvInstruction.setText("Fotografiaza raspunsul scris.");
        }
        if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
    }
}
