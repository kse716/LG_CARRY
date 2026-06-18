package com.example.dx_carry;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int APP_BACKGROUND = 0xFFF7F9F9;
    private static final int REQUEST_RECORD_AUDIO = 1001;
    private static final int REQUEST_POST_NOTIFICATIONS = 1002;
    private static final String NOTIFICATION_CHANNEL_ID = "carry_notifications";
    private static final String[] VOICE_INTENT_API_URLS = {
            "http://10.37.161.133:5000/voice/parse",
            "http://192.168.0.21:5000/voice/parse"
    };
    private static final String[] MISSION_API_BASE_URLS = {
            "http://10.37.161.133:5001"
    };
    private static final long BATTERY_REFRESH_INTERVAL_MS = 60_000L;
    private static final long MISSION_STATUS_REFRESH_INTERVAL_MS = 5_000L;
    private static final long MISSION_PHASE_REFRESH_INTERVAL_MS = 1_000L;

    private LinearLayout appRoot;
    private FrameLayout contentContainer;
    private FrameLayout bottomNavContainer;
    private String screen = "home";
    private String selectedModule = "1번 트레이";
    private String selectedTrayId = "tray1";
    private final List<TrayData> trays = new ArrayList<>();
    private final List<RoutineData> routines = new ArrayList<>();
    private final List<String> places = new ArrayList<>();
    private final List<MemberData> members = new ArrayList<>();
    private String representativeTrayId = "";
    private String currentUserId = "";
    private String currentUserName = "LG Carry";
    private String currentFamilyId = "";
    private String selectedRoutineId = "routine1";
    private String logFilter = "today";
    private final boolean[] routineEnabled = {true, true, false};
    private final boolean[] routineVisible = {true, true, true};
    private int selectedRoutineHour = 7;
    private int selectedRoutineMinute = 30;
    private int selectedReserveHour = 10;
    private int selectedReserveMinute = 0;
    private int selectedRoutinePlaceIndex = 0;
    private int selectedModuleAddPlaceIndex = 0;
    private boolean loadedPlacesFromDb = false;
    private boolean moduleAddPlaceDropdownOpen = false;
    private int[] moduleAddPlaceDropdownOrder = new int[0];
    private boolean routinePlaceExpanded = false;
    private int voiceState = 0;
    private String recognizedCommand = "거실에 있는 트레이 가져와줘";
    private String recognizedModule = "-";
    private String recognizedLocation = "-";
    private String recognizedIntent = "";
    private String recognizedLabel = "";
    private String recognizedMessage = "";
    private double recognizedConfidence = 0.0;
    private boolean voiceIntentAccepted = false;
    private int parsedVoiceMissionId = -1;
    private AnimatorSet voiceMicPulseAnimator;
    private SpeechRecognizer speechRecognizer;
    private DatabaseReference db;
    private DatabaseReference membersRef;
    private ValueEventListener membersListener;
    private boolean pushNotificationsEnabled = true;
    private boolean moveNotificationsEnabled = true;
    private boolean batteryNotificationsEnabled = true;
    private boolean batteryLowNotified = false;
    private int selectedNotificationSoundIndex = 0;
    private String notificationLogFilter = "today";
    private final Handler batteryRefreshHandler = new Handler(Looper.getMainLooper());
    private String robotBatteryDisplay = "--";
    private boolean batteryRequestInFlight = false;
    private final Runnable batteryRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchRobotBatteryStatus();
            batteryRefreshHandler.postDelayed(this, BATTERY_REFRESH_INTERVAL_MS);
        }
    };
    private final Handler missionStatusHandler = new Handler(Looper.getMainLooper());
    private String robotMissionState = "IDLE";
    private boolean missionStatusRequestInFlight = false;
    private final Runnable missionStatusRunnable = new Runnable() {
        @Override
        public void run() {
            fetchMissionStatus();
            missionStatusHandler.postDelayed(this, MISSION_STATUS_REFRESH_INTERVAL_MS);
        }
    };
    private final Handler missionPhaseHandler = new Handler(Looper.getMainLooper());
    private String robotMissionFrontStatus = "대기중";
    private String robotMissionPhaseLabel = "대기 중";
    private String robotMissionPhaseDetail = "충전 스테이션 대기 중";
    private String lastMissionPhaseToastCode = "";
    private boolean missionPhaseRequestInFlight = false;
    private final Runnable missionPhaseRunnable = new Runnable() {
        @Override
        public void run() {
            fetchMissionPhase();
            missionPhaseHandler.postDelayed(this, MISSION_PHASE_REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(APP_BACKGROUND);
        getWindow().setNavigationBarColor(0xFFFFFFFF);
        int systemUiFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            systemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(systemUiFlags);

        appRoot = findViewById(R.id.appRoot);
        contentContainer = findViewById(R.id.contentContainer);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);

        db = FirebaseDatabase.getInstance().getReference();
        restoreCurrentUser();
        addFallbackPlaces();
        listenToPlaces();
        listenToTrays();
        listenToRoutines();
        listenToMembers();
        createNotificationChannel();
        listenToNotificationSettings();
        listenToBatteryLevel();
        startBatteryRefresh();
        startMissionStatusRefresh();
        startMissionPhaseRefresh();

        ViewCompat.setOnApplyWindowInsetsListener(appRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            contentContainer.setPadding(0, bars.top, 0, 0);
            bottomNavContainer.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });

        render();
    }


    @Override
    protected void onDestroy() {
        batteryRefreshHandler.removeCallbacks(batteryRefreshRunnable);
        missionStatusHandler.removeCallbacks(missionStatusRunnable);
        missionPhaseHandler.removeCallbacks(missionPhaseRunnable);
        stopVoiceMicPulse();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                resetVoiceResult();
                setScreen("voiceListening");
            } else {
                Toast.makeText(this, "마이크 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "CARRY 알림",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("CARRY 이동, 호출, 배터리 알림");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS
            );
        }
    }

    @SuppressLint("MissingPermission")
    private void sendLocalNotification(String title, String message) {
        if (!pushNotificationsEnabled) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionIfNeeded();
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManagerCompat.from(this).notify((int) (System.currentTimeMillis() % 100000), builder.build());
    }

    private void listenToNotificationSettings() {
        db.child("notificationSettings").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean push = snapshot.child("pushEnabled").getValue(Boolean.class);
                Boolean move = snapshot.child("moveCompleteEnabled").getValue(Boolean.class);
                Boolean battery = snapshot.child("batteryLowEnabled").getValue(Boolean.class);
                if (push == null) {
                    push = snapshot.child("push").getValue(Boolean.class);
                }
                if (move == null) {
                    move = snapshot.child("move").getValue(Boolean.class);
                }
                if (battery == null) {
                    battery = snapshot.child("battery").getValue(Boolean.class);
                }
                if (push != null) pushNotificationsEnabled = push;
                if (move != null) moveNotificationsEnabled = move;
                if (battery != null) {
                    batteryNotificationsEnabled = battery;
                    if (battery) batteryLowNotified = false;
                }
                if ("notification".equals(screen)) {
                    render();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void listenToBatteryLevel() {
        db.child("carry").child("battery").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer battery = snapshot.getValue(Integer.class);
                if (battery == null) return;
                if (battery < 10 && batteryNotificationsEnabled && !batteryLowNotified) {
                    batteryLowNotified = true;
                    sendLocalNotification("배터리 부족", "CARRY 배터리가 10% 미만입니다.");
                    saveNotificationLog("배터리 부족", "CARRY", "", "배터리 부족", "battery", System.currentTimeMillis());
                } else if (battery >= 10) {
                    batteryLowNotified = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setScreen(String nextScreen) {
        screen = nextScreen;
        render();
    }

    private void listenToPlaces() {
        listenToPlacePath("locations");
        listenToPlacePath("places");
        listenToPlacePath("bases");
    }

    private void listenToPlacePath(String path) {
        db.child(path).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean changed = false;
                for (DataSnapshot placeSnapshot : snapshot.getChildren()) {
                    String place = parsePlaceName(placeSnapshot);
                    if (!place.isEmpty() && !containsPlace(place)) {
                        if (!loadedPlacesFromDb) {
                            places.clear();
                            loadedPlacesFromDb = true;
                        }
                        places.add(place);
                        changed = true;
                    }
                }
                if (places.isEmpty() && !loadedPlacesFromDb) {
                    addFallbackPlaces();
                    changed = true;
                }
                selectedRoutinePlaceIndex = clampPlaceIndex(selectedRoutinePlaceIndex);
                selectedModuleAddPlaceIndex = clampPlaceIndex(selectedModuleAddPlaceIndex);
                if (changed && ("moduleAdd".equals(screen) || "moduleRename".equals(screen) || "routineEdit".equals(screen) || "location".equals(screen))) {
                    render();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (places.isEmpty() && !loadedPlacesFromDb) {
                    addFallbackPlaces();
                }
            }
        });
    }

    private String parsePlaceName(DataSnapshot snapshot) {
        String value = snapshot.hasChildren() ? null : snapshot.getValue(String.class);
        if (value == null || value.trim().isEmpty()) value = snapshot.child("name").getValue(String.class);
        if (value == null || value.trim().isEmpty()) value = snapshot.child("label").getValue(String.class);
        if (value == null || value.trim().isEmpty()) value = snapshot.child("title").getValue(String.class);
        if (value == null || value.trim().isEmpty()) value = snapshot.child("location").getValue(String.class);
        return value == null ? "" : value.trim();
    }

    private boolean containsPlace(String place) {
        for (String existing : places) {
            if (existing.equals(place)) return true;
        }
        return false;
    }

    private void addFallbackPlaces() {
        places.clear();
        places.add("아이방");
        places.add("안방");
        places.add("거실");
        places.add("현관");
    }

    private String[] placeChoices() {
        if (places.isEmpty()) addFallbackPlaces();
        String[] choices = new String[places.size()];
        for (int i = 0; i < places.size(); i++) {
            choices[i] = places.get(i);
        }
        return choices;
    }

    private int clampPlaceIndex(int index) {
        if (places.isEmpty()) addFallbackPlaces();
        if (index < 0) return 0;
        if (index >= places.size()) return places.size() - 1;
        return index;
    }

    private void listenToTrays() {
        db.child("trays").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trays.clear();
                representativeTrayId = "";
                for (DataSnapshot traySnapshot : snapshot.getChildren()) {
                    trays.add(parseTray(traySnapshot));
                }
                if (trays.isEmpty()) {
                    addFallbackTrays();
                }
                if (findTray(selectedTrayId) == null) {
                    selectedTrayId = trays.get(0).id;
                    selectedModule = trays.get(0).name;
                }
                if ("home".equals(screen) || "modules".equals(screen) || "moduleDetail".equals(screen) || "moduleRename".equals(screen) || "itemSearch".equals(screen) || "routine".equals(screen) || "routineEdit".equals(screen)) {
                    render();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (trays.isEmpty()) {
                    addFallbackTrays();
                }
            }
        });
    }

    private void listenToMembers() {
        if (membersRef != null && membersListener != null) {
            membersRef.removeEventListener(membersListener);
        }
        membersRef = currentFamilyId.isEmpty()
                ? db.child("members")
                : db.child("familyAccounts").child(currentFamilyId).child("members");
        membersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                members.clear();
                for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                    String id = memberSnapshot.getKey() == null ? "" : memberSnapshot.getKey();
                    String name = stringValue(memberSnapshot.child("name"), "");
                    String memberId = stringValue(memberSnapshot.child("id"), id);
                    Long createdAt = memberSnapshot.child("createdAt").getValue(Long.class);
                    if (!memberId.isEmpty() && !name.trim().isEmpty()) {
                        members.add(new MemberData(memberId, name.trim(), createdAt == null ? 0 : createdAt));
                    }
                }
                if ("members".equals(screen) || "moduleDetail".equals(screen)) {
                    render();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        membersRef.addValueEventListener(membersListener);
    }

    private void restoreCurrentUser() {
        SharedPreferences prefs = getSharedPreferences("userSettings", MODE_PRIVATE);
        currentUserId = emptyToFallback(prefs.getString("userId", ""), "");
        currentUserName = emptyToFallback(prefs.getString("userName", ""), "LG Carry");
        currentFamilyId = emptyToFallback(prefs.getString("familyId", ""), "");
    }

    private void saveCurrentUser(String userId, String userName, String familyId) {
        boolean familyChanged = !emptyToFallback(familyId, "").equals(currentFamilyId);
        currentUserId = userId;
        currentUserName = emptyToFallback(userName, userId);
        currentFamilyId = familyId;
        getSharedPreferences("userSettings", MODE_PRIVATE)
                .edit()
                .putString("userId", currentUserId)
                .putString("userName", currentUserName)
                .putString("familyId", currentFamilyId)
                .apply();
        if (familyChanged && db != null) {
            listenToMembers();
        }
    }

    private String currentEditorName() {
        return emptyToFallback(currentUserName, "LG Carry");
    }

    private String currentEditorId() {
        return emptyToFallback(currentUserId, "local-user");
    }

    private void markTrayUpdated(DatabaseReference trayRef) {
        long now = System.currentTimeMillis();
        trayRef.child("updatedBy").setValue(currentEditorName());
        trayRef.child("updatedById").setValue(currentEditorId());
        trayRef.child("updatedAt").setValue(now);
    }

    private TrayData parseTray(DataSnapshot snapshot) {
        String id = snapshot.getKey() == null ? "tray1" : snapshot.getKey();
        String name = snapshot.child("name").getValue(String.class);
        String location = snapshot.child("location").getValue(String.class);
        if (location == null || location.trim().isEmpty()) {
            location = snapshot.child("label_location").getValue(String.class);
        }
        Boolean representative = snapshot.child("representative").getValue(Boolean.class);
        String updatedBy = stringValue(snapshot.child("updatedBy"), "");
        Long updatedAt = snapshot.child("updatedAt").getValue(Long.class);
        TrayData tray = new TrayData(
                id,
                emptyToFallback(name, defaultTrayName(id)),
                emptyToFallback(location, defaultTrayLocation(id)),
                representative != null && representative,
                updatedBy,
                updatedAt == null ? 0 : updatedAt
        );
        if (tray.representative) {
            representativeTrayId = tray.id;
        }
        for (DataSnapshot itemSnapshot : snapshot.child("items").getChildren()) {
            String itemName = itemSnapshot.child("itemName").getValue(String.class);
            if (itemName == null || itemName.trim().isEmpty()) {
                itemName = itemSnapshot.child("name").getValue(String.class);
            }
            if (itemName != null && !itemName.trim().isEmpty()) {
                tray.items.add(itemName.trim());
            }
        }
        return tray;
    }

    private List<TrayData> currentTrays() {
        if (trays.isEmpty()) {
            addFallbackTrays();
        }
        return trays;
    }

    private TrayData selectedTray() {
        TrayData tray = findTray(selectedTrayId);
        if (tray != null) return tray;
        if (currentTrays().isEmpty()) {
            addFallbackTrays();
        }
        selectedTrayId = trays.get(0).id;
        selectedModule = trays.get(0).name;
        return trays.get(0);
    }

    private TrayData findTray(String trayId) {
        for (TrayData tray : trays) {
            if (tray.id.equals(trayId)) return tray;
        }
        return null;
    }

    private void addFallbackTrays() {
        trays.clear();
        TrayData tray1 = new TrayData("tray1", "아이방 서랍", "아이방", true);
        tray1.items.add("리모컨");
        tray1.items.add("안경");
        tray1.items.add("상비약");
        TrayData tray2 = new TrayData("tray2", "2번 트레이", "현관");
        tray2.items.add("차키");
        tray2.items.add("마스크");
        tray2.items.add("소독제");
        TrayData tray3 = new TrayData("tray3", "안방 서랍", "안방");
        tray3.items.add("휴지");
        tray3.items.add("손소독제");
        tray3.items.add("간식");
        trays.add(tray1);
        trays.add(tray2);
        trays.add(tray3);
        representativeTrayId = tray1.id;
    }

    private void saveTray(String trayName, String location) {
        if (trayName.isEmpty()) {
            Toast.makeText(this, "트레이 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference trayRef = db.child("trays").push();
        long now = System.currentTimeMillis();
        Map<String, Object> values = new HashMap<>();
        values.put("name", trayName);
        values.put("location", location);
        values.put("createdBy", currentEditorName());
        values.put("createdById", currentEditorId());
        values.put("createdAt", now);
        values.put("updatedBy", currentEditorName());
        values.put("updatedById", currentEditorId());
        values.put("updatedAt", now);
        trayRef.updateChildren(values).addOnSuccessListener(unused -> {
            selectedTrayId = trayRef.getKey();
            selectedModule = trayName;
            setScreen("modules");
        });
    }

    private void renameSelectedTray(String trayName, String location) {
        if (trayName.isEmpty()) {
            Toast.makeText(this, "트레이 이름을 입력하세요. ", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference trayRef = db.child("trays").child(selectedTrayId);
        Map<String, Object> values = new HashMap<>();
        values.put("name", trayName);
        values.put("location", location);
        values.put("updatedBy", currentEditorName());
        values.put("updatedById", currentEditorId());
        values.put("updatedAt", System.currentTimeMillis());
        trayRef.updateChildren(values).addOnSuccessListener(unused -> {
            selectedModule = trayName;
            setScreen("moduleDetail");
        });
    }

    private void addItemToSelectedTray(String itemName) {
        if (itemName.isEmpty()) {
            Toast.makeText(this, "물건명을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference itemRef = db.child("trays").child(selectedTrayId).child("items").push();
        long now = System.currentTimeMillis();
        Map<String, Object> values = new HashMap<>();
        values.put("itemName", itemName);
        values.put("trayId", selectedTrayId);
        values.put("createdBy", currentEditorName());
        values.put("createdById", currentEditorId());
        values.put("createdAt", now);
        values.put("updatedBy", currentEditorName());
        values.put("updatedById", currentEditorId());
        values.put("updatedAt", now);
        itemRef.updateChildren(values)
                .addOnSuccessListener(unused -> {
                    markTrayUpdated(db.child("trays").child(selectedTrayId));
                    setScreen("moduleDetail");
                });
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String defaultTrayName(String trayId) {
        if ("tray2".equals(trayId)) return "2번 트레이";
        if ("tray3".equals(trayId)) return "3번 트레이";
        return "1번 트레이";
    }

    private String defaultTrayLocation(String trayId) {
        if ("tray2".equals(trayId)) return "현관";
        if ("tray3".equals(trayId)) return "안방";
        return "아이방";
    }

    private void render() {
        contentContainer.removeAllViews();
        bottomNavContainer.removeAllViews();

        switch (screen) {
            case "signup":
                renderSignup();
                break;
            case "voice":
                voiceState = 0;
                resetVoiceResult();
                renderVoice();
                break;
            case "voiceListening":
                voiceState = 1;
                renderVoice();
                startSpeechRecognition();
                break;
            case "voiceResult":
                voiceState = 2;
                renderVoice();
                break;
            case "modules":
                renderModules();
                break;
            case "moduleDetail":
                renderModuleDetail();
                break;
            case "moduleAdd":
                renderModuleAdd();
                break;
            case "moduleRename":
                renderModuleRename();
                break;
            case "itemSearch":
                renderItemSearch();
                break;
            case "itemAdd":
                renderItemAdd();
                break;
            case "routine":
                renderRoutine();
                break;
            case "routineEdit":
                renderRoutineEdit();
                break;
            case "moduleSelect":
                renderModuleSelect();
                break;
            case "routineTime":
                renderTimeSelect("루틴 시간 설정", "routineEdit");
                break;
            case "reserveTime":
                renderTimeSelect("예약 시간 설정", "moduleDetail");
                break;
            case "location":
                renderLocation();
                break;
            case "menu":
                renderMenu();
                break;
            case "map":
                renderMap();
                break;
            case "members":
                renderMembers();
                break;
            case "alarm":
                renderAlarm();
                break;
            case "notification":
                renderNotification();
                break;
            case "notificationLogs":
                renderNotificationLogs();
                break;
            case "logs":
                renderLogs();
                break;
            case "home":
                renderHome();
                break;
            case "login":
            default:
                renderLogin();
                break;
        }
    }


    private void renderLogin() {
        FrameLayout c = inflateFixedCanvas(R.layout.screen_login);
        EditText idInput = c.findViewById(R.id.loginIdInput);
        EditText password = c.findViewById(R.id.loginPasswordInputXml);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        c.findViewById(R.id.loginSubmitButton).setOnClickListener(v -> loginWithId(idInput.getText().toString().trim()));
        c.findViewById(R.id.loginSignupButton).setOnClickListener(v -> setScreen("signup"));
    }

    private void loginWithId(String userId) {
        if (userId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String userName = stringValue(snapshot.child("name"), userId);
                String familyId = stringValue(snapshot.child("familyId"), currentFamilyId);
                saveCurrentUser(userId, userName, familyId);
                setScreen("home");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                saveCurrentUser(userId, userId, currentFamilyId);
                setScreen("home");
            }
        });
    }

    private void renderSignup() {
        FrameLayout c = inflateFixedCanvas(R.layout.screen_signup);
        c.findViewById(R.id.signupBackButton).setOnClickListener(v -> setScreen("login"));
        c.findViewById(R.id.signupSubmitButton).setOnClickListener(v -> {
            EditText idInput = c.findViewById(R.id.signupIdInput);
            EditText nameInput = c.findViewById(R.id.signupNameInput);
            saveSignupUser(
                    idInput.getText().toString().trim(),
                    nameInput.getText().toString().trim()
            );
        });
    }

    private void saveSignupUser(String userId, String userName) {
        if (userId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userName.isEmpty()) {
            Toast.makeText(this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(MainActivity.this, "이미 가입된 아이디입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                createSignupUser(userId, userName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "회원가입 정보를 확인하지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createSignupUser(String userId, String userName) {
        String familyId = generateFamilyId();
        saveCurrentUser(userId, userName, familyId);
        long now = System.currentTimeMillis();
        Map<String, Object> userValues = new HashMap<>();
        userValues.put("id", userId);
        userValues.put("name", userName);
        userValues.put("familyId", familyId);
        userValues.put("createdAt", now);
        db.child("users").child(userId).updateChildren(userValues);

        Map<String, Object> memberValues = new HashMap<>();
        memberValues.put("id", userId);
        memberValues.put("name", userName);
        memberValues.put("createdAt", now);
        memberValues.put("familyId", familyId);
        db.child("familyAccounts").child(familyId).child("id").setValue(familyId);
        db.child("familyAccounts").child(familyId).child("createdAt").setValue(now);
        db.child("familyAccounts").child(familyId).child("members").child(userId).updateChildren(memberValues);
        db.child("members").child(userId).updateChildren(memberValues);
        setScreen("home");
    }

    private String generateFamilyId() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "FAM-" + token;
    }

    private void renderHome() {
        renderHomeXml();
    }

    private void renderHomeXml() {
        View homeView = LayoutInflater.from(this).inflate(R.layout.screen_home, contentContainer, false);
        contentContainer.addView(homeView);
        centerScreen(homeView);
        addBottomNav();

        bindHomeQuickRoutines(homeView);
        bindHomeRepresentativeTray(homeView);
        loadHomeRecentLogs(homeView);
        updateHomeBatteryValue(homeView);
        updateHomeRobotState(homeView);
        fetchRobotBatteryStatus();

        homeView.findViewById(R.id.homeBellIcon).setOnClickListener(v -> setScreen("notification"));
        homeView.findViewById(R.id.homeSettingsIcon).setOnClickListener(v -> setScreen("menu"));
        homeView.findViewById(R.id.homeAllLogsButton).setOnClickListener(v -> setScreen("logs"));
        homeView.findViewById(R.id.homeLogsChevron).setOnClickListener(v -> setScreen("logs"));
        homeView.findViewById(R.id.homeVoiceButton).setOnClickListener(v -> setScreen("voice"));
        homeView.findViewById(R.id.homeEmergencyStopButton).setOnClickListener(v -> stopCarryMission());
        homeView.findViewById(R.id.homeRoutineManageButton).setOnClickListener(v -> setScreen("routine"));
        homeView.findViewById(R.id.homeModuleCard).setOnClickListener(v -> openRepresentativeTrayDetail());
    }

    private void updateHomeBatteryValue(View homeView) {
        TextView batteryValue = homeView.findViewById(R.id.homeBatteryValue);
        if (batteryValue != null) {
            batteryValue.setText(robotBatteryDisplay);
        }
    }

    private void updateHomeRobotState(View homeView) {
        TextView stateBadge = homeView.findViewById(R.id.homeRobotStateBadge);
        if (stateBadge != null) {
            stateBadge.setText(homeRobotStateBadgeText());
        }
        TextView phaseLabel = homeView.findViewById(R.id.homeRobotPhaseLabel);
        if (phaseLabel != null) {
            phaseLabel.setText(emptyToFallback(robotMissionPhaseLabel, "대기 중"));
        }
        TextView phaseDetail = homeView.findViewById(R.id.homeRobotPhaseDetail);
        if (phaseDetail != null) {
            phaseDetail.setText(emptyToFallback(robotMissionPhaseDetail, "충전 스테이션 대기 중"));
        }
    }

    private void bindHomeQuickRoutines(View homeView) {
        List<RoutineData> quickRoutines = new ArrayList<>();
        for (RoutineData routine : routines) {
            if (routine.quickSlot > 0) {
                int insertIndex = 0;
                while (insertIndex < quickRoutines.size() && quickRoutines.get(insertIndex).quickSlot < routine.quickSlot) {
                    insertIndex++;
                }
                quickRoutines.add(insertIndex, routine);
            }
        }
        if (quickRoutines.isEmpty()) {
            for (int i = 0; i < Math.min(2, routines.size()); i++) {
                quickRoutines.add(routines.get(i));
            }
        }
        bindHomeRoutineRow(homeView, 0, quickRoutines.size() > 0 ? quickRoutines.get(0) : null);
        bindHomeRoutineRow(homeView, 1, quickRoutines.size() > 1 ? quickRoutines.get(1) : null);
    }

    private void bindHomeRoutineRow(View homeView, int index, RoutineData routine) {
        TextView title = homeView.findViewById(index == 0 ? R.id.homeQuickRoutineFirstTitle : R.id.homeQuickRoutineSecondTitle);
        TextView sub = homeView.findViewById(index == 0 ? R.id.homeRoutineFirstSub : R.id.homeRoutineSecondSub);
        View track = homeView.findViewById(index == 0 ? R.id.homeQuickRoutineFirstToggleTrack : R.id.homeQuickRoutineSecondToggleTrack);
        View knob = homeView.findViewById(index == 0 ? R.id.homeQuickRoutineFirstToggleKnob : R.id.homeQuickRoutineSecondToggleKnob);
        if (routine == null) {
            title.setText("루틴 없음");
            sub.setText("루틴에서 빠른 루틴을 설정");
            applyRoutineToggleState(track, knob, false);
            track.setOnClickListener(v -> setScreen("routine"));
            return;
        }
        title.setText(routine.title);
        sub.setText(routine.place + " · " + formatDialTime(routine.hour, routine.minute));
        applyRoutineToggleState(track, knob, routine.enabled);
        track.setOnClickListener(v -> {
            boolean enabled = !routine.enabled;
            routine.enabled = enabled;
            applyRoutineToggleState(track, knob, enabled);
            db.child("routines").child(routine.id).child("enabled").setValue(enabled);
        });
    }

    private void bindHomeRepresentativeTray(View homeView) {
        TrayData tray = representativeTray();
        ((TextView) homeView.findViewById(R.id.homeModuleName)).setText(tray.name);
        ((TextView) homeView.findViewById(R.id.homeModuleLocation)).setText("현재 " + tray.location);
        ((TextView) homeView.findViewById(R.id.homeModuleItemCount)).setText("보관 물품 " + tray.items.size() + "개");
        bindHomeItemText((TextView) homeView.findViewById(R.id.homeModuleItemFirst), tray, 0);
        bindHomeItemText((TextView) homeView.findViewById(R.id.homeModuleItemSecond), tray, 1);
        bindHomeItemText((TextView) homeView.findViewById(R.id.homeModuleItemThird), tray, 2);
    }

    private void openRepresentativeTrayDetail() {
        TrayData tray = representativeTray();
        selectedTrayId = tray.id;
        selectedModule = tray.name;
        setScreen("moduleDetail");
    }

    private TrayData representativeTray() {
        for (TrayData tray : currentTrays()) {
            if (tray.representative || tray.id.equals(representativeTrayId)) {
                return tray;
            }
        }
        return currentTrays().get(0);
    }

    private void bindHomeItemText(TextView textView, TrayData tray, int index) {
        if (index >= tray.items.size()) {
            textView.setText("• -");
            textView.setTextColor(0xFF798385);
            return;
        }
        textView.setText("• " + tray.items.get(index));
        textView.setTextColor(0xFF14191B);
    }

    private void loadHomeRecentLogs(View homeView) {
        db.child("logs").orderByChild("createdAt").limitToLast(2).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LogEntry> entries = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    LogEntry entry = parseGenericLogEntry(child);
                    if (entry != null) {
                        insertLogEntryDesc(entries, entry);
                    }
                }
                bindHomeRecentLogRow(homeView, 0, entries.size() > 0 ? entries.get(0) : null);
                bindHomeRecentLogRow(homeView, 1, entries.size() > 1 ? entries.get(1) : null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                bindHomeRecentLogRow(homeView, 0, null);
                bindHomeRecentLogRow(homeView, 1, null);
            }
        });
    }

    private void bindHomeRecentLogRow(View homeView, int index, LogEntry entry) {
        TextView title = homeView.findViewById(index == 0 ? R.id.homeRecentFirstTitle : R.id.homeRecentSecondTitle);
        TextView sub = homeView.findViewById(index == 0 ? R.id.homeRecentFirstSub : R.id.homeRecentSecondSub);
        TextView time = homeView.findViewById(index == 0 ? R.id.homeRecentFirstTime : R.id.homeRecentSecondTime);
        if (entry == null) {
            title.setText("사용 기록 없음");
            sub.setText("호출/루틴 실행 후 표시");
            time.setText("-");
            return;
        }
        title.setText(entry.title);
        sub.setText("• " + entry.place);
        time.setText(formatHomeLogTime(entry.createdAt));
    }

    private String formatHomeLogTime(long createdAt) {
        Calendar log = Calendar.getInstance(Locale.KOREA);
        log.setTimeInMillis(createdAt);
        Calendar today = Calendar.getInstance(Locale.KOREA);
        if (sameDay(log, today)) {
            return new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        }
        today.add(Calendar.DAY_OF_MONTH, -1);
        if (sameDay(log, today)) {
            return "어제 " + new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        }
        return new SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(new Date(createdAt));
    }

    private boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }


    private void renderVoice() {
        View voiceView = LayoutInflater.from(this).inflate(R.layout.screen_voice, contentContainer, false);
        contentContainer.addView(voiceView);
        centerScreen(voiceView);

        View.OnClickListener startOrFinishClick = v -> {
            if (voiceState == 1) {
                stopSpeechRecognition();
                setVoiceRecognitionFailed();
                setScreen("voiceResult");
            } else {
                requestOrStartVoice();
            }
        };

        voiceView.findViewById(R.id.voiceBackIcon).setOnClickListener(v -> setScreen("home"));
        voiceView.findViewById(R.id.voiceIdleMic).setOnClickListener(startOrFinishClick);
        View listeningMic = voiceView.findViewById(R.id.voiceListeningMic);
        listeningMic.setSelected(true);
        listeningMic.setOnClickListener(startOrFinishClick);
        voiceView.findViewById(R.id.voiceStopButton).setOnClickListener(v -> {
            stopSpeechRecognition();
            setVoiceRecognitionFailed();
            setScreen("voiceResult");
        });
        voiceView.findViewById(R.id.voiceRetryButton).setOnClickListener(v -> setScreen("voice"));
        voiceView.findViewById(R.id.voiceExecuteButton).setOnClickListener(v -> {
            if (!voiceIntentAccepted) {
                Toast.makeText(this, emptyToFallback(recognizedMessage, "명령을 다시 말해주세요."), Toast.LENGTH_SHORT).show();
                return;
            }
            startCarryMissionFromVoice();
        });
        voiceView.findViewById(R.id.voiceMissionOneButton).setOnClickListener(v -> startCarryMission(1, "아이방 서랍을 안방으로 전달 후 반납"));
        voiceView.findViewById(R.id.voiceMissionTwoButton).setOnClickListener(v -> startCarryMission(2, "안방 서랍을 아이방으로 전달 후 반납"));
        voiceView.findViewById(R.id.voiceMissionThreeButton).setOnClickListener(v -> startCarryMission(3, "아이방 서랍을 현관으로 전달 후 반납"));
        voiceView.findViewById(R.id.voiceMissionFourButton).setOnClickListener(v -> startCarryMission(4, "안방 서랍을 현관으로 전달 후 반납"));
        voiceView.findViewById(R.id.voiceMissionFiveButton).setOnClickListener(v -> startCarryMission(5, "아이방 서랍을 거실로 전달 후 반납"));
        voiceView.findViewById(R.id.voiceMissionSixButton).setOnClickListener(v -> startCarryMission(6, "안방 서랍을 거실로 전달 후 반납"));
        voiceView.findViewById(R.id.voiceEmergencyStopButton).setOnClickListener(v -> stopCarryMission());

        voiceView.findViewById(R.id.voiceIdleGroup).setVisibility(voiceState == 0 ? View.VISIBLE : View.GONE);
        voiceView.findViewById(R.id.voiceListeningGroup).setVisibility(voiceState == 1 ? View.VISIBLE : View.GONE);
        voiceView.findViewById(R.id.voiceResultGroup).setVisibility(voiceState == 2 ? View.VISIBLE : View.GONE);
        if (voiceState == 1) {
            startVoiceMicPulse(voiceView.findViewById(R.id.voiceMicPulseRing));
        } else {
            stopVoiceMicPulse();
        }

        if (voiceState == 2) {
            TextView resultTitle = voiceView.findViewById(R.id.voiceResultTitle);
            TextView executeButton = voiceView.findViewById(R.id.voiceExecuteButton);
            View resultCheckIcon = voiceView.findViewById(R.id.voiceResultCheckIcon);
            ImageView resultStatusIcon = voiceView.findViewById(R.id.voiceResultStatusIcon);
            boolean speechRecognitionFailed = recognizedCommand == null || recognizedCommand.trim().isEmpty();
            boolean commandRejected = !voiceIntentAccepted;
            if (commandRejected) {
                resultTitle.setVisibility(View.VISIBLE);
                resultTitle.setText(speechRecognitionFailed ? "\uC74C\uC131 \uC778\uC2DD \uC2E4\uD328" : "\uBA85\uB839 \uC778\uC2DD \uC2E4\uD328");
                resultCheckIcon.setVisibility(View.VISIBLE);
                resultStatusIcon.setImageResource(R.drawable.ic_x_white);
                executeButton.setVisibility(View.GONE);
                ((TextView) voiceView.findViewById(R.id.voiceResultCommand)).setText(
                        speechRecognitionFailed ? recognizedMessage : recognizedCommand
                );
            } else {
                resultTitle.setVisibility(View.VISIBLE);
                resultTitle.setText("\uC778\uC2DD \uC644\uB8CC");
                resultCheckIcon.setVisibility(View.VISIBLE);
                resultStatusIcon.setImageResource(R.drawable.ic_check_white);
                executeButton.setVisibility(View.VISIBLE);
                ((TextView) voiceView.findViewById(R.id.voiceResultCommand)).setText(recognizedCommand);
            }
            ((TextView) voiceView.findViewById(R.id.voiceResultModule)).setText(recognizedModule);
            ((TextView) voiceView.findViewById(R.id.voiceResultLocation)).setText(recognizedLocation);
        }
    }



    private void bindItemChip(View rootView, int chipId, EditText input, String value) {
        rootView.findViewById(chipId).setOnClickListener(v -> input.setText(value));
    }

    private void bindRoutinePlace(View rootView) {
        View placeRow = rootView.findViewById(R.id.routinePlaceRow);
        TextView placeValue = rootView.findViewById(R.id.routinePlaceValue);
        if (placeRow != null && placeValue != null) {
            selectedRoutinePlaceIndex = clampPlaceIndex(selectedRoutinePlaceIndex);
            final int[] selectedPlace = {selectedRoutinePlaceIndex};
            placeValue.setText(routinePlaceName(selectedPlace[0]));
            placeRow.setOnClickListener(v -> showPlacePopup(placeRow, selectedPlace, placeValue));
            return;
        }
    }

    private void renderRoutinePlaceSelector(LinearLayout list) {
        list.removeAllViews();
        int[] orderedIndexes = orderedRoutinePlaceIndexes();
        for (int i = 0; i < orderedIndexes.length; i++) {
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE2E7E7);
                list.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
            }
            list.addView(createRoutinePlaceRow(list, orderedIndexes[i]));
        }
        setHeight(list, dp(128));
    }

    private void updateRoutinePlaceLayout(LinearLayout list) {
        int listHeight = routinePlaceExpanded ? dp(166) : dp(56);
        int cardHeight = routinePlaceExpanded ? dp(212) : dp(102);
        setHeight(list, listHeight);

        View placeCard = (View) list.getParent();
        if (placeCard != null) {
            setHeight(placeCard, cardHeight);
        }

        View canvas = placeCard == null ? null : (View) placeCard.getParent();
        if (canvas == null) return;

        View helpText = canvas.findViewById(R.id.routineEditHelpText);
        View saveButton = canvas.findViewById(R.id.routineEditSaveButton);
        int helpTop = dp(335) + cardHeight + dp(12);
        if (helpText != null) setTopMargin(helpText, helpTop);
        if (saveButton != null) setTopMargin(saveButton, helpTop + dp(41));
        setHeight(canvas, helpTop + dp(120));
    }

    private String routinePlaceName(int index) {
        if (places.isEmpty()) addFallbackPlaces();
        return places.get(clampPlaceIndex(index));
    }

    private int[] orderedRoutinePlaceIndexes() {
        if (places.isEmpty()) addFallbackPlaces();
        selectedRoutinePlaceIndex = clampPlaceIndex(selectedRoutinePlaceIndex);
        int[] result = new int[places.size()];
        result[0] = selectedRoutinePlaceIndex;
        int cursor = 1;
        for (int i = 0; i < places.size(); i++) {
            if (i != selectedRoutinePlaceIndex) {
                result[cursor++] = i;
            }
        }
        return result;
    }

    private View createRoutinePlaceRow(LinearLayout list, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setOnClickListener(v -> {
            selectedRoutinePlaceIndex = index;
            routinePlaceExpanded = false;
            renderRoutinePlaceSelector(list);
        });

        TextView label = new TextView(this);
        label.setText(routinePlaceName(index));
        label.setTextColor(0xFF14191B);
        label.setTextSize(15);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(42), 1f));

        TextView marker = new TextView(this);
        marker.setGravity(Gravity.CENTER);
        marker.setIncludeFontPadding(false);
        marker.setTextColor(0xFF14191B);
        marker.setTextSize(22);
        marker.setTypeface(null, android.graphics.Typeface.BOLD);
        marker.setText(index == selectedRoutinePlaceIndex ? "✓" : "");
        row.addView(marker, new LinearLayout.LayoutParams(dp(32), dp(42)));
        return row;
    }

    private int placeIndex(String place) {
        if (place != null) {
            for (int i = 0; i < places.size(); i++) {
                if (place.equals(places.get(i))) return i;
            }
        }
        return 0;
    }

    private void showPlacePopup(View anchor, int[] selectedIndex, TextView valueView) {
        showPlacePopup(anchor, selectedIndex, valueView, null);
    }

    private void showPlacePopup(View anchor, int[] selectedIndex, TextView valueView, PlaceSelectionHandler selectionHandler) {
        ScrollView popupScroll = new ScrollView(this);
        LinearLayout popupList = new LinearLayout(this);
        popupList.setOrientation(LinearLayout.VERTICAL);
        popupList.setBackgroundResource(R.drawable.bg_card);
        popupScroll.setBackgroundResource(R.drawable.bg_card);
        popupScroll.addView(popupList, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        int[] orderedIndexes = orderedPlaceIndexes(selectedIndex[0]);
        PopupWindow popup = new PopupWindow(
                popupScroll,
                anchor.getWidth(),
                Math.min(dp(236), Math.max(dp(56), orderedIndexes.length * dp(47))),
                true
        );
        List<TextView> checks = new ArrayList<>();
        for (int i = 0; i < orderedIndexes.length; i++) {
            final int placeIndex = orderedIndexes[i];
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE2E7E7);
                popupList.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
            }
            View row = createPopupPlaceRow(placeIndex, placeIndex == selectedIndex[0], () -> {
                selectedIndex[0] = placeIndex;
                if (valueView.getId() == R.id.routinePlaceValue) {
                    selectedRoutinePlaceIndex = placeIndex;
                }
                if (selectionHandler != null) {
                    selectionHandler.onPlaceSelected(placeIndex);
                }
                valueView.setText(routinePlaceName(placeIndex));
                for (int checkIndex = 0; checkIndex < checks.size(); checkIndex++) {
                    checks.get(checkIndex).setText(orderedIndexes[checkIndex] == selectedIndex[0] ? "✓" : "");
                }
            });
            checks.add((TextView) row.getTag());
            popupList.addView(row);
        }
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.showAsDropDown(anchor, 0, dp(6));
    }

    private int[] orderedPlaceIndexes(int selectedIndex) {
        if (places.isEmpty()) addFallbackPlaces();
        selectedIndex = clampPlaceIndex(selectedIndex);
        int[] result = new int[places.size()];
        result[0] = selectedIndex;
        int cursor = 1;
        for (int i = 0; i < places.size(); i++) {
            if (i != selectedIndex) result[cursor++] = i;
        }
        return result;
    }

    private View createPopupPlaceRow(int index, boolean selected, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(14), 0);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> onClick.run());

        TextView label = new TextView(this);
        label.setText(routinePlaceName(index));
        label.setTextColor(0xFF14191B);
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView check = new TextView(this);
        check.setGravity(Gravity.CENTER);
        check.setIncludeFontPadding(false);
        check.setTextColor(0xFF14191B);
        check.setTextSize(18);
        check.setTypeface(null, android.graphics.Typeface.BOLD);
        check.setText(selected ? "✓" : "");
        row.addView(check, new LinearLayout.LayoutParams(dp(30), dp(46)));
        row.setTag(check);
        return row;
    }

    private void handleChoiceSelection(String title, int index, String back) {
        if (title.contains("트레이")) {
            List<TrayData> visibleTrays = currentTrays();
            if (index >= 0 && index < visibleTrays.size()) {
                selectedTrayId = visibleTrays.get(index).id;
                selectedModule = visibleTrays.get(index).name;
            }
        } else if (title.contains("위치") || title.contains("거점")) {
            selectedRoutinePlaceIndex = clampPlaceIndex(index);
        }
        setScreen(back);
    }

    private void renderModules() {
        List<TrayData> visibleTrays = currentTrays();
        View modulesView = LayoutInflater.from(this).inflate(R.layout.screen_modules, contentContainer, false);
        contentContainer.addView(modulesView);
        centerScreen(modulesView);
        addBottomNav();

        modulesView.findViewById(R.id.modulesAddButton).setOnClickListener(v -> setScreen("moduleAdd"));
        modulesView.findViewById(R.id.modulesAddBottomButton).setOnClickListener(v -> setScreen("moduleAdd"));
        modulesView.findViewById(R.id.modulesSearchButton).setOnClickListener(v -> setScreen("itemSearch"));

        FrameLayout list = modulesView.findViewById(R.id.modulesList);
        list.removeAllViews();
        int cardHeight = dp(216);
        int gap = dp(16);
        for (int i = 0; i < visibleTrays.size(); i++) {
            TrayData tray = visibleTrays.get(i);
            View card = LayoutInflater.from(this).inflate(R.layout.item_tray_card, list, false);
            bindTrayCard(card, tray);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    cardHeight
            );
            params.topMargin = i * (cardHeight + gap);
            list.addView(card, params);
        }
        int listHeight = visibleTrays.isEmpty() ? 0 : visibleTrays.size() * cardHeight + Math.max(0, visibleTrays.size() - 1) * gap;
        setHeight(list, listHeight);
        setTopMargin(modulesView.findViewById(R.id.modulesAddBottomButton), dp(110) + listHeight + dp(16));
        setHeight(modulesView.findViewById(R.id.modulesCanvas), dp(110) + listHeight + dp(110));
    }

    private void bindTrayCard(View card, TrayData tray) {
        ((TextView) card.findViewById(R.id.trayNameText)).setText(tray.name);
        ((TextView) card.findViewById(R.id.trayLocationText)).setText(tray.location + " 보관 중");
        ((TextView) card.findViewById(R.id.trayItemCountText)).setText("보관 물품 " + tray.items.size() + "개");
        bindTrayItemChips((LinearLayout) card.findViewById(R.id.trayItemsChipGroup), tray.items);
        View representativeButton = card.findViewById(R.id.trayRepresentativeButton);
        ImageView representativeIcon = card.findViewById(R.id.trayRepresentativeIcon);
        representativeIcon.setColorFilter(tray.representative ? 0xFF008E84 : 0xFF798385);
        representativeButton.setOnClickListener(v -> setRepresentativeTray(tray));
        card.setOnClickListener(v -> {
            selectedTrayId = tray.id;
            selectedModule = tray.name;
            setScreen("moduleDetail");
        });
    }

    private void setRepresentativeTray(TrayData selected) {
        representativeTrayId = selected.id;
        for (TrayData tray : trays) {
            tray.representative = tray.id.equals(selected.id);
            db.child("trays").child(tray.id).child("representative").setValue(tray.representative);
        }
        Toast.makeText(this, selected.name + "을(를) 대표 트레이로 설정했습니다.", Toast.LENGTH_SHORT).show();
        render();
    }

    private void bindTrayItemChips(LinearLayout chipGroup, List<String> items) {
        chipGroup.removeAllViews();
        int visibleCount = Math.min(items.size(), 3);
        for (int i = 0; i < visibleCount; i++) {
            TextView chip = new TextView(this);
            chip.setText(items.get(i));
            chip.setGravity(Gravity.CENTER);
            chip.setIncludeFontPadding(false);
            chip.setTextColor(0xFF303B3D);
            chip.setTextSize(12);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setBackgroundResource(R.drawable.bg_chip_soft);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(30)
            );
            if (i > 0) params.leftMargin = dp(8);
            chip.setPadding(dp(12), 0, dp(12), 0);
            chipGroup.addView(chip, params);
        }
    }

    private void renderModuleDetail() {
        TrayData tray = selectedTray();

        View detailView = LayoutInflater.from(this).inflate(R.layout.screen_module_detail, contentContainer, false);
        contentContainer.addView(detailView);
        centerScreen(detailView);
        detailView.findViewById(R.id.moduleDetailBackButton).setOnClickListener(v -> setScreen("modules"));
        ((TextView) detailView.findViewById(R.id.moduleDetailTitle)).setText(tray.name);
        ((TextView) detailView.findViewById(R.id.moduleDetailTrayName)).setText(tray.name);
        ((TextView) detailView.findViewById(R.id.moduleDetailTrayLocation)).setText("현재 위치 · " + tray.location + trayUpdatedSuffix(tray));
        ((TextView) detailView.findViewById(R.id.moduleDetailItemCount)).setText("보관 물품 " + tray.items.size() + "개");
        bindModuleDetailItems(detailView, tray);
        EditText itemInput = detailView.findViewById(R.id.moduleDetailItemInput);
        bindItemChip(detailView, R.id.moduleDetailChipRemote, itemInput, "리모컨");
        bindItemChip(detailView, R.id.moduleDetailChipMedicine, itemInput, "상비약");
        bindItemChip(detailView, R.id.moduleDetailChipGlasses, itemInput, "안경");
        bindItemChip(detailView, R.id.moduleDetailChipKey, itemInput, "열쇠");
        bindItemChip(detailView, R.id.moduleDetailChipCharger, itemInput, "충전기");
        bindItemChip(detailView, R.id.moduleDetailChipMask, itemInput, "마스크");
        bindItemChip(detailView, R.id.moduleDetailChipVitamin, itemInput, "영양제");
        bindItemChip(detailView, R.id.moduleDetailChipTumbler, itemInput, "텀블러");
        detailView.findViewById(R.id.moduleDetailEditButton).setOnClickListener(v -> setScreen("moduleRename"));
        detailView.findViewById(R.id.moduleDetailAddItemButton).setOnClickListener(v -> addItemToSelectedTray(itemInput.getText().toString().trim()));
        View reserveButton = findOptionalView(detailView, "moduleDetailReserveButton");
        if (reserveButton != null) {
            reserveButton.setOnClickListener(v -> showTimePickerSheet("예약 시간 설정", "moduleDetail"));
        }
    }

    private void bindModuleDetailItems(View root, TrayData tray) {
        FrameLayout card = root.findViewById(R.id.moduleDetailItemsCard);
        card.removeAllViews();
        updateModuleDetailLayout(root, tray.items.size());

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, dp(8));
        card.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        if (tray.items.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("보관 중인 물품이 없습니다.");
            emptyText.setTextColor(0xFF798385);
            emptyText.setTextSize(14);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setIncludeFontPadding(false);
            emptyText.setPadding(0, dp(18), 0, dp(18));
            list.addView(emptyText, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            return;
        }

        for (int i = 0; i < tray.items.size(); i++) {
            list.addView(createModuleDetailItemRow(tray.items.get(i)));
            if (i < tray.items.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE2E7E7);
                list.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
            }
        }
    }

    private void updateModuleDetailLayout(View root, int itemCount) {
        int rowCount = Math.max(itemCount, 1);
        int contentHeight = dp(16) + rowCount * dp(56) + Math.max(0, rowCount - 1) * dp(1);
        int cardHeight = Math.max(dp(142), contentHeight);
        int cardTop = dp(225);
        int addLabelTop = cardTop + cardHeight + dp(16);

        setHeight(root.findViewById(R.id.moduleDetailItemsCard), cardHeight);
        setTopMargin(root.findViewById(R.id.moduleDetailAddLabel), addLabelTop);
        setTopMargin(root.findViewById(R.id.moduleDetailItemInput), addLabelTop + dp(28));
        setTopMargin(root.findViewById(R.id.moduleDetailFrequentLabel), addLabelTop + dp(96));
        setTopMargin(root.findViewById(R.id.moduleDetailChipRows), addLabelTop + dp(123));
        setTopMargin(root.findViewById(R.id.moduleDetailAddItemButton), addLabelTop + dp(219));
        setHeight(root.findViewById(R.id.moduleDetailCanvas), addLabelTop + dp(377));
    }

    private View createModuleDetailItemRow(String itemName) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(14), 0);

        View dot = new View(this);
        dot.setBackgroundResource(R.drawable.bg_voice_circle_teal_dark);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(7), dp(7));
        row.addView(dot, dotParams);

        TextView name = new TextView(this);
        name.setText(itemName);
        name.setTextColor(0xFF14191B);
        name.setTextSize(15);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setIncludeFontPadding(false);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, dp(56), 1f);
        nameParams.setMargins(dp(14), 0, 0, 0);
        name.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(name, nameParams);

        TextView remove = new TextView(this);
        remove.setText("×");
        remove.setTextColor(0xFFFF5C5C);
        remove.setTextSize(20);
        remove.setGravity(Gravity.CENTER);
        remove.setIncludeFontPadding(false);
        remove.setBackgroundResource(R.drawable.bg_item_remove_circle);
        remove.setOnClickListener(v -> confirmRemoveItem(itemName));
        row.addView(remove, new LinearLayout.LayoutParams(dp(34), dp(34)));
        return row;
    }

    private void confirmRemoveItem(String itemName) {
        String message = itemName + "을/를 " + selectedTray().name + "에서 삭제 하시겠습니까?";
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("예", (dialog, which) -> removeItemFromSelectedTray(itemName))
                .setNegativeButton("아니오", null)
                .show();
    }

    private void removeItemFromSelectedTray(String itemName) {
        db.child("trays").child(selectedTrayId).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String storedName = itemSnapshot.child("itemName").getValue(String.class);
                    if (storedName == null || storedName.trim().isEmpty()) {
                        storedName = itemSnapshot.child("name").getValue(String.class);
                    }
                    if (itemName.equals(storedName)) {
                        itemSnapshot.getRef().removeValue().addOnSuccessListener(unused -> {
                            markTrayUpdated(db.child("trays").child(selectedTrayId));
                            setScreen("moduleDetail");
                        });
                        return;
                    }
                }
                Toast.makeText(MainActivity.this, "삭제할 물품을 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "물품 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenToRoutines() {
        db.child("routines").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routines.clear();
                for (DataSnapshot routineSnapshot : snapshot.getChildren()) {
                    routines.add(parseRoutine(routineSnapshot));
                }
                if (!routines.isEmpty() && findRoutine(selectedRoutineId) == null) {
                    selectedRoutineId = routines.get(0).id;
                }
                if ("home".equals(screen) || "routine".equals(screen) || "routineEdit".equals(screen)) {
                    render();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "루틴 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RoutineData parseRoutine(DataSnapshot snapshot) {
        String id = snapshot.getKey() == null ? "routine1" : snapshot.getKey();
        String title = snapshot.child("title").getValue(String.class);
        if (title == null || title.trim().isEmpty()) {
            title = snapshot.child("name").getValue(String.class);
        }
        String trayId = snapshot.child("trayId").getValue(String.class);
        String trayName = snapshot.child("trayName").getValue(String.class);
        if (trayName == null || trayName.trim().isEmpty()) {
            trayName = snapshot.child("module").getValue(String.class);
        }
        String place = snapshot.child("place").getValue(String.class);
        if (place == null || place.trim().isEmpty()) {
            place = snapshot.child("location").getValue(String.class);
        }
        if (place == null || place.trim().isEmpty()) {
            place = snapshot.child("destination").getValue(String.class);
        }
        Integer hour = snapshot.child("hour").getValue(Integer.class);
        Integer minute = snapshot.child("minute").getValue(Integer.class);
        if (hour == null || minute == null) {
            int[] parsedTime = parseRoutineTime(snapshot.child("time").getValue(String.class));
            hour = parsedTime[0];
            minute = parsedTime[1];
        }
        Boolean enabled = snapshot.child("enabled").getValue(Boolean.class);
        Integer quickSlot = snapshot.child("quickSlot").getValue(Integer.class);
        String updatedBy = stringValue(snapshot.child("updatedBy"), "");
        String updatedById = stringValue(snapshot.child("updatedById"), "");
        Long updatedAt = snapshot.child("updatedAt").getValue(Long.class);
        return new RoutineData(
                id,
                emptyToFallback(title, defaultRoutineTitle(id)),
                emptyToFallback(trayId, selectedTrayId),
                emptyToFallback(trayName, selectedTray().name),
                hour == null ? 7 : hour,
                minute == null ? 30 : minute,
                emptyToFallback(place, "거실"),
                enabled == null || enabled,
                quickSlot == null ? 0 : quickSlot,
                updatedBy,
                updatedById,
                updatedAt == null ? 0 : updatedAt
        );
    }

    private int[] parseRoutineTime(String time) {
        if (time == null) return new int[]{7, 30};
        try {
            String[] parts = time.replace("오전", "").replace("오후", "").trim().split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (time.contains("오후") && hour < 12) hour += 12;
            return new int[]{hour, minute};
        } catch (Exception ignored) {
            return new int[]{7, 30};
        }
    }

    private void addFallbackRoutines() {
        routines.clear();
        List<TrayData> visibleTrays = currentTrays();
        TrayData first = visibleTrays.get(0);
        TrayData second = visibleTrays.size() > 1 ? visibleTrays.get(1) : first;
        routines.add(new RoutineData("routine1", "아침 거실 세팅", first.id, first.name, 7, 30, "거실", true, 1));
        routines.add(new RoutineData("routine2", "출근 준비", second.id, second.name, 8, 0, "현관", true, 2));
        routines.add(new RoutineData("routine3", "취침 정리", first.id, first.name, 23, 0, "침실", false));
    }

    private RoutineData selectedRoutine() {
        RoutineData routine = findRoutine(selectedRoutineId);
        if (routine != null) return routine;
        return new RoutineData(selectedRoutineId, "새 루틴", selectedTrayId, selectedTray().name, selectedRoutineHour, selectedRoutineMinute, routinePlaceName(selectedRoutinePlaceIndex), true);
    }

    private RoutineData findRoutine(String routineId) {
        for (RoutineData routine : routines) {
            if (routine.id.equals(routineId)) return routine;
        }
        return null;
    }

    private String defaultRoutineTitle(String routineId) {
        if ("routine2".equals(routineId)) return "출근 준비";
        if ("routine3".equals(routineId)) return "취침 정리";
        return "아침 거실 세팅";
    }

    private void prepareNewRoutine() {
        String newRoutineId = db.child("routines").push().getKey();
        selectedRoutineId = newRoutineId == null ? "routine" + System.currentTimeMillis() : newRoutineId;
        TrayData tray = selectedTray();
        selectedTrayId = tray.id;
        selectedModule = tray.name;
        selectedRoutineHour = 7;
        selectedRoutineMinute = 30;
        selectedRoutinePlaceIndex = 0;
        routinePlaceExpanded = false;
        setScreen("routineEdit");
    }

    private void renderModuleAdd() {
        View addView = LayoutInflater.from(this).inflate(R.layout.screen_module_add, contentContainer, false);
        contentContainer.addView(addView);
        centerScreen(addView);
        addView.findViewById(R.id.moduleAddBackButton).setOnClickListener(v -> setScreen("modules"));
        EditText nameInput = addView.findViewById(R.id.moduleAddNameInput);
        bindModuleAddPlace(addView);
        addView.findViewById(R.id.moduleAddSaveButton).setOnClickListener(v ->
                saveTray(nameInput.getText().toString().trim(), routinePlaceName(selectedModuleAddPlaceIndex)));
    }

    private void bindModuleAddPlace(View rootView) {
        LinearLayout selector = rootView.findViewById(R.id.moduleAddLocationSelector);
        FrameLayout dropdown = rootView.findViewById(R.id.moduleAddLocationDropdown);
        selector.removeAllViews();
        selector.setGravity(Gravity.CENTER_VERTICAL);
        selector.setPadding(dp(16), 0, dp(14), 0);
        selector.setClickable(true);
        selector.setFocusable(true);
        selectedModuleAddPlaceIndex = clampPlaceIndex(selectedModuleAddPlaceIndex);
        moduleAddPlaceDropdownOpen = false;
        if (dropdown != null) {
            dropdown.removeAllViews();
            setHeight(dropdown, 0);
            dropdown.setVisibility(View.GONE);
        }
        setTopMargin(rootView.findViewById(R.id.moduleAddHelpText), dp(270));
        setTopMargin(rootView.findViewById(R.id.moduleAddSaveButton), dp(312));

        TextView value = new TextView(this);
        value.setGravity(Gravity.CENTER_VERTICAL);
        value.setIncludeFontPadding(false);
        value.setText(routinePlaceName(selectedModuleAddPlaceIndex));
        value.setTextColor(0xFF14191B);
        value.setTextSize(14);
        value.setTypeface(null, android.graphics.Typeface.BOLD);
        selector.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView selectedCheck = new TextView(this);
        selectedCheck.setGravity(Gravity.CENTER);
        selectedCheck.setIncludeFontPadding(false);
        selectedCheck.setText("\u2713");
        selectedCheck.setTextColor(0xFF14191B);
        selectedCheck.setTextSize(16);
        selectedCheck.setTypeface(null, android.graphics.Typeface.BOLD);
        selector.addView(selectedCheck, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_left);
        chevron.setRotation(270);
        chevron.setContentDescription(null);
        selector.addView(chevron, new LinearLayout.LayoutParams(dp(18), dp(18)));

        selector.setOnClickListener(v -> {
            final int[] selectedPlace = {selectedModuleAddPlaceIndex};
            showPlacePopup(selector, selectedPlace, value, index -> selectedModuleAddPlaceIndex = index);
        });
    }

    private void bindModuleAddPlaceDropdown(View rootView, TextView valueView) {
        FrameLayout dropdown = rootView.findViewById(R.id.moduleAddLocationDropdown);
        dropdown.removeAllViews();
        int dropdownHeight = moduleAddPlaceDropdownOpen
                ? Math.min(dp(188), Math.max(dp(43), places.size() * dp(42) + dp(1)))
                : 0;
        setHeight(dropdown, dropdownHeight);
        dropdown.setVisibility(moduleAddPlaceDropdownOpen ? View.VISIBLE : View.GONE);

        int helpTop = moduleAddPlaceDropdownOpen ? dp(260) + dropdownHeight + dp(18) : dp(270);
        setTopMargin(rootView.findViewById(R.id.moduleAddHelpText), helpTop);
        setTopMargin(rootView.findViewById(R.id.moduleAddSaveButton), helpTop + dp(42));

        if (!moduleAddPlaceDropdownOpen) return;

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        dropdown.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        View divider = new View(this);
        divider.setBackgroundColor(0xFFE2E7E7);
        list.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
        ));

        int[] displayOrder = moduleAddPlaceDropdownOrder.length == places.size()
                ? moduleAddPlaceDropdownOrder
                : orderedPlaceIndexes(selectedModuleAddPlaceIndex);
        moduleAddPlaceDropdownOrder = displayOrder;
        List<TextView> checks = new ArrayList<>();
        for (int i = 0; i < displayOrder.length; i++) {
            final int index = displayOrder[i];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), 0, dp(16), 0);
            row.setBackgroundColor(index == selectedModuleAddPlaceIndex ? 0xFFF2F4F8 : 0xFFFFFFFF);

            TextView label = new TextView(this);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setIncludeFontPadding(false);
            label.setText(routinePlaceName(index));
            label.setTextColor(0xFF14191B);
            label.setTextSize(14);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(42), 1f));

            TextView check = new TextView(this);
            check.setGravity(Gravity.CENTER);
            check.setIncludeFontPadding(false);
            check.setText(index == selectedModuleAddPlaceIndex ? "\u2713" : "");
            check.setTextColor(0xFF14191B);
            check.setTextSize(16);
            check.setTypeface(null, android.graphics.Typeface.BOLD);
            checks.add(check);
            row.addView(check, new LinearLayout.LayoutParams(dp(28), dp(42)));

            row.setOnClickListener(v -> {
                selectedModuleAddPlaceIndex = index;
                valueView.setText(routinePlaceName(index));
                for (int checkIndex = 0; checkIndex < checks.size(); checkIndex++) {
                    checks.get(checkIndex).setText(displayOrder[checkIndex] == selectedModuleAddPlaceIndex ? "\u2713" : "");
                }
            });
            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(42)
            ));
        }
    }

    private void renderModuleRename() {
        TrayData tray = selectedTray();
        View renameView = LayoutInflater.from(this).inflate(R.layout.screen_module_rename, contentContainer, false);
        contentContainer.addView(renameView);
        centerScreen(renameView);
        renameView.findViewById(R.id.moduleRenameBackButton).setOnClickListener(v -> setScreen("moduleDetail"));
        EditText edit = renameView.findViewById(R.id.moduleRenameNameInput);
        edit.setText(tray.name);
        TextView locationValue = renameView.findViewById(R.id.moduleRenameLocationValue);
        final int[] selectedLocationIndex = {placeIndex(tray.location)};
        locationValue.setText(routinePlaceName(selectedLocationIndex[0]));
        renameView.findViewById(R.id.moduleRenameLocationRow).setOnClickListener(v ->
                showPlacePopup(renameView.findViewById(R.id.moduleRenameLocationRow), selectedLocationIndex, locationValue));
        renameView.findViewById(R.id.moduleRenameSaveButton).setOnClickListener(v ->
                renameSelectedTray(edit.getText().toString().trim(), routinePlaceName(selectedLocationIndex[0])));
    }

    private void renderItemSearch() {
        View searchView = LayoutInflater.from(this).inflate(R.layout.screen_item_search, contentContainer, false);
        contentContainer.addView(searchView);
        centerScreen(searchView);
        searchView.findViewById(R.id.itemSearchBackButton).setOnClickListener(v -> setScreen("modules"));
        EditText searchInput = searchView.findViewById(R.id.itemSearchInput);
        bindItemSearchResults(searchView, "");
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                bindItemSearchResults(searchView, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void bindItemSearchResults(View root, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.KOREA);
        LinearLayout list = root.findViewById(R.id.itemSearchResultsList);
        TextView resultCount = root.findViewById(R.id.itemSearchResultCount);
        TextView emptyText = root.findViewById(R.id.itemSearchEmptyText);
        list.removeAllViews();

        if (normalizedQuery.isEmpty()) {
            resultCount.setVisibility(View.GONE);
            resultCount.setText("물품명을 입력하세요");
            emptyText.setVisibility(View.GONE);
            return;
        }

        List<ItemSearchResult> results = findItemSearchResults(normalizedQuery);
        resultCount.setVisibility(View.VISIBLE);
        resultCount.setText("검색 결과 " + results.size() + "건");
        emptyText.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < results.size(); i++) {
            View card = createItemSearchResultCard(results.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(130)
            );
            if (i > 0) params.topMargin = dp(12);
            list.addView(card, params);
        }
    }

    private List<ItemSearchResult> findItemSearchResults(String normalizedQuery) {
        List<ItemSearchResult> results = new ArrayList<>();
        for (TrayData tray : currentTrays()) {
            for (String item : tray.items) {
                if (item.toLowerCase(Locale.KOREA).contains(normalizedQuery)) {
                    results.add(new ItemSearchResult(item, tray.id, tray.name, tray.location));
                }
            }
        }
        return results;
    }

    private View createItemSearchResultCard(ItemSearchResult result) {
        FrameLayout card = new FrameLayout(this);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            selectedTrayId = result.trayId;
            selectedModule = result.trayName;
            setScreen("moduleDetail");
        });

        FrameLayout iconCircle = new FrameLayout(this);
        iconCircle.setBackgroundResource(R.drawable.bg_module_icon_circle);
        FrameLayout.LayoutParams iconCircleParams = new FrameLayout.LayoutParams(dp(56), dp(56));
        iconCircleParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        iconCircleParams.leftMargin = dp(16);
        card.addView(iconCircle, iconCircleParams);

        ImageView trayIcon = new ImageView(this);
        trayIcon.setImageResource(R.drawable.ic_tray);
        trayIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        trayIcon.setContentDescription(null);
        FrameLayout.LayoutParams trayIconParams = new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER);
        iconCircle.addView(trayIcon, trayIconParams);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER_VERTICAL;
        textParams.leftMargin = dp(86);
        textParams.rightMargin = dp(42);
        card.addView(textGroup, textParams);

        TextView itemName = new TextView(this);
        itemName.setText(result.itemName);
        itemName.setTextColor(0xFF14191B);
        itemName.setTextSize(17);
        itemName.setTypeface(null, android.graphics.Typeface.BOLD);
        itemName.setIncludeFontPadding(false);
        textGroup.addView(itemName);

        LinearLayout metaRow = new LinearLayout(this);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaParams.setMargins(0, dp(7), 0, 0);
        textGroup.addView(metaRow, metaParams);

        ImageView boxIcon = new ImageView(this);
        boxIcon.setImageResource(R.drawable.ic_box);
        boxIcon.setColorFilter(0xFF14191B);
        metaRow.addView(boxIcon, new LinearLayout.LayoutParams(dp(14), dp(14)));

        TextView trayName = new TextView(this);
        trayName.setText(result.trayName);
        trayName.setTextColor(0xFF14191B);
        trayName.setTextSize(12);
        trayName.setTypeface(null, android.graphics.Typeface.BOLD);
        trayName.setIncludeFontPadding(false);
        LinearLayout.LayoutParams trayNameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        trayNameParams.setMargins(dp(4), 0, dp(8), 0);
        metaRow.addView(trayName, trayNameParams);

        ImageView pinIcon = new ImageView(this);
        pinIcon.setImageResource(R.drawable.ic_pin);
        pinIcon.setColorFilter(0xFF798385);
        metaRow.addView(pinIcon, new LinearLayout.LayoutParams(dp(14), dp(14)));

        TextView location = new TextView(this);
        location.setText(result.location);
        location.setTextColor(0xFF798385);
        location.setTextSize(12);
        location.setIncludeFontPadding(false);
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        locationParams.setMargins(dp(4), 0, 0, 0);
        metaRow.addView(location, locationParams);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_left);
        chevron.setRotation(180f);
        chevron.setColorFilter(0xFF7C878A);
        chevron.setContentDescription(null);
        FrameLayout.LayoutParams chevronParams = new FrameLayout.LayoutParams(dp(22), dp(22));
        chevronParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        chevronParams.rightMargin = dp(14);
        card.addView(chevron, chevronParams);
        return card;
    }

    private void renderItemAdd() {
        View addView = LayoutInflater.from(this).inflate(R.layout.screen_item_add, contentContainer, false);
        contentContainer.addView(addView);
        centerScreen(addView);
        addView.findViewById(R.id.itemAddBackButton).setOnClickListener(v -> setScreen("moduleDetail"));
        EditText itemInput = addView.findViewById(R.id.itemAddNameInput);
        bindItemChip(addView, R.id.itemChipRemote, itemInput, "리모컨");
        bindItemChip(addView, R.id.itemChipMedicine, itemInput, "상비약");
        bindItemChip(addView, R.id.itemChipGlasses, itemInput, "안경");
        bindItemChip(addView, R.id.itemChipKey, itemInput, "열쇠");
        bindItemChip(addView, R.id.itemChipCharger, itemInput, "충전기");
        bindItemChip(addView, R.id.itemChipMask, itemInput, "마스크");
        bindItemChip(addView, R.id.itemChipVitamin, itemInput, "영양제");
        bindItemChip(addView, R.id.itemChipTumbler, itemInput, "텀블러");
        addView.findViewById(R.id.itemAddSaveButton).setOnClickListener(v -> addItemToSelectedTray(itemInput.getText().toString().trim()));
    }

    private void renderRoutine() {
        View routineView = LayoutInflater.from(this).inflate(R.layout.screen_routine, contentContainer, false);
        contentContainer.addView(routineView);
        centerScreen(routineView);
        addBottomNav();
        routineView.findViewById(R.id.routineAddTopButton).setOnClickListener(v -> prepareNewRoutine());
        View addBottomButton = findOptionalView(routineView, "routineAddBottomButton");
        if (addBottomButton != null) {
            addBottomButton.setOnClickListener(v -> prepareNewRoutine());
        }
        FrameLayout list = routineView.findViewById(R.id.routineList);
        list.removeAllViews();
        for (int i = 0; i < routines.size(); i++) {
            addRoutineCard(list, i, routines.get(i));
        }
        int cardStep = dp(278);
        int listHeight = routines.isEmpty() ? 0 : routines.size() * cardStep;
        setHeight(list, listHeight);
        if (addBottomButton != null) {
            setTopMargin(addBottomButton, dp(108) + listHeight + dp(18));
        }
        setHeight(routineView.findViewById(R.id.routineCanvas), dp(108) + listHeight + dp(120));
    }

    private void addRoutineCard(FrameLayout list, int index, RoutineData routine) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_routine_card, list, false);
        ((TextView) card.findViewById(R.id.routineTitleText)).setText(routine.title);
        TextView updatedText = card.findViewById(R.id.routineUpdatedText);
        if (updatedText != null) {
            updatedText.setText(routineUpdatedText(routine));
            updatedText.setVisibility(routine.updatedAt > 0 ? View.VISIBLE : View.GONE);
        }
        ((TextView) card.findViewById(R.id.routineTrayText)).setText(routine.trayName);
        ((TextView) card.findViewById(R.id.routineTimeText)).setText(formatDialTime(routine.hour, routine.minute));
        ((TextView) card.findViewById(R.id.routinePlaceText)).setText(routine.place);
        View track = card.findViewById(R.id.routineToggleTrack);
        View knob = card.findViewById(R.id.routineToggleKnob);
        applyRoutineToggleState(track, knob, routine.enabled);
        TextView quickButton = card.findViewById(R.id.routineQuickButton);
        quickButton.setText(routine.quickSlot > 0 ? "빠른 루틴 " + routine.quickSlot : "빠른 루틴으로 설정");
        quickButton.setOnClickListener(v -> toggleQuickRoutine(routine));
        track.setOnClickListener(v -> {
            boolean enabled = !routine.enabled;
            routine.enabled = enabled;
            applyRoutineToggleState(track, knob, enabled);
            db.child("routines").child(routine.id).child("enabled").setValue(enabled);
        });
        card.setOnClickListener(v -> {
            selectedRoutineId = routine.id;
            selectedTrayId = routine.trayId;
            selectedModule = routine.trayName;
            selectedRoutineHour = routine.hour;
            selectedRoutineMinute = routine.minute;
            selectedRoutinePlaceIndex = placeIndex(routine.place);
            routinePlaceExpanded = false;
            setScreen("routineEdit");
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = index * dp(278);
        list.addView(card, params);
    }

    private void toggleQuickRoutine(RoutineData selected) {
        if (selected.quickSlot > 0) {
            selected.quickSlot = 0;
            db.child("routines").child(selected.id).child("quickSlot").setValue(0);
            render();
            return;
        }
        boolean slotOneUsed = false;
        boolean slotTwoUsed = false;
        for (RoutineData routine : routines) {
            if (routine.quickSlot == 1) slotOneUsed = true;
            if (routine.quickSlot == 2) slotTwoUsed = true;
        }
        int targetSlot = !slotOneUsed ? 1 : (!slotTwoUsed ? 2 : 2);
        if (slotOneUsed && slotTwoUsed) {
            for (RoutineData routine : routines) {
                if (routine.quickSlot == targetSlot) {
                    routine.quickSlot = 0;
                    db.child("routines").child(routine.id).child("quickSlot").setValue(0);
                    break;
                }
            }
        }
        selected.quickSlot = targetSlot;
        db.child("routines").child(selected.id).child("quickSlot").setValue(targetSlot);
        render();
    }

    private void applyRoutineToggleState(View track, View knob, boolean enabled) {
        track.setBackgroundResource(enabled ? R.drawable.bg_toggle_on : R.drawable.bg_toggle_off);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) knob.getLayoutParams();
        params.gravity = (enabled ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
        knob.setLayoutParams(params);
    }

    private void renderRoutineEdit() {
        RoutineData routine = selectedRoutine();
        View editView = LayoutInflater.from(this).inflate(R.layout.screen_routine_edit, contentContainer, false);
        contentContainer.addView(editView);
        centerScreen(editView);

        EditText nameInput = editView.findViewById(R.id.routineEditNameInput);
        nameInput.setText(routine.title);
        nameInput.setSelection(nameInput.getText().length());
        ((TextView) editView.findViewById(R.id.routineEditTrayValue)).setText(selectedTray().name);
        ((TextView) editView.findViewById(R.id.routineEditTimeValue)).setText("매일 " + formatDialTime(selectedRoutineHour, selectedRoutineMinute));
        TextView updatedText = editView.findViewById(R.id.routineEditUpdatedText);
        if (updatedText != null) {
            updatedText.setText(routineUpdatedText(routine));
            updatedText.setVisibility(routine.updatedAt > 0 ? View.VISIBLE : View.GONE);
        }
        bindRoutinePlace(editView);

        editView.findViewById(R.id.routineEditBackButton).setOnClickListener(v -> setScreen("routine"));
        editView.findViewById(R.id.routineEditTrayRow).setOnClickListener(v -> setScreen("moduleSelect"));
        editView.findViewById(R.id.routineEditTimeRow).setOnClickListener(v -> showTimePickerSheet("루틴 시간 설정", "routineEdit"));
        editView.findViewById(R.id.routineEditSaveButton).setOnClickListener(v -> saveRoutineEdit(nameInput.getText().toString().trim()));
    }

    private void saveRoutineEdit(String routineName) {
        RoutineData routine = selectedRoutine();
        TrayData tray = selectedTray();
        String place = routinePlaceName(selectedRoutinePlaceIndex);
        String title = emptyToFallback(routineName, routine.title);
        DatabaseReference ref = db.child("routines").child(routine.id);
        Map<String, Object> values = new HashMap<>();
        values.put("title", title);
        values.put("name", title);
        values.put("trayId", tray.id);
        values.put("trayName", tray.name);
        values.put("hour", selectedRoutineHour);
        values.put("minute", selectedRoutineMinute);
        values.put("time", formatDialTime(selectedRoutineHour, selectedRoutineMinute));
        values.put("place", place);
        values.put("location", place);
        values.put("destination", place);
        values.put("quickSlot", routine.quickSlot);
        values.put("enabled", routine.enabled);
        values.put("updatedBy", currentEditorName());
        values.put("updatedById", currentEditorId());
        values.put("updatedAt", System.currentTimeMillis());
        ref.updateChildren(values)
                .addOnSuccessListener(unused -> {
                    routinePlaceExpanded = false;
                    setScreen("routine");
                });
    }

    private String routineUpdatedText(RoutineData routine) {
        if (routine.updatedBy == null || routine.updatedBy.trim().isEmpty() || routine.updatedAt <= 0) {
            return "";
        }
        String updatedTime = new SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(new Date(routine.updatedAt));
        return "마지막 수정 " + routine.updatedBy + " " + updatedTime;
    }

    private void renderModuleSelect() {
        List<TrayData> visibleTrays = currentTrays();
        String[] choices = new String[visibleTrays.size()];
        for (int i = 0; i < visibleTrays.size(); i++) {
            choices[i] = visibleTrays.get(i).name;
        }
        choiceScreen("트레이 선택", "트레이 선택", choices, "routineEdit");
    }

    private void renderTimeSelect(String title, String back) {
        screen = back;
        if ("moduleDetail".equals(back)) {
            renderModuleDetail();
        } else {
            renderRoutineEdit();
        }
        contentContainer.post(() -> showTimePickerSheet(title, back));
    }

    private void renderLocation() {
        choiceScreen("이동 위치", "위치 선택", placeChoices(), "routineEdit");
    }

    private void showTimePickerSheet(String title, String back) {
        boolean reserve = "moduleDetail".equals(back);
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_time_picker, null, false);
        ((TextView) sheetView.findViewById(R.id.timePickerTitle)).setText(title);

        NumberPicker hourPicker = sheetView.findViewById(R.id.timePickerHour);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setFormatter(value -> String.format(Locale.KOREA, "%02d", value));
        hourPicker.setValue(reserve ? selectedReserveHour : selectedRoutineHour);

        NumberPicker minutePicker = sheetView.findViewById(R.id.timePickerMinute);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(value -> String.format(Locale.KOREA, "%02d", value));
        minutePicker.setValue(reserve ? selectedReserveMinute : selectedRoutineMinute);

        sheetView.findViewById(R.id.timePickerCancelButton).setOnClickListener(v -> sheet.dismiss());
        sheetView.findViewById(R.id.timePickerSaveButton).setOnClickListener(v -> {
            if (reserve) {
                selectedReserveHour = hourPicker.getValue();
                selectedReserveMinute = minutePicker.getValue();
            } else {
                selectedRoutineHour = hourPicker.getValue();
                selectedRoutineMinute = minutePicker.getValue();
            }
            sheet.dismiss();
            setScreen(back);
        });

        sheet.setContentView(sheetView);
        sheet.show();
    }


    private String formatDialTime(int hour, int minute) {
        String period = hour < 12 ? "오전" : "오후";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.KOREA, "%s %d:%02d", period, displayHour, minute);
    }

    private void renderMenu() {
        View menuView = LayoutInflater.from(this).inflate(R.layout.screen_menu, contentContainer, false);
        contentContainer.addView(menuView);
        centerScreen(menuView);
        addBottomNav();
        menuView.findViewById(R.id.menuNotificationRow).setOnClickListener(v -> setScreen("notification"));
        menuView.findViewById(R.id.menuMapRow).setOnClickListener(v -> setScreen("map"));
        View memberRow = findOptionalView(menuView, "menuMemberRow");
        if (memberRow != null) {
            memberRow.setOnClickListener(v -> setScreen("members"));
        }
    }

    private String trayUpdatedSuffix(TrayData tray) {
        if (tray.updatedBy == null || tray.updatedBy.trim().isEmpty() || tray.updatedAt <= 0) {
            return "";
        }
        String updatedTime = new SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(new Date(tray.updatedAt));
        return " · 마지막 수정 " + tray.updatedBy + " " + updatedTime;
    }

    private void renderMembers() {
        View membersView = LayoutInflater.from(this).inflate(R.layout.screen_members, contentContainer, false);
        contentContainer.addView(membersView);
        centerScreen(membersView);
        membersView.findViewById(R.id.membersBackButton).setOnClickListener(v -> setScreen("menu"));
        EditText idInput = membersView.findViewById(R.id.membersIdInput);
        membersView.findViewById(R.id.membersAddButton).setOnClickListener(v -> {
            addMember(idInput.getText().toString().trim());
            idInput.setText("");
        });
        bindMembersList(membersView);
    }

    private void addMember(String memberId) {
        if (memberId.isEmpty()) {
            Toast.makeText(this, "구성원의 ID를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentFamilyId.isEmpty()) {
            Toast.makeText(this, "가족 계정 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.child("users").child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = stringValue(snapshot.child("name"), "");
                if (name.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "해당 아이디의 사용자를 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                long now = System.currentTimeMillis();
                Map<String, Object> values = new HashMap<>();
                values.put("id", memberId);
                values.put("name", name.trim());
                values.put("familyId", currentFamilyId);
                values.put("createdAt", now);
                Map<String, Object> updates = new HashMap<>();
                updates.put("/members/" + memberId, values);
                updates.put("/familyAccounts/" + currentFamilyId + "/members/" + memberId, values);
                db.updateChildren(updates)
                        .addOnSuccessListener(unused -> Toast.makeText(MainActivity.this, "구성원을 추가했습니다.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(error -> Toast.makeText(MainActivity.this, "구성원 추가에 실패했습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteMember(MemberData member) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(24), dp(24), dp(24), dp(20));
        card.setBackgroundResource(R.drawable.bg_member_dialog);

        FrameLayout iconCircle = new FrameLayout(this);
        iconCircle.setBackgroundResource(R.drawable.bg_item_remove_circle);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        card.addView(iconCircle, iconParams);

        TextView icon = new TextView(this);
        icon.setText("×");
        icon.setTextColor(0xFFE85D5D);
        icon.setTextSize(30);
        icon.setGravity(Gravity.CENTER);
        icon.setIncludeFontPadding(false);
        iconCircle.addView(icon, new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER));

        TextView title = new TextView(this);
        title.setText("구성원을 삭제할까요?");
        title.setTextColor(0xFF12181B);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setIncludeFontPadding(false);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, dp(18), 0, 0);
        card.addView(title, titleParams);

        TextView message = new TextView(this);
        message.setText(member.name + "님을 가족 계정 구성원 목록에서 삭제합니다.");
        message.setTextColor(0xFF798385);
        message.setTextSize(14);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(dp(2), 1.0f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, dp(10), 0, 0);
        card.addView(message, messageParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        buttonsParams.setMargins(0, dp(24), 0, 0);
        card.addView(buttons, buttonsParams);

        TextView cancelButton = new TextView(this);
        cancelButton.setText("취소");
        cancelButton.setTextColor(0xFF6F7A7C);
        cancelButton.setTextSize(15);
        cancelButton.setTypeface(null, android.graphics.Typeface.BOLD);
        cancelButton.setGravity(Gravity.CENTER);
        cancelButton.setIncludeFontPadding(false);
        cancelButton.setBackgroundResource(R.drawable.bg_member_dialog_cancel);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        buttons.addView(cancelButton, cancelParams);

        TextView deleteButton = new TextView(this);
        deleteButton.setText("삭제");
        deleteButton.setTextColor(0xFFFFFFFF);
        deleteButton.setTextSize(15);
        deleteButton.setTypeface(null, android.graphics.Typeface.BOLD);
        deleteButton.setGravity(Gravity.CENTER);
        deleteButton.setIncludeFontPadding(false);
        deleteButton.setBackgroundResource(R.drawable.bg_member_dialog_delete);
        deleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            deleteMember(member);
        });
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        deleteParams.setMargins(dp(10), 0, 0, 0);
        buttons.addView(deleteButton, deleteParams);

        dialog.setView(card);
        dialog.setOnShowListener(shownDialog -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void deleteMember(MemberData member) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/members/" + member.id, null);
        if (!currentFamilyId.isEmpty()) {
            updates.put("/familyAccounts/" + currentFamilyId + "/members/" + member.id, null);
        }
        db.updateChildren(updates)
                .addOnSuccessListener(unused -> Toast.makeText(this, "구성원을 삭제했습니다.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(error -> Toast.makeText(this, "구성원 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show());
    }

    private void bindMembersList(View root) {
        LinearLayout list = root.findViewById(R.id.membersList);
        TextView emptyText = root.findViewById(R.id.membersEmptyText);
        TextView currentText = root.findViewById(R.id.membersCurrentText);
        list.removeAllViews();
        currentText.setText(currentFamilyId.isEmpty() ? "가족 계정 ID가 설정되지 않았습니다." : "가족 계정 ID: " + currentFamilyId);
        emptyText.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < members.size(); i++) {
            MemberData member = members.get(i);
            list.addView(createMemberRow(member));
            if (i < members.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE2E7E7);
                list.addView(divider, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
            }
        }
    }

    private View createMemberRow(MemberData member) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(14), 0);

        FrameLayout iconCircle = new FrameLayout(this);
        iconCircle.setBackgroundResource(R.drawable.bg_module_icon_circle);
        row.addView(iconCircle, new LinearLayout.LayoutParams(dp(38), dp(38)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_user);
        icon.setColorFilter(0xFF008E84);
        icon.setContentDescription(null);
        iconCircle.addView(icon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, dp(56), 1f);
        textParams.setMargins(dp(12), 0, dp(8), 0);
        row.addView(textGroup, textParams);

        TextView name = new TextView(this);
        name.setText(member.name);
        name.setTextColor(0xFF14191B);
        name.setTextSize(15);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setIncludeFontPadding(false);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, dp(9), 0, 0);
        textGroup.addView(name, nameParams);

        TextView id = new TextView(this);
        id.setText("ID " + member.id);
        id.setTextColor(0xFF798385);
        id.setTextSize(12);
        id.setIncludeFontPadding(false);
        LinearLayout.LayoutParams idParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        idParams.setMargins(0, dp(5), 0, 0);
        textGroup.addView(id, idParams);

        TextView deleteButton = new TextView(this);
        deleteButton.setText("×");
        deleteButton.setTextColor(0xFFFF5C5C);
        deleteButton.setTextSize(20);
        deleteButton.setGravity(Gravity.CENTER);
        deleteButton.setIncludeFontPadding(false);
        deleteButton.setBackgroundResource(R.drawable.bg_item_remove_circle);
        deleteButton.setOnClickListener(v -> confirmDeleteMember(member));
        row.addView(deleteButton, new LinearLayout.LayoutParams(dp(34), dp(34)));
        return row;
    }


    private void renderNotification() {
        View notificationView = LayoutInflater.from(this).inflate(R.layout.screen_notification, contentContainer, false);
        contentContainer.addView(notificationView);
        centerScreen(notificationView);
        notificationView.findViewById(R.id.notificationBackButton).setOnClickListener(v -> setScreen("menu"));
        bindNotificationToggle(notificationView, R.id.notificationPushRow, R.id.notificationPushToggleTrack, R.id.notificationPushToggleKnob, "push");
        bindNotificationToggle(notificationView, R.id.notificationMoveRow, R.id.notificationMoveToggleTrack, R.id.notificationMoveToggleKnob, "move");
        bindNotificationToggle(notificationView, R.id.notificationBatteryRow, R.id.notificationBatteryToggleTrack, R.id.notificationBatteryToggleKnob, "battery");
        notificationView.findViewById(R.id.notificationLogRow).setOnClickListener(v -> setScreen("notificationLogs"));
        notificationView.findViewById(R.id.notificationSoundRow).setOnClickListener(v -> setScreen("alarm"));
        ((TextView) notificationView.findViewById(R.id.notificationSoundValue)).setText(notificationSoundName());
    }

    private void renderAlarm() {
        View alarmView = LayoutInflater.from(this).inflate(R.layout.screen_alarm, contentContainer, false);
        contentContainer.addView(alarmView);
        centerScreen(alarmView);
        alarmView.findViewById(R.id.alarmBackButton).setOnClickListener(v -> setScreen("notification"));
        bindAlarmRow(alarmView, R.id.alarmDefaultRow, 0);
        bindAlarmRow(alarmView, R.id.alarmSoftRow, 1);
        bindAlarmRow(alarmView, R.id.alarmSilentRow, 2);
        updateAlarmChecks(alarmView);
    }

    private void bindNotificationToggle(View root, int rowId, int trackId, int knobId, String type) {
        View row = root.findViewById(rowId);
        View track = root.findViewById(trackId);
        View knob = root.findViewById(knobId);
        applyRoutineToggleState(track, knob, notificationToggleValue(type));
        View.OnClickListener listener = v -> {
            boolean enabled = !notificationToggleValue(type);
            setNotificationToggleValue(type, enabled);
            applyRoutineToggleState(track, knob, enabled);
            if (enabled) {
                requestNotificationPermissionIfNeeded();
            }
            if ("push".equals(type) && enabled) {
                sendLocalNotification("푸시 알림", "앱 알림이 활성화되었습니다.");
                saveNotificationLog("푸시 알림", "", "", "알림 활성화", "push", System.currentTimeMillis());
            }
        };
        row.setOnClickListener(listener);
        track.setOnClickListener(listener);
    }

    private boolean notificationToggleValue(String type) {
        if ("move".equals(type)) return moveNotificationsEnabled;
        if ("battery".equals(type)) return batteryNotificationsEnabled;
        return pushNotificationsEnabled;
    }

    private void setNotificationToggleValue(String type, boolean enabled) {
        if ("move".equals(type)) {
            moveNotificationsEnabled = enabled;
            db.child("notificationSettings").child("moveCompleteEnabled").setValue(enabled);
            db.child("notificationSettings").child("move").setValue(enabled);
        } else if ("battery".equals(type)) {
            batteryNotificationsEnabled = enabled;
            if (enabled) batteryLowNotified = false;
            db.child("notificationSettings").child("batteryLowEnabled").setValue(enabled);
            db.child("notificationSettings").child("battery").setValue(enabled);
        } else {
            pushNotificationsEnabled = enabled;
            db.child("notificationSettings").child("pushEnabled").setValue(enabled);
            db.child("notificationSettings").child("push").setValue(enabled);
        }
    }

    private void bindAlarmRow(View root, int rowId, int index) {
        root.findViewById(rowId).setOnClickListener(v -> {
            selectedNotificationSoundIndex = index;
            updateAlarmChecks(root);
        });
    }

    private void updateAlarmChecks(View root) {
        setAlarmCheck(root, R.id.alarmDefaultCheck, selectedNotificationSoundIndex == 0);
        setAlarmCheck(root, R.id.alarmSoftCheck, selectedNotificationSoundIndex == 1);
        setAlarmCheck(root, R.id.alarmSilentCheck, selectedNotificationSoundIndex == 2);
    }

    private void setAlarmCheck(View root, int id, boolean selected) {
        TextView check = root.findViewById(id);
        check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    private String notificationSoundName() {
        if (selectedNotificationSoundIndex == 1) return "부드러운 알림음";
        if (selectedNotificationSoundIndex == 2) return "무음";
        return "기본 알림음";
    }

    private void renderNotificationLogs() {
        View logsView = LayoutInflater.from(this).inflate(R.layout.screen_notification_logs, contentContainer, false);
        contentContainer.addView(logsView);
        centerScreen(logsView);
        logsView.findViewById(R.id.notificationLogsBackButton).setOnClickListener(v -> setScreen("notification"));
        bindNotificationLogFilter(logsView, R.id.notificationLogsFilterToday, "today");
        bindNotificationLogFilter(logsView, R.id.notificationLogsFilterWeek, "week");
        bindNotificationLogFilter(logsView, R.id.notificationLogsFilterMonth, "month");
        bindNotificationLogFilter(logsView, R.id.notificationLogsFilterAll, "all");
        ((TextView) logsView.findViewById(R.id.notificationLogsDateLabel)).setText(notificationLogDateLabel());
        loadNotificationLogs(logsView);
    }

    private void bindNotificationLogFilter(View root, int chipId, String filter) {
        TextView chip = root.findViewById(chipId);
        boolean selected = filter.equals(notificationLogFilter);
        chip.setBackgroundResource(selected ? R.drawable.bg_toggle_on : R.drawable.bg_pill);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF7C878A);
        chip.setOnClickListener(v -> {
            notificationLogFilter = filter;
            renderNotificationLogs();
        });
    }

    private String notificationLogDateLabel() {
        if ("all".equals(notificationLogFilter)) return "전체 기간";
        Calendar start = Calendar.getInstance(Locale.KOREA);
        Calendar end = Calendar.getInstance(Locale.KOREA);
        clearTime(start);
        clearTime(end);
        if ("week".equals(notificationLogFilter)) {
            int day = start.get(Calendar.DAY_OF_WEEK);
            int daysFromMonday = day == Calendar.SUNDAY ? 6 : day - Calendar.MONDAY;
            start.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
            end.setTimeInMillis(start.getTimeInMillis());
            end.add(Calendar.DAY_OF_MONTH, 6);
            return formatLogDateRange(start, end);
        }
        if ("month".equals(notificationLogFilter)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end.setTimeInMillis(start.getTimeInMillis());
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            return formatLogDateRange(start, end);
        }
        return new SimpleDateFormat("yyyy.MM.dd(E)", Locale.KOREA).format(new Date());
    }

    private void loadNotificationLogs(View root) {
        db.child("notificationLogs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NotificationLogEntry> entries = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotificationLogEntry entry = parseNotificationLogEntry(child);
                    if (entry != null && isNotificationLogInSelectedRange(entry.createdAt)) {
                        insertNotificationLogEntryDesc(entries, entry);
                    }
                }
                bindNotificationLogList(root, entries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                bindNotificationLogList(root, new ArrayList<>());
            }
        });
    }

    private NotificationLogEntry parseNotificationLogEntry(DataSnapshot snapshot) {
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        if (createdAt == null) return null;
        String title = stringValue(snapshot.child("title"), "알림");
        String trayName = stringValue(snapshot.child("trayName"), "");
        String destination = stringValue(snapshot.child("destination"), "");
        String status = stringValue(snapshot.child("status"), "");
        String source = stringValue(snapshot.child("source"), "call");
        String time = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        return new NotificationLogEntry(createdAt, title, trayName, destination, status, source, time);
    }

    private void bindNotificationLogList(View root, List<NotificationLogEntry> entries) {
        LinearLayout list = root.findViewById(R.id.notificationLogsList);
        TextView emptyText = root.findViewById(R.id.notificationLogsEmptyText);
        list.removeAllViews();
        emptyText.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        if (entries.isEmpty()) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        list.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        for (int i = 0; i < entries.size(); i++) {
            card.addView(createNotificationLogRow(entries.get(i)));
            if (i < entries.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE2E7E7);
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                );
                dividerParams.setMargins(dp(52), 0, 0, 0);
                card.addView(divider, dividerParams);
            }
        }
    }

    private View createNotificationLogRow(NotificationLogEntry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        FrameLayout iconCircle = new FrameLayout(this);
        iconCircle.setBackgroundResource(R.drawable.bg_module_icon_circle);
        LinearLayout.LayoutParams iconCircleParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        row.addView(iconCircle, iconCircleParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource("routine".equals(entry.source) ? R.drawable.ic_share : R.drawable.ic_bottom_box);
        icon.setColorFilter(0xFF14191B);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
        iconCircle.addView(icon, iconParams);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(14), 0, dp(8), 0);
        row.addView(textGroup, textParams);

        TextView title = new TextView(this);
        title.setText(notificationLogTitle(entry));
        title.setTextColor(0xFF14191B);
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setIncludeFontPadding(false);
        textGroup.addView(title);

        TextView item = new TextView(this);
        item.setText(notificationLogSubtitle(entry));
        item.setTextColor(0xFF798385);
        item.setTextSize(12);
        item.setTypeface(null, android.graphics.Typeface.BOLD);
        item.setIncludeFontPadding(false);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, dp(4), 0, 0);
        textGroup.addView(item, itemParams);

        TextView meta = new TextView(this);
        meta.setText(notificationLogMeta(entry));
        meta.setTextColor(0xFF798385);
        meta.setTextSize(12);
        meta.setIncludeFontPadding(false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaParams.setMargins(0, dp(5), 0, 0);
        textGroup.addView(meta, metaParams);

        TextView time = new TextView(this);
        time.setText(entry.time);
        time.setTextColor(0xFF798385);
        time.setTextSize(13);
        time.setIncludeFontPadding(false);
        row.addView(time);
        return row;
    }

    private String notificationLogTitle(NotificationLogEntry entry) {
        if ("routine".equals(entry.source)) {
            return entry.title.isEmpty() ? "루틴 실행" : entry.title;
        }
        if (!entry.trayName.isEmpty()) return entry.trayName;
        return entry.title;
    }

    private String notificationLogSubtitle(NotificationLogEntry entry) {
        if ("routine".equals(entry.source)) {
            return notificationLogIsFailure(entry) ? "루틴 실행 실패" : "루틴 실행";
        }
        if ("battery".equals(entry.source)) return "배터리 부족 알림";
        if ("call".equals(entry.source)) {
            return notificationLogIsFailure(entry) ? "호출 실패" : "호출 완료 알림";
        }
        return entry.title.isEmpty() ? "알림" : entry.title;
    }

    private String notificationLogMeta(NotificationLogEntry entry) {
        String status = notificationLogSubtitle(entry);
        String destination = entry.destination.isEmpty() ? "목적지 미상" : entry.destination;
        if ("battery".equals(entry.source) || "push".equals(entry.source)) {
            return status;
        }
        return status + "  · " + destination;
    }

    private boolean notificationLogIsFailure(NotificationLogEntry entry) {
        String value = (entry.title + " " + entry.status).toLowerCase(Locale.ROOT);
        return value.contains("실패") || value.contains("fail") || value.contains("fallback");
    }

    private boolean isNotificationLogInSelectedRange(long createdAt) {
        if ("all".equals(notificationLogFilter)) return true;
        Calendar start = Calendar.getInstance(Locale.KOREA);
        Calendar end = Calendar.getInstance(Locale.KOREA);
        clearTime(start);
        clearTime(end);
        if ("week".equals(notificationLogFilter)) {
            int day = start.get(Calendar.DAY_OF_WEEK);
            int daysFromMonday = day == Calendar.SUNDAY ? 6 : day - Calendar.MONDAY;
            start.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
            end.setTimeInMillis(start.getTimeInMillis());
            end.add(Calendar.DAY_OF_MONTH, 7);
        } else if ("month".equals(notificationLogFilter)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end.setTimeInMillis(start.getTimeInMillis());
            end.add(Calendar.MONTH, 1);
        } else {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }
        return createdAt >= start.getTimeInMillis() && createdAt < end.getTimeInMillis();
    }

    private void insertNotificationLogEntryDesc(List<NotificationLogEntry> entries, NotificationLogEntry entry) {
        int index = 0;
        while (index < entries.size() && entries.get(index).createdAt > entry.createdAt) {
            index++;
        }
        entries.add(index, entry);
    }

    private void renderLogs() {
        View logsView = LayoutInflater.from(this).inflate(R.layout.screen_logs, contentContainer, false);
        contentContainer.addView(logsView);
        centerScreen(logsView);
        logsView.findViewById(R.id.logsBackButton).setOnClickListener(v -> setScreen("home"));
        bindLogFilterChip(logsView, R.id.logsChipToday, "today");
        bindLogFilterChip(logsView, R.id.logsChipWeek, "week");
        bindLogFilterChip(logsView, R.id.logsChipMonth, "month");
        bindLogFilterChip(logsView, R.id.logsChipAll, "all");
        ((TextView) logsView.findViewById(R.id.logsFilterTitle)).setText(logDateRangeTitle());
        loadLogs(logsView);
    }

    private void renderMap() {
        FrameLayout c = inflateFixedCanvas(R.layout.screen_map);
        addBottomNav();
        updateMapPreviewCardSize(c);
        updateMapBasesCardPosition(c);
        c.findViewById(R.id.mapBackButton).setOnClickListener(v -> setScreen("menu"));
    }

    private void updateMapPreviewCardSize(View mapView) {
        View previewCard = mapView.findViewById(R.id.mapPreviewCard);
        if (previewCard == null) return;
        previewCard.post(() -> {
            int cardWidth = previewCard.getWidth();
            if (cardWidth <= 0) {
                cardWidth = getResources().getDisplayMetrics().widthPixels - dp(88);
            }
            int cardHeight = Math.round(cardWidth * 886f / 868f);
            setHeight(previewCard, cardHeight);
            updateMapBasesCardPosition(mapView);
        });
    }


    private void updateMapBasesCardPosition(View mapView) {
        View previewCard = mapView.findViewById(R.id.mapPreviewCard);
        View basesCard = mapView.findViewById(R.id.mapBasesCard);
        if (previewCard == null || basesCard == null) return;

        ViewGroup.LayoutParams previewParams = previewCard.getLayoutParams();
        if (!(previewParams instanceof FrameLayout.LayoutParams)) return;

        FrameLayout.LayoutParams previewFrameParams = (FrameLayout.LayoutParams) previewParams;
        int basesTop = previewFrameParams.topMargin + previewFrameParams.height + dp(12);
        setTopMargin(basesCard, basesTop);
    }

    private void bindLogFilterChip(View c, int chipId, String filter) {
        TextView chip = c.findViewById(chipId);
        boolean selected = filter.equals(logFilter);
        chip.setBackgroundResource(selected ? R.drawable.bg_toggle_on : R.drawable.bg_pill);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF7C878A);
        chip.setOnClickListener(v -> {
            logFilter = filter;
            render();
        });
    }


    private String logFilterLabel() {
        switch (logFilter) {
            case "week":
                return "이번 주";
            case "month":
                return "이번 달";
            case "all":
                return "전체";
            case "today":
            default:
                return "오늘";
        }
    }

    private String logFilterTitle() {
        if ("all".equals(logFilter)) {
            return "모든 기록";
        }
        if ("week".equals(logFilter)) {
            Calendar start = Calendar.getInstance(Locale.KOREA);
            clearTime(start);
            int day = start.get(Calendar.DAY_OF_WEEK);
            int daysFromMonday = day == Calendar.SUNDAY ? 6 : day - Calendar.MONDAY;
            start.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
            Calendar end = Calendar.getInstance(Locale.KOREA);
            end.setTimeInMillis(start.getTimeInMillis());
            end.add(Calendar.DAY_OF_MONTH, 6);
            return formatLogDateRange(start, end);
        }
        if ("month".equals(logFilter)) {
            Calendar start = Calendar.getInstance(Locale.KOREA);
            clearTime(start);
            start.set(Calendar.DAY_OF_MONTH, 1);
            Calendar end = Calendar.getInstance(Locale.KOREA);
            end.setTimeInMillis(start.getTimeInMillis());
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            return formatLogDateRange(start, end);
        }
        return new SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREA).format(new Date());
    }

    private String formatLogDateRange(Calendar start, Calendar end) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd(E)", Locale.KOREA);
        return formatter.format(start.getTime()) + " ~ " + formatter.format(end.getTime());
    }

    private String logDateRangeTitle() {
        if ("all".equals(logFilter)) {
            return "전체 기간";
        }
        Calendar start = Calendar.getInstance(Locale.KOREA);
        Calendar end = Calendar.getInstance(Locale.KOREA);
        clearTime(start);
        clearTime(end);
        if ("week".equals(logFilter)) {
            int day = start.get(Calendar.DAY_OF_WEEK);
            int daysFromMonday = day == Calendar.SUNDAY ? 6 : day - Calendar.MONDAY;
            start.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
            end.setTimeInMillis(start.getTimeInMillis());
            end.add(Calendar.DAY_OF_MONTH, 6);
            return formatLogDateRange(start, end);
        }
        if ("month".equals(logFilter)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end.setTimeInMillis(start.getTimeInMillis());
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
            return formatLogDateRange(start, end);
        }
        return new SimpleDateFormat("yyyy.MM.dd(E)", Locale.KOREA).format(new Date());
    }

    private LogEntry parseNotificationDisplayLogEntry(DataSnapshot snapshot) {
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        if (createdAt == null) return null;
        String title = stringValue(snapshot.child("trayName"), stringValue(snapshot.child("title"), "알림"));
        String status = stringValue(snapshot.child("status"), stringValue(snapshot.child("title"), "알림"));
        String destination = stringValue(snapshot.child("destination"), "");
        String source = stringValue(snapshot.child("source"), "");
        String action = "routine".equals(source) ? "루틴 실행" : status;
        String time = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        return new LogEntry(createdAt, title, "", action, emptyToFallback(destination, "목적지 미상"), time);
    }

    private LogEntry parseDisplayLogEntry(DataSnapshot snapshot) {
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        if (createdAt == null) {
            createdAt = parseVoiceRecordKeyTime(snapshot.getKey());
        }
        if (createdAt == null) return null;
        String command = snapshot.child("command").getValue(String.class);
        String status = emptyToFallback(snapshot.child("status").getValue(String.class), "");
        String module = snapshot.child("module").getValue(String.class);
        String location = snapshot.child("location").getValue(String.class);
        boolean routineLog = isDisplayRoutineLog(command, status);
        boolean failed = "fallback".equals(status) || status.toLowerCase(Locale.ROOT).contains("fail");
        String action = failed
                ? (routineLog ? "루틴 실행 실패" : "호출 실패")
                : (routineLog ? "루틴 실행" : "호출 완료");
        String title = routineLog ? emptyToFallback(command, "루틴 실행") : emptyToFallback(module, "트레이 호출");
        String item = routineLog ? "" : emptyToFallback(command, "");
        String place = emptyToFallback(location, "목적지 미상");
        String time = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        return new LogEntry(createdAt, title, item, action, place, time);
    }

    private boolean isDisplayRoutineLog(String command, String status) {
        String value = (emptyToFallback(command, "") + " " + emptyToFallback(status, "")).toLowerCase(Locale.ROOT);
        return value.contains("루틴") || value.contains("routine");
    }

    private void loadLogs(View root) {
        db.child("logs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot logsSnapshot) {
                List<LogEntry> entries = new ArrayList<>();
                for (DataSnapshot child : logsSnapshot.getChildren()) {
                    LogEntry entry = parseGenericLogEntry(child);
                    if (entry != null && isLogInSelectedRange(entry.createdAt)) {
                        insertLogEntryDesc(entries, entry);
                    }
                }
                bindLogsList(root, entries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                bindLogsList(root, new ArrayList<>());
            }
        });
    }

    private LogEntry parseNotificationAsLogEntry(DataSnapshot snapshot) {
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        if (createdAt == null) return null;
        String title = stringValue(snapshot.child("trayName"), stringValue(snapshot.child("title"), "알림"));
        String status = stringValue(snapshot.child("status"), stringValue(snapshot.child("title"), "알림"));
        String destination = stringValue(snapshot.child("destination"), "");
        String source = stringValue(snapshot.child("source"), "");
        String action = "routine".equals(source) ? "루틴 실행" : status;
        String time = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        return new LogEntry(createdAt, title, "", action, emptyToFallback(destination, "목적지 미상"), time);
    }

    private LogEntry parseGenericLogEntry(DataSnapshot snapshot) {
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        if (createdAt == null) createdAt = snapshot.child("timestamp").getValue(Long.class);
        if (createdAt == null) createdAt = snapshot.child("timeMillis").getValue(Long.class);
        if (createdAt == null) createdAt = parseVoiceRecordKeyTime(snapshot.getKey());
        if (createdAt == null) return null;

        String source = stringValue(snapshot.child("source"), stringValue(snapshot.child("type"), ""));
        String status = stringValue(snapshot.child("status"), stringValue(snapshot.child("action"), ""));
        String title = stringValue(snapshot.child("title"), "");
        String trayName = stringValue(snapshot.child("trayName"), stringValue(snapshot.child("module"), ""));
        String routineName = stringValue(snapshot.child("routineName"), stringValue(snapshot.child("routine"), ""));
        String command = stringValue(snapshot.child("command"), "");
        String destination = stringValue(snapshot.child("destination"), stringValue(snapshot.child("location"), stringValue(snapshot.child("place"), "")));

        boolean routineLog = "routine".equals(source) || !routineName.isEmpty() || isDisplayRoutineLog(command, status);
        String displayTime = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        if (status.contains("호출") || status.contains("루틴")) {
            String displayTitle = routineLog
                    ? emptyToFallback(routineName, emptyToFallback(title, "루틴 실행"))
                    : emptyToFallback(trayName, emptyToFallback(title, "트레이 호출"));
            String item = routineLog ? "" : command;
            return new LogEntry(createdAt, displayTitle, item, status, emptyToFallback(destination, "목적지 미상"), displayTime);
        }
        String lowerStatus = status.toLowerCase(Locale.ROOT);
        boolean failed = status.contains("실패") || lowerStatus.contains("fail") || lowerStatus.contains("fallback");
        String action = failed
                ? (routineLog ? "루틴 실행 실패" : "호출 실패")
                : (routineLog ? "루틴 실행" : "호출 완료");
        String displayTitle = routineLog
                ? emptyToFallback(routineName, emptyToFallback(title, "루틴 실행"))
                : emptyToFallback(trayName, emptyToFallback(title, "트레이 호출"));
        String item = routineLog ? "" : command;
        return new LogEntry(createdAt, displayTitle, item, action, emptyToFallback(destination, "목적지 미상"), displayTime);
    }

    private void bindLogsList(View root, List<LogEntry> entries) {
        LinearLayout list = root.findViewById(R.id.logsList);
        TextView emptyText = root.findViewById(R.id.logsEmptyText);
        list.removeAllViews();
        emptyText.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < entries.size(); i++) {
            View row = createLogEntryCard(entries.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) params.topMargin = dp(12);
            list.addView(row, params);
        }
    }

    private View createLogEntryCard(LogEntry entry) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);

        FrameLayout iconCircle = new FrameLayout(this);
        iconCircle.setBackgroundResource(R.drawable.bg_module_icon_circle);
        card.addView(iconCircle, new LinearLayout.LayoutParams(dp(42), dp(42)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(entry.action.contains("루틴") ? R.drawable.ic_share : R.drawable.ic_bottom_box);
        icon.setColorFilter(0xFF14191B);
        iconCircle.addView(icon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(14), 0, dp(8), 0);
        card.addView(textGroup, textParams);

        TextView title = new TextView(this);
        title.setText(entry.title);
        title.setTextColor(0xFF14191B);
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setIncludeFontPadding(false);
        textGroup.addView(title);

        TextView action = new TextView(this);
        action.setText(entry.action + "  · " + entry.place);
        action.setTextColor(0xFF4D585B);
        action.setTextSize(12);
        action.setIncludeFontPadding(false);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        actionParams.setMargins(0, dp(6), 0, 0);
        textGroup.addView(action, actionParams);

        if (!entry.item.isEmpty()) {
            TextView item = new TextView(this);
            item.setText(entry.item);
            item.setTextColor(0xFF798385);
            item.setTextSize(12);
            item.setIncludeFontPadding(false);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            itemParams.setMargins(0, dp(4), 0, 0);
            textGroup.addView(item, itemParams);
        }

        TextView time = new TextView(this);
        time.setText(entry.time);
        time.setTextColor(0xFF798385);
        time.setTextSize(13);
        time.setIncludeFontPadding(false);
        card.addView(time);
        return card;
    }

    private List<LogEntry> collectFilteredLogs(DataSnapshot snapshot) {
        List<LogEntry> entries = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            LogEntry entry = parseDisplayLogEntry(child);
            if (entry != null && isLogInSelectedRange(entry.createdAt)) {
                insertLogEntryDesc(entries, entry);
            }
        }
        return entries;
    }

    private LogEntry parseLogEntry(DataSnapshot snapshot) {
        Long createdAt = snapshot.child("createdAt").getValue(Long.class);
        if (createdAt == null) {
            createdAt = parseVoiceRecordKeyTime(snapshot.getKey());
        }
        if (createdAt == null) return null;
        String command = snapshot.child("command").getValue(String.class);
        String status = emptyToFallback(snapshot.child("status").getValue(String.class), "");
        String module = snapshot.child("module").getValue(String.class);
        String location = snapshot.child("location").getValue(String.class);
        String action = isRoutineLog(command, status) ? "루틴 실행" : "호출 완료";
        String title = isRoutineLog(command, status) ? emptyToFallback(command, "루틴 실행") : emptyToFallback(module, "트레이 호출");
        String item = isRoutineLog(command, status) ? "" : emptyToFallback(command, "");
        String place = emptyToFallback(location, "목적지 미상");
        String time = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(createdAt));
        return new LogEntry(createdAt, title, item, action, place, time);
    }

    private Long parseVoiceRecordKeyTime(String key) {
        if (key == null) return null;
        try {
            return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).parse(key).getTime();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isRoutineLog(String command, String status) {
        String value = (emptyToFallback(command, "") + " " + emptyToFallback(status, "")).toLowerCase(Locale.ROOT);
        return value.contains("루틴") || value.contains("routine");
    }

    private boolean isLogInSelectedRange(long createdAt) {
        if ("all".equals(logFilter)) return true;
        Calendar log = Calendar.getInstance(Locale.KOREA);
        log.setTimeInMillis(createdAt);
        Calendar start = Calendar.getInstance(Locale.KOREA);
        Calendar end = Calendar.getInstance(Locale.KOREA);
        clearTime(start);
        clearTime(end);
        if ("week".equals(logFilter)) {
            int day = start.get(Calendar.DAY_OF_WEEK);
            int daysFromMonday = day == Calendar.SUNDAY ? 6 : day - Calendar.MONDAY;
            start.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
            end.setTimeInMillis(start.getTimeInMillis());
            end.add(Calendar.DAY_OF_MONTH, 7);
        } else if ("month".equals(logFilter)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
            end.setTimeInMillis(start.getTimeInMillis());
            end.add(Calendar.MONTH, 1);
        } else {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }
        return createdAt >= start.getTimeInMillis() && createdAt < end.getTimeInMillis();
    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void insertLogEntryDesc(List<LogEntry> entries, LogEntry entry) {
        int index = 0;
        while (index < entries.size() && entries.get(index).createdAt > entry.createdAt) {
            index++;
        }
        entries.add(index, entry);
    }


    private void choiceScreen(String title, String section, String[] choices, String back) {
        View choiceView = LayoutInflater.from(this).inflate(R.layout.screen_choice, contentContainer, false);
        contentContainer.addView(choiceView);
        centerScreen(choiceView);
        choiceView.findViewById(R.id.choiceBackButton).setOnClickListener(v -> setScreen(back));
        ((TextView) choiceView.findViewById(R.id.choiceTitle)).setText(title);
        ((TextView) choiceView.findViewById(R.id.choiceSection)).setText(section);
        LinearLayout list = choiceView.findViewById(R.id.choiceList);
        list.removeAllViews();
        for (int i = 0; i < choices.length; i++) {
            final int index = i;
            View row = createChoiceRow(title, choices[i]);
            row.setOnClickListener(v -> handleChoiceSelection(title, index, back));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(72)
            );
            if (i > 0) params.topMargin = dp(10);
            list.addView(row, params);
        }
        int listHeight = choices.length == 0 ? 0 : choices.length * dp(72) + Math.max(0, choices.length - 1) * dp(10);
        setHeight(choiceView.findViewById(R.id.choiceCard), listHeight);
        setHeight(choiceView.findViewById(R.id.choiceCanvas), dp(108) + listHeight + dp(32));
    }

    private View createChoiceRow(String title, String label) {
        FrameLayout row = new FrameLayout(this);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(16), 0, dp(14), 0);

        ImageView icon = new ImageView(this);
        icon.setImageResource(title.contains("트레이") ? R.drawable.ic_box : R.drawable.ic_pin);
        icon.setColorFilter(0xFF008E84);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(22), dp(22));
        iconParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        row.addView(icon, iconParams);

        TextView text = new TextView(this);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setIncludeFontPadding(false);
        text.setText(label);
        text.setTextColor(0xFF14191B);
        text.setTextSize(15);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        textParams.leftMargin = dp(34);
        textParams.rightMargin = dp(32);
        row.addView(text, textParams);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_left);
        chevron.setRotation(180);
        chevron.setColorFilter(0xFF7C878A);
        FrameLayout.LayoutParams chevronParams = new FrameLayout.LayoutParams(dp(22), dp(22));
        chevronParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        row.addView(chevron, chevronParams);

        return row;
    }

    private FrameLayout inflateFixedCanvas(int layoutRes) {
        FrameLayout c = (FrameLayout) LayoutInflater.from(this).inflate(layoutRes, contentContainer, false);
        contentContainer.addView(c);
        centerScreen(c);
        return c;
    }


    private void centerScreen(View screenView) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        screenView.setLayoutParams(params);
    }

    private View findOptionalView(View rootView, String idName) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        return id == 0 ? null : rootView.findViewById(id);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        view.setLayoutParams(params);
    }

    private void setTopMargin(View view, int topMargin) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) params).topMargin = topMargin;
            view.setLayoutParams(params);
        }
    }

    private void addBottomNav() {
        bottomNavContainer.removeAllViews();

        View nav = LayoutInflater.from(this).inflate(R.layout.view_bottom_nav, bottomNavContainer, false);
        bottomNavContainer.addView(nav);

        bindBottomNavItem(nav, R.id.bottomNavHome, "home");
        bindBottomNavItem(nav, R.id.bottomNavModules, "modules");
        bindBottomNavItem(nav, R.id.bottomNavRoutine, "routine");
        bindBottomNavItem(nav, R.id.bottomNavMenu, "menu");
    }

    private void bindBottomNavItem(View nav, int itemId, String target) {
        View item = nav.findViewById(itemId);
        item.setSelected(isBottomNavSelected(target));
        item.setOnClickListener(v -> setScreen(target));
    }

    private void startVoiceMicPulse(View pulseRing) {
        if (pulseRing == null) return;
        stopVoiceMicPulse();
        pulseRing.setVisibility(View.VISIBLE);
        pulseRing.setAlpha(0.72f);
        pulseRing.setScaleX(0.86f);
        pulseRing.setScaleY(0.86f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(pulseRing, View.SCALE_X, 0.86f, 1.32f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(pulseRing, View.SCALE_Y, 0.86f, 1.32f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(pulseRing, View.ALPHA, 0.72f, 0f);
        voiceMicPulseAnimator = new AnimatorSet();
        voiceMicPulseAnimator.playTogether(scaleX, scaleY, alpha);
        voiceMicPulseAnimator.setDuration(1200);
        voiceMicPulseAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (voiceState == 1 && pulseRing.getParent() != null) {
                    startVoiceMicPulse(pulseRing);
                }
            }
        });
        voiceMicPulseAnimator.start();
    }

    private void stopVoiceMicPulse() {
        if (voiceMicPulseAnimator != null) {
            voiceMicPulseAnimator.cancel();
            voiceMicPulseAnimator = null;
        }
    }

    private boolean isBottomNavSelected(String target) {
        if ("home".equals(target)) {
            return "home".equals(screen);
        }
        if ("modules".equals(target)) {
            return screen.startsWith("module") || screen.startsWith("item");
        }
        if ("routine".equals(target)) {
            return screen.startsWith("routine");
        }
        if ("menu".equals(target)) {
            return "menu".equals(screen) || "map".equals(screen);
        }
        return false;
    }

    private void requestOrStartVoice() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "음성 인식을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }
        resetVoiceResult();
        setScreen("voiceListening");
    }

    private void startSpeechRecognition() {
        stopSpeechRecognition();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                setVoiceRecognitionFailed();
                setScreen("voiceResult");
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    recognizedCommand = matches.get(0);
                    inferVoiceTarget(recognizedCommand);
                    postVoiceIntent(recognizedCommand);
                } else {
                    setVoiceRecognitionFailed();
                }
                setScreen("voiceResult");
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.startListening(intent);
    }

    private void stopSpeechRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    private void setSampleVoiceResult() {
        recognizedCommand = "거실에 있는 트레이 가져와줘";
        recognizedModule = selectedTray().name;
        recognizedLocation = "거실";
        recognizedIntent = "CALL_TRAY";
        recognizedLabel = "";
        recognizedMessage = "";
        recognizedConfidence = 0.0;
        voiceIntentAccepted = true;
    }

    private void setVoiceRecognitionFailed() {
        recognizedCommand = "";
        recognizedModule = "-";
        recognizedLocation = "-";
        recognizedIntent = "UNKNOWN";
        recognizedLabel = "UNKNOWN";
        recognizedMessage = "\uC74C\uC131 \uC778\uC2DD\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.";
        recognizedConfidence = 0.0;
        voiceIntentAccepted = false;
        parsedVoiceMissionId = -1;
    }

    private void resetVoiceResult() {
        recognizedCommand = "";
        recognizedModule = "-";
        recognizedLocation = "-";
        recognizedIntent = "";
        recognizedLabel = "";
        recognizedMessage = "";
        recognizedConfidence = 0.0;
        voiceIntentAccepted = false;
        parsedVoiceMissionId = -1;
    }

    private void inferVoiceTarget(String command) {
        if (command == null) return;
        List<TrayData> visibleTrays = currentTrays();
        if ((command.contains("B") || command.contains("비")) && visibleTrays.size() > 1) {
            recognizedModule = visibleTrays.get(1).name;
        } else if (command.contains("1번") || command.contains("첫") || command.contains("A")) {
            recognizedModule = visibleTrays.isEmpty() ? "-" : visibleTrays.get(0).name;
        } else {
            recognizedModule = "-";
        }
        if (command.contains("현관")) {
            recognizedLocation = "현관";
        } else if (command.contains("침실")) {
            recognizedLocation = "침실";
        } else {
            recognizedLocation = "-";
        }
    }

    private void applyVoiceIntentResult(JSONObject json) {
        if (json.has("mission") && json.optBoolean("ok", false)) {
            parsedVoiceMissionId = json.optInt("mission", -1);
            recognizedIntent = "CARRY";
            recognizedLabel = json.optString("label", "");
            recognizedMessage = json.optString("message", "명령 후보가 생성되었습니다.");
            recognizedModule = emptyToFallback(json.optString("source_label", ""), "-");
            if (!"-".equals(recognizedModule)) {
                recognizedModule = recognizedModule + " 서랍";
            }
            recognizedLocation = emptyToFallback(json.optString("destination_label", ""), "-");
            recognizedConfidence = 1.0;
            voiceIntentAccepted = parsedVoiceMissionId > 0;
            return;
        }

        recognizedLabel = json.optString("label", recognizedLabel);
        recognizedIntent = json.optString("intent", recognizedIntent);
        recognizedMessage = json.optString("message", recognizedMessage);
        recognizedConfidence = json.optDouble("confidence", recognizedConfidence);
        voiceIntentAccepted = json.optBoolean("accepted", voiceIntentAccepted);

        if (applyLocalVoiceCommand(recognizedCommand)) {
            return;
        }

        if (!voiceIntentAccepted || "UNKNOWN".equals(recognizedIntent)) {
            recognizedModule = "-";
            recognizedLocation = "-";
            return;
        }

        String targetTrayId = json.optString("targetTrayId", "");
        TrayData targetTray = trayForVoiceTarget(targetTrayId, recognizedLabel);
        if (targetTray != null) {
            selectedTrayId = targetTray.id;
            selectedModule = targetTray.name;
            recognizedModule = targetTray.name;
        } else {
            recognizedModule = "-";
            if (voiceIntentAccepted) {
                voiceIntentAccepted = false;
                recognizedMessage = "호출할 트레이를 찾을 수 없습니다.";
            }
        }

        String labelLocation = json.optString("label_location", "");
        if (!labelLocation.isEmpty() && !"null".equals(labelLocation)) {
            recognizedLocation = displayLocationFromVoiceLabel(labelLocation);
        } else {
            recognizedLocation = "-";
        }
    }

    private TrayData trayForVoiceTarget(String targetTrayId, String label) {
        List<TrayData> visibleTrays = currentTrays();
        String target = emptyToFallback(targetTrayId, "").toLowerCase(Locale.ROOT);
        for (TrayData tray : visibleTrays) {
            if (tray.id.equalsIgnoreCase(targetTrayId) || tray.name.equalsIgnoreCase(targetTrayId)) {
                return tray;
            }
        }

        String normalizedLabel = emptyToFallback(label, "").toUpperCase(Locale.ROOT);
        if (target.contains("baby") || normalizedLabel.contains("BABY")) {
            TrayData tray = findTrayByNameOrIdKeywords("아기", "육아", "baby", "TRAY_BABY");
            return tray != null ? tray : findTrayByItemKeywords("기저귀", "물티슈", "젖병", "장난감");
        }
        if (target.contains("medicine") || normalizedLabel.contains("MEDICINE")) {
            TrayData tray = findTrayByNameOrIdKeywords("약", "복약", "medicine", "TRAY_MEDICINE");
            return tray != null ? tray : findTrayByItemKeywords("비타민", "영양제", "혈압", "당뇨", "안경", "돋보기");
        }
        if (target.contains("commute") || normalizedLabel.contains("COMMUTE")) {
            TrayData tray = findTrayByNameOrIdKeywords("출근", "통근", "commute", "TRAY_COMMUTE");
            return tray != null ? tray : findTrayByItemKeywords("차 키", "차키", "교통카드");
        }
        return null;
    }

    private TrayData findTrayByNameOrIdKeywords(String... keywords) {
        for (TrayData tray : currentTrays()) {
            String text = (tray.id + " " + tray.name).toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return tray;
                }
            }
        }
        return null;
    }

    private TrayData findTrayByItemKeywords(String... keywords) {
        for (TrayData tray : currentTrays()) {
            for (String item : tray.items) {
                String text = item.toLowerCase(Locale.ROOT);
                for (String keyword : keywords) {
                    if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                        return tray;
                    }
                }
            }
        }
        return null;
    }

    private boolean applyLocalVoiceCommand(String command) {
        if (command == null || command.trim().isEmpty()) return false;

        int destinationStart = voiceDestinationStart(command);
        String traySearchText = destinationStart >= 0 ? command.substring(0, destinationStart) : command;
        TrayData tray = findTrayMentionedInText(traySearchText);
        String destination = findVoiceDestinationPlace(command);
        if (tray == null || destination.isEmpty()) return false;

        selectedTrayId = tray.id;
        selectedModule = tray.name;
        recognizedModule = tray.name;
        recognizedLocation = destination;
        recognizedIntent = "CALL_TRAY";
        recognizedLabel = "LOCAL_TRAY_NAME";
        recognizedMessage = "명령 후보가 생성되었습니다.";
        voiceIntentAccepted = true;
        return true;
    }

    private TrayData findTrayMentionedInText(String text) {
        String normalizedText = normalizeVoiceText(text);
        for (TrayData tray : currentTrays()) {
            if (normalizedText.contains(normalizeVoiceText(tray.name))) {
                return tray;
            }
            String shortName = tray.name
                    .replace("트레이", "")
                    .replace("서랍", "")
                    .trim();
            if (!shortName.isEmpty() && normalizedText.contains(normalizeVoiceText(shortName))) {
                return tray;
            }
            for (String item : tray.items) {
                String normalizedItem = normalizeVoiceText(item);
                if (!normalizedItem.isEmpty() && normalizedText.contains(normalizedItem)) {
                    return tray;
                }
            }
        }
        return null;
    }

    private int drawerIdForTray(TrayData tray) {
        if (tray == null) return -1;
        String normalized = normalizeVoiceText(tray.id + " " + tray.name + " " + tray.location);
        if (normalized.contains("tray1")
                || normalized.contains("child")
                || normalized.contains("아이방")
                || normalized.contains("아이")
                || normalized.contains("육아")
                || normalized.contains("baby")) return 1;
        if (normalized.contains("tray2")
                || normalized.contains("master")
                || normalized.contains("안방")
                || normalized.contains("출근")
                || normalized.contains("통근")
                || normalized.contains("commute")) return 2;
        return -1;
    }

    private String findVoiceDestinationPlace(String command) {
        int bestStart = -1;
        String bestPlace = "";
        for (String place : places) {
            int start = voiceDestinationStart(command, place);
            if (start >= 0 && start > bestStart) {
                bestStart = start;
                bestPlace = place;
            }
        }
        return bestPlace;
    }

    private int voiceDestinationStart(String command) {
        int bestStart = -1;
        for (String place : places) {
            int start = voiceDestinationStart(command, place);
            if (start >= 0 && start > bestStart) {
                bestStart = start;
            }
        }
        return bestStart;
    }

    private int voiceDestinationStart(String command, String place) {
        String[] suffixes = {"쪽으로", "앞으로", "앞에", "으로", "까지", "로", "에"};
        int bestStart = -1;
        for (String suffix : suffixes) {
            int start = command.indexOf(place + suffix);
            if (start >= 0 && start > bestStart) {
                bestStart = start;
            }
        }
        return bestStart;
    }

    private String normalizeVoiceText(String text) {
        return emptyToFallback(text, "").replace(" ", "").toLowerCase(Locale.ROOT);
    }

    private String displayLocationFromVoiceLabel(String labelLocation) {
        String normalized = labelLocation.toLowerCase(Locale.ROOT);
        if ("porch".equals(normalized)) return "현관";
        if ("living_room".equals(normalized)) return "방2";
        if ("bedroom".equals(normalized)) return "방1";
        for (String place : places) {
            if (place.equals(labelLocation)) return place;
        }
        return labelLocation;
    }

    private void saveVoiceRecord(String command, String status, String message) {
        try {
            long now = System.currentTimeMillis();
            String key = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date(now));
            db.child("voiceRecords").child(key).child("command").setValue(command);
            db.child("voiceRecords").child(key).child("status").setValue(status);
            db.child("voiceRecords").child(key).child("message").setValue(message);
            db.child("voiceRecords").child(key).child("module").setValue(recognizedModule);
            db.child("voiceRecords").child(key).child("location").setValue(recognizedLocation);
            db.child("voiceRecords").child(key).child("intent").setValue(recognizedIntent);
            db.child("voiceRecords").child(key).child("label").setValue(recognizedLabel);
            db.child("voiceRecords").child(key).child("confidence").setValue(recognizedConfidence);
            db.child("voiceRecords").child(key).child("accepted").setValue(voiceIntentAccepted);
            db.child("voiceRecords").child(key).child("createdAt").setValue(now);
            saveCallExecutionLog(command, status, now);
            saveMovementNotificationFromVoiceStatus(status, now);
        } catch (Exception ignored) {
        }
    }

    private void saveMovementNotificationFromVoiceStatus(String status, long createdAt) {
        if (!moveNotificationsEnabled) return;
        if ("fallback".equals(status)) {
            sendLocalNotification("호출 실패", "CARRY 호출에 실패했습니다.");
            saveNotificationLog("호출 실패", recognizedModule, recognizedLocation, "호출 실패", "call", createdAt);
        } else if ("sent".equals(status)) {
            sendLocalNotification("호출 완료 알림", recognizedModule + " 트레이의 이동이 완료되었습니다.");
            saveNotificationLog("호출 완료 알림", recognizedModule, recognizedLocation, "호출 완료", "call", createdAt);
        }
    }

    private void saveNotificationLog(String title, String trayName, String destination, String status, String source, long createdAt) {
        try {
            DatabaseReference ref = db.child("notificationLogs").push();
            ref.child("title").setValue(title);
            ref.child("trayName").setValue(trayName);
            ref.child("destination").setValue(destination);
            ref.child("status").setValue(status);
            ref.child("source").setValue(source);
            ref.child("createdAt").setValue(createdAt);
        } catch (Exception ignored) {
        }
    }

    private void saveCallExecutionLog(String command, String status, long createdAt) {
        if ("recognized".equals(status)) return;
        String lowerStatus = emptyToFallback(status, "").toLowerCase(Locale.ROOT);
        boolean failed = "fallback".equals(status) || lowerStatus.contains("fail");
        saveExecutionLog(
                "call",
                emptyToFallback(recognizedModule, "트레이 호출"),
                emptyToFallback(command, ""),
                failed ? "호출 실패" : "호출 완료",
                emptyToFallback(recognizedLocation, "목적지 미상"),
                createdAt
        );
    }

    private void saveRoutineExecutionLog(RoutineData routine, boolean success, long createdAt) {
        if (routine == null) return;
        String title = emptyToFallback(routine.title, "루틴 실행");
        String destination = emptyToFallback(routine.place, "목적지 미상");
        String action = success ? "루틴 실행" : "루틴 실행 실패";
        saveExecutionLog(
                "routine",
                title,
                "",
                action,
                destination,
                createdAt
        );
        if(!success) {
            sendLocalNotification("루틴 실행 실패", title + " 루틴 실행에 실패했습니다.");
            saveNotificationLog("루틴 실행 실패", title, destination, action, "routine", createdAt);
        }
    }

    private void saveExecutionLog(String source, String title, String command, String action, String destination, long createdAt) {
        try {
            DatabaseReference ref = db.child("logs").push();
            ref.child("source").setValue(source);
            ref.child("title").setValue(title);
            ref.child("trayName").setValue(title);
            ref.child("command").setValue(command);
            ref.child("action").setValue(action);
            ref.child("status").setValue(action);
            ref.child("destination").setValue(destination);
            ref.child("location").setValue(destination);
            ref.child("createdAt").setValue(createdAt);
        } catch (Exception ignored) {
        }
    }

    private String stringValue(DataSnapshot snapshot, String fallback) {
        String value = snapshot.getValue(String.class);
        return value == null ? fallback : value;
    }

    private void postVoiceIntent(String command) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("text", command);
                JSONObject json = new JSONObject(postVoiceIntentPayload(payload));
                runOnUiThread(() -> {
                    applyVoiceIntentResult(json);
                    saveVoiceRecord(command, voiceIntentAccepted ? "recognized" : "rejected", json.toString());
                    if ("voiceResult".equals(screen)) {
                        render();
                    }
                    if (!recognizedMessage.isEmpty()) {
                        Toast.makeText(this, recognizedMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!applyLocalVoiceCommand(command)) {
                        recognizedModule = "-";
                        recognizedLocation = "-";
                        recognizedIntent = "FALLBACK";
                        recognizedLabel = "";
                        recognizedMessage = "음성 모델 서버에 연결하지 못했습니다.";
                        recognizedConfidence = 0.0;
                        voiceIntentAccepted = false;
                    }
                    saveVoiceRecord(command, "fallback", e.getMessage());
                    if ("voiceResult".equals(screen)) {
                        render();
                    }
                    Toast.makeText(this, recognizedMessage, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String postVoiceIntentPayload(JSONObject payload) throws Exception {
        Exception lastError = null;
        for (String apiUrl : VOICE_INTENT_API_URLS) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(apiUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                int statusCode = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                        StandardCharsets.UTF_8
                ));
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = br) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
                if (response.length() == 0) {
                    throw new IllegalStateException("Empty voice intent response");
                }
                return response.toString();
            } catch (Exception e) {
                lastError = e;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        throw lastError == null ? new IllegalStateException("Voice intent API unavailable") : lastError;
    }

    private void startCarryMissionFromVoice() {
        int missionId = parsedVoiceMissionId > 0 ? parsedVoiceMissionId : missionIdForVoiceCommand();
        if (missionId <= 0) {
            return;
        }
        startCarryMission(missionId, recognizedCommand);
    }

    private void startCarryMission(int missionId, String commandLabel) {
        String command = emptyToFallback(commandLabel, "Mission " + missionId + " 테스트");
        Toast.makeText(this, "Mission " + missionId + " 실행 명령을 보내는 중입니다.", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("mission", missionId);
                String response = postMissionJson("/mission/start", payload);
                runOnUiThread(() -> {
                    robotMissionState = "RUNNING";
                    robotMissionFrontStatus = "준비중";
                    robotMissionPhaseLabel = "미션 시작 준비";
                    robotMissionPhaseDetail = "Mission " + missionId;
                    updateVisibleHomeRobotState();
                    saveVoiceRecord(command, "sent", response);
                    Toast.makeText(this, "Mission " + missionId + " 실행 명령을 보냈습니다.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saveVoiceRecord(command, "fallback", e.getMessage());
                    Toast.makeText(this, "로봇 서버에 연결하지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private int missionIdForVoiceCommand() {
        int drawer = drawerIdForVoiceText(recognizedCommand + " " + recognizedModule + " " + recognizedLabel);
        int destination = destinationIdForVoiceText(recognizedCommand + " " + recognizedLocation);

        if (drawer == 1 && destination == 2) return 1;
        if (drawer == 2 && destination == 1) return 2;
        if (drawer == 1 && destination == 3) return 3;
        if (drawer == 2 && destination == 3) return 4;
        if (drawer == 1 && destination == 4) return 5;
        if (drawer == 2 && destination == 4) return 6;
        return -1;
    }

    private int drawerIdForVoiceText(String text) {
        String normalized = normalizeVoiceText(text);
        TrayData tray = findTrayMentionedInText(text);
        int drawerFromTray = drawerIdForTray(tray);
        if (drawerFromTray > 0) {
            return drawerFromTray;
        }
        if (normalized.contains("서랍1")
                || normalized.contains("1번서랍")
                || normalized.contains("첫번째서랍")
                || normalized.contains("아이방서랍")
                || normalized.contains("육아트레이")
                || normalized.contains("육아")
                || normalized.contains("baby")
                || normalized.contains("tray_baby")) return 1;
        if (normalized.contains("서랍2")
                || normalized.contains("2번서랍")
                || normalized.contains("두번째서랍")
                || normalized.contains("안방서랍")
                || normalized.contains("출근트레이")
                || normalized.contains("출근")
                || normalized.contains("통근")
                || normalized.contains("commute")
                || normalized.contains("tray_commute")) return 2;
        return -1;
    }

    private int destinationIdForVoiceText(String text) {
        String normalized = normalizeVoiceText(text);
        if (normalized.contains("방1") || normalized.contains("안방")) return 1;
        if (normalized.contains("방2") || normalized.contains("아이방") || normalized.contains("선반")) return 2;
        if (normalized.contains("현관")) return 3;
        if (normalized.contains("거실")) return 4;
        return -1;
    }

    private void startBatteryRefresh() {
        batteryRefreshHandler.removeCallbacks(batteryRefreshRunnable);
        batteryRefreshRunnable.run();
    }

    private void startMissionStatusRefresh() {
        missionStatusHandler.removeCallbacks(missionStatusRunnable);
        missionStatusRunnable.run();
    }

    private void startMissionPhaseRefresh() {
        missionPhaseHandler.removeCallbacks(missionPhaseRunnable);
        missionPhaseRunnable.run();
    }

    private void fetchMissionStatus() {
        if (missionStatusRequestInFlight) return;
        missionStatusRequestInFlight = true;
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(getMissionText("/mission/status"));
                String state = json.optString("state", json.optString("status", robotMissionState));
                runOnUiThread(() -> {
                    robotMissionState = emptyToFallback(state, "IDLE").toUpperCase(Locale.ROOT);
                    missionStatusRequestInFlight = false;
                    updateVisibleHomeRobotState();
                });
            } catch (Exception e) {
                runOnUiThread(() -> missionStatusRequestInFlight = false);
            }
        }).start();
    }

    private void fetchMissionPhase() {
        if (missionPhaseRequestInFlight) return;
        missionPhaseRequestInFlight = true;
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(getMissionText("/mission/phase"));
                String code = json.optString("code", "").toUpperCase(Locale.ROOT);
                String frontStatus = json.optString("front_status", robotMissionFrontStatus);
                String label = json.optString("label", robotMissionPhaseLabel);
                String detail = json.optString("detail", robotMissionPhaseDetail);
                runOnUiThread(() -> {
                    missionPhaseRequestInFlight = false;
                    if (isMissionPhaseToastCode(code)) {
                        showMissionPhaseToastOnce(code, label);
                        return;
                    }
                    if (isMissionPhaseDisplayCode(code)) {
                        lastMissionPhaseToastCode = "";
                        robotMissionFrontStatus = emptyToFallback(frontStatus, robotMissionFrontStatus);
                        robotMissionPhaseLabel = emptyToFallback(label, robotMissionPhaseLabel);
                        robotMissionPhaseDetail = emptyToFallback(detail, robotMissionPhaseDetail);
                        updateVisibleHomeRobotState();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> missionPhaseRequestInFlight = false);
            }
        }).start();
    }

    private boolean isMissionPhaseDisplayCode(String code) {
        switch (emptyToFallback(code, "").toUpperCase(Locale.ROOT)) {
            case "IDLE":
            case "MISSION_STARTING":
            case "NAV_READY":
            case "NAV_TO_DRAWER":
            case "PICKUP_BACK_OUT":
            case "NAV_TO_DESTINATION":
            case "ARRIVED_WAITING":
            case "NAV_TO_RETURN":
            case "RETURN_BACK_OUT":
            case "PARKING_TURN":
            case "NAV_TO_STATION":
            case "STATION_PARKING":
            case "STOPPED":
                return true;
            default:
                return false;
        }
    }

    private boolean isMissionPhaseToastCode(String code) {
        String normalized = emptyToFallback(code, "").toUpperCase(Locale.ROOT);
        return "SUCCESS".equals(normalized) || "FAILED".equals(normalized);
    }

    private void showMissionPhaseToastOnce(String code, String label) {
        String normalized = emptyToFallback(code, "").toUpperCase(Locale.ROOT);
        if (normalized.equals(lastMissionPhaseToastCode)) return;
        lastMissionPhaseToastCode = normalized;
        String message = emptyToFallback(label, "SUCCESS".equals(normalized) ? "미션 완료" : "미션 실패");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateVisibleHomeRobotState() {
        if (!"home".equals(screen)) return;
        View stateView = contentContainer.findViewById(R.id.homeRobotStateBadge);
        if (stateView instanceof TextView) {
            ((TextView) stateView).setText(homeRobotStateBadgeText());
        }
        View labelView = contentContainer.findViewById(R.id.homeRobotPhaseLabel);
        if (labelView instanceof TextView) {
            ((TextView) labelView).setText(emptyToFallback(robotMissionPhaseLabel, "대기 중"));
        }
        View detailView = contentContainer.findViewById(R.id.homeRobotPhaseDetail);
        if (detailView instanceof TextView) {
            ((TextView) detailView).setText(emptyToFallback(robotMissionPhaseDetail, "충전 스테이션 대기 중"));
        }
    }

    private String homeRobotStateBadgeText() {
        String frontStatus = emptyToFallback(robotMissionFrontStatus, "");
        if (!frontStatus.isEmpty()) return frontStatus;
        return robotMissionStateText(robotMissionState);
    }

    private String robotMissionStateText(String state) {
        String normalized = emptyToFallback(state, "").toUpperCase(Locale.ROOT);
        if ("STOPPED".equals(normalized)) return "사용자가 정지함";
        if (isRobotMovingState(normalized)) return "이동 중";
        return "대기 중";
    }

    private boolean isRobotMovingState(String state) {
        String normalized = emptyToFallback(state, "").toUpperCase(Locale.ROOT);
        return "RUNNING".equals(normalized) || "STOPPING".equals(normalized);
    }

    private void fetchRobotBatteryStatus() {
        if (batteryRequestInFlight) return;
        batteryRequestInFlight = true;
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(getMissionText("/battery/status"));
                String display = batteryDisplayText(json);
                runOnUiThread(() -> {
                    robotBatteryDisplay = display;
                    batteryRequestInFlight = false;
                    updateVisibleHomeBattery();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    robotBatteryDisplay = "--";
                    batteryRequestInFlight = false;
                    updateVisibleHomeBattery();
                });
            }
        }).start();
    }

    private void updateVisibleHomeBattery() {
        if (!"home".equals(screen)) return;
        View batteryView = contentContainer.findViewById(R.id.homeBatteryValue);
        if (batteryView instanceof TextView) {
            ((TextView) batteryView).setText(robotBatteryDisplay);
        }
    }

    private String batteryDisplayText(JSONObject json) {
        if (json.has("percent") && !json.isNull("percent")) {
            double percent = json.optDouble("percent", -1);
            if (percent >= 0) {
                return Math.round(percent) + "%";
            }
        }

        double voltage = -1;
        if (json.has("voltage") && !json.isNull("voltage")) {
            voltage = json.optDouble("voltage", -1);
        } else if (json.has("battery") && !json.isNull("battery")) {
            voltage = json.optDouble("battery", -1);
        }
        if (voltage >= 0) {
            return String.format(Locale.KOREA, "%.1fV", voltage);
        }
        return "--";
    }

    private void stopCarryMission() {
        Toast.makeText(this, "긴급 정지 명령을 보내는 중입니다.", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String response = postMissionWithoutBody("/mission/stop");
                runOnUiThread(() -> {
                    robotMissionState = "STOPPED";
                    robotMissionFrontStatus = "정지됨";
                    robotMissionPhaseLabel = "사용자 정지 요청";
                    robotMissionPhaseDetail = "긴급 정지";
                    updateVisibleHomeRobotState();
                    saveVoiceRecord("긴급 정지", "sent", response);
                    Toast.makeText(this, "긴급 정지 명령을 보냈습니다.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    saveVoiceRecord("긴급 정지", "fallback", e.getMessage());
                    Toast.makeText(this, "긴급 정지 요청에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String postMissionJson(String path, JSONObject payload) throws Exception {
        Exception lastError = null;
        for (String baseUrl : MISSION_API_BASE_URLS) {
            try {
                return postJson(baseUrl + path, payload);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw lastError == null ? new IllegalStateException("Mission API unavailable") : lastError;
    }

    private String postMissionWithoutBody(String path) throws Exception {
        Exception lastError = null;
        for (String baseUrl : MISSION_API_BASE_URLS) {
            try {
                return postWithoutBody(baseUrl + path);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw lastError == null ? new IllegalStateException("Mission API unavailable") : lastError;
    }

    private String getMissionText(String path) throws Exception {
        Exception lastError = null;
        for (String baseUrl : MISSION_API_BASE_URLS) {
            try {
                return getText(baseUrl + path);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw lastError == null ? new IllegalStateException("Mission API unavailable") : lastError;
    }

    private String getText(String apiUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            int statusCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8
            ));
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = br) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
            if (statusCode >= 400) {
                throw new IllegalStateException(response.length() == 0 ? "Mission API error: " + statusCode : response.toString());
            }
            return response.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String postWithoutBody(String apiUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            int statusCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8
            ));
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = br) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
            if (statusCode >= 400) {
                throw new IllegalStateException(response.length() == 0 ? "Mission API error: " + statusCode : response.toString());
            }
            return response.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String postJson(String apiUrl, JSONObject payload) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int statusCode = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8
            ));
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = br) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
            if (statusCode >= 400) {
                throw new IllegalStateException(response.length() == 0 ? "Mission API error: " + statusCode : response.toString());
            }
            return response.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static class ItemSearchResult {
        final String itemName;
        final String trayId;
        final String trayName;
        final String location;

        ItemSearchResult(String itemName, String trayId, String trayName, String location) {
            this.itemName = itemName;
            this.trayId = trayId;
            this.trayName = trayName;
            this.location = location;
        }
    }

    private static class RoutineData {
        final String id;
        final String title;
        final String trayId;
        final String trayName;
        final int hour;
        final int minute;
        final String place;
        final String updatedBy;
        final String updatedById;
        final long updatedAt;
        boolean enabled;
        int quickSlot;

        RoutineData(String id, String title, String trayId, String trayName, int hour, int minute, String place, boolean enabled) {
            this(id, title, trayId, trayName, hour, minute, place, enabled, 0);
        }

        RoutineData(String id, String title, String trayId, String trayName, int hour, int minute, String place, boolean enabled, int quickSlot) {
            this(id, title, trayId, trayName, hour, minute, place, enabled, quickSlot, "", "", 0);
        }

        RoutineData(String id, String title, String trayId, String trayName, int hour, int minute, String place, boolean enabled, int quickSlot, String updatedBy, String updatedById, long updatedAt) {
            this.id = id;
            this.title = title;
            this.trayId = trayId;
            this.trayName = trayName;
            this.hour = hour;
            this.minute = minute;
            this.place = place;
            this.enabled = enabled;
            this.quickSlot = quickSlot;
            this.updatedBy = updatedBy;
            this.updatedById = updatedById;
            this.updatedAt = updatedAt;
        }
    }

    private static class TrayData {
        final String id;
        final String name;
        final String location;
        final String updatedBy;
        final long updatedAt;
        boolean representative;
        final List<String> items = new ArrayList<>();

        TrayData(String id, String name, String location) {
            this(id, name, location, false);
        }

        TrayData(String id, String name, String location, boolean representative) {
            this(id, name, location, representative, "", 0);
        }

        TrayData(String id, String name, String location, boolean representative, String updatedBy, long updatedAt) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.representative = representative;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
        }
    }

    private static class MemberData {
        final String id;
        final String name;
        final long createdAt;

        MemberData(String id, String name, long createdAt) {
            this.id = id;
            this.name = name;
            this.createdAt = createdAt;
        }
    }

    private interface PlaceSelectionHandler {
        void onPlaceSelected(int index);
    }

    private static class NotificationLogEntry {
        final long createdAt;
        final String title;
        final String trayName;
        final String destination;
        final String status;
        final String source;
        final String time;

        NotificationLogEntry(long createdAt, String title, String trayName, String destination, String status, String source, String time) {
            this.createdAt = createdAt;
            this.title = title;
            this.trayName = trayName;
            this.destination = destination;
            this.status = status;
            this.source = source;
            this.time = time;
        }
    }

    private static class LogEntry {
        final long createdAt;
        final String title;
        final String item;
        final String action;
        final String place;
        final String time;

        LogEntry(long createdAt, String title, String item, String action, String place, String time) {
            this.createdAt = createdAt;
            this.title = title;
            this.item = item;
            this.action = action;
            this.place = place;
            this.time = time;
        }
    }

}


