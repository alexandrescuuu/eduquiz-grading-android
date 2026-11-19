package com.example.quizapledu.fragments;

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

public class AdminReportsFragment extends Fragment {

    private TextView tvAvg, tvHighest, tvLowest, tvTotalGraded;
    private BarChart chartAccuracy;
    private LinearLayout containerTeacherStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        tvAvg               = view.findViewById(R.id.tv_platform_avg);
        tvHighest           = view.findViewById(R.id.tv_platform_highest);
        tvLowest            = view.findViewById(R.id.tv_platform_lowest);
        tvTotalGraded       = view.findViewById(R.id.tv_total_graded);
        chartAccuracy       = view.findViewById(R.id.chart_accuracy);
        containerTeacherStats = view.findViewById(R.id.container_teacher_stats);

        setupEmptyChart();
        loadReports();

        return view;
    }

    private void loadReports() {
        RetrofitClient.getApiService().getAdminReports()
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) return;

                           // double avg = json.optDouble("platform_avg", Double.NaN);
                          //  double highest = json.optDouble("highest", Double.NaN);
                          //  double lowest = json.optDouble("lowest", Double.NaN);
                           // int graded = json.optInt("total_graded", 0);

                          //  tvAvg.setText(!Double.isNaN(avg) ? String.format("%.1f%%", avg) : "--");
                          //  tvHighest.setText(!Double.isNaN(highest) ? String.format("%.1f%%", highest) : "--");
                          //  tvLowest.setText(!Double.isNaN(lowest) ? String.format("%.1f%%", lowest) : "--");
                          //  tvTotalGraded.setText(String.valueOf(graded));
                            double avg     = json.isNull("platform_avg") ? Double.NaN : json.optDouble("platform_avg", Double.NaN);
                            double highest = json.isNull("highest")      ? Double.NaN : json.optDouble("highest", Double.NaN);
                            double lowest  = json.isNull("lowest")       ? Double.NaN : json.optDouble("lowest", Double.NaN);
                            int graded     = json.optInt("total_graded", 0);

                            tvAvg.setText(!Double.isNaN(avg)         ? String.format("%.1f%%", avg)     : "--");
                            tvHighest.setText(!Double.isNaN(highest) ? String.format("%.1f%%", highest) : "--");
                            tvLowest.setText(!Double.isNaN(lowest)   ? String.format("%.1f%%", lowest)  : "--");
                            tvTotalGraded.setText(String.valueOf(graded));

                            // Grafic per tip intrebare
                            JSONObject byType = json.optJSONObject("by_question_type");
                            if (byType != null) {
                                double mcq  = byType.optDouble("multiple_choice", 0);
                                double text = byType.optDouble("text_answer", 0);
                                double scan = byType.optDouble("scan_answer", 0);
                                setupAccuracyChart(mcq, text, scan);
                            }

                            // Statistici per profesor
                            JSONArray teachers = json.optJSONArray("per_teacher");
                            if (teachers != null) {
                                containerTeacherStats.removeAllViews();
                                for (int i = 0; i < teachers.length(); i++) {
                                    JSONObject t = teachers.getJSONObject(i);
                                    addTeacherRow(
                                            t.optString("name", "Profesor"),
                                            t.optDouble("avg_score", 0),
                                            t.optInt("test_count", 0),
                                            t.optInt("submission_count", 0)
                                    );
                                }
                            }

                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) { }
                });
    }

    private void setupAccuracyChart(double mcq, double text, double scan) {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, (float) mcq));
        entries.add(new BarEntry(1, (float) text));
        entries.add(new BarEntry(2, (float) scan));

        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColors(
                Color.parseColor("#34D399"),
                Color.parseColor("#A78BFA"),
                Color.parseColor("#FBBF24")
        );
        ds.setValueTextColor(Color.parseColor("#FAFAFA"));
        ds.setValueTextSize(10f);

        BarData data = new BarData(ds);
        data.setBarWidth(0.5f);
        chartAccuracy.setData(data);
        chartAccuracy.setFitBars(true);
        chartAccuracy.getDescription().setEnabled(false);
        chartAccuracy.getLegend().setEnabled(false);
        chartAccuracy.setDrawGridBackground(false);
        chartAccuracy.setBackgroundColor(Color.TRANSPARENT);
        chartAccuracy.setTouchEnabled(false);

        String[] labels = {"MCQ", "Text", "Scan"};
        XAxis x = chartAccuracy.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(Color.parseColor("#52525B"));
        x.setTextSize(10f);
        x.setDrawGridLines(false);
        x.setGranularity(1f);

        YAxis y = chartAccuracy.getAxisLeft();
        y.setTextColor(Color.parseColor("#52525B"));
        y.setAxisMinimum(0f);
        y.setAxisMaximum(100f);
        y.setDrawGridLines(false);
        chartAccuracy.getAxisRight().setEnabled(false);
        chartAccuracy.animateY(700);
        chartAccuracy.invalidate();
    }

    private void setupEmptyChart() {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 3; i++) entries.add(new BarEntry(i, 0));
        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColor(Color.parseColor("#27272A"));
        BarData data = new BarData(ds);
        data.setBarWidth(0.5f);
        chartAccuracy.setData(data);
        chartAccuracy.getDescription().setEnabled(false);
        chartAccuracy.getLegend().setEnabled(false);
        chartAccuracy.setBackgroundColor(Color.TRANSPARENT);
        chartAccuracy.getAxisRight().setEnabled(false);
        chartAccuracy.invalidate();
    }

    private void addTeacherRow(String name, double avg, int tests, int submissions) {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 16, 20, 16);
        row.setBackgroundColor(Color.parseColor("#18181B"));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 6);
        row.setLayoutParams(rp);

        // Initiale
        String[] parts = name.split(" ");
        StringBuilder ini = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) ini.append(p.charAt(0));
        String initials = ini.toString().toUpperCase();

        TextView tvAv = new TextView(getActivity());
        tvAv.setText(initials.length() >= 2 ? initials.substring(0, 2) : initials);
        tvAv.setTextColor(Color.parseColor("#60A5FA"));
        tvAv.setTextSize(11f);
        tvAv.setTypeface(null, Typeface.BOLD);
        tvAv.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(40, 40);
        ap.setMargins(0, 0, 14, 0);
        tvAv.setLayoutParams(ap);
        tvAv.setBackgroundColor(Color.parseColor("#1E3A5F"));
        row.addView(tvAv);

        LinearLayout info = new LinearLayout(getActivity());
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(ip);

        TextView tvName = new TextView(getActivity());
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#FAFAFA"));
        tvName.setTextSize(13f);
        info.addView(tvName);

        TextView tvSub = new TextView(getActivity());
        tvSub.setText(tests + " teste · " + submissions + " submisii");
        tvSub.setTextColor(Color.parseColor("#52525B"));
        tvSub.setTextSize(11f);
        info.addView(tvSub);

        row.addView(info);

        int scoreColor = avg >= 80 ? Color.parseColor("#34D399")
                : avg >= 60 ? Color.parseColor("#FBBF24")
                : Color.parseColor("#F87171");
        TextView tvScore = new TextView(getActivity());
        tvScore.setText(String.format("%.0f%%", avg));
        tvScore.setTextColor(scoreColor);
        tvScore.setTextSize(15f);
        tvScore.setTypeface(null, Typeface.BOLD);
        row.addView(tvScore);

        containerTeacherStats.addView(row);
    }
}
