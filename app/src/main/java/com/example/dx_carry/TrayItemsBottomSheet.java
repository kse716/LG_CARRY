package com.example.dx_carry;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TrayItemsBottomSheet {
    private TrayItemsBottomSheet() {
    }

    public static void show(Context context, String trayName, DataSnapshot itemsSnapshot) {
        BottomSheetDialog sheet = new BottomSheetDialog(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.popup_round_bg);
        container.setPadding(dp(context, 22), dp(context, 18), dp(context, 22), dp(context, 24));

        TextView title = new TextView(context);
        title.setText(trayName + " 보관 물품");
        title.setTextColor(0xFF111111);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        container.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("보관된 물품과 일시를 확인하세요.");
        subtitle.setTextColor(0xFF74787F);
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(context, 6), 0, dp(context, 14));
        container.addView(subtitle);

        ScrollView scrollView = new ScrollView(context);
        LinearLayout itemContainer = new LinearLayout(context);
        itemContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(itemContainer);

        if (!itemsSnapshot.exists()) {
            addEmptyView(context, itemContainer);
        } else {
            for (DataSnapshot child : itemsSnapshot.getChildren()) {
                addItemRow(context, itemContainer, child);
            }
        }

        container.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 320)
        ));

        sheet.setContentView(container);
        sheet.show();
    }

    private static void addEmptyView(Context context, LinearLayout container) {
        TextView empty = new TextView(context);
        empty.setText("보관된 물품이 없어요.");
        empty.setTextColor(0xFF74787F);
        empty.setTextSize(14);
        empty.setGravity(Gravity.CENTER);
        empty.setTypeface(null, Typeface.BOLD);
        empty.setPadding(0, dp(context, 24), 0, dp(context, 20));
        container.addView(empty);
    }

    private static void addItemRow(Context context, LinearLayout container, DataSnapshot itemSnapshot) {
        String name = itemSnapshot.child("itemName").getValue(String.class);
        String createdAtText = itemSnapshot.child("createdAtText").getValue(String.class);
        Long createdAt = itemSnapshot.child("createdAt").getValue(Long.class);
        if (name == null || name.trim().isEmpty()) {
            name = "이름 없는 물품";
        }

        TextView nameView = new TextView(context);
        nameView.setText(name.trim());
        nameView.setTextColor(0xFF111111);
        nameView.setTextSize(18);
        nameView.setTypeface(null, Typeface.BOLD);
        container.addView(nameView);

        TextView dateView = new TextView(context);
        dateView.setText("보관 일시 " + formatCreatedAt(createdAtText, createdAt));
        dateView.setTextColor(0xFF8A8D94);
        dateView.setTextSize(12);
        dateView.setPadding(0, dp(context, 5), 0, dp(context, 14));
        container.addView(dateView);

        View divider = new View(context);
        divider.setBackgroundColor(0xFFE6E7EA);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 1)
        );
        dividerParams.setMargins(0, 0, 0, dp(context, 14));
        container.addView(divider, dividerParams);
    }

    private static String formatCreatedAt(String createdAtText, Long createdAt) {
        if (createdAtText != null && !createdAtText.trim().isEmpty()) {
            return createdAtText;
        }
        if (createdAt != null) {
            return new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(new Date(createdAt));
        }
        return "등록일 미상";
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
