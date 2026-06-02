package com.example.dx_carry;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;

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
    private TextView homeTrayName1;
    private TextView homeTrayName2;
    private TextView homeTrayName3;
    private TextView routineModeTitle;
    private LinearLayout modeListContainer;
    private View modeCreatePanel;
    private View modeMoreMenu;

    private EditText itemNameInput;
    private EditText itemSearchInput;
    private EditText trayNameInput;
    private EditText loginNameInput;
    private EditText loginPasswordInput;
    private EditText modeNameInput;
    private EditText modeRouteInput;

    private DatabaseReference db;
    private String selectedTrayId = "tray1";
    private String selectedModeId = null;

    private View micButton;
    private View micPulseRing;
    private AnimatorSet micPulseAnimator;

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
        bindRoutineControls();
        bindHomeTrayNames();
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
        homeTrayName1 = findViewById(R.id.tray_name1);
        homeTrayName2 = findViewById(R.id.tray_name2);
        homeTrayName3 = findViewById(R.id.tray_name3);
        routineModeTitle = findViewById(R.id.routineModeTitle);
        modeListContainer = findViewById(R.id.modeListContainer);
        modeCreatePanel = findViewById(R.id.modeCreatePanel);
        modeMoreMenu = findViewById(R.id.modeMoreMenu);

        itemNameInput = findViewById(R.id.itemNameInput);
        itemSearchInput = findViewById(R.id.itemSearchInput);
        trayNameInput = findViewById(R.id.trayNameInput);
        loginNameInput = findViewById(R.id.loginNameInput);
        loginPasswordInput = findViewById(R.id.loginPasswordInput);
        modeNameInput = findViewById(R.id.modeNameInput);
        modeRouteInput = findViewById(R.id.modeRouteInput);

        micButton = findViewById(R.id.micButton);
        micPulseRing = findViewById(R.id.micPulseRing);
    }

    private void bindNavigation() {
        navHome.setOnClickListener(v -> showScreen("home"));
        navCall.setOnClickListener(v -> showScreen("call"));
        navItems.setOnClickListener(v -> showScreen("items"));
        navRoutine.setOnClickListener(v -> showScreen("routine"));
        navSettings.setOnClickListener(v -> showScreen("settings"));
        micButton.setOnClickListener(v -> toggleMicButton());
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
        findViewById(R.id.btn_tray1).setOnClickListener(v -> loadHomeTrayItems("tray1"));
        findViewById(R.id.btn_tray2).setOnClickListener(v -> loadHomeTrayItems("tray2"));
        findViewById(R.id.btn_tray3).setOnClickListener(v -> loadHomeTrayItems("tray3"));

        itemSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchItemAcrossTrays(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void bindRoutineControls() {
        findViewById(R.id.addRoutineButton).setOnClickListener(v -> showModeCreatePanel(false));
        findViewById(R.id.editModeButton).setOnClickListener(v -> showModeCreatePanel(true));
        findViewById(R.id.deleteModeButton).setOnClickListener(v -> deleteCurrentMode());
        findViewById(R.id.saveModeButton).setOnClickListener(v -> saveCurrentMode());
        findViewById(R.id.cancelModeButton).setOnClickListener(v -> {
            selectedModeId = null;
            modeCreatePanel.setVisibility(View.GONE);
        });
    }

    private void bindHomeTrayNames() {
        bindHomeTrayName("tray1", homeTrayName1);
        bindHomeTrayName("tray2", homeTrayName2);
        bindHomeTrayName("tray3", homeTrayName3);
    }

    private void bindHomeTrayName(String trayId, TextView target) {
        db.child("trays").child(trayId).child("name")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String trayName = snapshot.getValue(String.class);
                        if (trayName == null || trayName.trim().isEmpty()) {
                            trayName = defaultTrayName(trayId);
                        }
                        target.setText(trayName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        target.setText(defaultTrayName(trayId));
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
                loadModes();
                break;
            case "settings":
                settingsScreen.setVisibility(View.VISIBLE);
                navSettings.setTextColor(activeColor);
                break;
            case "home":
            default:
                homeScreen.setVisibility(View.VISIBLE);
                navHome.setTextColor(activeColor);
                homeTrayItemsText.setText("트레이를 누르면 저장된 물품이 팝업으로 보여요.");
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
        searchItemAcrossTrays(itemSearchInput.getText().toString().trim());
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
            itemSearchResultText.setText(name + " → " + trayNumber(selectedTrayId) + "번 tray / 방금 전 등록");
            Toast.makeText(this, "물품이 저장됐어요.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadSelectedTrayItems() {
        db.child("trays").child(selectedTrayId).child("items")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        itemListText.setText("물품 이름을 검색하면 보관 트레이를 확인할 수 있어요.");
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
                        showTrayItemsPopup(trayId, buildItemNameList(snapshot));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this, "트레이 물품을 불러오지 못했어요.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchItemAcrossTrays(String query) {
        if (query.isEmpty()) {
            itemSearchResultText.setText("예: 안경 검색 → 1번 tray");
            return;
        }

        db.child("trays")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot traySnapshot : snapshot.getChildren()) {
                            String trayId = traySnapshot.getKey();
                            for (DataSnapshot child : traySnapshot.child("items").getChildren()) {
                                String name = child.child("itemName").getValue(String.class);
                                Long createdAt = child.child("createdAt").getValue(Long.class);
                                if (name != null && name.contains(query)) {
                                    itemSearchResultText.setText(name + " → " + trayNumber(trayId) + "번 tray / " + relativeTime(createdAt) + " 등록");
                                    return;
                                }
                            }
                        }
                        itemSearchResultText.setText(query + " 물품은 아직 등록되지 않았어요.");
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
            Long createdAt = child.child("createdAt").getValue(Long.class);
            if (name != null) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(name).append(" : ").append(relativeTime(createdAt)).append(" 등록");
            }
        }
        return builder.length() == 0 ? "등록된 물품이 없어요." : builder.toString();
    }

    private String buildItemNameList(DataSnapshot snapshot) {
        if (!snapshot.exists()) {
            return "등록된 물품이 없어요.";
        }

        StringBuilder builder = new StringBuilder();
        for (DataSnapshot child : snapshot.getChildren()) {
            String name = child.child("itemName").getValue(String.class);
            if (name != null && !name.trim().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("• ").append(name);
            }
        }
        return builder.length() == 0 ? "등록된 물품이 없어요." : builder.toString();
    }

    private void showTrayItemsPopup(String trayId, String itemList) {
        db.child("trays").child(trayId).child("name").get().addOnSuccessListener(snapshot -> {
            String trayName = snapshot.getValue(String.class);
            if (trayName == null || trayName.trim().isEmpty()) {
                trayName = defaultTrayName(trayId);
            }

            TrayDialogFragment.newInstance(trayName + " 물품", itemList)
                    .show(getSupportFragmentManager(), "trayItemsPopup");
        });
    }

    private String firstItemSummary(DataSnapshot snapshot, String trayNumber) {
        for (DataSnapshot child : snapshot.getChildren()) {
            String name = child.child("itemName").getValue(String.class);
            Long createdAt = child.child("createdAt").getValue(Long.class);
            if (name != null) {
                return trayNumber + "번 트레이 / " + name + " : " + relativeTime(createdAt) + " 등록";
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
            seedTray("tray2", "출근", new String[]{"지갑", "사원증", "이어폰"});
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

    private void showModeCreatePanelLegacy(boolean editMode) {
        modeMoreMenu.setVisibility(View.GONE);
        modeCreatePanel.setVisibility(View.VISIBLE);
        if (editMode) {
            db.child("modes").child("currentMode").get().addOnSuccessListener(snapshot -> {
                String modeName = snapshot.child("modeName").getValue(String.class);
                String route = snapshot.child("route").getValue(String.class);
                modeNameInput.setText(modeName == null ? "" : modeName);
                modeRouteInput.setText(route == null ? "" : route);
            });
        } else {
            modeNameInput.setText("");
            modeRouteInput.setText("");
        }
    }

    private void saveCurrentModeLegacy() {
        String modeName = modeNameInput.getText().toString().trim();
        String route = modeRouteInput.getText().toString().trim();
        if (modeName.isEmpty()) {
            Toast.makeText(this, "모드 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (route.isEmpty()) {
            route = "출근 모듈을 현관 앞으로 이동";
        }

        Map<String, Object> mode = new HashMap<>();
        mode.put("modeId", "currentMode");
        mode.put("modeName", modeName);
        mode.put("route", route);
        mode.put("active", true);
        mode.put("updatedAt", System.currentTimeMillis());

        db.child("modes").child("currentMode").setValue(mode).addOnSuccessListener(unused -> {
            modeCreatePanel.setVisibility(View.GONE);
            loadCurrentMode();
            Toast.makeText(this, "모드가 저장됐어요.", Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteCurrentModeLegacy() {
        db.child("modes").child("currentMode").removeValue().addOnSuccessListener(unused -> {
            modeMoreMenu.setVisibility(View.GONE);
            routineModeTitle.setText("모드 없음");
            TextView status = findViewById(R.id.routineStatusText);
            status.setText("+ 버튼을 눌러 모드를 만들어주세요.");
            Toast.makeText(this, "모드가 삭제됐어요.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCurrentModeLegacy() {
        db.child("modes").child("currentMode").get().addOnSuccessListener(snapshot -> {
            TextView status = findViewById(R.id.routineStatusText);
            String modeName = snapshot.child("modeName").getValue(String.class);
            String route = snapshot.child("route").getValue(String.class);
            if (modeName == null || modeName.trim().isEmpty()) {
                routineModeTitle.setText("출근 루틴");
                status.setText("출근 모듈을 현관 앞으로 이동");
                return;
            }
            routineModeTitle.setText(modeName);
            status.setText(route == null || route.trim().isEmpty() ? "이동 경로 미설정" : route);
        });
    }

    private void showModeCreatePanel(boolean editMode) {
        modeMoreMenu.setVisibility(View.GONE);
        modeCreatePanel.setVisibility(View.VISIBLE);
        if (editMode && selectedModeId != null) {
            db.child("modes").child(selectedModeId).get().addOnSuccessListener(snapshot -> {
                String modeName = snapshot.child("modeName").getValue(String.class);
                String route = snapshot.child("route").getValue(String.class);
                modeNameInput.setText(modeName == null ? "" : modeName);
                modeRouteInput.setText(route == null ? "" : route);
            });
        } else {
            selectedModeId = null;
            modeNameInput.setText("");
            modeRouteInput.setText("");
        }
    }

    private void saveCurrentMode() {
        String modeName = modeNameInput.getText().toString().trim();
        String route = modeRouteInput.getText().toString().trim();
        if (modeName.isEmpty()) {
            Toast.makeText(this, "모드 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (route.isEmpty()) {
            route = "출근 모듈을 현관 앞으로 이동";
        }

        DatabaseReference modeRef = selectedModeId == null
                ? db.child("modes").push()
                : db.child("modes").child(selectedModeId);
        String modeId = modeRef.getKey();

        Map<String, Object> mode = new HashMap<>();
        mode.put("modeId", modeId);
        mode.put("modeName", modeName);
        mode.put("route", route);
        mode.put("active", true);
        mode.put("updatedAt", System.currentTimeMillis());

        modeRef.setValue(mode).addOnSuccessListener(unused -> {
            selectedModeId = null;
            modeCreatePanel.setVisibility(View.GONE);
            loadModes();
            Toast.makeText(this, "모드가 저장됐어요.", Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteCurrentMode() {
        if (selectedModeId == null) {
            modeMoreMenu.setVisibility(View.GONE);
            return;
        }
        db.child("modes").child(selectedModeId).removeValue().addOnSuccessListener(unused -> {
            selectedModeId = null;
            modeMoreMenu.setVisibility(View.GONE);
            loadModes();
            Toast.makeText(this, "모드가 삭제됐어요.", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCurrentMode() {
        loadModes();
    }

    private void loadModes() {
        db.child("modes").get().addOnSuccessListener(snapshot -> {
            modeListContainer.removeAllViews();
            boolean hasMode = false;
            for (DataSnapshot child : snapshot.getChildren()) {
                String modeId = child.getKey();
                String modeName = child.child("modeName").getValue(String.class);
                String route = child.child("route").getValue(String.class);
                if (modeName == null || modeName.trim().isEmpty()) {
                    continue;
                }
                hasMode = true;
                addModeCard(modeId, modeName, route);
            }
            if (!hasMode) {
                TextView empty = new TextView(this);
                empty.setText("+ 버튼을 눌러 모드를 만들어주세요.");
                empty.setTextColor(0xFF74787F);
                empty.setTextSize(14);
                empty.setGravity(android.view.Gravity.CENTER);
                empty.setPadding(0, 22, 0, 10);
                modeListContainer.addView(empty);
            }
        });
    }

    private void addModeCard(String modeId, String modeName, String route) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        LinearLayout cardTop = new LinearLayout(this);
        cardTop.setOrientation(LinearLayout.HORIZONTAL);
        cardTop.setGravity(android.view.Gravity.CENTER_VERTICAL);
        cardTop.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(this);
        title.setText(modeName);
        title.setTextColor(0xFF111111);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText(route == null || route.trim().isEmpty() ? "이동 경로 미설정" : route);
        subtitle.setTextColor(0xFF74787F);
        subtitle.setTextSize(13);
        subtitle.setTypeface(null, android.graphics.Typeface.BOLD);
        subtitle.setPadding(0, dp(12), 0, 0);

        TextView more = new TextView(this);
        more.setText("...");
        more.setTextColor(0xFF111111);
        more.setTextSize(18);
        more.setTypeface(null, android.graphics.Typeface.BOLD);
        more.setGravity(android.view.Gravity.CENTER);
        more.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout inlineMenu = new LinearLayout(this);
        inlineMenu.setOrientation(LinearLayout.HORIZONTAL);
        inlineMenu.setGravity(android.view.Gravity.RIGHT);
        inlineMenu.setPadding(0, dp(10), 0, 0);
        inlineMenu.setVisibility(View.GONE);

        TextView edit = buildInlineModeButton("수정", 0xFF111111);
        TextView delete = buildInlineModeButton("삭제", 0xFFC30B45);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(62), dp(36));
        deleteParams.setMargins(dp(8), 0, 0, 0);
        delete.setLayoutParams(deleteParams);

        edit.setOnClickListener(v -> {
            selectedModeId = modeId;
            modeMoreMenu.setVisibility(View.GONE);
            inlineMenu.setVisibility(View.GONE);
            showModeCreatePanel(true);
        });
        delete.setOnClickListener(v -> {
            selectedModeId = modeId;
            modeMoreMenu.setVisibility(View.GONE);
            inlineMenu.setVisibility(View.GONE);
            deleteCurrentMode();
        });
        more.setOnClickListener(v -> {
            selectedModeId = modeId;
            modeMoreMenu.setVisibility(View.GONE);
            inlineMenu.setVisibility(inlineMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        textBox.addView(title);
        textBox.addView(subtitle);
        cardTop.addView(textBox);
        cardTop.addView(more);
        inlineMenu.addView(edit);
        inlineMenu.addView(delete);
        card.addView(cardTop);
        card.addView(inlineMenu);
        modeListContainer.addView(card);
    }

    private TextView buildInlineModeButton(String label, int textColor) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(13);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setGravity(android.view.Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_card);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(62), dp(36)));
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String trayNumber(String trayId) {
        if ("tray2".equals(trayId)) {
            return "2";
        }
        if ("tray3".equals(trayId)) {
            return "3";
        }
        return "1";
    }

    private String relativeTime(Long timeMillis) {
        if (timeMillis == null) {
            return "등록일 미상";
        }
        long diffMillis = Math.max(0, System.currentTimeMillis() - timeMillis);
        long minutes = diffMillis / (60 * 1000);
        if (minutes < 1) {
            return "방금 전";
        }
        if (minutes < 60) {
            return minutes + "분 전";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "시간 전";
        }
        long days = hours / 24;
        return days + "일 전";
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

    private void toggleMicButton() {
        boolean isCollectingVoice = !micButton.isSelected();
        micButton.setSelected(isCollectingVoice);
        micButton.setContentDescription(isCollectingVoice ? "Voice input active" : "Voice input");

        if (isCollectingVoice) {
            startMicPulse();
        } else {
            stopMicPulse();
        }
    }

    private void startMicPulse() {
        micPulseRing.setVisibility(View.VISIBLE);
        micPulseRing.setAlpha(0.75f);
        micPulseRing.setScaleX(1f);
        micPulseRing.setScaleY(1f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(micPulseRing, View.SCALE_X, 1f, 1.35f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(micPulseRing, View.SCALE_Y, 1f, 1.35f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(micPulseRing, View.ALPHA, 0.75f, 0f);

        micPulseAnimator = new AnimatorSet();
        micPulseAnimator.playTogether(scaleX, scaleY, alpha);
        micPulseAnimator.setDuration(1200);
        micPulseAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (micButton.isSelected()) {
                    startMicPulse();
                }
            }
        });
        micPulseAnimator.start();
    }

    private void stopMicPulse() {
        if (micPulseAnimator != null) {
            micPulseAnimator.cancel();
            micPulseAnimator = null;
        }
        micPulseRing.setVisibility(View.GONE);
        micPulseRing.setAlpha(0f);
        micPulseRing.setScaleX(1f);
        micPulseRing.setScaleY(1f);
    }
}
