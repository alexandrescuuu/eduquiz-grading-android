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
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class TeacherClassesFragment extends Fragment {

    private LinearLayout containerClasses;
    private TextView tvClassCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_classes, container, false);

        containerClasses = view.findViewById(R.id.rv_classes);
        tvClassCount = view.findViewById(R.id.tv_class_count);

        loadClasses();

        return view;
    }

    private void loadClasses() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(Constants.PREFS_NAME, requireActivity().MODE_PRIVATE);
        int teacherId = prefs.getInt(Constants.KEY_USER_ID, 1);

        tvClassCount.setText("Se incarca...");

        RetrofitClient.getApiService().getTeacherClasses(teacherId)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            tvClassCount.setText("0 clase");
                            return;
                        }
                        try {
                            String raw = response.body().string();
                            JSONObject json = new JSONObject(raw);
                            if (!json.optBoolean("success", false)) {
                                tvClassCount.setText("0 clase");
                                return;
                            }

                            JSONArray classesArr = json.optJSONArray("classes");
                            int classCount = classesArr != null ? classesArr.length() : 0;
                            tvClassCount.setText(classCount + " " + (classCount == 1 ? "clasa" : "clase"));

                            if (containerClasses == null || classesArr == null) return;
                            containerClasses.removeAllViews();

                            for (int i = 0; i < classesArr.length(); i++) {
                                JSONObject cls = classesArr.getJSONObject(i);
                                String className = cls.optString("class_name", "Clasa");
                                int studentCount = cls.optInt("student_count", 0);
                                JSONArray students = cls.optJSONArray("students");

                                // Card clasa
                                LinearLayout card = new LinearLayout(getActivity());
                                card.setOrientation(LinearLayout.VERTICAL);
                                card.setPadding(32, 28, 32, 28);
                                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT);
                                cardParams.setMargins(0, 0, 0, 24);
                                card.setLayoutParams(cardParams);
                                card.setBackgroundColor(Color.parseColor("#18181B"));

                                // Header clasa
                                LinearLayout headerRow = new LinearLayout(getActivity());
                                headerRow.setOrientation(LinearLayout.HORIZONTAL);

                                TextView tvClassName = new TextView(getActivity());
                                tvClassName.setText(className);
                                tvClassName.setTextColor(Color.parseColor("#FAFAFA"));
                                tvClassName.setTextSize(16f);
                                tvClassName.setTypeface(null, Typeface.BOLD);
                                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                                tvClassName.setLayoutParams(nameParams);
                                headerRow.addView(tvClassName);

                                TextView tvCount = new TextView(getActivity());
                                tvCount.setText(studentCount + " elevi");
                                tvCount.setTextColor(Color.parseColor("#A78BFA"));
                                tvCount.setTextSize(12f);
                                headerRow.addView(tvCount);

                                card.addView(headerRow);

                                // Separator
                                View sep = new View(getActivity());
                                LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, 1);
                                sepParams.setMargins(0, 16, 0, 16);
                                sep.setLayoutParams(sepParams);
                                sep.setBackgroundColor(Color.parseColor("#27272A"));
                                card.addView(sep);

                                // Lista elevi
                                if (students != null) {
                                    for (int j = 0; j < students.length(); j++) {
                                        JSONObject student = students.getJSONObject(j);
                                        String studentName = student.optString("name", "Elev");

                                        // Avatar + Nume
                                        LinearLayout studentRow = new LinearLayout(getActivity());
                                        studentRow.setOrientation(LinearLayout.HORIZONTAL);
                                        LinearLayout.LayoutParams srParams = new LinearLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.WRAP_CONTENT);
                                        srParams.setMargins(0, 0, 0, 10);
                                        studentRow.setLayoutParams(srParams);

                                        // Avatar cerc cu initiale
                                        TextView tvAvatar = new TextView(getActivity());
                                        String[] nameParts = studentName.split(" ");
                                        StringBuilder ini = new StringBuilder();
                                        for (String p : nameParts) {
                                            if (!p.isEmpty()) ini.append(p.charAt(0));
                                        }
                                        String initials = ini.toString().toUpperCase();
                                        tvAvatar.setText(initials.length() >= 2 ? initials.substring(0, 2) : initials);
                                        tvAvatar.setTextColor(Color.parseColor("#A78BFA"));
                                        tvAvatar.setTextSize(11f);
                                        tvAvatar.setTypeface(null, Typeface.BOLD);
                                        tvAvatar.setGravity(android.view.Gravity.CENTER);
                                        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(60, 60);
                                        avatarParams.setMargins(0, 0, 16, 0);
                                        tvAvatar.setLayoutParams(avatarParams);
                                        tvAvatar.setBackgroundColor(Color.parseColor("#2D2250"));
                                        studentRow.addView(tvAvatar);

                                        // Nume student
                                        TextView tvStudentName = new TextView(getActivity());
                                        tvStudentName.setText(studentName);
                                        tvStudentName.setTextColor(Color.parseColor("#D4D4D8"));
                                        tvStudentName.setTextSize(13f);
                                        tvStudentName.setGravity(android.view.Gravity.CENTER_VERTICAL);
                                        LinearLayout.LayoutParams snParams = new LinearLayout.LayoutParams(
                                                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                                        tvStudentName.setLayoutParams(snParams);
                                        studentRow.addView(tvStudentName);

                                        card.addView(studentRow);
                                    }
                                }

                                containerClasses.addView(card);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            tvClassCount.setText("Eroare la incarcare");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        tvClassCount.setText("0 clase");
                    }
                });
    }
}
