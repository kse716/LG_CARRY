package com.example.dx_carry;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private View authScreen;
    private View homeScreen;
    private View callScreen;
    private View itemsScreen;
    private View routineScreen;
    private View settingsScreen;
    private View bottomNav;

    private TextView navHome;
    private TextView navCall;
    private TextView navItems;
    private TextView navRoutine;
    private TextView navSettings;

    private TextView tray1Button;
    private TextView tray2Button;
    private TextView tray3Button;
    private TextView selectedTrayLabel;
    private TextView selectedTrayTitle;
    private TextView itemListText;
    private TextView itemSearchResultText;
    private TextView homeTrayItemsText;
    private TextView homeFavoriteText;

    private EditText itemNameInput;
    private EditText itemSearchInput;
    private EditText trayNameInput;
    private EditText loginNameInput;
    private EditText loginPasswordInput;

    private DatabaseReference db;
    private String selectedTrayId = "tray1";

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

        db = FirebaseDatabase.getInstance().getReference();
        bindViews();
        bindNavigation();
        bindAuth();
        bindTrayControls();
        seedTestData();
        showAuth();
    }

    private void bindViews() {
        authScreen = findViewById(R.id.authScreen);
        homeScreen = findViewById(R.id.homeScreen);
        callScreen = findViewById(R.id.callScreen);
        itemsScreen = findViewById(R.id.itemsScreen);
        routineScreen = findViewById(R.id.routineScreen);
        settingsScreen = findViewById(R.id.settingsScreen);
        bottomNav = findViewById(R.id.bottomNav);

        navHome = findViewById(R.id.navHome);
        navCall = findViewById(R.id.navCall);
        navItems = findViewById(R.id.navItems);
        navRoutine = findViewById(R.id.navRoutine);
        navSettings = findViewById(R.id.navSettings);

        tray1Button = findViewById(R.id.tray1Button);
        tray2Button = findViewById(R.id.tray2Button);
        tray3Button = findViewById(R.id.tray3Button);
        selectedTrayLabel = findViewById(R.id.selectedTrayLabel);
        selectedTrayTitle = findViewById(R.id.selectedTrayTitle);
        itemListText = findViewById(R.id.itemListText);
        itemSearchResultText = findViewById(R.id.itemSearchResultText);
        homeTrayItemsText = findViewById(R.id.homeTrayItemsText);
        homeFavoriteText = findViewById(R.id.homeFavoriteText);

        itemNameInput = findViewById(R.id.itemNameInput);
        itemSearchInput = findViewById(R.id.itemSearchInput);
        trayNameInput = findViewById(R.id.trayNameInput);
        loginNameInput = findViewById(R.id.loginNameInput);
        loginPasswordInput = findViewById(R.id.loginPasswordInput);
    }

    private void bindNavigation() {
        navHome.setOnClickListener(v -> showScreen("home"));
        navCall.setOnClickListener(v -> showScreen("call"));
        navItems.setOnClickListener(v -> showScreen("items"));
        navRoutine.setOnClickListener(v -> showScreen("routine"));
        navSettings.setOnClickListener(v -> showScreen("settings"));
    }

    private void bindAuth() {
        findViewById(R.id.loginButton).setOnClickListener(v -> saveUserAndEnter(false));
        findViewById(R.id.signupButton).setOnClickListener(v -> saveUserAndEnter(true));
    }

    private void bindTrayControls() {
        findViewById(R.id.addItemButton).setOnClickListener(v -> addItemToSelectedTray());
        findViewById(R.id.saveTrayNameButton).setOnClickListener(v -> saveTrayName());
        tray1Button.setOnClickListener(v -> selectTray("tray1"));
        tray2Button.setOnClickListener(v -> selectTray("tray2"));
        tray3Button.setOnClickListener(v -> selectTray("tray3"));

        findViewById(R.id.homeTray1Button).setOnClickListener(v -> loadHomeTrayItems("tray1"));
        findViewById(R.id.homeTray2Button).setOnClickListener(v -> loadHomeTrayItems("tray2"));
        findViewById(R.id.homeTray3Button).setOnClickListener(v -> loadHomeTrayItems("tray3"));

        itemSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchItemInSelectedTray(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void showAuth() {
        authScreen.setVisibility(View.VISIBLE);
        homeScreen.setVisibility(View.GONE);
        callScreen.setVisibility(View.GONE);
        itemsScreen.setVisibility(View.GONE);
        routineScreen.setVisibility(View.GONE);
        settingsScreen.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);
    }

    private void showScreen(String screen) {
        authScreen.setVisibility(View.GONE);
        homeScreen.setVisibility(View.GONE);
        callScreen.setVisibility(View.GONE);
        itemsScreen.setVisibility(View.GONE);
        routineScreen.setVisibility(View.GONE);
        settingsScreen.setVisibility(View.GONE);
        bottomNav.setVisibility(View.VISIBLE);

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
                selectTray(selectedTrayId);
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
                loadHomeTrayItems("tray1");
                break;
        }
    }

    private void saveUserAndEnter(boolean isSignup) {
        String name = loginNameInput.getText().toString().trim();
        String password = loginPasswordInput.getText().toString().trim();
        if (name.isEmpty()) {
            name = "testUser";
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("password", password);
        user.put("lastLoginAt", System.currentTimeMillis());
        user.put("type", isSignup ? "signup" : "login");
        db.child("users").child("testUser").setValue(user);
        showScreen("home");
    }

    private void selectTray(String trayId) {
        selectedTrayId = trayId;
        tray1Button.setBackgroundResource("tray1".equals(trayId) ? R.drawable.bg_card_selected : R.drawable.bg_card);
        tray2Button.setBackgroundResource("tray2".equals(trayId) ? R.drawable.bg_card_selected : R.drawable.bg_card);
        tray3Button.setBackgroundResource("tray3".equals(trayId) ? R.drawable.bg_card_selected : R.drawable.bg_card);
        tray1Button.setTextColor("tray1".equals(trayId) ? 0xFFFFFFFF : 0xFF111111);
        tray2Button.setTextColor("tray2".equals(trayId) ? 0xFFFFFFFF : 0xFF111111);
        tray3Button.setTextColor("tray3".equals(trayId) ? 0xFFFFFFFF : 0xFF111111);

        String number = trayId.substring(trayId.length() - 1);
        selectedTrayLabel.setText(number + "번");
        db.child("trays").child(trayId).child("name").get().addOnSuccessListener(snapshot -> {
            String name = snapshot.getValue(String.class);
            if (name == null || name.trim().isEmpty()) {
                name = defaultTrayName(trayId);
            }
            selectedTrayTitle.setText(name + " 모듈");
            trayNameInput.setHint("트레이 이름 설정 (" + name + ")");
        });
        loadSelectedTrayItems();
        searchItemInSelectedTray(itemSearchInput.getText().toString().trim());
    }

    private void saveTrayName() {
        String trayName = trayNameInput.getText().toString().trim();
        if (trayName.isEmpty()) {
            Toast.makeText(this, "트레이 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.child("trays").child(selectedTrayId).child("name").setValue(trayName);
        trayNameInput.setText("");
        selectTray(selectedTrayId);
    }

    private void addItemToSelectedTray() {
        String name = itemNameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "물품명을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Object> item = new HashMap<>();
        item.put("itemName", name);
        item.put("trayId", selectedTrayId);
        item.put("createdAt", now);
        item.put("createdAtText", formatTime(now));

        DatabaseReference itemRef = db.child("trays").child(selectedTrayId).child("items").push();
        itemRef.setValue(item).addOnSuccessListener(unused -> {
            itemNameInput.setText("");
            writeHistory("create", selectedTrayId, name, now);
            loadSelectedTrayItems();
            loadHomeTrayItems(selectedTrayId);
            Toast.makeText(this, "물품이 저장됐어요.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSelectedTrayItems() {
        db.child("trays").child(selectedTrayId).child("items")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        itemListText.setText(buildItemList(snapshot));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        itemListText.setText("물품 목록을 불러오지 못했어요.");
                    }
                });
    }

    private void loadHomeTrayItems(String trayId) {
        db.child("trays").child(trayId).child("items")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String number = trayId.substring(trayId.length() - 1);
                        homeTrayItemsText.setText(number + "번 트레이\n" + buildItemList(snapshot));
                        homeFavoriteText.setText(firstItemSummary(snapshot, number));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        homeTrayItemsText.setText("트레이 물품을 불러오지 못했어요.");
                    }
                });
    }

    private void searchItemInSelectedTray(String query) {
        if (query.isEmpty()) {
            itemSearchResultText.setText("검색어를 입력하면 등록 일시를 확인할 수 있어요.");
            return;
        }

        db.child("trays").child(selectedTrayId).child("items")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String name = child.child("itemName").getValue(String.class);
                            String createdAtText = child.child("createdAtText").getValue(String.class);
                            if (name != null && name.contains(query)) {
                                itemSearchResultText.setText(name + " : " + createdAtText + " 등록");
                                return;
                            }
                        }
                        itemSearchResultText.setText(query + " 물품은 현재 트레이에 없어요.");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        itemSearchResultText.setText("검색에 실패했어요.");
                    }
                });
    }

    private String buildItemList(DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            return "등록된 물품이 없어요.";
        }

        StringBuilder builder = new StringBuilder();
        for (DataSnapshot child : snapshot.getChildren()) {
            String name = child.child("itemName").getValue(String.class);
            String createdAtText = child.child("createdAtText").getValue(String.class);
            if (name != null) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(name).append(" : ").append(createdAtText).append(" 등록");
            }
        }
        return builder.length() == 0 ? "등록된 물품이 없어요." : builder.toString();
    }

    private String firstItemSummary(DataSnapshot snapshot, String trayNumber) {
        for (DataSnapshot child : snapshot.getChildren()) {
            String name = child.child("itemName").getValue(String.class);
            String createdAtText = child.child("createdAtText").getValue(String.class);
            if (name != null) {
                return trayNumber + "번 트레이 / " + name + " : " + createdAtText + " 등록";
            }
        }
        return trayNumber + "번 트레이 / 등록된 물품 없음";
    }

    private void seedTestData() {
        db.child("meta").child("testSeeded").get().addOnSuccessListener(snapshot -> {
            Boolean seeded = snapshot.getValue(Boolean.class);
            if (Boolean.TRUE.equals(seeded)) {
                return;
            }
            seedTray("tray1", "외출", new String[]{"안경", "립밤", "지갑"});
            seedTray("tray2", "출근", new String[]{"노트북", "사원증", "이어폰"});
            seedTray("tray3", "육아", new String[]{"기저귀", "물티슈", "젖병"});
            seedLocations();
            db.child("meta").child("testSeeded").setValue(true);
        });
    }

    private void seedTray(String trayId, String trayName, String[] itemNames) {
        db.child("trays").child(trayId).child("name").setValue(trayName);
        for (int i = 0; i < itemNames.length; i++) {
            long createdAt = System.currentTimeMillis() - ((long) (i + 1) * 24 * 60 * 60 * 1000);
            Map<String, Object> item = new HashMap<>();
            item.put("itemName", itemNames[i]);
            item.put("trayId", trayId);
            item.put("createdAt", createdAt);
            item.put("createdAtText", formatTime(createdAt));
            db.child("trays").child(trayId).child("items").push().setValue(item);
        }
    }

    private void seedLocations() {
        Map<String, Object> livingRoom = new HashMap<>();
        livingRoom.put("locationName", "거실");
        livingRoom.put("coordinate", "x:10,y:20");
        db.child("locations").child("livingRoom").setValue(livingRoom);

        Map<String, Object> entrance = new HashMap<>();
        entrance.put("locationName", "현관");
        entrance.put("coordinate", "x:2,y:4");
        db.child("locations").child("entrance").setValue(entrance);
    }

    private void writeHistory(String action, String trayId, String itemName, long time) {
        Map<String, Object> history = new HashMap<>();
        history.put("action", action);
        history.put("trayId", trayId);
        history.put("itemName", itemName);
        history.put("createdAt", time);
        history.put("createdAtText", formatTime(time));
        db.child("storageHistory").push().setValue(history);
    }

    private String defaultTrayName(String trayId) {
        switch (trayId) {
            case "tray2":
                return "출근";
            case "tray3":
                return "육아";
            case "tray1":
            default:
                return "외출";
        }
    }

    private String formatTime(long timeMillis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(new Date(timeMillis));
    }
}
