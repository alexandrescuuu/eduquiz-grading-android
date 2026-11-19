package com.example.quizapledu.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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

public class TestAnalysisActivity extends AppCompatActivity {

    private TextView tvTestName, tvSubtitle;
    private LinearLayout containerQuestions;
    private int testId;
    private String testName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_analysis);

        testId   = getIntent().getIntExtra("test_id", -1);
        testName = getIntent().getStringExtra("test_name");

        tvTestName        = findViewById(R.id.tv_test_name);
        tvSubtitle        = findViewById(R.id.tv_subtitle);
        containerQuestions = findViewById(R.id.container_questions);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTestName.setText(testName != null ? testName : "Analiza test");
        tvSubtitle.setText("Se incarca analiza...");

        if (testId != -1) loadAnalysis();
        else { Toast.makeText(this, "Test invalid", Toast.LENGTH_SHORT).show(); finish(); }
    }

    private void loadAnalysis() {
        RetrofitClient.getApiService().getTestAnalysis(testId)
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
                                tvSubtitle.setText("Nicio submisie gasita");
                                return;
                            }

                            int totalSubs = json.optInt("total_submissions", 0);
                            JSONArray questions = json.optJSONArray("questions_analysis");
                            int qCount = questions != null ? questions.length() : 0;

                            tvSubtitle.setText(qCount + " întrebări · " + totalSubs + " elevi");

                            containerQuestions.removeAllViews();

                            if (questions == null || qCount == 0) {
                                addEmptyState("Nicio întrebare sau submisie găsită.");
                                return;
                            }

                            for (int i = 0; i < questions.length(); i++) {
                                JSONObject q = questions.getJSONObject(i);
                                addQuestionCard(q, i + 1);
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

    private void addQuestionCard(JSONObject q, int num) throws Exception {
        String questionText  = q.optString("question_text", "—");
        String correctAnswer = q.optString("correct_answer", "—");
        int points           = q.optInt("points", 10);
        double avgPct        = q.optDouble("avg_score_pct", -1);
        int wrongCount       = q.optInt("wrong_count", 0);
        int totalAnswers     = q.optInt("total_answers", 0);
        JSONObject worst     = q.optJSONObject("worst_answer");

        // Culoare medie
        int avgColor = avgPct < 0 ? Color.parseColor("#52525B")
                : avgPct >= 80 ? Color.parseColor("#34D399")
                : avgPct >= 55 ? Color.parseColor("#FBBF24")
                : Color.parseColor("#F87171");

        // Card principal
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, 0, 0, 0);
        card.setBackgroundColor(Color.parseColor("#18181B"));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cp);

        // Header: Q + medie
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(20, 20, 20, 16);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Numar intrebare + text
        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftCol.setLayoutParams(lp);

        TextView tvNum = new TextView(this);
        tvNum.setText("ÎNTREBAREA " + num + "  ·  " + points + " pct");
        tvNum.setTextColor(Color.parseColor("#A78BFA"));
        tvNum.setTextSize(10f);
        tvNum.setTypeface(null, Typeface.BOLD);
        leftCol.addView(tvNum);

        TextView tvQ = new TextView(this);
        tvQ.setText(questionText);
        tvQ.setTextColor(Color.parseColor("#FAFAFA"));
        tvQ.setTextSize(14f);
        tvQ.setLineSpacing(2, 1.2f);
        leftCol.addView(tvQ);

        header.addView(leftCol);

        // Medie dreapta
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                80, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(12, 0, 0, 0);
        rightCol.setLayoutParams(rp);

        TextView tvAvg = new TextView(this);
        tvAvg.setText(avgPct >= 0 ? String.format("%.0f%%", avgPct) : "--");
        tvAvg.setTextColor(avgColor);
        tvAvg.setTextSize(22f);
        tvAvg.setTypeface(null, Typeface.BOLD);
        tvAvg.setGravity(android.view.Gravity.CENTER);
        rightCol.addView(tvAvg);

        TextView tvAvgLabel = new TextView(this);
        tvAvgLabel.setText("medie");
        tvAvgLabel.setTextColor(Color.parseColor("#52525B"));
        tvAvgLabel.setTextSize(10f);
        tvAvgLabel.setGravity(android.view.Gravity.CENTER);
        rightCol.addView(tvAvgLabel);

        header.addView(rightCol);
        card.addView(header);

        // Separator
        addSep(card);

        // Stat greseli
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setPadding(20, 12, 20, 12);
        statsRow.setBackgroundColor(Color.parseColor("#111113"));
        statsRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Raspuns corect
        addStatItem(statsRow, "RASPUNS CORECT", correctAnswer, Color.parseColor("#34D399"), 1f);

        // Separator vertical
        View vSep = new View(this);
        LinearLayout.LayoutParams vsp = new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT);
        vsp.setMargins(0, 4, 0, 4);
        vSep.setLayoutParams(vsp);
        vSep.setBackgroundColor(Color.parseColor("#27272A"));
        statsRow.addView(vSep);

        // Greseli
        String wrongLabel = wrongCount + "/" + totalAnswers + " elevi";
        addStatItem(statsRow, "AU GREȘIT (<50%)", wrongLabel,
                wrongCount > 0 ? Color.parseColor("#F87171") : Color.parseColor("#34D399"), 0.6f);

        card.addView(statsRow);

        // Cel mai gresit raspuns
        if (worst != null && totalAnswers > 0) {
            addSep(card);

            LinearLayout worstSection = new LinearLayout(this);
            worstSection.setOrientation(LinearLayout.VERTICAL);
            worstSection.setPadding(20, 16, 20, 16);
            worstSection.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Header worst
            LinearLayout worstHeader = new LinearLayout(this);
            worstHeader.setOrientation(LinearLayout.HORIZONTAL);
            worstHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvWorstLabel = new TextView(this);
            tvWorstLabel.setText("⚠  CEL MAI GREȘIT RĂSPUNS");
            tvWorstLabel.setTextColor(Color.parseColor("#F87171"));
            tvWorstLabel.setTextSize(10f);
            tvWorstLabel.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvWorstLabel.setLayoutParams(wlp);
            worstHeader.addView(tvWorstLabel);

            double worstPct = worst.optDouble("score_pct", 0);
            TextView tvWorstScore = new TextView(this);
            tvWorstScore.setText(String.format("%.0f%%", worstPct));
            tvWorstScore.setTextColor(Color.parseColor("#F87171"));
            tvWorstScore.setTextSize(16f);
            tvWorstScore.setTypeface(null, Typeface.BOLD);
            worstHeader.addView(tvWorstScore);

            worstSection.addView(worstHeader);

            // Numele elevului
            String studentName = worst.optString("student_name", "Elev");
            TextView tvStudentName = new TextView(this);
            tvStudentName.setText(studentName);
            tvStudentName.setTextColor(Color.parseColor("#71717A"));
            tvStudentName.setTextSize(11f);
            LinearLayout.LayoutParams snlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            snlp.setMargins(0, 2, 0, 8);
            tvStudentName.setLayoutParams(snlp);
            worstSection.addView(tvStudentName);

            // Raspunsul elevului
            String studentAnswer = worst.optString("student_answer", "(fără răspuns)");
            if (studentAnswer.trim().isEmpty()) studentAnswer = "(fără răspuns)";

            LinearLayout answerBox = new LinearLayout(this);
            answerBox.setOrientation(LinearLayout.VERTICAL);
            answerBox.setBackgroundColor(Color.parseColor("#1C0A0A"));
            answerBox.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams abp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            abp.setMargins(0, 0, 0, 8);
            answerBox.setLayoutParams(abp);

            TextView tvAnswer = new TextView(this);
            tvAnswer.setText(studentAnswer);
            tvAnswer.setTextColor(Color.parseColor("#FCA5A5"));
            tvAnswer.setTextSize(14f);
            answerBox.addView(tvAnswer);
            worstSection.addView(answerBox);

            // Feedback AI
            String feedback = worst.optString("ai_feedback", "");
            if (!feedback.isEmpty()) {
                TextView tvFeedback = new TextView(this);
                tvFeedback.setText("AI: " + feedback);
                tvFeedback.setTextColor(Color.parseColor("#52525B"));
                tvFeedback.setTextSize(11f);
                tvFeedback.setLineSpacing(2, 1.2f);
                worstSection.addView(tvFeedback);
            }

            card.addView(worstSection);
        } else if (totalAnswers == 0) {
            // Niciun elev nu a raspuns
            LinearLayout noAns = new LinearLayout(this);
            noAns.setPadding(20, 12, 20, 12);
            noAns.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView tvNoAns = new TextView(this);
            tvNoAns.setText("Niciun elev nu a răspuns la această întrebare.");
            tvNoAns.setTextColor(Color.parseColor("#52525B"));
            tvNoAns.setTextSize(13f);
            noAns.addView(tvNoAns);
            card.addView(noAns);
        }

        containerQuestions.addView(card);
    }

    private void addStatItem(LinearLayout parent, String label, String value, int valueColor, float weight) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, 0, 16, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        col.setLayoutParams(lp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#52525B"));
        tvLabel.setTextSize(9f);
        tvLabel.setTypeface(null, Typeface.BOLD);
        col.addView(tvLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(valueColor);
        tvValue.setTextSize(13f);
        tvValue.setTypeface(null, Typeface.BOLD);
        col.addView(tvValue);

        parent.addView(col);
    }

    private void addSep(LinearLayout parent) {
        View sep = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        sep.setLayoutParams(lp);
        sep.setBackgroundColor(Color.parseColor("#27272A"));
        parent.addView(sep);
    }

    private void addEmptyState(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#52525B"));
        tv.setTextSize(14f);
        tv.setPadding(20, 40, 20, 40);
        tv.setGravity(android.view.Gravity.CENTER);
        containerQuestions.addView(tv);
    }
}
