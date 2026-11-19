package com.example.quizapledu.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.quizapledu.R;
import com.example.quizapledu.activities.BatchScanActivity;
import com.example.quizapledu.activities.OMRScanActivity;
import com.example.quizapledu.activities.ScanActivity;

public class TeacherScanFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_scan, container, false);

        LinearLayout cardFullPage = view.findViewById(R.id.card_full_page);
        LinearLayout cardOmr      = view.findViewById(R.id.card_omr);
        LinearLayout cardBatch    = view.findViewById(R.id.card_batch);

        // Full Page Scan — foaie cu text scris de mana
        cardFullPage.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScanActivity.class);
            intent.putExtra("scan_mode", "full_page");
            startActivity(intent);
        });

        // OMR Scan — detectie X sau cerc pe orice foaie
        cardOmr.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), OMRScanActivity.class)));

        // Batch Scan — mai multe foi, camera + galerie
        cardBatch.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), BatchScanActivity.class)));

        return view;
    }
}
