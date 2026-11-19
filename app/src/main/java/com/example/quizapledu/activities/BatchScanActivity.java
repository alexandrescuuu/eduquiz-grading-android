package com.example.quizapledu.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BatchScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 102;
    private static final int GALLERY_PICK_CODE      = 103;

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private Button btnCapture, btnFromGallery, btnProcess;
    private TextView tvInstruction, tvCount;
    private LinearLayout layoutProcessing, containerScanned;

    private int selectedTestId = -1;
    private String selectedTestName = "";
    private List<Integer> studentIds = new ArrayList<>();
    private List<File> capturedFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_scan);

        previewView      = findViewById(R.id.camera_preview);
        btnCapture       = findViewById(R.id.btn_capture);
        btnFromGallery   = findViewById(R.id.btn_from_gallery);
        btnProcess       = findViewById(R.id.btn_process);
        tvInstruction    = findViewById(R.id.tv_instruction);
        tvCount          = findViewById(R.id.tv_count);
        containerScanned = findViewById(R.id.container_scanned);
        layoutProcessing = findViewById(R.id.layout_processing);

        if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
        btnProcess.setEnabled(false);
        updateCount();

        // Selecteaza test
        selectTest();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        btnCapture.setOnClickListener(v -> captureNextSheet());
        btnFromGallery.setOnClickListener(v -> openGallery());
        btnProcess.setOnClickListener(v -> processAllSheets());
    }

    private void selectTest() {
        RetrofitClient.getApiService().getTests()
                .enqueue(new Callback<TestListResponse>() {
                    @Override
                    public void onResponse(Call<TestListResponse> call, Response<TestListResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(BatchScanActivity.this, "Eroare incarcare teste",
                                    Toast.LENGTH_SHORT).show();
                            finish(); return;
                        }
                        List<TestListResponse.Test> tests = response.body().tests;
                        if (tests == null || tests.isEmpty()) {
                            Toast.makeText(BatchScanActivity.this, "Niciun test disponibil",
                                    Toast.LENGTH_SHORT).show();
                            finish(); return;
                        }

                        String[] names = new String[tests.size()];
                        int[] ids = new int[tests.size()];
                        for (int i = 0; i < tests.size(); i++) {
                            names[i] = tests.get(i).test_name;
                            ids[i] = tests.get(i).id;
                        }

                        new AlertDialog.Builder(BatchScanActivity.this)
                                .setTitle("Selecteaza testul pentru batch scan")
                                .setItems(names, (d, which) -> {
                                    selectedTestId = ids[which];
                                    selectedTestName = names[which];
                                    tvInstruction.setText("Test: " + selectedTestName
                                            + "\nFotografiaza foile sau selecteaza din galerie.");
                                })
                                .setNegativeButton("Anuleaza", (d, w) -> finish())
                                .setCancelable(false)
                                .show();
                    }

                    @Override
                    public void onFailure(Call<TestListResponse> call, Throwable t) {
                        Toast.makeText(BatchScanActivity.this, "Eroare conexiune", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
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

    private void captureNextSheet() {
        if (imageCapture == null || selectedTestId == -1) {
            Toast.makeText(this, "Selecteaza testul mai intai", Toast.LENGTH_SHORT).show();
            return;
        }
        askStudentId(sheetIndex -> {
            btnCapture.setEnabled(false);
            imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull androidx.camera.core.ImageProxy image) {
                            Bitmap bmp = imageProxyToBitmap(image);
                            image.close();
                            if (bmp != null) {
                                File f = saveBitmap(bmp, "batch_" + System.currentTimeMillis());
                                if (f != null) {
                                    capturedFiles.add(f);
                                    studentIds.add(sheetIndex);
                                    addSheetRow("Foaie " + capturedFiles.size()
                                            + " (student " + sheetIndex + ")", true);
                                    updateCount();
                                }
                            }
                            btnCapture.setEnabled(true);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException e) {
                            btnCapture.setEnabled(true);
                            Toast.makeText(BatchScanActivity.this,
                                    "Eroare captura", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void openGallery() {
        if (selectedTestId == -1) {
            Toast.makeText(this, "Selecteaza testul mai intai", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, GALLERY_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK_CODE && resultCode == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }

            if (uris.isEmpty()) return;

            // Cere ID studentilor pentru imaginile selectate
            askStudentIdsForUris(uris, 0);
        }
    }

    private void askStudentIdsForUris(List<Uri> uris, int index) {
        if (index >= uris.size()) {
            updateCount();
            return;
        }

        Uri uri = uris.get(index);
        askStudentId(studentId -> {
            File f = saveUri(uri, "gallery_" + System.currentTimeMillis());
            if (f != null) {
                capturedFiles.add(f);
                studentIds.add(studentId);
                addSheetRow("Galerie " + (index + 1) + " (student " + studentId + ")", false);
            }
            askStudentIdsForUris(uris, index + 1);
        });
    }

    private void askStudentId(StudentIdCallback callback) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("ID student");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(0xFFFAFAFA);
        et.setHintTextColor(0xFF52525B);
        et.setPadding(32, 16, 32, 16);

        new AlertDialog.Builder(this)
                .setTitle("Foaia " + (capturedFiles.size() + 1))
                .setMessage("ID-ul studentului pentru aceasta foaie:")
                .setView(et)
                .setPositiveButton("OK", (d, w) -> {
                    String s = et.getText().toString().trim();
                    if (!s.isEmpty()) callback.onId(Integer.parseInt(s));
                })
                .setNegativeButton("Skip", null)
                .show();
    }

    interface StudentIdCallback {
        void onId(int id);
    }

    private void processAllSheets() {
        if (capturedFiles.isEmpty()) {
            Toast.makeText(this, "Nu ai adaugat nicio foaie", Toast.LENGTH_SHORT).show();
            return;
        }
        if (capturedFiles.size() != studentIds.size()) {
            Toast.makeText(this, "Numarul de foi nu corespunde cu numarul de studenti",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnProcess.setEnabled(false);
        btnCapture.setEnabled(false);
        btnFromGallery.setEnabled(false);
        if (layoutProcessing != null) layoutProcessing.setVisibility(View.VISIBLE);
        tvInstruction.setText("Se proceseaza " + capturedFiles.size() + " foi...");

        // Construieste multipart request
        try {
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            for (File f : capturedFiles) {
                RequestBody imageBody = RequestBody.create(MediaType.parse("image/jpeg"), f);
                builder.addFormDataPart("images", f.getName(), imageBody);
            }

            builder.addFormDataPart("test_id", String.valueOf(selectedTestId));
            builder.addFormDataPart("student_ids",
                    new JSONArray(studentIds).toString());

            RetrofitClient.getApiService()
                    .batchScan(
                            capturedFiles.stream().map(f -> {
                                RequestBody rb = RequestBody.create(MediaType.parse("image/jpeg"), f);
                                return MultipartBody.Part.createFormData("images", f.getName(), rb);
                            }).collect(java.util.stream.Collectors.toList()),
                            RequestBody.create(MediaType.parse("text/plain"),
                                    String.valueOf(selectedTestId)),
                            RequestBody.create(MediaType.parse("text/plain"),
                                    new JSONArray(studentIds).toString())
                    )
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
                            btnProcess.setEnabled(true);
                            btnCapture.setEnabled(true);
                            btnFromGallery.setEnabled(true);

                            if (!response.isSuccessful() || response.body() == null) {
                                Toast.makeText(BatchScanActivity.this,
                                        "Eroare server", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                String raw = response.body().string();
                                JSONObject json = new JSONObject(raw);
                                if (json.optBoolean("success", false)) {
                                    showBatchResults(json);
                                } else {
                                    Toast.makeText(BatchScanActivity.this,
                                            json.optString("error", "Eroare"),
                                            Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(BatchScanActivity.this,
                                        "Eroare procesare", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
                            btnProcess.setEnabled(true);
                            btnCapture.setEnabled(true);
                            btnFromGallery.setEnabled(true);
                            Toast.makeText(BatchScanActivity.this,
                                    "Eroare: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (Exception e) {
            if (layoutProcessing != null) layoutProcessing.setVisibility(View.GONE);
            Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showBatchResults(JSONObject json) {
        int processed  = json.optInt("processed", 0);
        int successful = json.optInt("successful", 0);
        int failed     = json.optInt("failed", 0);
        JSONArray results = json.optJSONArray("results");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Procesate: %d\nReușite: %d\nEșuate: %d\n\n",
                processed, successful, failed));

        if (results != null) {
            for (int i = 0; i < results.length(); i++) {
                try {
                    JSONObject r = results.getJSONObject(i);
                    int sid = r.optInt("student_id", 0);
                    if (r.optBoolean("success", false)) {
                        double score = r.optDouble("total_score", 0);
                        sb.append(String.format("Student %d: %.1f%%\n", sid, score));
                    } else {
                        sb.append(String.format("Student %d: EROARE\n", sid));
                    }
                } catch (Exception e) { /* skip */ }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Batch scan complet!")
                .setMessage(sb.toString())
                .setPositiveButton("Alta sesiune", (d, w) -> {
                    capturedFiles.clear();
                    studentIds.clear();
                    containerScanned.removeAllViews();
                    updateCount();
                })
                .setNegativeButton("Gata", (d, w) -> finish())
                .show();
    }

    private void addSheetRow(String label, boolean isCamera) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 12, 16, 12);
        row.setBackgroundColor(Color.parseColor("#18181B"));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 4);
        row.setLayoutParams(rp);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(isCamera ? "📷" : "🖼");
        tvIcon.setTextSize(16f);
        tvIcon.setPadding(0, 0, 12, 0);
        row.addView(tvIcon);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#D4D4D8"));
        tvLabel.setTextSize(13f);
        row.addView(tvLabel);

        containerScanned.addView(row);
    }

    private void updateCount() {
        int n = capturedFiles.size();
        tvCount.setText(n + (n == 1 ? " foaie adăugată" : " foi adăugate"));
        btnProcess.setEnabled(n > 0);
    }

    private Bitmap imageProxyToBitmap(androidx.camera.core.ImageProxy image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private File saveBitmap(Bitmap bmp, String name) {
        try {
            int maxW = 1600;
            if (bmp.getWidth() > maxW) {
                float r = (float) maxW / bmp.getWidth();
                bmp = Bitmap.createScaledBitmap(bmp, maxW, (int)(bmp.getHeight()*r), true);
            }
            File f = new File(getCacheDir(), name + ".jpg");
            FileOutputStream fos = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return f;
        } catch (Exception e) { return null; }
    }

    private File saveUri(Uri uri, String name) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp != null ? saveBitmap(bmp, name) : null;
        } catch (Exception e) { return null; }
    }
}
