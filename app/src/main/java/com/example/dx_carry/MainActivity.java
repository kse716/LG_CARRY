package com.example.dx_carry;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    private View executionLogScreen;
    private View storageHistoryScreen;
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
    private TextView editSearchItemButton;
    private TextView deleteSearchItemButton;
    private TextView homeTrayItemsText;
    private TextView homeTrayName1;
    private TextView homeTrayName2;
    private TextView homeTrayName3;
    private TextView routineModeTitle;
    private LinearLayout modeListContainer;
    private LinearLayout executionLogContainer;
    private LinearLayout storageHistoryContainer;
    private View itemSearchActionRow;
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
    private Query storageHistoryQuery;
    private ValueEventListener storageHistoryListener;
    private String selectedTrayId = "tray1";
    private String selectedModeId = null;
    private String searchedItemTrayId = null;
    private String searchedItemKey = null;
    private String searchedItemName = null;

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
        showAuth();
    }

    private void bindViews() {
        authScreen = findViewById(R.id.authScreen);
        homeScreen = findViewById(R.id.homeScreen);
        callScreen = findViewById(R.id.callScreen);
        itemsScreen = findViewById(R.id.itemsScreen);
        routineScreen = findViewById(R.id.routineScreen);
        settingsScreen = findViewById(R.id.settingsScreen);
        executionLogScreen = findViewById(R.id.executionLogScreen);
        storageHistoryScreen = findViewById(R.id.storageHistoryScreen);
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
        editSearchItemButton = findViewById(R.id.editSearchItemButton);
        deleteSearchItemButton = findViewById(R.id.deleteSearchItemButton);
        homeTrayItemsText = findViewById(R.id.homeTrayItemsText);
        homeTrayName1 = findViewById(R.id.tray_name1);
        homeTrayName2 = findViewById(R.id.tray_name2);
        homeTrayName3 = findViewById(R.id.tray_name3);
        routineModeTitle = findViewById(R.id.routineModeTitle);
        modeListContainer = findViewById(R.id.modeListContainer);
        executionLogContainer = findViewById(R.id.executionLogContainer);
        storageHistoryContainer = findViewById(R.id.storageHistoryContainer);
        itemSearchActionRow = findViewById(R.id.itemSearchActionRow);
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
        findViewById(R.id.storageHistoryButton).setOnClickListener(v -> showScreen("storageHistory"));
        findViewById(R.id.logDetailButton).setOnClickListener(v -> showScreen("logs"));
        findViewById(R.id.backToSettingsButton).setOnClickListener(v -> showScreen("settings"));
        findViewById(R.id.backToSettingsFromHistoryButton).setOnClickListener(v -> showScreen("settings"));
        micButton.setOnClickListener(v -> toggleMicButton());
    }

    private void bindAuth() {
        findViewById(R.id.loginButton).setOnClickListener(v -> saveUserAndEnter(false));
        findViewById(R.id.signupButton).setOnClickListener(v -> saveUserAndEnter(true));
    }

    private void bindTrayControls() {
        findViewById(R.id.addItemButton).setOnClickListener(v -> addItemToSelectedTray());
        findViewById(R.id.saveTrayNameButton).setOnClickListener(v -> saveTrayName());
        editSearchItemButton.setOnClickListener(v -> showEditItemDialog());
        deleteSearchItemButton.setOnClickListener(v -> showDeleteItemDialog());
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
        executionLogScreen.setVisibility(View.GONE);
        storageHistoryScreen.setVisibility(View.GONE);
        bottomNav.setVisibility(View.GONE);
    }

    private void showScreen(String screen) {
        if (!"storageHistory".equals(screen)) {
            detachStorageHistoryListener();
        }
        authScreen.setVisibility(View.GONE);
        homeScreen.setVisibility(View.GONE);
        callScreen.setVisibility(View.GONE);
        itemsScreen.setVisibility(View.GONE);
        routineScreen.setVisibility(View.GONE);
        settingsScreen.setVisibility(View.GONE);
        executionLogScreen.setVisibility(View.GONE);
        storageHistoryScreen.setVisibility(View.GONE);
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
            case "logs":
                executionLogScreen.setVisibility(View.VISIBLE);
                navSettings.setTextColor(activeColor);
                loadExecutionLogs();
                break;
            case "storageHistory":
                storageHistoryScreen.setVisibility(View.VISIBLE);
                navSettings.setTextColor(activeColor);
                loadStorageHistory();
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
            clearSearchedItem();
            writeHistory("create", selectedTrayId, name, now, null);
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
                        itemListText.setText(buildItemNameList(snapshot));
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
            clearSearchedItem();
            itemSearchResultText.setText("물품 이름을 입력하면 DB에서 보관 트레이를 찾아요.");
            itemListText.setText("검색하면 물품의 트레이 번호와 등록 시점을 보여줘요.");
            return;
        }

        db.child("trays")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot traySnapshot : snapshot.getChildren()) {
                            String trayId = traySnapshot.getKey();
                            String trayName = traySnapshot.child("name").getValue(String.class);
                            if (trayName == null || trayName.trim().isEmpty()) {
                                trayName = defaultTrayName(trayId);
                            }
                            for (DataSnapshot child : traySnapshot.child("items").getChildren()) {
                                String name = child.child("itemName").getValue(String.class);
                                Long createdAt = child.child("createdAt").getValue(Long.class);
                                String createdAtText = child.child("createdAtText").getValue(String.class);
                                if (name != null && name.contains(query)) {
                                    searchedItemTrayId = trayId;
                                    searchedItemKey = child.getKey();
                                    searchedItemName = name;
                                    itemSearchResultText.setText(name);
                                    itemListText.setText("트레이 위치 : " + trayName + " (" + trayNumber(trayId) + "번)\n넣은 일시 : " + formatCreatedAt(createdAtText, createdAt));
                                    itemSearchActionRow.setVisibility(View.VISIBLE);
                                    return;
                                }
                            }
                        }
                        clearSearchedItem();
                        itemSearchResultText.setText(query + " 물품은 아직 등록되지 않았어요.");
                        itemListText.setText("검색어를 다시 확인해주세요.");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        clearSearchedItem();
                        itemSearchResultText.setText("검색에 실패했어요.");
                        itemListText.setText("네트워크 상태를 확인해주세요.");
                    }
                });
    }

    private String formatCreatedAt(String createdAtText, Long createdAt) {
        if (createdAtText != null && !createdAtText.trim().isEmpty()) {
            return createdAtText;
        }
        if (createdAt != null) {
            return formatTime(createdAt);
        }
        return "등록일 미상";
    }

    private void clearSearchedItem() {
        searchedItemTrayId = null;
        searchedItemKey = null;
        searchedItemName = null;
        itemSearchActionRow.setVisibility(View.GONE);
    }

    private boolean hasSearchedItem() {
        return searchedItemTrayId != null && searchedItemKey != null;
    }

    private void showEditItemDialog() {
        if (!hasSearchedItem()) {
            Toast.makeText(this, "수정할 물품을 먼저 검색하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.child("trays").child(searchedItemTrayId).child("items").child(searchedItemKey).get()
                .addOnSuccessListener(snapshot -> showEditItemDialogWithData(snapshot));
    }

    private void showEditItemDialogWithData(DataSnapshot snapshot) {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_item_edit, null);
        EditText nameInput = sheetView.findViewById(R.id.editItemNameInput);
        TextView dateInput = sheetView.findViewById(R.id.editItemDateInput);
        Spinner editTraySpinner = sheetView.findViewById(R.id.editTraySpinner);
        Map<String, String> trayIdByLabel = new HashMap<>();

        String currentName = snapshot.child("itemName").getValue(String.class);
        String createdAtText = snapshot.child("createdAtText").getValue(String.class);
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        if (currentName == null || currentName.trim().isEmpty()) {
            currentName = searchedItemName == null ? "" : searchedItemName;
        }

        final String[] selectedItemDate = {formatCreatedAt(createdAtText, createdAt)};
        nameInput.setText(currentName);
        nameInput.setSelectAllOnFocus(true);
        dateInput.setText(selectedItemDate[0]);
        dateInput.setTextColor(0xFF111111);
        dateInput.setOnClickListener(v -> showRoutineDateTimePicker(dateInput, selectedItemDate));
        loadEditTrayOptions(editTraySpinner, trayIdByLabel, searchedItemTrayId);

        BottomSheetDialog sheet = new BottomSheetDialog(this);

        sheetView.findViewById(R.id.cancelEditItemButton).setOnClickListener(v -> sheet.dismiss());
        sheetView.findViewById(R.id.saveEditItemButton).setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            Object selectedTray = editTraySpinner.getSelectedItem();
            String newTrayId = selectedTray == null ? searchedItemTrayId : trayIdByLabel.get(selectedTray.toString());
            if (newTrayId == null || newTrayId.trim().isEmpty()) {
                Toast.makeText(this, "트레이를 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            String newCreatedAtText = selectedItemDate[0].trim();
            updateSearchedItem(newName, newTrayId, newCreatedAtText, sheet);
        });

        sheet.setContentView(sheetView);
        sheet.show();
    }

    private void loadEditTrayOptions(Spinner spinner, Map<String, String> trayIdByLabel, String selectedTrayId) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        trayIdByLabel.clear();

        db.child("trays").get().addOnSuccessListener(snapshot -> {
            adapter.clear();
            trayIdByLabel.clear();
            addEditTrayOption(adapter, trayIdByLabel, snapshot, "tray1");
            addEditTrayOption(adapter, trayIdByLabel, snapshot, "tray2");
            addEditTrayOption(adapter, trayIdByLabel, snapshot, "tray3");
            adapter.notifyDataSetChanged();
            selectEditTrayOption(spinner, trayIdByLabel, selectedTrayId);
        }).addOnFailureListener(error -> {
            adapter.clear();
            trayIdByLabel.clear();
            addFallbackEditTrayOptions(adapter, trayIdByLabel);
            adapter.notifyDataSetChanged();
            selectEditTrayOption(spinner, trayIdByLabel, selectedTrayId);
        });
    }

    private void addEditTrayOption(ArrayAdapter<String> adapter, Map<String, String> trayIdByLabel, DataSnapshot snapshot, String trayId) {
        String trayName = snapshot.child(trayId).child("name").getValue(String.class);
        if (trayName == null || trayName.trim().isEmpty()) {
            trayName = defaultTrayName(trayId);
        }
        String label = trayName.trim() + " (" + trayNumber(trayId) + "번)";
        adapter.add(label);
        trayIdByLabel.put(label, trayId);
    }

    private void addFallbackEditTrayOptions(ArrayAdapter<String> adapter, Map<String, String> trayIdByLabel) {
        addFallbackEditTrayOption(adapter, trayIdByLabel, "tray1");
        addFallbackEditTrayOption(adapter, trayIdByLabel, "tray2");
        addFallbackEditTrayOption(adapter, trayIdByLabel, "tray3");
    }

    private void addFallbackEditTrayOption(ArrayAdapter<String> adapter, Map<String, String> trayIdByLabel, String trayId) {
        String label = defaultTrayName(trayId) + " (" + trayNumber(trayId) + "번)";
        adapter.add(label);
        trayIdByLabel.put(label, trayId);
    }

    private void selectEditTrayOption(Spinner spinner, Map<String, String> trayIdByLabel, String selectedTrayId) {
        if (selectedTrayId == null || spinner.getAdapter() == null) {
            return;
        }
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            Object item = spinner.getAdapter().getItem(i);
            if (item != null && selectedTrayId.equals(trayIdByLabel.get(item.toString()))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void updateSearchedItem(String newName, String newTrayId, String newCreatedAtText, android.app.Dialog dialog) {
        if (!hasSearchedItem()) {
            return;
        }
        if (newName.isEmpty()) {
            Toast.makeText(this, "물품명을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newCreatedAtText.isEmpty()) {
            Toast.makeText(this, "넣은 날짜를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        String oldTrayId = searchedItemTrayId;
        String itemKey = searchedItemKey;

        db.child("trays").child(oldTrayId).child("items").child(itemKey).get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Object> item = new HashMap<>();
                    String oldName = snapshot.child("itemName").getValue(String.class);
                    String oldCreatedAtText = snapshot.child("createdAtText").getValue(String.class);
                    if (oldName == null || oldName.trim().isEmpty()) {
                        oldName = searchedItemName == null ? "" : searchedItemName;
                    }
                    if (oldCreatedAtText == null || oldCreatedAtText.trim().isEmpty()) {
                        oldCreatedAtText = formatCreatedAt(null, snapshot.child("createdAt").getValue(Long.class));
                    }
                    final String previousName = oldName;
                    final String previousCreatedAtText = oldCreatedAtText;
                    Object createdAt = snapshot.child("createdAt").getValue();
                    if (createdAt != null) {
                        item.put("createdAt", createdAt);
                    }
                    item.put("itemName", newName);
                    item.put("trayId", newTrayId);
                    item.put("createdAtText", newCreatedAtText);
                    item.put("updatedAt", now);
                    item.put("updatedAtText", formatTime(now));

                    if (oldTrayId.equals(newTrayId)) {
                        db.child("trays").child(oldTrayId).child("items").child(itemKey)
                                .updateChildren(item)
                                .addOnSuccessListener(unused -> finishItemUpdate(previousName, newName, oldTrayId, newTrayId, previousCreatedAtText, newCreatedAtText, itemKey, dialog));
                    } else {
                        db.child("trays").child(newTrayId).child("items").child(itemKey)
                                .setValue(item)
                                .addOnSuccessListener(unused -> db.child("trays").child(oldTrayId).child("items").child(itemKey)
                                        .removeValue()
                                        .addOnSuccessListener(removed -> finishItemUpdate(previousName, newName, oldTrayId, newTrayId, previousCreatedAtText, newCreatedAtText, itemKey, dialog)));
                    }
                });
    }

    private void finishItemUpdate(String oldName, String newName, String oldTrayId, String newTrayId, String oldCreatedAtText, String newCreatedAtText, String itemKey, android.app.Dialog dialog) {
        long now = System.currentTimeMillis();
        searchedItemName = newName;
        searchedItemTrayId = newTrayId;
        searchedItemKey = itemKey;
        if (!oldTrayId.equals(newTrayId)) {
            Map<String, Object> trayUpdate = new HashMap<>();
            trayUpdate.put("oldTrayId", oldTrayId);
            trayUpdate.put("newTrayId", newTrayId);
            writeHistory("tray_update", newTrayId, newName, now, trayUpdate);
        }
        if (!oldName.equals(newName) || !oldCreatedAtText.equals(newCreatedAtText)) {
            Map<String, Object> infoUpdate = new HashMap<>();
            infoUpdate.put("oldName", oldName);
            infoUpdate.put("newName", newName);
            infoUpdate.put("oldStoredAtText", oldCreatedAtText);
            infoUpdate.put("newStoredAtText", newCreatedAtText);
            writeHistory("info_update", newTrayId, newName, now, infoUpdate);
        }
        dialog.dismiss();
        Toast.makeText(this, "물품 정보가 수정됐어요.", Toast.LENGTH_SHORT).show();
        searchItemAcrossTrays(newName);
        loadSelectedTrayItems();
    }

    private void showDeleteItemDialog() {
        if (!hasSearchedItem()) {
            Toast.makeText(this, "삭제할 물품을 먼저 검색하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_item_delete, null);
        TextView messageText = dialogView.findViewById(R.id.deleteItemMessageText);
        messageText.setText((searchedItemName == null ? "선택한 물품" : searchedItemName) + "을 삭제할까요?");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.cancelDeleteItemButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.confirmDeleteItemButton).setOnClickListener(v -> {
            dialog.dismiss();
            deleteSearchedItem();
        });

        dialog.setOnShowListener(unused -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void deleteSearchedItem() {
        if (!hasSearchedItem()) {
            return;
        }

        String trayId = searchedItemTrayId;
        String itemName = searchedItemName == null ? "삭제된 물품" : searchedItemName;
        long now = System.currentTimeMillis();
        db.child("trays").child(searchedItemTrayId).child("items").child(searchedItemKey)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    writeHistory("delete", trayId, itemName, now, null);
                    clearSearchedItem();
                    itemSearchInput.setText("");
                    itemSearchResultText.setText("물품이 삭제됐어요.");
                    itemListText.setText("다른 물품을 검색해보세요.");
                    loadSelectedTrayItems();
                    Toast.makeText(this, "물품이 삭제됐어요.", Toast.LENGTH_SHORT).show();
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

    private void loadExecutionLogs() {
        executionLogContainer.removeAllViews();

        TextView loading = buildLogEmptyText("실행 로그를 불러오는 중...");
        executionLogContainer.addView(loading);

        db.child("executionLogs").orderByChild("createdAt").limitToLast(30)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        executionLogContainer.removeAllViews();
                        if (!snapshot.exists()) {
                            executionLogContainer.addView(buildLogEmptyText("아직 실행 로그가 없어요."));
                            return;
                        }

                        java.util.ArrayList<DataSnapshot> logs = new java.util.ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            logs.add(child);
                        }
                        for (int i = logs.size() - 1; i >= 0; i--) {
                            addExecutionLogCard(logs.get(i));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        executionLogContainer.removeAllViews();
                        executionLogContainer.addView(buildLogEmptyText("실행 로그를 불러오지 못했어요."));
                    }
                });
    }

    private void loadStorageHistory() {
        storageHistoryContainer.removeAllViews();
        storageHistoryContainer.addView(buildLogEmptyText("보관 기록을 불러오는 중이에요."));

        detachStorageHistoryListener();
        storageHistoryQuery = db.child("storageHistory").orderByChild("createdAt").limitToLast(80);
        storageHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                renderStorageHistory(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                storageHistoryContainer.removeAllViews();
                storageHistoryContainer.addView(buildLogEmptyText("보관 기록을 불러오지 못했어요."));
            }
        };
        storageHistoryQuery.addValueEventListener(storageHistoryListener);
    }

    private void detachStorageHistoryListener() {
        if (storageHistoryQuery != null && storageHistoryListener != null) {
            storageHistoryQuery.removeEventListener(storageHistoryListener);
        }
        storageHistoryQuery = null;
        storageHistoryListener = null;
    }

    private void renderStorageHistory(DataSnapshot snapshot) {
        storageHistoryContainer.removeAllViews();
        List<DataSnapshot> histories = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            histories.add(child);
        }
        if (histories.isEmpty()) {
            storageHistoryContainer.addView(buildLogEmptyText("아직 보관 기록이 없어요."));
            return;
        }
        for (int i = histories.size() - 1; i >= 0; i--) {
            storageHistoryContainer.addView(buildStorageHistoryCard(histories.get(i)));
        }
    }

    private LinearLayout buildStorageHistoryCard(DataSnapshot history) {
        String action = history.child("action").getValue(String.class);
        String itemName = history.child("itemName").getValue(String.class);
        String trayId = history.child("trayId").getValue(String.class);
        String createdAtText = history.child("createdAtText").getValue(String.class);
        Long createdAt = history.child("createdAt").getValue(Long.class);
        if (itemName == null || itemName.trim().isEmpty()) {
            itemName = "이름 없는 물품";
        }
        if ((createdAtText == null || createdAtText.trim().isEmpty()) && createdAt != null) {
            createdAtText = formatTime(createdAt);
        }
        if (createdAtText == null || createdAtText.trim().isEmpty()) {
            createdAtText = "시간 미상";
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView itemNameView = new TextView(this);
        itemNameView.setText(buildHistoryTitle(itemName, action));
        itemNameView.setTextColor(0xFF111111);
        itemNameView.setTextSize(18);
        itemNameView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView historyView = new TextView(this);
        historyView.setText(buildStorageHistoryDetail(action, history, trayId, createdAtText));
        historyView.setTextColor(0xFF74787F);
        historyView.setTextSize(13);
        historyView.setLineSpacing(dp(2), 1.0f);
        historyView.setPadding(0, dp(12), 0, 0);

        card.addView(itemNameView);
        card.addView(historyView);
        return card;
    }

    private String buildStorageHistoryDetail(String action, DataSnapshot history, String trayId, String createdAtText) {
        if ("create".equals(action)) {
            return "물품 생성 시간 : " + createdAtText + "\n"
                    + "보관한 트레이 : " + trayLabel(trayId);
        }
        if ("delete".equals(action)) {
            return "물품 삭제 시간 : " + createdAtText + "\n"
                    + "보관했던 트레이 : " + trayLabel(trayId);
        }
        if ("tray_update".equals(action)) {
            String oldTrayId = history.child("oldTrayId").getValue(String.class);
            String newTrayId = history.child("newTrayId").getValue(String.class);
            return "수정 시간 : " + createdAtText + "\n"
                    + "보관했던 트레이 : " + trayLabel(oldTrayId) + "\n"
                    + "현재 트레이 : " + trayLabel(newTrayId);
        }
        if ("info_update".equals(action)) {
            String oldName = history.child("oldName").getValue(String.class);
            String newName = history.child("newName").getValue(String.class);
            String oldStoredAtText = history.child("oldStoredAtText").getValue(String.class);
            String newStoredAtText = history.child("newStoredAtText").getValue(String.class);
            String changed = buildInfoChangeText(oldName, newName, oldStoredAtText, newStoredAtText);
            return "보관 시간 : " + valueOrFallback(newStoredAtText, createdAtText) + "\n"
                    + "수정 요소 : " + changed + "\n"
                    + "현재 트레이 : " + trayLabel(trayId);
        }
        return "보관 시간 : " + createdAtText + "\n"
                + "현재 트레이 : " + trayLabel(trayId);
    }

    private SpannableString buildHistoryTitle(String itemName, String action) {
        String label = historyActionLabel(action);
        String text = itemName + " " + label;
        SpannableString title = new SpannableString(text);
        int labelStart = itemName.length() + 1;
        title.setSpan(new ForegroundColorSpan(0xFF8A8D94), labelStart, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setSpan(new AbsoluteSizeSpan(12, true), labelStart, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return title;
    }

    private String historyActionLabel(String action) {
        if ("create".equals(action)) {
            return "  |  생성";
        }
        if ("delete".equals(action)) {
            return "  |  삭제";
        }
        return "  |  수정";
    }

    private String buildInfoChangeText(String oldName, String newName, String oldStoredAtText, String newStoredAtText) {
        List<String> changes = new ArrayList<>();
        if (oldName != null && newName != null && !oldName.equals(newName)) {
            changes.add(oldName + " -> " + newName);
        }
        if (oldStoredAtText != null && newStoredAtText != null && !oldStoredAtText.equals(newStoredAtText)) {
            changes.add("보관 시간 " + oldStoredAtText + " -> " + newStoredAtText);
        }
        if (changes.isEmpty()) {
            return "변경 내용 없음";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < changes.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(changes.get(i));
        }
        return builder.toString();
    }

    private String trayLabel(String trayId) {
        if (trayId == null || trayId.trim().isEmpty()) {
            return "트레이 미상";
        }
        return trayNumber(trayId) + "번 트레이";
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void renderStorageHistoryLegacy(DataSnapshot snapshot) {
        storageHistoryContainer.removeAllViews();
        Map<String, Map<String, String>> historyByItem = new LinkedHashMap<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            String itemName = child.child("itemName").getValue(String.class);
            if (itemName == null || itemName.trim().isEmpty()) {
                itemName = "이름 없는 물품";
            } else {
                itemName = itemName.trim();
            }

            String action = child.child("action").getValue(String.class);
            String trayId = child.child("trayId").getValue(String.class);
            String createdAtText = child.child("createdAtText").getValue(String.class);
            Long createdAt = child.child("createdAt").getValue(Long.class);
            if ((createdAtText == null || createdAtText.trim().isEmpty()) && createdAt != null) {
                createdAtText = formatTime(createdAt);
            }
            if (createdAtText == null || createdAtText.trim().isEmpty()) {
                createdAtText = "시간 미상";
            }

            Map<String, String> itemHistory = historyByItem.get(itemName);
            if (itemHistory == null) {
                itemHistory = new HashMap<>();
                itemHistory.put("itemName", itemName);
                itemHistory.put("createHistory", "해당 없음");
                itemHistory.put("deleteHistory", "해당 없음");
                itemHistory.put("keepHistory", "해당 없음");
                itemHistory.put("currentStatus", "보관 중");
                historyByItem.put(itemName, itemHistory);
            }

            if ("create".equals(action)) {
                itemHistory.put("createHistory", createdAtText + " 리스트에 물품 생성");
                itemHistory.put("keepHistory", storageKeepHistorySummary(action, trayId, createdAtText));
                itemHistory.put("currentStatus", "보관 중");
            } else if ("delete".equals(action)) {
                itemHistory.put("deleteHistory", createdAtText + " 삭제");
                itemHistory.put("currentStatus", "삭제됨");
            } else if ("update".equals(action) || "storage".equals(action)) {
                itemHistory.put("keepHistory", storageKeepHistorySummary(action, trayId, createdAtText));
                itemHistory.put("currentStatus", "보관 중");
            }
        }

        if (historyByItem.isEmpty()) {
            storageHistoryContainer.addView(buildLogEmptyText("아직 보관 기록이 없어요."));
            return;
        }

        List<Map<String, String>> histories = new ArrayList<>(historyByItem.values());
        for (int i = histories.size() - 1; i >= 0; i--) {
            storageHistoryContainer.addView(buildStorageHistoryCardLegacy(histories.get(i)));
        }
    }

    private LinearLayout buildStorageHistoryCardLegacy(Map<String, String> history) {
        String itemName = history.get("itemName");
        String createHistory = history.get("createHistory");
        String deleteHistory = history.get("deleteHistory");
        String keepHistory = history.get("keepHistory");
        String currentStatus = history.get("currentStatus");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView itemNameView = new TextView(this);
        itemNameView.setText(itemName);
        itemNameView.setTextColor(0xFF111111);
        itemNameView.setTextSize(18);
        itemNameView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView historyView = new TextView(this);
        historyView.setText(
                "리스트에 물품 생성 이력 : " + createHistory + "\n"
                        + "삭제 이력 : " + deleteHistory + "\n"
                        + "보관 이력 : " + keepHistory + "\n"
                        + "현재 상태 : " + currentStatus
        );
        historyView.setTextColor(0xFF74787F);
        historyView.setTextSize(13);
        historyView.setLineSpacing(dp(2), 1.0f);
        historyView.setPadding(0, dp(12), 0, 0);

        card.addView(itemNameView);
        card.addView(historyView);
        return card;
    }

    private void loadStorageHistoryLegacy() {
        storageHistoryContainer.removeAllViews();
        storageHistoryContainer.addView(buildLogEmptyText("보관 기록을 불러오는 중이에요."));

        db.child("storageHistory").orderByChild("createdAt").limitToLast(80)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        storageHistoryContainer.removeAllViews();
                        List<DataSnapshot> histories = new ArrayList<>();
                        Map<String, String> latestActionByItem = new HashMap<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            histories.add(child);
                            String itemName = child.child("itemName").getValue(String.class);
                            String action = child.child("action").getValue(String.class);
                            if (itemName != null && action != null) {
                                latestActionByItem.put(itemName, action);
                            }
                        }
                        if (histories.isEmpty()) {
                            storageHistoryContainer.addView(buildLogEmptyText("아직 보관 기록이 없어요."));
                            return;
                        }
                        for (int i = histories.size() - 1; i >= 0; i--) {
                            storageHistoryContainer.addView(buildStorageHistoryCardLegacy(histories.get(i), latestActionByItem));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        storageHistoryContainer.removeAllViews();
                        storageHistoryContainer.addView(buildLogEmptyText("보관 기록을 불러오지 못했어요."));
                    }
                });
    }

    private LinearLayout buildStorageHistoryCardLegacy(DataSnapshot snapshot, Map<String, String> latestActionByItem) {
        String action = snapshot.child("action").getValue(String.class);
        String trayId = snapshot.child("trayId").getValue(String.class);
        String itemName = snapshot.child("itemName").getValue(String.class);
        String createdAtText = snapshot.child("createdAtText").getValue(String.class);
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);

        if (itemName == null || itemName.trim().isEmpty()) {
            itemName = "이름 없는 물품";
        }
        if (action == null || action.trim().isEmpty()) {
            action = "storage";
        }
        if ((createdAtText == null || createdAtText.trim().isEmpty()) && createdAt != null) {
            createdAtText = formatTime(createdAt);
        }
        if (createdAtText == null || createdAtText.trim().isEmpty()) {
            createdAtText = "시간 미상";
        }

        String latestAction = latestActionByItem.get(itemName);
        String currentStatus = "delete".equals(latestAction) ? "삭제됨" : "보관 중";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView itemNameView = new TextView(this);
        itemNameView.setText(itemName);
        itemNameView.setTextColor(0xFF111111);
        itemNameView.setTextSize(18);
        itemNameView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView historyView = new TextView(this);
        historyView.setText(
                "리스트에 물품 생성 이력 : " + storageCreateHistoryText(action, createdAtText) + "\n"
                        + "삭제 이력 : " + storageDeleteHistoryText(action, createdAtText) + "\n"
                        + "보관 이력 : " + storageKeepHistoryText(action, trayId, createdAtText) + "\n"
                        + "현재 상태 : " + currentStatus
        );
        historyView.setTextColor(0xFF74787F);
        historyView.setTextSize(13);
        historyView.setLineSpacing(dp(2), 1.0f);
        historyView.setPadding(0, dp(12), 0, 0);

        card.addView(itemNameView);
        card.addView(historyView);
        return card;
    }

    private String storageCreateHistoryText(String action, String createdAtText) {
        return "create".equals(action) ? createdAtText + " 생성" : "해당 없음";
    }

    private String storageDeleteHistoryText(String action, String createdAtText) {
        return "delete".equals(action) ? createdAtText + " 삭제" : "해당 없음";
    }

    private String storageKeepHistoryText(String action, String trayId, String createdAtText) {
        String trayText = trayId == null || trayId.trim().isEmpty() ? "트레이 미상" : trayNumber(trayId) + "번 트레이";
        if ("create".equals(action)) {
            return createdAtText + " " + trayText + " 보관";
        }
        if ("update".equals(action)) {
            return createdAtText + " " + trayText + " 보관 정보 수정";
        }
        return "해당 없음";
    }

    private String storageKeepHistorySummary(String action, String trayId, String createdAtText) {
        String trayText = trayId == null || trayId.trim().isEmpty() ? "트레이 미상" : trayNumber(trayId) + "번 트레이";
        if ("create".equals(action)) {
            return createdAtText + " " + trayText + " 보관";
        }
        if ("update".equals(action)) {
            return createdAtText + " " + trayText + " 보관 정보 수정";
        }
        if ("storage".equals(action)) {
            return createdAtText + " " + trayText + " 보관";
        }
        return "해당 없음";
    }

    private TextView buildLogEmptyText(String message) {
        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextColor(0xFF74787F);
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(0, dp(24), 0, dp(16));
        empty.setTypeface(null, android.graphics.Typeface.BOLD);
        return empty;
    }

    private void addExecutionLogCard(DataSnapshot snapshot) {
        String status = snapshot.child("status").getValue(String.class);
        String title = snapshot.child("title").getValue(String.class);
        String message = snapshot.child("message").getValue(String.class);
        String createdAtText = snapshot.child("createdAtText").getValue(String.class);
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);

        if (status == null || status.trim().isEmpty()) {
            status = "이동";
        }
        if (title == null || title.trim().isEmpty()) {
            title = status + " 상태";
        }
        if (message == null || message.trim().isEmpty()) {
            message = "상세 메시지가 없어요.";
        }
        if ((createdAtText == null || createdAtText.trim().isEmpty()) && createdAt != null) {
            createdAtText = formatTime(createdAt);
        }
        if (createdAtText == null || createdAtText.trim().isEmpty()) {
            createdAtText = "시간 미상";
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView statusView = new TextView(this);
        statusView.setText(status);
        statusView.setTextColor(statusColor(status));
        statusView.setTextSize(12);
        statusView.setTypeface(null, android.graphics.Typeface.BOLD);
        statusView.setBackgroundResource(R.drawable.bg_pill);
        statusView.setGravity(android.view.Gravity.CENTER);
        statusView.setPadding(dp(12), 0, dp(12), 0);
        topRow.addView(statusView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(30)
        ));

        TextView timeView = new TextView(this);
        timeView.setText(createdAtText);
        timeView.setTextColor(0xFF8A8D94);
        timeView.setTextSize(12);
        timeView.setGravity(android.view.Gravity.RIGHT);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        topRow.addView(timeView, timeParams);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xFF111111);
        titleView.setTextSize(18);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(0, dp(12), 0, 0);

        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextColor(0xFF74787F);
        messageView.setTextSize(14);
        messageView.setPadding(0, dp(8), 0, 0);

        card.addView(topRow);
        card.addView(titleView);
        card.addView(messageView);
        executionLogContainer.addView(card);
    }

    private int statusColor(String status) {
        if ("실패".equals(status)) {
            return 0xFFC30B45;
        }
        if ("도착".equals(status)) {
            return 0xFF111111;
        }
        return 0xFF74787F;
    }

    private void writeHistory(String action, String trayId, String itemName, long time) {
        writeHistory(action, trayId, itemName, time, null);
    }

    private void writeHistory(String action, String trayId, String itemName, long time, Map<String, Object> extra) {
        Map<String, Object> history = new HashMap<>();
        history.put("action", action);
        history.put("trayId", trayId);
        history.put("itemName", itemName);
        history.put("createdAt", time);
        history.put("createdAtText", formatTime(time));
        if (extra != null) {
            history.putAll(extra);
        }
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
            route = "이동 경로 미설정";
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
                routineModeTitle.setText("모드 없음");
                status.setText("DB에 저장된 루틴이 없어요.");
                return;
            }
            routineModeTitle.setText(modeName);
            status.setText(route == null || route.trim().isEmpty() ? "이동 경로 미설정" : route);
        });
    }

    private void showModeCreatePanel(boolean editMode) {
        modeMoreMenu.setVisibility(View.GONE);
        modeCreatePanel.setVisibility(View.GONE);

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_routine, null);
        TextView title = sheetView.findViewById(R.id.routineSheetTitle);
        EditText routineNameInput = sheetView.findViewById(R.id.routineNameInput);
        Spinner routineBaseSpinner = sheetView.findViewById(R.id.routineBaseSpinner);
        TextView routineTimeButton = sheetView.findViewById(R.id.routineTimeButton);
        EditText routineRouteInput = sheetView.findViewById(R.id.routineRouteInput);
        final String[] selectedRunTime = {""};
        List<String> selectedWeekdays = new ArrayList<>();
        TextView[] weekdayButtons = {
                sheetView.findViewById(R.id.routineWeekdaySun),
                sheetView.findViewById(R.id.routineWeekdayMon),
                sheetView.findViewById(R.id.routineWeekdayTue),
                sheetView.findViewById(R.id.routineWeekdayWed),
                sheetView.findViewById(R.id.routineWeekdayThu),
                sheetView.findViewById(R.id.routineWeekdayFri),
                sheetView.findViewById(R.id.routineWeekdaySat)
        };
        routineBaseSpinner.setTag("");

        title.setText(editMode ? "루틴 수정" : "루틴 생성");
        bindRoutineWeekdayButtons(weekdayButtons, selectedWeekdays);
        if (editMode && selectedModeId != null) {
            db.child("modes").child(selectedModeId).get().addOnSuccessListener(snapshot -> {
                String modeName = snapshot.child("modeName").getValue(String.class);
                String basePoint = snapshot.child("basePoint").getValue(String.class);
                String runTime = snapshot.child("runTime").getValue(String.class);
                String repeatDays = snapshot.child("repeatDays").getValue(String.class);
                String route = snapshot.child("route").getValue(String.class);
                routineNameInput.setText(modeName == null ? "" : modeName);
                routineBaseSpinner.setTag(basePoint == null ? "" : basePoint);
                selectRoutineBase(routineBaseSpinner, basePoint);
                setSelectedWeekdays(selectedWeekdays, repeatDays);
                updateRoutineWeekdayButtons(weekdayButtons, selectedWeekdays);
                selectedRunTime[0] = runTime == null ? "" : runTime;
                routineTimeButton.setText(selectedRunTime[0].isEmpty() ? "실행 시간 선택" : selectedRunTime[0]);
                routineTimeButton.setTextColor(selectedRunTime[0].isEmpty() ? 0xFF8A8D94 : 0xFF111111);
                routineRouteInput.setText(route == null ? "" : route);
            });
        } else {
            selectedModeId = null;
        }
        loadRoutineBaseOptions(routineBaseSpinner);
        routineTimeButton.setOnClickListener(v -> showRoutineTimePicker(routineTimeButton, selectedRunTime));
        sheetView.findViewById(R.id.cancelRoutineSheetButton).setOnClickListener(v -> {
            selectedModeId = null;
            sheet.dismiss();
        });
        sheetView.findViewById(R.id.saveRoutineSheetButton).setOnClickListener(v -> saveCurrentModeFromSheet(
                routineNameInput.getText().toString().trim(),
                routineBaseSpinner.getSelectedItem() == null ? "" : routineBaseSpinner.getSelectedItem().toString().trim(),
                selectedWeekdaysCsv(selectedWeekdays),
                selectedWeekdaysText(selectedWeekdays),
                selectedRunTime[0],
                routineRouteInput.getText().toString().trim(),
                sheet
        ));
        sheet.setContentView(sheetView);
        sheet.show();
    }

    private void bindRoutineWeekdayButtons(TextView[] buttons, List<String> selectedWeekdays) {
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].setOnClickListener(v -> {
                String code = weekdayCode(index);
                if (selectedWeekdays.contains(code)) {
                    selectedWeekdays.remove(code);
                } else {
                    selectedWeekdays.add(code);
                }
                updateRoutineWeekdayButtons(buttons, selectedWeekdays);
            });
        }
        updateRoutineWeekdayButtons(buttons, selectedWeekdays);
    }

    private void updateRoutineWeekdayButtons(TextView[] buttons, List<String> selectedWeekdays) {
        for (int i = 0; i < buttons.length; i++) {
            boolean selected = selectedWeekdays.contains(weekdayCode(i));
            buttons[i].setBackgroundResource(selected ? R.drawable.bg_weekday_circle_selected : R.drawable.bg_weekday_circle);
            buttons[i].setTextColor(selected ? 0xFFFFFFFF : weekdayColor(i));
        }
    }

    private void setSelectedWeekdays(List<String> selectedWeekdays, String repeatDays) {
        selectedWeekdays.clear();
        if (repeatDays == null || repeatDays.trim().isEmpty()) {
            return;
        }
        String[] days = repeatDays.split(",");
        for (String day : days) {
            String code = day.trim();
            if (!code.isEmpty() && !selectedWeekdays.contains(code)) {
                selectedWeekdays.add(code);
            }
        }
    }

    private String selectedWeekdaysCsv(List<String> selectedWeekdays) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            String code = weekdayCode(i);
            if (selectedWeekdays.contains(code)) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(code);
            }
        }
        return builder.toString();
    }

    private String selectedWeekdaysText(List<String> selectedWeekdays) {
        if (selectedWeekdays.size() == 7) {
            return "매일";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            String code = weekdayCode(i);
            if (selectedWeekdays.contains(code)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(weekdayLabel(i));
            }
        }
        return builder.toString();
    }

    private String weekdayCode(int index) {
        switch (index) {
            case 0:
                return "sun";
            case 1:
                return "mon";
            case 2:
                return "tue";
            case 3:
                return "wed";
            case 4:
                return "thu";
            case 5:
                return "fri";
            case 6:
            default:
                return "sat";
        }
    }

    private String weekdayLabel(int index) {
        switch (index) {
            case 0:
                return "일";
            case 1:
                return "월";
            case 2:
                return "화";
            case 3:
                return "수";
            case 4:
                return "목";
            case 5:
                return "금";
            case 6:
            default:
                return "토";
        }
    }

    private int weekdayColor(int index) {
        if (index == 0) {
            return 0xFFC30B45;
    }
        if (index == 6) {
        return 0xFF2F73B8;
    }
        return 0xFF111111;
}

