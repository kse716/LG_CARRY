package com.example.dx_carry;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private View homeScreen;
    private View callScreen;
    private View itemsScreen;
    private View routineScreen;
    private View settingsScreen;
    private TextView navHome;
    private TextView navCall;
    private TextView navItems;
    private TextView navRoutine;
    private TextView navSettings;

    private final int activeColor = 0xFF111111;
    private final int inactiveColor = 0xFF8A8D94;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        bindNavigation();
        showScreen("home");
    }

    private void bindViews() {
        homeScreen = findViewById(R.id.homeScreen);
        callScreen = findViewById(R.id.callScreen);
        itemsScreen = findViewById(R.id.itemsScreen);
        routineScreen = findViewById(R.id.routineScreen);
        settingsScreen = findViewById(R.id.settingsScreen);
        navHome = findViewById(R.id.navHome);
        navCall = findViewById(R.id.navCall);
        navItems = findViewById(R.id.navItems);
        navRoutine = findViewById(R.id.navRoutine);
        navSettings = findViewById(R.id.navSettings);
    }

    private void bindNavigation() {
        navHome.setOnClickListener(v -> showScreen("home"));
        navCall.setOnClickListener(v -> showScreen("call"));
        navItems.setOnClickListener(v -> showScreen("items"));
        navRoutine.setOnClickListener(v -> showScreen("routine"));
        navSettings.setOnClickListener(v -> showScreen("settings"));
    }

    private void showScreen(String screen) {
        homeScreen.setVisibility(View.GONE);
        callScreen.setVisibility(View.GONE);
        itemsScreen.setVisibility(View.GONE);
        routineScreen.setVisibility(View.GONE);
        settingsScreen.setVisibility(View.GONE);

        navHome.setTextColor(inactiveColor);
        navCall.setTextColor(inactiveColor);
        navItems.setTextColor(inactiveColor);
        navRoutine.setTextColor(inactiveColor);
        navSettings.setTextColor(inactiveColor);

        switch (screen) {
            case "call":
                callScreen.setVisibility(View.VISIBLE);
                navCall.setTextColor(activeColor);
                break;
            case "items":
                itemsScreen.setVisibility(View.VISIBLE);
                navItems.setTextColor(activeColor);
                break;
            case "routine":
                routineScreen.setVisibility(View.VISIBLE);
                navRoutine.setTextColor(activeColor);
                break;
            case "settings":
                settingsScreen.setVisibility(View.VISIBLE);
                navSettings.setTextColor(activeColor);
                break;
            case "home":
            default:
                homeScreen.setVisibility(View.VISIBLE);
                navHome.setTextColor(activeColor);
                break;
        }
    }
}
