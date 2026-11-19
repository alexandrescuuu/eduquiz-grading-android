package com.example.quizapledu.fragments;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.utils.Constants;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import java.util.ArrayList;
import java.util.List;

public class StudentProgressFragment extends Fragment {

    private LineChart chartTrend;
    private LinearLayout containerScores, cardSummary;
    private TextView tvBestTest, tvWorstTest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_progress, container, false);

        chartTrend = view.findViewById(R.id.chart_trend);
        containerScores = view.findViewById(R.id.container_scores);
        cardSummary = view.findViewById(R.id.card_summary);
        tvBestTest = view.findViewById(R.id.tv_best_test);
        tvWorstTest = view.findViewById(R.id.tv_worst_test);

        loadProgress();

        return view;
    }

    private void loadProgress() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);
        int studentId = prefs.getInt(Constants.KEY_USER_ID, 0);

        RetrofitClient.getApiService().getStudentProgress(studentId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) return;

                            JSONArray progressArr = json.optJSONArray("progress");
                            if (progressArr == null || progressArr.length() == 0) {
                                containerScores.removeAllViews();
                                TextView empty = new TextView(getActivity());
                                empty.setText("Nu ai rezolvat niciun test inca.");
                                empty.setTextColor(0xFF71717A);
                                empty.setTextSize(13f);
                                containerScores.addView(empty);
                                return;
                            }

                            // Construieste lista locala
                            List<String> names = new ArrayList<>();
                            List<Double> scores = new ArrayList<>();

                            for (int i = 0; i < progressArr.length(); i++) {
                                JSONObject item = progressArr.getJSONObject(i);
                                names.add(item.optString("test_name", "Test"));
                                scores.add(item.optDouble("score", 0.0));
                            }

                            setupLineChart(names, scores);
                            showScoresList(names, scores);
                            cardSummary.setVisibility(View.VISIBLE);

                            // Best / Worst
                            int bestIdx = 0, worstIdx = 0;
                            for (int i = 1; i < scores.size(); i++) {
                                if (scores.get(i) > scores.get(bestIdx)) bestIdx = i;
                                if (scores.get(i) < scores.get(worstIdx)) worstIdx = i;
                            }
                            tvBestTest.setText(names.get(bestIdx) + " — " +
                                    String.format("%.0f%%", scores.get(bestIdx)));
                            tvWorstTest.setText(names.get(worstIdx) + " — " +
                                    String.format("%.0f%%", scores.get(worstIdx)));

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) { }
                });
    }

    private void setupLineChart(List<String> names, List<Double> scores) {
        List<Entry> entries = new ArrayList<>();
        String[] labels = new String[names.size()];

        for (int i = 0; i < names.size(); i++) {
            entries.add(new Entry(i, scores.get(i).floatValue()));
            String name = names.get(i);
            labels[i] = name.length() > 8 ? name.substring(0, 6) + ".." : name;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Score %");
        dataSet.setColor(Color.parseColor("#A78BFA"));
        dataSet.setCircleColor(Color.parseColor("#A78BFA"));
        dataSet.setValueTextColor(Color.parseColor("#FAFAFA"));
        dataSet.setValueTextSize(10f);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#2D2250"));
        dataSet.setFillAlpha(80);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chartTrend.setData(lineData);
        chartTrend.getDescription().setEnabled(false);
        chartTrend.getLegend().setEnabled(false);
        chartTrend.setDrawGridBackground(false);
        chartTrend.setBackgroundColor(Color.TRANSPARENT);
        chartTrend.setTouchEnabled(true);

        XAxis xAxis = chartTrend.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#52525B"));
        xAxis.setTextSize(9f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chartTrend.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#52525B"));
        leftAxis.setTextSize(9f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#27272A"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);

        chartTrend.getAxisRight().setEnabled(false);
        chartTrend.animateX(800);
        chartTrend.invalidate();
    }

    private void showScoresList(List<String> names, List<Double> scores) {
        containerScores.removeAllViews();

        for (int i = 0; i < names.size(); i++) {
            double score = scores.get(i);
            int dotColor = score >= 80 ? 0xFF34D399 : score >= 60 ? 0xFFFBBF24 : 0xFFF87171;

            LinearLayout row = new LinearLayout(getActivity());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 24, 0, 24);

            View dot = new View(getActivity());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(8, 8);
            dotParams.setMargins(0, 18, 16, 0);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundColor(dotColor);
            row.addView(dot);

            TextView tvName = new TextView(getActivity());
            tvName.setText(names.get(i));
            tvName.setTextColor(0xFFFAFAFA);
            tvName.setTextSize(13f);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameParams);
            row.addView(tvName);

            TextView tvScore = new TextView(getActivity());
            tvScore.setText(String.format("%.0f%%", score));
            tvScore.setTextColor(dotColor);
            tvScore.setTextSize(14f);
            tvScore.setTypeface(null, Typeface.BOLD);
            row.addView(tvScore);

            containerScores.addView(row);

            if (i < names.size() - 1) {
                View separator = new View(getActivity());
                LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                separator.setLayoutParams(sepParams);
                separator.setBackgroundColor(0xFF27272A);
                containerScores.addView(separator);
            }
        }
    }
}
