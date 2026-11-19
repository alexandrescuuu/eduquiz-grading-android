package com.example.quizapledu.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapledu.R;
import android.widget.Toast;
public class ResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Get data from Intent
        String testName = getIntent().getStringExtra("test_name");
        String studentAnswer = getIntent().getStringExtra("student_answer");
        String feedback = getIntent().getStringExtra("feedback");
        double score = getIntent().getDoubleExtra("score", 0.0);
        String scoreDetails = getIntent().getStringExtra("score_details");
        int submissionId = getIntent().getIntExtra("submission_id", -1);

        // Find views
        TextView tvTestName = findViewById(R.id.tv_test_name);
        TextView tvScore = findViewById(R.id.tv_score);
        TextView tvPoints = findViewById(R.id.tv_points);
        TextView tvStudentAnswer = findViewById(R.id.tv_student_answer);
        TextView tvFeedback = findViewById(R.id.tv_feedback);
        Button btnDone = findViewById(R.id.btn_done);
        Button btnViewDetails = findViewById(R.id.btn_view_details);

        // Set data
        tvTestName.setText(testName);
        tvScore.setText(String.format("%.1f%%", score));
        tvPoints.setText(scoreDetails);
        tvStudentAnswer.setText(studentAnswer);
        tvFeedback.setText(feedback);

        // Done button
        btnDone.setOnClickListener(v -> finish());

        // View Details button
        btnViewDetails.setOnClickListener(v -> {
            if (submissionId != -1) {
                Intent intent = new Intent(ResultsActivity.this, SubmissionDetailActivity.class);
                intent.putExtra("submission_id", submissionId);
                intent.putExtra("test_name", testName);
                intent.putExtra("student_name", "");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Detalii indisponibile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}