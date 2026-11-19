package com.example.quizapledu.activities;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.TestCreateRequest;
import com.example.quizapledu.api.models.TestCreateResponse;
import com.example.quizapledu.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestCreationActivity extends AppCompatActivity {

    private EditText etTestName;
    private LinearLayout questionsContainer;
    private Button btnCreateTest;
    private LinearLayout btnAddQuestion;

    // Lista cu datele fiecarei intrebari
    private final List<QuestionData> questionDataList = new ArrayList<>();
    private int questionCount = 0;

    // Tipuri de intrebari
    private static final int TYPE_MCQ = 0;
    private static final int TYPE_TEXT = 1;
    private static final int TYPE_SCAN = 2;

    // Inner class pentru datele unei intrebari
    static class QuestionData {
        int type = TYPE_TEXT;
        EditText etQuestion;
        EditText etAnswer;         // pentru text si scan
        EditText etPoints;
        // MCQ
        EditText etOption1, etOption2, etOption3, etOption4;
        EditText etCorrectOption;  // litera corecta: A, B, C sau D
        RadioGroup typeSelector;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_creation);

        etTestName = findViewById(R.id.et_test_name);
        questionsContainer = findViewById(R.id.questions_container);
        btnCreateTest = findViewById(R.id.btn_create_test);
        btnAddQuestion = findViewById(R.id.btn_add_question);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Adauga prima intrebare automat
        addQuestion();

        btnAddQuestion.setOnClickListener(v -> addQuestion());
        btnCreateTest.setOnClickListener(v -> createTest());
    }

    private void addQuestion() {
        questionCount++;
        QuestionData data = new QuestionData();
        questionDataList.add(data);

        // Card container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(40, 40, 40, 40);
        card.setBackground(getResources().getDrawable(R.drawable.bg_card, null));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 32);
        card.setLayoutParams(cardParams);

        // Header intrebare (numar + buton stergere)
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, 24);
        header.setLayoutParams(headerParams);

        TextView tvNumber = new TextView(this);
        tvNumber.setText("Question " + questionCount);
        tvNumber.setTextColor(0xFFA78BFA);
        tvNumber.setTextSize(15f);
        tvNumber.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvNumber.setLayoutParams(tvParams);
        header.addView(tvNumber);

        // Buton stergere (doar daca e mai mult de 1 intrebare)
        Button btnDelete = new Button(this);
        btnDelete.setText("Remove");
        btnDelete.setTextColor(0xFFF87171);
        btnDelete.setTextSize(12f);
        btnDelete.setBackgroundColor(0x00000000);
        int idx = questionDataList.size() - 1;
        btnDelete.setOnClickListener(v -> removeQuestion(card, idx));
        header.addView(btnDelete);

        card.addView(header);

        // ---- SELECTOR TIP INTREBARE ----
        TextView tvTypeLabel = new TextView(this);
        tvTypeLabel.setText("Question type");
        tvTypeLabel.setTextColor(0xFF71717A);
        tvTypeLabel.setTextSize(12f);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(0, 0, 0, 12);
        tvTypeLabel.setLayoutParams(labelParams);
        card.addView(tvTypeLabel);

        // 3 butoane tip (MCQ / Text / Scan)
        LinearLayout typeRow = new LinearLayout(this);
        typeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams typeRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        typeRowParams.setMargins(0, 0, 0, 24);
        typeRow.setLayoutParams(typeRowParams);

        Button btnMCQ = createTypeButton("Multiple Choice", true);
        Button btnText = createTypeButton("Text Answer", false);
        Button btnScan = createTypeButton("Scan Answer", false);

        LinearLayout.LayoutParams btnTypeParams = new LinearLayout.LayoutParams(
                0, 80, 1f);
        btnTypeParams.setMargins(0, 0, 8, 0);
        btnMCQ.setLayoutParams(btnTypeParams);
        btnText.setLayoutParams(btnTypeParams);
        btnScan.setLayoutParams(btnTypeParams);

        typeRow.addView(btnMCQ);
        typeRow.addView(btnText);
        typeRow.addView(btnScan);
        card.addView(typeRow);

        // ---- CAMPURI INTREBARE ----
        // Textul intrebarii (comun pentru toate tipurile)
        TextView tvQuestion = new TextView(this);
        tvQuestion.setText("Question text");
        tvQuestion.setTextColor(0xFF71717A);
        tvQuestion.setTextSize(12f);
        LinearLayout.LayoutParams tvQParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tvQParams.setMargins(0, 0, 0, 8);
        tvQuestion.setLayoutParams(tvQParams);
        card.addView(tvQuestion);

        EditText etQuestion = new EditText(this);
        etQuestion.setHint("Enter your question here...");
        etQuestion.setTextColor(0xFFFAFAFA);
        etQuestion.setHintTextColor(0xFF52525B);
        etQuestion.setBackground(getResources().getDrawable(R.drawable.bg_card_border_purple, null));
        etQuestion.setPadding(32, 28, 32, 28);
        etQuestion.setMinHeight(160);
        etQuestion.setGravity(Gravity.TOP);
        etQuestion.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams etQParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        etQParams.setMargins(0, 0, 0, 20);
        etQuestion.setLayoutParams(etQParams);
        card.addView(etQuestion);
        data.etQuestion = etQuestion;

        // ---- SECTIUNEA MCQ (optiuni A B C D) ----
        LinearLayout mcqSection = new LinearLayout(this);
        mcqSection.setOrientation(LinearLayout.VERTICAL);
        mcqSection.setVisibility(View.VISIBLE); // MCQ e implicit
        card.addView(mcqSection);

        String[] optionLabels = {"Option A", "Option B", "Option C", "Option D"};
        EditText[] optionFields = new EditText[4];
        for (int i = 0; i < 4; i++) {
            TextView tvOpt = new TextView(this);
            tvOpt.setText(optionLabels[i]);
            tvOpt.setTextColor(0xFF71717A);
            tvOpt.setTextSize(12f);
            LinearLayout.LayoutParams tvOptParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            tvOptParams.setMargins(0, 0, 0, 8);
            tvOpt.setLayoutParams(tvOptParams);
            mcqSection.addView(tvOpt);

            EditText etOpt = new EditText(this);
            etOpt.setHint("Enter option " + (char)('A' + i));
            etOpt.setTextColor(0xFFFAFAFA);
            etOpt.setHintTextColor(0xFF52525B);
            etOpt.setBackground(getResources().getDrawable(R.drawable.bg_card, null));
            etOpt.setPadding(32, 24, 32, 24);
            etOpt.setSingleLine(true);
            LinearLayout.LayoutParams etOptParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            etOptParams.setMargins(0, 0, 0, 16);
            etOpt.setLayoutParams(etOptParams);
            mcqSection.addView(etOpt);
            optionFields[i] = etOpt;
        }
        data.etOption1 = optionFields[0];
        data.etOption2 = optionFields[1];
        data.etOption3 = optionFields[2];
        data.etOption4 = optionFields[3];

        // Raspuns corect MCQ (A / B / C / D)
        TextView tvCorrectOpt = new TextView(this);
        tvCorrectOpt.setText("Correct answer (A, B, C or D)");
        tvCorrectOpt.setTextColor(0xFF71717A);
        tvCorrectOpt.setTextSize(12f);
        LinearLayout.LayoutParams tvCOParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tvCOParams.setMargins(0, 0, 0, 8);
        tvCorrectOpt.setLayoutParams(tvCOParams);
        mcqSection.addView(tvCorrectOpt);

        EditText etCorrectOpt = new EditText(this);
        etCorrectOpt.setHint("e.g. B");
        etCorrectOpt.setTextColor(0xFFFAFAFA);
        etCorrectOpt.setHintTextColor(0xFF52525B);
        etCorrectOpt.setBackground(getResources().getDrawable(R.drawable.bg_card, null));
        etCorrectOpt.setPadding(32, 24, 32, 24);
        etCorrectOpt.setSingleLine(true);
        etCorrectOpt.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        LinearLayout.LayoutParams etCOParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        etCOParams.setMargins(0, 0, 0, 16);
        etCorrectOpt.setLayoutParams(etCOParams);
        mcqSection.addView(etCorrectOpt);
        data.etCorrectOption = etCorrectOpt;

        // ---- SECTIUNEA TEXT/SCAN (raspuns liber) ----
        LinearLayout answerSection = new LinearLayout(this);
        answerSection.setOrientation(LinearLayout.VERTICAL);
        answerSection.setVisibility(View.GONE); // ascuns initial
        card.addView(answerSection);

        TextView tvAnswer = new TextView(this);
        tvAnswer.setText("Model answer (answer key)");
        tvAnswer.setTextColor(0xFF71717A);
        tvAnswer.setTextSize(12f);
        LinearLayout.LayoutParams tvAParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tvAParams.setMargins(0, 0, 0, 8);
        tvAnswer.setLayoutParams(tvAParams);
        answerSection.addView(tvAnswer);

        EditText etAnswer = new EditText(this);
        etAnswer.setHint("Enter the model answer for AI grading...");
        etAnswer.setTextColor(0xFFFAFAFA);
        etAnswer.setHintTextColor(0xFF52525B);
        etAnswer.setBackground(getResources().getDrawable(R.drawable.bg_card_border_purple, null));
        etAnswer.setPadding(32, 28, 32, 28);
        etAnswer.setMinHeight(160);
        etAnswer.setGravity(Gravity.TOP);
        etAnswer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams etAParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        etAParams.setMargins(0, 0, 0, 16);
        etAnswer.setLayoutParams(etAParams);
        answerSection.addView(etAnswer);
        data.etAnswer = etAnswer;

        // ---- PUNCTAJ ----
        TextView tvPoints = new TextView(this);
        tvPoints.setText("Points");
        tvPoints.setTextColor(0xFF71717A);
        tvPoints.setTextSize(12f);
        LinearLayout.LayoutParams tvPParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tvPParams.setMargins(0, 16, 0, 8);
        tvPoints.setLayoutParams(tvPParams);
        card.addView(tvPoints);

        EditText etPoints = new EditText(this);
        etPoints.setHint("10");
        etPoints.setText("10");
        etPoints.setTextColor(0xFFFAFAFA);
        etPoints.setHintTextColor(0xFF52525B);
        etPoints.setBackground(getResources().getDrawable(R.drawable.bg_card, null));
        etPoints.setPadding(32, 24, 32, 24);
        etPoints.setSingleLine(true);
        etPoints.setInputType(InputType.TYPE_CLASS_NUMBER);
        LinearLayout.LayoutParams etPParams = new LinearLayout.LayoutParams(
                200, ViewGroup.LayoutParams.WRAP_CONTENT);
        etPParams.setMargins(0, 0, 0, 0);
        etPoints.setLayoutParams(etPParams);
        card.addView(etPoints);
        data.etPoints = etPoints;

        // ---- LOGICA SELECTOR TIP ----
        data.type = TYPE_MCQ;

        btnMCQ.setOnClickListener(v -> {
            data.type = TYPE_MCQ;
            setTypeButtonActive(btnMCQ, btnText, btnScan);
            mcqSection.setVisibility(View.VISIBLE);
            answerSection.setVisibility(View.GONE);
        });

        btnText.setOnClickListener(v -> {
            data.type = TYPE_TEXT;
            setTypeButtonActive(btnText, btnMCQ, btnScan);
            mcqSection.setVisibility(View.GONE);
            answerSection.setVisibility(View.VISIBLE);
        });

        btnScan.setOnClickListener(v -> {
            data.type = TYPE_SCAN;
            setTypeButtonActive(btnScan, btnMCQ, btnText);
            mcqSection.setVisibility(View.GONE);
            answerSection.setVisibility(View.VISIBLE);
        });

        questionsContainer.addView(card);
    }

    private Button createTypeButton(String text, boolean active) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11f);
        btn.setPadding(8, 0, 8, 0);
        if (active) {
            btn.setBackgroundColor(0xFFA78BFA);
            btn.setTextColor(0xFF09090B);
        } else {
            btn.setBackgroundColor(0xFF27272A);
            btn.setTextColor(0xFF71717A);
        }
        return btn;
    }

    private void setTypeButtonActive(Button active, Button b2, Button b3) {
        active.setBackgroundColor(0xFFA78BFA);
        active.setTextColor(0xFF09090B);
        b2.setBackgroundColor(0xFF27272A);
        b2.setTextColor(0xFF71717A);
        b3.setBackgroundColor(0xFF27272A);
        b3.setTextColor(0xFF71717A);
    }

    private void removeQuestion(LinearLayout card, int idx) {
        if (questionDataList.size() <= 1) {
            Toast.makeText(this, "You need at least one question", Toast.LENGTH_SHORT).show();
            return;
        }
        questionsContainer.removeView(card);
        questionDataList.remove(idx);
    }

    private void createTest() {
        String testName = etTestName.getText().toString().trim();
        if (testName.isEmpty()) {
            Toast.makeText(this, "Please enter test name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (questionDataList.isEmpty()) {
            Toast.makeText(this, "Please add at least one question", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<TestCreateRequest.Question> questions = new ArrayList<>();

        for (int i = 0; i < questionDataList.size(); i++) {
            QuestionData d = questionDataList.get(i);
            String questionText = d.etQuestion.getText().toString().trim();
            if (questionText.isEmpty()) {
                Toast.makeText(this, "Question " + (i + 1) + " text is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int points = 10;
            try {
                points = Integer.parseInt(d.etPoints.getText().toString().trim());
            } catch (NumberFormatException e) {
                points = 10;
            }

            if (d.type == TYPE_MCQ) {
                // Valideaza optiunile
                String opt1 = d.etOption1.getText().toString().trim();
                String opt2 = d.etOption2.getText().toString().trim();
                String opt3 = d.etOption3.getText().toString().trim();
                String opt4 = d.etOption4.getText().toString().trim();
                String correctLetter = d.etCorrectOption.getText().toString().trim().toUpperCase();

                if (opt1.isEmpty() || opt2.isEmpty()) {
                    Toast.makeText(this, "Question " + (i+1) + ": add at least 2 options", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!correctLetter.matches("[ABCD]")) {
                    Toast.makeText(this, "Question " + (i+1) + ": correct answer must be A, B, C or D", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Construieste lista optiuni
                List<String> options = new ArrayList<>();
                options.add(opt1);
                options.add(opt2);
                if (!opt3.isEmpty()) options.add(opt3);
                if (!opt4.isEmpty()) options.add(opt4);

                // Afla raspunsul corect din litera
                int correctIdx = correctLetter.charAt(0) - 'A';
                if (correctIdx >= options.size()) {
                    Toast.makeText(this, "Question " + (i+1) + ": correct option doesn't exist", Toast.LENGTH_SHORT).show();
                    return;
                }
                String correctAnswer = options.get(correctIdx);

                questions.add(new TestCreateRequest.Question(questionText, correctAnswer, points, options));

            } else {
                // Text Answer sau Scan Answer
                String answer = d.etAnswer.getText().toString().trim();
                if (answer.isEmpty()) {
                    Toast.makeText(this, "Question " + (i+1) + ": model answer is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                String type = (d.type == TYPE_SCAN) ? "scan_answer" : "text_answer";
                questions.add(new TestCreateRequest.Question(questionText, answer, points, type));
            }
        }

        btnCreateTest.setEnabled(false);
        btnCreateTest.setText("Creating...");

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        int teacherId = prefs.getInt(Constants.KEY_USER_ID, 1);

        TestCreateRequest request = new TestCreateRequest();
        request.teacher_id = teacherId;
        request.test_name = testName;
        request.questions = questions;

        RetrofitClient.getApiService().createTest(request).enqueue(new Callback<TestCreateResponse>() {
            @Override
            public void onResponse(Call<TestCreateResponse> call, Response<TestCreateResponse> response) {
                btnCreateTest.setEnabled(true);
                btnCreateTest.setText("Create Test");

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(TestCreationActivity.this,
                            "Test created successfully!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(TestCreationActivity.this,
                            "Error creating test", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TestCreateResponse> call, Throwable t) {
                btnCreateTest.setEnabled(true);
                btnCreateTest.setText("Create Test");
                Toast.makeText(TestCreationActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}