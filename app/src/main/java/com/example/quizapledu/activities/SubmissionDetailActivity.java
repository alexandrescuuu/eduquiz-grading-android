package com.example.quizapledu.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.SubmissionDetail;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class SubmissionDetailActivity extends AppCompatActivity {

    private TextView tvTestName, tvStudentName, tvTotalScore, tvDate;
    private LinearLayout containerAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_detail);

        int submissionId = getIntent().getIntExtra("submission_id", -1);
        String studentName = getIntent().getStringExtra("student_name");
        String testName    = getIntent().getStringExtra("test_name");

        tvTestName     = findViewById(R.id.tv_test_name);
        tvStudentName  = findViewById(R.id.tv_student_name);
        tvTotalScore   = findViewById(R.id.tv_total_score);
        tvDate         = findViewById(R.id.tv_submitted_date);
        containerAnswers = findViewById(R.id.container_answers);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvTestName.setText(testName != null ? testName : "Test");
        tvStudentName.setText(studentName != null ? studentName : "Student");

        if (submissionId != -1) {
            loadDetails(submissionId);
        } else {
            Toast.makeText(this, "ID submisie invalid", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadDetails(int submissionId) {
        RetrofitClient.getApiService().getSubmissionDetails(submissionId)
                .enqueue(new Callback<SubmissionDetail>() {
                    @Override
                    public void onResponse(Call<SubmissionDetail> call, Response<SubmissionDetail> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        SubmissionDetail data = response.body();
                        displayDetails(data);
                    }

                    @Override
                    public void onFailure(Call<SubmissionDetail> call, Throwable t) {
                        Toast.makeText(SubmissionDetailActivity.this,
                                "Eroare: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayDetails(SubmissionDetail data) {
        SubmissionDetail.Submission sub = data.getSubmission();

        double score = sub.getTotalScore();
        tvTotalScore.setText(String.format("%.1f%%", score));

        int scoreColor = score >= 80 ? Color.parseColor("#34D399")
                : score >= 60 ? Color.parseColor("#FBBF24")
                : Color.parseColor("#F87171");
        tvTotalScore.setTextColor(scoreColor);

        // Format data
        String dateStr = sub.getSubmittedAt();
        if (dateStr != null && dateStr.length() >= 10) dateStr = dateStr.substring(0, 10);
        tvDate.setText(dateStr != null ? dateStr : "--");

        // Raspunsuri
        List<SubmissionDetail.Answer> answers = data.getAnswers();
        containerAnswers.removeAllViews();

        if (answers == null || answers.isEmpty()) return;

        for (int i = 0; i < answers.size(); i++) {
            SubmissionDetail.Answer ans = answers.get(i);
            addAnswerCard(i + 1, ans);
        }
    }

    private void addAnswerCard(int num, SubmissionDetail.Answer ans) {
        double score   = ans.getAiScore();
        double maxPts  = ans.getPoints();
        double pct     = maxPts > 0 ? (score / maxPts) * 100 : 0;

        int scoreColor = pct >= 80 ? Color.parseColor("#34D399")
                : pct >= 50 ? Color.parseColor("#FBBF24")
                : Color.parseColor("#F87171");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundColor(Color.parseColor("#18181B"));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cp);

        // Header: numar + scor
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvNum = new TextView(this);
        tvNum.setText("Întrebarea " + num);
        tvNum.setTextColor(Color.parseColor("#A78BFA"));
        tvNum.setTextSize(12f);
        tvNum.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvNum.setLayoutParams(np);
        header.addView(tvNum);

        TextView tvScore = new TextView(this);
        tvScore.setText(String.format("%.1f/%.0f", score, maxPts));
        tvScore.setTextColor(scoreColor);
        tvScore.setTextSize(13f);
        tvScore.setTypeface(null, Typeface.BOLD);
        header.addView(tvScore);

        card.addView(header);

        // Intrebarea
        addLabel(card, "Întrebare", "#52525B");
        addValue(card, ans.getQuestionText(), "#FAFAFA");

        // Raspunsul elevului
        addLabel(card, "Răspuns elev", "#52525B");
        String studentAns = ans.getStudentAnswer();
        if (studentAns == null || studentAns.trim().isEmpty()) studentAns = "(fără răspuns)";
        addValue(card, studentAns, "#D4D4D8");

        // Raspunsul corect
        addLabel(card, "Răspuns corect", "#52525B");
        addValue(card, ans.getCorrectAnswer(), "#34D399");

        // Feedback AI
        String feedback = ans.getAiFeedback();
        if (feedback != null && !feedback.trim().isEmpty()) {
            addLabel(card, "Feedback AI", "#52525B");
            addValue(card, feedback, "#A78BFA");
        }

        containerAnswers.addView(card);
    }

    private void addLabel(LinearLayout parent, String text, String color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        tv.setTextSize(10f);
        tv.setAllCaps(true);
        tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 12, 0, 2);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private void addValue(LinearLayout parent, String text, String color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor(color));
        tv.setTextSize(13f);
        parent.addView(tv);
    }
}
