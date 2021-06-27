package com.example.world3d;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private OpenGLView openGLView;
    TextView colorText;

    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        openGLView = (OpenGLView) findViewById(R.id.openGLView);
        colorText = findViewById(R.id.colorText);

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public void setText(String message) {
        colorText.setText(message);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        openGLView.onPause();
    }
}