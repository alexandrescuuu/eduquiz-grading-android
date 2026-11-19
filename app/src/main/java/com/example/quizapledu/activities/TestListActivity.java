package com.example.quizapledu.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapledu.R;
import com.example.quizapledu.adapters.TestAdapter;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.Question;
import com.example.quizapledu.api.models.QuestionsResponse;
import com.example.quizapledu.api.models.TestListResponse;
import com.example.quizapledu.utils.Constants;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvTitle, tvSubtitle;
    private LinearLayout emptyState, btnAddTest;
    private SharedPreferences prefs;
    private boolean isTeacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_list);

        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String role = prefs.getString(Constants.KEY_ROLE, "");
        isTeacher = Constants.ROLE_TEACHER.equals(role);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        emptyState = findViewById(R.id.empty_state);
        btnAddTest = findViewById(R.id.btn_add_test);
        recyclerView = findViewById(R.id.recycler_tests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Header
        if (isTeacher) {
            tvTitle.setText("My tests");
            btnAddTest.setVisibility(View.VISIBLE);
            btnAddTest.setOnClickListener(v ->
                    startActivity(new Intent(this, TestCreationActivity.class)));
        } else {
            tvTitle.setText("Available tests");
            btnAddTest.setVisibility(View.GONE);
        }

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        loadTests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reincarca testele cand revii (ex: dupa creare test nou)
        loadTests();
    }

    private void loadTests() {
        tvSubtitle.setText("Loading...");

        RetrofitClient.getApiService().getTests().enqueue(new Callback<TestListResponse>() {
            @Override
            public void onResponse(Call<TestListResponse> call, Response<TestListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TestListResponse result = response.body();

                    if (result.tests != null && !result.tests.isEmpty()) {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        int count = result.tests.size();
                        tvSubtitle.setText(count + (count == 1 ? " test" : " tests"));

                        TestAdapter adapter = new TestAdapter(result.tests, test -> {
                            if (isTeacher) {
                                // Teacher: optiuni - vedea rezultate sau sterge
                                showTeacherOptions(test);
                            } else {
                                // Student: incepe testul
                                showStartTestDialog(test);
                            }
                        }, isTeacher);

                        recyclerView.setAdapter(adapter);

                    } else {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        tvSubtitle.setText("0 tests");
                    }
                } else {
                    tvSubtitle.setText("Error loading");
                    Toast.makeText(TestListActivity.this,
                            "Failed to load tests", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TestListResponse> call, Throwable t) {
                tvSubtitle.setText("Connection error");
                Toast.makeText(TestListActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTeacherOptions(TestListResponse.Test test) {
        String[] options = {"View results", "View questions", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle(test.test_name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Vedea rezultatele studentilor
                        Intent intent = new Intent(this, TestDetailsActivity.class);
                        intent.putExtra("test_id", test.id);
                        intent.putExtra("test_name", test.test_name);
                        startActivity(intent);
                    } else if (which == 1) {
                        // Vedea intrebarile testului
                        showQuestionsDialog(test);
                    }
                })
                .show();
    }

    private void showStartTestDialog(TestListResponse.Test test) {
        String message = "Questions: " + test.question_count
                + "\nTotal points: " + test.total_points
                + "\n\nYou can answer by typing, selecting options, or scanning handwritten answers.";

        new AlertDialog.Builder(this)
                .setTitle(test.test_name)
                .setMessage(message)
                .setPositiveButton("Start Test", (dialog, which) -> {
                    Intent intent = new Intent(this, TestSubmissionActivity.class);
                    intent.putExtra("test_id", test.id);
                    intent.putExtra("test_name", test.test_name);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---- VIEW QUESTIONS (teacher) ----

    private void showQuestionsDialog(TestListResponse.Test test) {
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle(test.test_name)
                .setMessage("Se incarca intrebarile...")
                .setCancelable(false)
                .show();

        RetrofitClient.getApiService().getQuestions(test.id).enqueue(new Callback<QuestionsResponse>() {
            @Override
            public void onResponse(Call<QuestionsResponse> call, Response<QuestionsResponse> response) {
                loadingDialog.dismiss();

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                    Toast.makeText(TestListActivity.this,
                            "Eroare la incarcarea intrebarilor", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Question> questions = response.body().getQuestions();
                if (questions == null || questions.isEmpty()) {
                    Toast.makeText(TestListActivity.this,
                            "Acest test nu are intrebari", Toast.LENGTH_SHORT).show();
                    return;
                }

                showQuestionsList(test.test_name, questions);
            }

            @Override
            public void onFailure(Call<QuestionsResponse> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(TestListActivity.this,
                        "Eroare conexiune: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQuestionsList(String testName, List<Question> questions) {
        int pad = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad, pad, pad);
        container.setBackgroundColor(Color.parseColor("#09090B"));

        for (Question q : questions) {
            container.addView(buildQuestionRow(q));
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        new AlertDialog.Builder(this)
                .setTitle(testName + " — Intrebari (" + questions.size() + ")")
                .setView(scrollView)
                .setPositiveButton("Inchide", null)
                .show();
    }

    private View buildQuestionRow(Question q) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(20, 16, 20, 16);
        row.setBackgroundColor(Color.parseColor("#18181B"));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 10);
        row.setLayoutParams(rp);

        // Header: numar intrebare + tip + puncte
        TextView tvHeader = new TextView(this);
        tvHeader.setText("Intrebarea " + q.getQuestionOrder() + "  •  " + typeLabel(q.getQuestionType())
                + "  •  " + q.getPoints() + (q.getPoints() == 1 ? " pct" : " pct"));
        tvHeader.setTextColor(Color.parseColor("#A78BFA"));
        tvHeader.setTextSize(11f);
        tvHeader.setTypeface(null, Typeface.BOLD);
        row.addView(tvHeader);

        // Textul intrebarii
        TextView tvText = new TextView(this);
        tvText.setText(q.getQuestionText());
        tvText.setTextColor(Color.parseColor("#FAFAFA"));
        tvText.setTextSize(14f);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.setMargins(0, 6, 0, 0);
        tvText.setLayoutParams(tp);
        row.addView(tvText);

        // Optiuni, doar pentru grila
        if (q.isMultipleChoice()) {
            List<String> options = q.getOptions();
            if (options != null && !options.isEmpty()) {
                char letter = 'a';
                for (String opt : options) {
                    TextView tvOpt = new TextView(this);
                    tvOpt.setText("   " + letter + ")  " + opt);
                    tvOpt.setTextColor(Color.parseColor("#D4D4D8"));
                    tvOpt.setTextSize(13f);
                    LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    op.setMargins(0, 4, 0, 0);
                    tvOpt.setLayoutParams(op);
                    row.addView(tvOpt);
                    letter++;
                }
            }
        }

        return row;
    }

    private String typeLabel(String type) {
        if (type == null) return "Raspuns liber";
        switch (type) {
            case "multiple_choice": return "Grila";
            case "scan_answer": return "Scanare";
            default: return "Raspuns liber";
        }
    }
}