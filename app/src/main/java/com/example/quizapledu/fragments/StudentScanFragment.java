package com.example.quizapledu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.activities.ScanActivity;

public class StudentScanFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_scan, container, false);

        LinearLayout btnCapture = view.findViewById(R.id.btn_capture);
        LinearLayout btnGallery = view.findViewById(R.id.btn_gallery);

        btnCapture.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScanActivity.class);
            startActivity(intent);
        });

        btnGallery.setOnClickListener(v ->
                Toast.makeText(getActivity(),
                        "Gallery scan coming soon", Toast.LENGTH_SHORT).show());

        return view;
    }
}
