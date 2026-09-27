package com.example.modmenu;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    public native void ApplyPatch(long address, boolean enable);

    static {
        System.loadLibrary("modcore");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        Button patchButton = new Button(this);
        patchButton.setText("Yamayı Aktif Et (Free Fire)");
        
        final boolean[] toggled = {false};
        patchButton.setOnClickListener(v -> {
            toggled[0] = !toggled[0];
            long targetAddress = 0x12345678; // Hedef adres
            
            ApplyPatch(targetAddress, toggled[0]);
            
            if (toggled[0]) {
                Toast.makeText(this, "Yama Uygulandı!", Toast.LENGTH_SHORT).show();
                patchButton.setText("Yamayı Kapat");
            } else {
                Toast.makeText(this, "Yama Kapatıldı!", Toast.LENGTH_SHORT).show();
                patchButton.setText("Yamayı Aktif Et (Free Fire)");
            }
        });

        layout.addView(patchButton);
        setContentView(layout);
    }
}
