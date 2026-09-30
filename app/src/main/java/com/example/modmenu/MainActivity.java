package com.example.modmenu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public native void ApplyPatch(long featureID, boolean enable);

    static {
        System.loadLibrary("modcore");
    }

    private WindowManager windowManager;
    private View floatingIconView;
    private View menuPanelView;
    private WindowManager.LayoutParams iconParams, menuParams;
    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ekran üstü çizim izni kontrolü (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
        } else {
            createFloatingUI();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                createFloatingUI();
            } else {
                Toast.makeText(this, "İzin verilmediği için menü başlatılamadı!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void createFloatingUI() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        // 1. Yüzen Anonymous Simgesi
        iconParams = new WindowManager.LayoutParams(
                140, 140, layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 50;
        iconParams.y = 200;

        ImageButton floatingIcon = new ImageButton(this);
        floatingIcon.setImageResource(R.drawable.anonymous_logo);
        floatingIcon.setBackgroundColor(Color.TRANSPARENT);
        floatingIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Simgeyi hem sürükleme hem de sorunsuz tıklama (açılma) özelliği ile donatıyoruz
        floatingIcon.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private static final int CLICK_THRESHOLD = 15;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = iconParams.x;
                        initialY = iconParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        iconParams.x = initialX + deltaX;
                        iconParams.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingIcon, iconParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float movedX = Math.abs(event.getRawX() - initialTouchX);
                        float movedY = Math.abs(event.getRawY() - initialTouchY);
                        
                        // Parmak hareket etmediyse tıklama kabul edip menüyü aç/kapat yapıyoruz
                        if (movedX < CLICK_THRESHOLD && movedY < CLICK_THRESHOLD) {
                            toggleMenuPanel();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingIcon, iconParams);
        floatingIconView = floatingIcon;

        // 2. Ana Menü Paneli (Anonymous - Free Fire v1.0)
        menuParams = new WindowManager.LayoutParams(
                700, 820, layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.CENTER;

        ScrollView scrollView = new ScrollView(this);
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        
        // Hacker tarzı koyu siyah arka plan ve neon yeşil çerçeve
        GradientDrawable menuBg = new GradientDrawable();
        menuBg.setColor(Color.parseColor("#DD0F0F0F"));
        menuBg.setStroke(2, Color.parseColor("#00FF66"));
        menuBg.setCornerRadius(15);
        menuLayout.setBackground(menuBg);
        menuLayout.setPadding(25, 25, 25, 25);

        // Menü Başlığı
        Button titleButton = new Button(this);
        titleButton.setText("anonymous - free fire v1.0");
        titleButton.setTextColor(Color.parseColor("#00FF66"));
        titleButton.setBackgroundColor(Color.TRANSPARENT);
        titleButton.setTextSize(16);
        titleButton.setEnabled(false);
        menuLayout.addView(titleButton);

        // Hile ve Koruma Özellikleri Butonları (5 Özellik Tam)
        addFeatureButton(menuLayout, "Anti-Ban / Bypass: KAPALI", 5);
        addFeatureButton(menuLayout, "Fake Lag: KAPALI", 1);
        addFeatureButton(menuLayout, "Aimbot: KAPALI", 2);
        addFeatureButton(menuLayout, "ESP Wallhack: KAPALI", 3);
        addFeatureButton(menuLayout, "No Recoil: KAPALI", 4);

        scrollView.addView(menuLayout);
        menuPanelView = scrollView;
        menuPanelView.setVisibility(View.GONE);
        windowManager.addView(menuPanelView, menuParams);
    }

    private void addFeatureButton(LinearLayout parentLayout, String title, long featureId) {
        Button btn = new Button(this);
        btn.setText(title);
        btn.setTextColor(Color.WHITE);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#FF222222"));
        btnBg.setCornerRadius(8);
        btn.setBackground(btnBg);

        final boolean[] state = {false};
        btn.setOnClickListener(v -> {
            state[0] = !state[0];
            ApplyPatch(featureId, state[0]);

            GradientDrawable updatedBg = new GradientDrawable();
            updatedBg.setCornerRadius(8);

            if (state[0]) {
                btn.setText(title.replace("KAPALI", "AÇIK"));
                if (featureId == 5) {
                    updatedBg.setColor(Color.parseColor("#CC00AAFF")); 
                    Toast.makeText(this, "Anti-Ban Koruması Etkinleştirildi!", Toast.LENGTH_SHORT).show();
                } else {
                    updatedBg.setColor(Color.parseColor("#CCFF0000"));
                    Toast.makeText(this, title.split(":")[0] + " Aktif!", Toast.LENGTH_SHORT).show();
                }
            } else {
                btn.setText(title.replace("AÇIK", "KAPALI"));
                updatedBg.setColor(Color.parseColor("#FF222222"));
                Toast.makeText(this, title.split(":")[0] + " Kapalı!", Toast.LENGTH_SHORT).show();
            }
            btn.setBackground(updatedBg);
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 10);
        btn.setLayoutParams(params);

        parentLayout.addView(btn);
    }

    private void toggleMenuPanel() {
        if (isMenuOpen) {
            menuPanelView.setVisibility(View.GONE);
            isMenuOpen = false;
        } else {
            menuPanelView.setVisibility(View.VISIBLE);
            isMenuOpen = true;
        }
    }
}
