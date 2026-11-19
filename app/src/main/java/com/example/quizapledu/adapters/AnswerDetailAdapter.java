package com.example.quizapledu.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapledu.R;
import com.example.quizapledu.api.models.SubmissionDetail;
import java.util.List;

public class AnswerDetailAdapter extends RecyclerView.Adapter<AnswerDetailAdapter.ViewHolder> {

    private List<SubmissionDetail.Answer> answers;

    public AnswerDetailAdapter(List<SubmissionDetail.Answer> answers) {
        this.answers = answers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_answer_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubmissionDetail.Answer answer = answers.get(position);

        holder.tvQuestionNumber.setText("Question " + (position + 1));
        holder.tvQuestionText.setText(answer.getQuestionText());
        holder.tvStudentAnswer.setText(answer.getStudentAnswer());
        holder.tvCorrectAnswer.setText(answer.getCorrectAnswer());
        holder.tvFeedback.setText(answer.getAiFeedback());

        double scorePercentage = (answer.getAiScore() / answer.getPoints()) * 100;
        holder.tvScoreBadge.setText(String.format("%.1f/%d", answer.getAiScore(), answer.getPoints()));

        // Color based on score
        if (scorePercentage >= 80) {
            holder.tvScoreBadge.setTextColor(Color.parseColor("#34D399")); // Green
        } else if (scorePercentage >= 50) {
            holder.tvScoreBadge.setTextColor(Color.parseColor("#FBBF24")); // Yellow
        } else {
            holder.tvScoreBadge.setTextColor(Color.parseColor("#EF4444")); // Red
        }
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionNumber, tvQuestionText, tvStudentAnswer;
        TextView tvCorrectAnswer, tvFeedback, tvScoreBadge;

        ViewHolder(View view) {
            super(view);
            tvQuestionNumber = view.findViewById(R.id.tv_question_number);
            tvQuestionText = view.findViewById(R.id.tv_question_text);
            tvStudentAnswer = view.findViewById(R.id.tv_student_answer);
            tvCorrectAnswer = view.findViewById(R.id.tv_correct_answer);
            tvFeedback = view.findViewById(R.id.tv_feedback);
            tvScoreBadge = view.findViewById(R.id.tv_score_badge);
        }
    }
}