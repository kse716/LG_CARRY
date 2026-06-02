package com.example.test;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class TrayDialogFragment extends DialogFragment{
    // 외부(Activity)에서 DB 데이터를 전달받기 위한 변수들
    private String trayTitle;
    private String trayContent;

    // 데이터 전달을 위한 생성자 팩토리 메서드 (안드로이드 권장 방식)
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
        // 커스텀 레이아웃 XML 파일 연결
        View view = inflater.inflate(R.layout.dialog_tray_popup, container, false);

        // 뷰 컴포넌트 연결
        TextView tvTitle = view.findViewById(R.id.popup_title);
        TextView tvContent = view.findViewById(R.id.popup_content);
        Button btnClose = view.findViewById(R.id.btn_close);

        // 전달받은 DB 데이터 반영
        if (trayTitle != null) tvTitle.setText(trayTitle);
        if (trayContent != null) tvContent.setText(trayContent);

        // 닫기 버튼 클릭 리스너 설정
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss(); // 팝업 창 닫기
            }
        });

        return view;
    }
}
