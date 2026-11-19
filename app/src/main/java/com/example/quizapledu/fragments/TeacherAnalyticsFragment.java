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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import java.util.ArrayList;
import java.util.List;

public class TeacherAnalyticsFragment extends Fragment {

    private BarChart chartDistribution;
    private TextView tvAvgScore, tvHighestScore, tvLowestScore;
    private LinearLayout containerRanking;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_analytics, container, false);

        chartDistribution = view.findViewById(R.id.chart_distribution);
        tvAvgScore = view.findViewById(R.id.tv_avg_score);
        tvHighestScore = view.findViewById(R.id.tv_highest_score);
        tvLowestScore = view.findViewById(R.id.tv_lowest_score);

        // Incarcam RecyclerView ca LinearLayout daca ID-ul exista
        View rankingView = view.findViewById(R.id.rv_student_ranking);
        if (rankingView instanceof LinearLayout) {
            containerRanking = (LinearLayout) rankingView;
        }

        tvAvgScore.setText("--");
        tvHighestScore.setText("--");
        tvLowestScore.setText("--");

        // Chart placeholder gol pana vin datele
        setupEmptyChart();

        loadAnalytics();

        return view;
    }

    private void loadAnalytics() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);
        int teacherId = prefs.getInt(Constants.KEY_USER_ID, 1);

        RetrofitClient.getApiService().getTeacherAnalytics(teacherId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) return;

                            // Statistici
                          //  double avg = json.optDouble("avg_score", Double.NaN);
                          // double highest = json.optDouble("highest", Double.NaN);
                          //  double lowest = json.optDouble("lowest", Double.NaN);

                          //  tvAvgScore.setText(!Double.isNaN(avg) ? String.format("%.1f%%", avg) : "--");
                          //  tvHighestScore.setText(!Double.isNaN(highest) ? String.format("%.1f%%", highest) : "--");
                          //  tvLowestScore.setText(!Double.isNaN(lowest) ? String.format("%.1f%%", lowest) : "--");

                            // Statistici
                            double avg     = json.isNull("avg_score") ? Double.NaN : json.optDouble("avg_score", Double.NaN);
                            double highest = json.isNull("highest")   ? Double.NaN : json.optDouble("highest", Double.NaN);
                            double lowest  = json.isNull("lowest")    ? Double.NaN : json.optDouble("lowest", Double.NaN);

                            tvAvgScore.setText(!Double.isNaN(avg)         ? String.format("%.1f%%", avg)     : "--");
                            tvHighestScore.setText(!Double.isNaN(highest) ? String.format("%.1f%%", highest) : "--");
                            tvLowestScore.setText(!Double.isNaN(lowest)   ? String.format("%.1f%%", lowest)  : "--");

                            // Distributie scoruri pentru grafic
                            JSONObject dist = json.optJSONObject("score_distribution");
                            if (dist != null) {
                                int under50 = dist.optInt("under50", 0);
                                int s50_60 = dist.optInt("s50_60", 0);
                                int s60_70 = dist.optInt("s60_70", 0);
                                int s70_80 = dist.optInt("s70_80", 0);
                                int s80_90 = dist.optInt("s80_90", 0);
                                int above90 = dist.optInt("above90", 0);

                                setupScoreDistributionChart(under50, s50_60, s60_70, s70_80, s80_90, above90);
                            }

                            // Top studenti
                            JSONArray topArr = json.optJSONArray("top_students");
                            if (topArr != null && containerRanking != null) {
                                containerRanking.removeAllViews();
                                for (int i = 0; i < topArr.length(); i++) {
                                    JSONObject student = topArr.getJSONObject(i);
                                    String name = student.optString("name", "Student");
                                    double sAvg = student.optDouble("avg_score", 0);
                                    int tests = student.optInt("test_count", 0);
                                    addStudentRankRow(i + 1, name, sAvg, tests);
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) { }
                });
    }

    private void setupScoreDistributionChart(int under50, int s50_60, int s60_70,
                                             int s70_80, int s80_90, int above90) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, under50));
        entries.add(new BarEntry(1, s50_60));
        entries.add(new BarEntry(2, s60_70));
        entries.add(new BarEntry(3, s70_80));
        entries.add(new BarEntry(4, s80_90));
        entries.add(new BarEntry(5, above90));

        BarDataSet dataSet = new BarDataSet(entries, "Studenti");
        int[] colors = {
                Color.parseColor("#F87171"),
                Color.parseColor("#FBBF24"),
                Color.parseColor("#FBBF24"),
                Color.parseColor("#A78BFA"),
                Color.parseColor("#A78BFA"),
                Color.parseColor("#34D399")
        };
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.parseColor("#FAFAFA"));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        chartDistribution.setData(data);
        chartDistribution.setFitBars(true);
        chartDistribution.getDescription().setEnabled(false);
        chartDistribution.getLegend().setEnabled(false);
        chartDistribution.setDrawGridBackground(false);
        chartDistribution.setBackgroundColor(Color.TRANSPARENT);
        chartDistribution.setTouchEnabled(false);

        String[] labels = {"0-50", "50-60", "60-70", "70-80", "80-90", "90+"};
        XAxis xAxis = chartDistribution.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#52525B"));
        xAxis.setTextSize(9f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chartDistribution.getAxisLeft();
        leftAxis.setTextColor(Color.parseColor("#52525B"));
        leftAxis.setTextSize(9f);
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);

        chartDistribution.getAxisRight().setEnabled(false);
        chartDistribution.animateY(800);
        chartDistribution.invalidate();
    }

    private void setupEmptyChart() {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) entries.add(new BarEntry(i, 0));
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#27272A"));
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);
        chartDistribution.setData(data);
        chartDistribution.getDescription().setEnabled(false);
        chartDistribution.getLegend().setEnabled(false);
        chartDistribution.setDrawGridBackground(false);
        chartDistribution.setBackgroundColor(Color.TRANSPARENT);
        chartDistribution.getAxisRight().setEnabled(false);
        chartDistribution.invalidate();
    }

    private void addStudentRankRow(int rank, String name, double avgScore, int testCount) {
        if (containerRanking == null) return;

        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 16);
        row.setLayoutParams(rp);

        // Rank badge
        TextView tvRank = new TextView(getActivity());
        tvRank.setText(String.valueOf(rank));
        tvRank.setTextColor(rank == 1 ? Color.parseColor("#FBBF24") : Color.parseColor("#71717A"));
        tvRank.setTextSize(13f);
        tvRank.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams rankP = new LinearLayout.LayoutParams(40, ViewGroup.LayoutParams.WRAP_CONTENT);
        rankP.setMargins(0, 0, 12, 0);
        tvRank.setLayoutParams(rankP);
        row.addView(tvRank);

        // Nume
        TextView tvName = new TextView(getActivity());
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#FAFAFA"));
        tvName.setTextSize(13f);
        LinearLayout.LayoutParams nameP = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameP);
        row.addView(tvName);

        // Scor
        int scoreColor = avgScore >= 80 ? Color.parseColor("#34D399")
                : avgScore >= 60 ? Color.parseColor("#FBBF24") : Color.parseColor("#F87171");
        TextView tvScore = new TextView(getActivity());
        tvScore.setText(String.format("%.0f%%", avgScore));
        tvScore.setTextColor(scoreColor);
        tvScore.setTextSize(14f);
        tvScore.setTypeface(null, Typeface.BOLD);
        row.addView(tvScore);

        containerRanking.addView(row);

        // Separator
        View sep = new View(getActivity());
        LinearLayout.LayoutParams sepP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        sepP.setMargins(0, 0, 0, 16);
        sep.setLayoutParams(sepP);
        sep.setBackgroundColor(Color.parseColor("#27272A"));
        containerRanking.addView(sep);
    }
}
