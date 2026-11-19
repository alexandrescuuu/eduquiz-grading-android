package com.example.quizapledu.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quizapledu.R;
import com.example.quizapledu.api.models.TestListResponse;
import java.util.List;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.ViewHolder> {

    private List<TestListResponse.Test> tests;
    private OnTestClickListener listener;
    private boolean isTeacher;

    public interface OnTestClickListener {
        void onTestClick(TestListResponse.Test test);
    }

    public TestAdapter(List<TestListResponse.Test> tests, OnTestClickListener listener, boolean isTeacher) {
        this.tests = tests;
        this.listener = listener;
        this.isTeacher = isTeacher;
    }

    // Constructor backward-compat (fara rol - default student)
    public TestAdapter(List<TestListResponse.Test> tests, OnTestClickListener listener) {
        this(tests, listener, false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_test, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TestListResponse.Test test = tests.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvTestName.setText(test.test_name);

        // Info row
        String info = test.question_count + " questions / " + test.total_points + " pts";
        if (isTeacher && test.submission_count > 0) {
            info += " / " + test.submission_count + " submitted";
        }
        holder.tvTestInfo.setText(info);

        // Status badge
        if ("active".equals(test.status)) {
            holder.tvStatusBadge.setText("active");
            holder.tvStatusBadge.setTextColor(0xFF34D399);
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
        } else {
            holder.tvStatusBadge.setText(test.status);
            holder.tvStatusBadge.setTextColor(0xFF71717A);
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_card);
        }

        // Dreapta: media scorurilor pentru profesor, gol pentru student
        if (isTeacher && test.avg_score != null && test.avg_score > 0) {
            holder.tvAvgScore.setText(String.format("%.0f%%", test.avg_score));
            holder.tvAvgScore.setVisibility(View.VISIBLE);
        } else {
            holder.tvAvgScore.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onTestClick(test));
    }

    @Override
    public int getItemCount() {
        return tests != null ? tests.size() : 0;
    }

    public void updateTests(List<TestListResponse.Test> newTests) {
        this.tests = newTests;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTestName, tvTestInfo, tvStatusBadge, tvAvgScore;

        ViewHolder(View view) {
            super(view);
            tvTestName = view.findViewById(R.id.tv_test_name);
            tvTestInfo = view.findViewById(R.id.tv_test_info);
            tvStatusBadge = view.findViewById(R.id.tv_status_badge);
            tvAvgScore = view.findViewById(R.id.tv_avg_score);
        }
    }
}
