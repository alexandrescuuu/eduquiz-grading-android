package com.example.quizapledu.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.Question;
import com.example.quizapledu.api.models.QuestionsResponse;
import com.example.quizapledu.api.models.SubmitRequest;
import com.example.quizapledu.api.models.SubmitResponse;
import com.example.quizapledu.utils.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestSubmissionActivity extends AppCompatActivity {

    private int testId;
    private String testName;
    private LinearLayout questionsContainer;
    private Button btnSubmit;
    private TextView tvTimer, tvTotalPoints;
    private Map<Integer, Object> answerViews = new HashMap<>(); // EditText, RadioGroup, or scan result
    private Map<Integer, String> scanAnswers = new HashMap<>(); // For scan_answer type
    private List<Question> questionsList;
    private CountDownTimer timer;
    private int totalPoints = 0;

    private static final int SCAN_REQUEST_CODE = 100;
    private int currentScanQuestionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_submission);

        testId = getIntent().getIntExtra("test_id", 0);
        testName = getIntent().getStringExtra("test_name");

        TextView tvTestName = findViewById(R.id.tv_test_name);
        tvTestName.setText(testName);

        tvTimer = findViewById(R.id.tv_timer);
        tvTotalPoints = findViewById(R.id.tv_total_points);
        questionsContainer = findViewById(R.id.questions_container);
        btnSubmit = findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(v -> submitTest());

        startTimer(60); // 60 minute
        loadQuestions();
    }

    private void startTimer(int minutes) {
        long millisInFuture = minutes * 60 * 1000L;

        timer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long mins = (millisUntilFinished / 1000) / 60;
                long secs = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(TestSubmissionActivity.this,
                        "Time's up! Auto-submitting...", Toast.LENGTH_SHORT).show();
                submitTest();
            }
        }.start();
    }

    private void loadQuestions() {
        RetrofitClient.getApiService().getQuestions(testId).enqueue(new Callback<QuestionsResponse>() {
            @Override
            public void onResponse(Call<QuestionsResponse> call, Response<QuestionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    questionsList = response.body().questions;

                    totalPoints = 0;
                    for (Question q : questionsList) {
                        totalPoints += q.getPoints();
                    }
                    tvTotalPoints.setText("Total Points: " + totalPoints);

                    displayQuestions(questionsList);
                }
            }

            @Override
            public void onFailure(Call<QuestionsResponse> call, Throwable t) {
                Toast.makeText(TestSubmissionActivity.this,
                        "Error loading questions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayQuestions(List<Question> questions) {
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);

            // Question header
            TextView tvQuestionHeader = new TextView(this);
            tvQuestionHeader.setText("Question " + (i + 1) + " (" + q.getPoints() + " points)");
            tvQuestionHeader.setTextColor(0xFFA78BFA);
            tvQuestionHeader.setTextSize(14);
            tvQuestionHeader.setTypeface(null, android.graphics.Typeface.BOLD);

            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            headerParams.setMargins(0, 0, 0, 8);
            tvQuestionHeader.setLayoutParams(headerParams);

            // Question type badge
            TextView tvQuestionType = new TextView(this);
            String typeText = "";
            int typeColor = 0xFF52525B;

            if (q.isMultipleChoice()) {
                typeText = " Multiple Choice";
                typeColor = 0xFF34D399;
            } else if (q.isTextAnswer()) {
                typeText = " Text Answer";
                typeColor = 0xFF60A5FA;
            } else if (q.isScanAnswer()) {
                typeText = " Scan Answer";
                typeColor = 0xFFF59E0B;
            }

            tvQuestionType.setText(typeText);
            tvQuestionType.setTextColor(typeColor);
            tvQuestionType.setTextSize(12);

            LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            typeParams.setMargins(0, 0, 0, 12);
            tvQuestionType.setLayoutParams(typeParams);

            // Question text
            TextView tvQuestion = new TextView(this);
            tvQuestion.setText(q.getQuestionText());
            tvQuestion.setTextColor(0xFFFAFAFA);
            tvQuestion.setTextSize(16);

            LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            questionParams.setMargins(0, 0, 0, 16);
            tvQuestion.setLayoutParams(questionParams);

            questionsContainer.addView(tvQuestionHeader);
            questionsContainer.addView(tvQuestionType);
            questionsContainer.addView(tvQuestion);

            // Render different input based on question type
            if (q.isMultipleChoice()) {
                // MULTIPLE CHOICE - RadioGroup
                RadioGroup radioGroup = createMultipleChoiceView(q);
                questionsContainer.addView(radioGroup);
                answerViews.put(q.getId(), radioGroup);

            } else if (q.isTextAnswer()) {
                // TEXT ANSWER - EditText
                EditText editText = createTextAnswerView();
                questionsContainer.addView(editText);
                answerViews.put(q.getId(), editText);

            } else if (q.isScanAnswer()) {
                // SCAN ANSWER - Camera Button
                Button scanButton = createScanAnswerView(q.getId());
                questionsContainer.addView(scanButton);
                answerViews.put(q.getId(), scanButton);
            }
        }
    }

    private RadioGroup createMultipleChoiceView(Question question) {
        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        LinearLayout.LayoutParams radioGroupParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        radioGroupParams.setMargins(0, 0, 0, 32);
        radioGroup.setLayoutParams(radioGroupParams);

        List<String> options = question.getOptions();
        if (options != null && !options.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText(options.get(i));
                radioButton.setTextColor(0xFFFAFAFA);
                radioButton.setTextSize(15);
                radioButton.setPadding(16, 16, 16, 16);
                radioButton.setId(i);
                radioGroup.addView(radioButton);
            }
        }

        return radioGroup;
    }

    private EditText createTextAnswerView() {
        EditText etAnswer = new EditText(this);
        etAnswer.setHint("Enter your answer here... (max 500 characters)");
        etAnswer.setTextColor(0xFFFAFAFA);
        etAnswer.setHintTextColor(0xFF52525B);
        etAnswer.setBackgroundColor(0xFF18181B);
        etAnswer.setPadding(32, 32, 32, 32);
        etAnswer.setMinHeight(200);
        etAnswer.setGravity(android.view.Gravity.TOP);
        etAnswer.setMaxLines(10);

        // Max 500 characters
        android.text.InputFilter[] filters = new android.text.InputFilter[1];
        filters[0] = new android.text.InputFilter.LengthFilter(500);
        etAnswer.setFilters(filters);

        LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        answerParams.setMargins(0, 0, 0, 32);
        etAnswer.setLayoutParams(answerParams);

        return etAnswer;
    }

    private Button createScanAnswerView(int questionId) {
        Button scanButton = new Button(this);
        scanButton.setText(" Scan Handwritten Answer");
        scanButton.setTextColor(0xFFFFFFFF);
        scanButton.setBackgroundColor(0xFF7C3AED);
        scanButton.setPadding(32, 32, 32, 32);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(0, 0, 0, 32);
        scanButton.setLayoutParams(buttonParams);

        scanButton.setOnClickListener(v -> {
            currentScanQuestionId = questionId;
            Intent intent = new Intent(TestSubmissionActivity.this, ScanActivity.class);
            intent.putExtra("test_id", testId);
            intent.putExtra("question_id", questionId);
            startActivityForResult(intent, SCAN_REQUEST_CODE);
        });

        return scanButton;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCAN_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String extractedText = data.getStringExtra("extracted_text");
            if (currentScanQuestionId != -1 && extractedText != null) {
                scanAnswers.put(currentScanQuestionId, extractedText);

                // Update button text to show scan completed
                Object view = answerViews.get(currentScanQuestionId);
                if (view instanceof Button) {
                    ((Button) view).setText(" Scanned! Tap to rescan");
                }

                Toast.makeText(this, "Answer scanned successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitTest() {
        if (timer != null) {
            timer.cancel();
        }

        List<SubmitRequest.Answer> answers = new ArrayList<>();

        for (Question q : questionsList) {
            String answerText = "";

            if (q.isMultipleChoice()) {
                // Get selected radio button
                Object view = answerViews.get(q.getId());
                if (view instanceof RadioGroup) {
                    RadioGroup radioGroup = (RadioGroup) view;
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    if (selectedId != -1) {
                        RadioButton selectedButton = radioGroup.findViewById(selectedId);
                        answerText = selectedButton.getText().toString();
                    }
                }

            } else if (q.isTextAnswer()) {
                // Get text from EditText
                Object view = answerViews.get(q.getId());
                if (view instanceof EditText) {
                    answerText = ((EditText) view).getText().toString().trim();
                }

            } else if (q.isScanAnswer()) {
                // Get scanned text
                answerText = scanAnswers.getOrDefault(q.getId(), "");

                if (answerText.isEmpty()) {
                    Toast.makeText(this, "Please scan answer for Question " +
                            (questionsList.indexOf(q) + 1), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            answers.add(new SubmitRequest.Answer(q.getId(), answerText));
        }

        btnSubmit.setEnabled(false);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        int studentId = prefs.getInt(Constants.KEY_USER_ID, 0);

        SubmitRequest request = new SubmitRequest();
        request.test_id = testId;
        request.student_id = studentId;
        request.answers = answers;

        RetrofitClient.getApiService().submitTest(request).enqueue(new Callback<SubmitResponse>() {
            @Override
            public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> response) {
                btnSubmit.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    SubmitResponse result = response.body();

                    // Construieste feedback combinat din toate intrebarile
                    StringBuilder feedbackBuilder = new StringBuilder();
                    if (result.answers != null) {
                        for (int i = 0; i < result.answers.size(); i++) {
                            SubmitResponse.AnswerResult ans = result.answers.get(i);
                            feedbackBuilder.append("Q").append(i + 1).append(": ");
                            feedbackBuilder.append(ans.feedback != null ? ans.feedback : "—");
                            if (i < result.answers.size() - 1) feedbackBuilder.append("\n");
                        }
                    }
                    String combinedFeedback = feedbackBuilder.length() > 0
                            ? feedbackBuilder.toString()
                            : result.message;

                    if (combinedFeedback == null || combinedFeedback.trim().isEmpty()) {
                        combinedFeedback = "Your answers have been submitted and graded.";
                    }

                    // Navigate to ResultsActivity with submission_id
                    Intent intent = new Intent(TestSubmissionActivity.this, ResultsActivity.class);
                    intent.putExtra("test_name", testName);
                    intent.putExtra("student_answer", "Mixed answer types");
                    intent.putExtra("feedback", combinedFeedback); // <-- schimbat din result.message
                    intent.putExtra("score", result.total_score);
                    intent.putExtra("score_details", result.score_details);
                    intent.putExtra("submission_id", result.submission_id);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(TestSubmissionActivity.this,
                            "Error submitting test", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SubmitResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(TestSubmissionActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}