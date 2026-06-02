package com.example.dx_carry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TrayDialogFragment extends DialogFragment {
    private String trayTitle;
    private String trayContent;

    public static TrayDialogFragment newInstance(String title, String content) {
        TrayDialogFragment fragment = new TrayDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("content", content);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            trayTitle = getArguments().getString("title");
            trayContent = getArguments().getString("content");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_tray_popup, container, false);

        TextView tvTitle = view.findViewById(R.id.popup_title);
        TextView tvContent = view.findViewById(R.id.popup_content);
        Button btnClose = view.findViewById(R.id.btn_close);

        if (trayTitle != null) {
            tvTitle.setText(trayTitle);
        }
        if (trayContent != null) {
            tvContent.setText(trayContent);
        }

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }
}
