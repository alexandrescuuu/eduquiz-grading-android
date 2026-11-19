package com.example.quizapledu.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class TestDetailsActivity extends AppCompatActivity {

    private TextView tvTestName, tvSubtitle, tvAvg, tvHighest, tvLowest, tvCount;
    private LinearLayout containerSubmissions;
    private int testId;
    private String testName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_details);

        testId   = getIntent().getIntExtra("test_id", -1);
        testName = getIntent().getStringExtra("test_name");

        tvTestName          = findViewById(R.id.tv_test_name);
        tvSubtitle          = findViewById(R.id.tv_subtitle);
        tvAvg               = findViewById(R.id.tv_avg);
        tvHighest           = findViewById(R.id.tv_highest);
        tvLowest            = findViewById(R.id.tv_lowest);
        tvCount             = findViewById(R.id.tv_count);
        containerSubmissions = findViewById(R.id.container_submissions);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvTestName.setText(testName != null ? testName : "Test");
        tvSubtitle.setText("Se incarca rezultatele...");

        if (testId != -1) {
            loadResults();
        } else {
            Toast.makeText(this, "Test invalid", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadResults() {
        RetrofitClient.getApiService().getTestResults(testId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            tvSubtitle.setText("Eroare la incarcare");
                            return;
                        }
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) {
                                tvSubtitle.setText("Niciun rezultat");
                                return;
                            }

                            JSONArray results = json.optJSONArray("results");
                            int count = results != null ? results.length() : 0;

                            tvSubtitle.setText(count + (count == 1 ? " elev" : " elevi") + " au rezolvat");

                            if (count == 0) {
                                tvAvg.setText("--");
                                tvHighest.setText("--");
                                tvLowest.setText("--");
                                tvCount.setText("0");
                                return;
                            }

                            // Calcul statistici
                            double sum = 0, max = -1, min = 101;
                            for (int i = 0; i < count; i++) {
                                double score = results.getJSONObject(i).optDouble("total_score", 0);
                                sum += score;
                                if (score > max) max = score;
                                if (score < min) min = score;
                            }
                            double avg = sum / count;

                            tvAvg.setText(String.format("%.0f%%", avg));
                            tvHighest.setText(String.format("%.0f%%", max));
                            tvLowest.setText(String.format("%.0f%%", min));
                            tvCount.setText(String.valueOf(count));

                            // Coloreaza media
                            int avgColor = avg >= 80 ? Color.parseColor("#34D399")
                                    : avg >= 60 ? Color.parseColor("#FBBF24")
                                    : Color.parseColor("#F87171");
                            tvAvg.setTextColor(avgColor);

                            // Populeaza lista submisii
                            containerSubmissions.removeAllViews();
                            for (int i = 0; i < count; i++) {
                                JSONObject sub = results.getJSONObject(i);
                                addSubmissionRow(sub, i + 1);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            tvSubtitle.setText("Eroare procesare date");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvSubtitle.setText("Eroare conexiune");
                    }
                });
    }

    private void addSubmissionRow(JSONObject sub, int rank) {
        int submissionId = sub.optInt("submission_id", -1);
        String studentName = sub.optString("student_name", "Elev");
        double score = sub.optDouble("total_score", 0);
        String date = sub.optString("submitted_at", "");

        // Format data
        String dateStr = date;
        if (date.length() >= 10) dateStr = date.substring(0, 10);

        int scoreColor = score >= 80 ? Color.parseColor("#34D399")
                : score >= 60 ? Color.parseColor("#FBBF24")
                : Color.parseColor("#F87171");

        // Row container
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 18, 20, 18);
        row.setBackgroundColor(Color.parseColor("#18181B"));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 6);
        row.setLayoutParams(rp);

        // Rank
        TextView tvRank = new TextView(this);
        tvRank.setText(String.valueOf(rank));
        tvRank.setTextColor(rank == 1 ? Color.parseColor("#FBBF24") : Color.parseColor("#52525B"));
        tvRank.setTextSize(13f);
        tvRank.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams rankP = new LinearLayout.LayoutParams(36, LinearLayout.LayoutParams.WRAP_CONTENT);
        rankP.setMargins(0, 0, 12, 0);
        tvRank.setLayoutParams(rankP);
        row.addView(tvRank);

        // Avatar
        String[] parts = studentName.split(" ");
        StringBuilder ini = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) ini.append(p.charAt(0));
        String initials = ini.toString().toUpperCase();

        TextView tvAvatar = new TextView(this);
        tvAvatar.setText(initials.length() >= 2 ? initials.substring(0, 2) : initials);
        tvAvatar.setTextColor(Color.parseColor("#A78BFA"));
        tvAvatar.setTextSize(11f);
        tvAvatar.setTypeface(null, Typeface.BOLD);
        tvAvatar.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(42, 42);
        avP.setMargins(0, 0, 14, 0);
        tvAvatar.setLayoutParams(avP);
        tvAvatar.setBackgroundColor(Color.parseColor("#2D2250"));
        row.addView(tvAvatar);

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(ip);

        TextView tvName = new TextView(this);
        tvName.setText(studentName);
        tvName.setTextColor(Color.parseColor("#FAFAFA"));
        tvName.setTextSize(14f);
        info.addView(tvName);

        TextView tvDate = new TextView(this);
        tvDate.setText(dateStr);
        tvDate.setTextColor(Color.parseColor("#52525B"));
        tvDate.setTextSize(11f);
        info.addView(tvDate);

        row.addView(info);

        // Score
        TextView tvScore = new TextView(this);
        tvScore.setText(String.format("%.0f%%", score));
        tvScore.setTextColor(scoreColor);
        tvScore.setTextSize(18f);
        tvScore.setTypeface(null, Typeface.BOLD);
        tvScore.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(tvScore);

        // Click → detalii submisie
        row.setClickable(true);
        row.setFocusable(true);
        int finalSubmissionId = submissionId;
        row.setOnClickListener(v -> {
            if (finalSubmissionId != -1) {
                Intent intent = new Intent(this, SubmissionDetailActivity.class);
                intent.putExtra("submission_id", finalSubmissionId);
                intent.putExtra("student_name", studentName);
                intent.putExtra("test_name", testName);
                startActivity(intent);
            }
        });

        containerSubmissions.addView(row);

        // Separator
        View sep = new View(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        sep.setLayoutParams(sp);
        sep.setBackgroundColor(Color.parseColor("#27272A"));
        containerSubmissions.addView(sep);
    }
}
