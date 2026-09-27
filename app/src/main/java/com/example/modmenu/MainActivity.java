package com.example.modmenu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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

        // 1. Ekranın köşesindeki hareketli simge
        iconParams = new WindowManager.LayoutParams(
                120, 120, layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        iconParams.gravity = Gravity.TOP | Gravity.START;
        iconParams.x = 50;
        iconParams.y = 200;

        Button floatingIcon = new Button(this);
        floatingIcon.setText("MOD");
        floatingIcon.setBackgroundColor(0xFF00FF00); // Yeşil logo
        floatingIcon.setTextColor(0xFF000000);
        
        floatingIcon.setOnClickListener(v -> toggleMenuPanel());

        // Simgeyi sürükleyip taşıma özelliği
        floatingIcon.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

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
                        iconParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        iconParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingIcon, iconParams);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingIcon, iconParams);
        floatingIconView = floatingIcon;

        // 2. İkona tıklandığında açılacak Ana Menü Paneli
        menuParams = new WindowManager.LayoutParams(
                650, 700, layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.CENTER;

        // Menü listesinin sığması için ScrollView ekliyoruz
        ScrollView scrollView = new ScrollView(this);
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setBackgroundColor(0xF0101010); // Koyu şeffaf panel arka planı
        menuLayout.setPadding(25, 25, 25, 25);

        // Başlık
        Button titleButton = new Button(this);
        titleButton.setText("=== FREE FIRE PRO MOD ===");
        titleButton.setEnabled(false);
        menuLayout.addView(titleButton);

        // --- ÖZELLİK BUTONLARI ---
        
        // 1. Fake Lag Butonu (ID: 1)
        addFeatureButton(menuLayout, "Fake Lag: KAPALI", 1);

        // 2. Aimbot Butonu (ID: 2)
        addFeatureButton(menuLayout, "Aimbot: KAPALI", 2);

        // 3. ESP Wallhack Butonu (ID: 3)
        addFeatureButton(menuLayout, "ESP Wallhack: KAPALI", 3);

        // 4. No Recoil Butonu (ID: 4)
        addFeatureButton(menuLayout, "No Recoil: KAPALI", 4);

        scrollView.addView(menuLayout);
        menuPanelView = scrollView;
        menuPanelView.setVisibility(View.GONE); // Başlangıçta gizli
        windowManager.addView(menuPanelView, menuParams);
    }

    // Butonları otomatik oluşturan yardımcı metot
    private void addFeatureButton(LinearLayout parentLayout, String title, long featureId) {
        Button btn = new Button(this);
        btn.setText(title);
        btn.setBackgroundColor(0xFF444444); // Gri (Pasif)
        btn.setTextColor(0xFFFFFFFF);

        final boolean[] state = {false};
        btn.setOnClickListener(v -> {
            state[0] = !state[0];
            ApplyPatch(featureId, state[0]); // C++ tarafına ID ve durumu gönder

            if (state[0]) {
                btn.setText(title.replace("KAPALI", "AÇIK"));
                btn.setBackgroundColor(0xFFFF0000); // Kırmızı (Aktif)
                Toast.makeText(this, title.split(":")[0] + " Aktif!", Toast.LENGTH_SHORT).show();
            } else {
                btn.setText(title.replace("AÇIK", "KAPALI"));
                btn.setBackgroundColor(0xFF444444); // Gri (Pasif)
                Toast.makeText(this, title.split(":")[0] + " Kapalı!", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 10);
        btn.setLayoutParams(params);

        parentLayout.addView(btn);
    }

    // Menüyü açıp kapatma fonksiyonu
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