private void loadRoutineBaseOptions(Spinner spinner) {
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    adapter.add("거점을 불러오는 중");
    spinner.setAdapter(adapter);

        db.child("locations").get().addOnSuccessListener(snapshot -> {
            adapter.clear();
            for (DataSnapshot child : snapshot.getChildren()) {
                String locationName = child.child("name").getValue(String.class);
                if (locationName == null || locationName.trim().isEmpty()) {
                    locationName = child.child("locationName").getValue(String.class);
                }
                if (locationName == null || locationName.trim().isEmpty()) {
                    locationName = child.child("title").getValue(String.class);
                }
                if (locationName == null || locationName.trim().isEmpty()) {
                    locationName = child.getKey();
                }
                if (locationName != null && !locationName.trim().isEmpty()) {
                    adapter.add(locationName.trim());
                }
            }
            if (adapter.getCount() == 0) {
                adapter.add("등록된 거점 없음");
            }
            adapter.notifyDataSetChanged();
            Object selectedBase = spinner.getTag();
            selectRoutineBase(spinner, selectedBase == null ? "" : selectedBase.toString());
        }).addOnFailureListener(error -> {
            adapter.clear();
            adapter.add("거점 불러오기 실패");
            adapter.notifyDataSetChanged();
        });
    }

    private void selectRoutineBase(Spinner spinner, String basePoint) {
        if (basePoint == null || basePoint.trim().isEmpty() || spinner.getAdapter() == null) {
            return;
        }
        String target = basePoint.trim();
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            Object item = spinner.getAdapter().getItem(i);
            if (item != null && target.equals(item.toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void showRoutineTimePicker(TextView timeButton, String[] selectedRunTime) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.popup_round_bg);
        container.setPadding(dp(22), dp(18), dp(22), dp(24));

        TextView title = new TextView(this);
        title.setText("실행 시간 선택");
        title.setTextColor(0xFF111111);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("위아래로 움직여 시간을 설정하세요.");
        subtitle.setTextColor(0xFF74787F);
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(6), 0, dp(14));
        container.addView(subtitle);

        LinearLayout pickerRow = new LinearLayout(this);
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(android.view.Gravity.CENTER);

        NumberPicker hourPicker = new NumberPicker(this);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setFormatter(value -> String.format(Locale.KOREA, "%02d", value));

        NumberPicker minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(value -> String.format(Locale.KOREA, "%02d", value));

        String currentTime = selectedRunTime[0];
        if (currentTime != null && currentTime.matches("\\d{2}:\\d{2}")) {
            hourPicker.setValue(Integer.parseInt(currentTime.substring(0, 2)));
            minutePicker.setValue(Integer.parseInt(currentTime.substring(3, 5)));
        } else {
            Calendar calendar = Calendar.getInstance();
            hourPicker.setValue(calendar.get(Calendar.HOUR_OF_DAY));
            minutePicker.setValue(calendar.get(Calendar.MINUTE));
        }

        TextView separator = new TextView(this);
        separator.setText(":");
        separator.setTextColor(0xFF111111);
        separator.setTextSize(26);
        separator.setTypeface(null, android.graphics.Typeface.BOLD);
        separator.setGravity(android.view.Gravity.CENTER);

        pickerRow.addView(hourPicker, new LinearLayout.LayoutParams(0, dp(150), 1));
        pickerRow.addView(separator, new LinearLayout.LayoutParams(dp(24), dp(150)));
        pickerRow.addView(minutePicker, new LinearLayout.LayoutParams(0, dp(150), 1));
        container.addView(pickerRow);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setPadding(0, dp(16), 0, 0);

        TextView cancel = new TextView(this);
        cancel.setText("취소");
        cancel.setTextColor(0xFFFFFFFF);
        cancel.setTypeface(null, android.graphics.Typeface.BOLD);
        cancel.setGravity(android.view.Gravity.CENTER);
        cancel.setBackgroundResource(R.drawable.bg_primary_button);

        TextView save = new TextView(this);
        save.setText("선택");
        save.setTextColor(0xFFFFFFFF);
        save.setTypeface(null, android.graphics.Typeface.BOLD);
        save.setGravity(android.view.Gravity.CENTER);
        save.setBackgroundResource(R.drawable.bg_card_selected);

        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        saveParams.setMargins(dp(12), 0, 0, 0);
        buttonRow.addView(cancel, cancelParams);
        buttonRow.addView(save, saveParams);
        container.addView(buttonRow);

        cancel.setOnClickListener(v -> sheet.dismiss());
        save.setOnClickListener(v -> {
            selectedRunTime[0] = String.format(Locale.KOREA, "%02d:%02d", hourPicker.getValue(), minutePicker.getValue());
            timeButton.setText(selectedRunTime[0]);
            timeButton.setTextColor(0xFF111111);
            sheet.dismiss();
        });

        sheet.setContentView(container);
        sheet.show();
    }

    private void showRoutineDateTimePicker(TextView dateButton, String[] selectedRunTime) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog timePicker = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                selectedRunTime[0] = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(calendar.getTime());
                dateButton.setText(selectedRunTime[0]);
                dateButton.setTextColor(0xFF111111);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePicker.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void saveCurrentMode() {
        String modeName = modeNameInput.getText().toString().trim();
        String route = modeRouteInput.getText().toString().trim();
        if (modeName.isEmpty()) {
            Toast.makeText(this, "모드 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (route.isEmpty()) {
            route = "이동 경로 미설정";
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

    private void saveCurrentModeFromSheet(String modeName, String basePoint, String repeatDays, String repeatDaysText, String runTime, String route, BottomSheetDialog sheet) {
        if (modeName.isEmpty()) {
            Toast.makeText(this, "루틴 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (basePoint.isEmpty()) {
            Toast.makeText(this, "거점을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (basePoint.contains("등록된 거점 없음") || basePoint.contains("불러오는 중") || basePoint.contains("불러오기 실패")) {
            Toast.makeText(this, "등록된 거점을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (repeatDays.isEmpty()) {
            Toast.makeText(this, "실행할 요일을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (runTime.isEmpty()) {
            Toast.makeText(this, "실행 시간을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (route.isEmpty()) {
            route = "이동 내용 미설정";
        }

        DatabaseReference modeRef = selectedModeId == null
                ? db.child("modes").push()
                : db.child("modes").child(selectedModeId);
        String modeId = modeRef.getKey();

        Map<String, Object> mode = new HashMap<>();
        mode.put("modeId", modeId);
        mode.put("modeName", modeName);
        mode.put("basePoint", basePoint);
        mode.put("repeatDays", repeatDays);
        mode.put("repeatDaysText", repeatDaysText);
        mode.put("runTime", runTime);
        mode.put("scheduleText", repeatDaysText + " " + runTime);
        mode.put("route", route);
        mode.put("active", true);
        mode.put("updatedAt", System.currentTimeMillis());

        modeRef.setValue(mode).addOnSuccessListener(unused -> {
            selectedModeId = null;
            modeCreatePanel.setVisibility(View.GONE);
            sheet.dismiss();
            loadModes();
            Toast.makeText(this, "루틴이 저장됐어요.", Toast.LENGTH_SHORT).show();
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
                String basePoint = child.child("basePoint").getValue(String.class);
                String runTime = child.child("runTime").getValue(String.class);
                String repeatDaysText = child.child("repeatDaysText").getValue(String.class);
                String route = child.child("route").getValue(String.class);
                Boolean active = child.child("active").getValue(Boolean.class);
                if (modeName == null || modeName.trim().isEmpty()) {
                    continue;
                }
                hasMode = true;
                addModeCard(modeId, modeName, basePoint, repeatDaysText, runTime, route, active == null || active);
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

    private void addModeCard(String modeId, String modeName, String basePoint, String repeatDaysText, String runTime, String route, boolean isActive) {
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
        String baseText = basePoint == null || basePoint.trim().isEmpty() ? "거점 미설정" : basePoint.trim();
        String timeText = runTime == null || runTime.trim().isEmpty() ? "시간 미설정" : runTime.trim();
        String routeText = route == null || route.trim().isEmpty() ? "이동 내용 미설정" : route.trim();
        subtitle.setText("거점: " + baseText + "  |  시간: " + timeText + "\n" + routeText);
        String displayBaseText = basePoint == null || basePoint.trim().isEmpty() ? "거점 미설정" : basePoint.trim();
        String displayRepeatText = repeatDaysText == null || repeatDaysText.trim().isEmpty() ? "요일 미설정" : repeatDaysText.trim();
        String displayTimeText = runTime == null || runTime.trim().isEmpty() ? "시간 미설정" : runTime.trim();
        String displayRouteText = route == null || route.trim().isEmpty() ? "이동 내용 미설정" : route.trim();
        subtitle.setText("거점: " + displayBaseText + "  |  반복: " + displayRepeatText + "  |  시간: " + displayTimeText + "\n" + displayRouteText);
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

        LinearLayout activeToggle = new LinearLayout(this);
        activeToggle.setOrientation(LinearLayout.HORIZONTAL);
        activeToggle.setGravity(android.view.Gravity.CENTER_VERTICAL);
        activeToggle.setPadding(dp(3), 0, dp(3), 0);
        activeToggle.setBackgroundResource(isActive ? R.drawable.bg_switch_on : R.drawable.bg_switch_off);
        activeToggle.setContentDescription(isActive ? "루틴 활성화" : "루틴 비활성화");
        activeToggle.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(24)));

        View toggleThumb = new View(this);
        toggleThumb.setBackgroundResource(R.drawable.bg_switch_thumb);
        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        if (isActive) {
            thumbParams.setMargins(dp(18), 0, 0, 0);
        }
        activeToggle.addView(toggleThumb, thumbParams);

        LinearLayout actionStack = new LinearLayout(this);
        actionStack.setOrientation(LinearLayout.VERTICAL);
        actionStack.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        actionStack.addView(activeToggle);
        actionStack.addView(more);

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
        activeToggle.setOnClickListener(v -> toggleModeActive(modeId, !isActive));
        more.setOnClickListener(v -> {
            selectedModeId = modeId;
            modeMoreMenu.setVisibility(View.GONE);
            inlineMenu.setVisibility(inlineMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        textBox.addView(title);
        textBox.addView(subtitle);
        cardTop.addView(textBox);
        cardTop.addView(actionStack);
        inlineMenu.addView(edit);
        inlineMenu.addView(delete);
        card.addView(cardTop);
        card.addView(inlineMenu);
        modeListContainer.addView(card);
    }

    private void toggleModeActive(String modeId, boolean active) {
        db.child("modes").child(modeId).child("active").setValue(active)
                .addOnSuccessListener(unused -> {
                    loadModes();
                    Toast.makeText(this, active ? "루틴이 활성화됐어요." : "루틴이 비활성화됐어요.", Toast.LENGTH_SHORT).show();
                });
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
                return "2번 트레이";
            case "tray3":
                return "3번 트레이";
            case "tray1":
            default:
                return "1번 트레이";
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
