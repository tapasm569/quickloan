package com.quickloan.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE);
            String userRole = prefs.getString("USER_ROLE", "");
            String userPhone = prefs.getString("USER_PHONE", "");

            Intent intent;

            // 1. Existing Lender Session
            if ("LENDER".equals(userRole)) {
                intent = new Intent(SplashActivity.this, LenderMainActivity.class);
                intent.putExtra("LENDER_PHONE", userPhone.isEmpty() ? "9932655607" : userPhone);
            } 
            // 2. Existing Customer Session
            else if ("CUSTOMER".equals(userRole) && !userPhone.isEmpty()) {
                boolean isProfileDone = prefs.getBoolean("IS_PROFILE_COMPLETED", false);
                if (isProfileDone) {
                    intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, CustomerProfileActivity.class);
                }
                intent.putExtra("CUSTOMER_PHONE", userPhone);
            } 
            // 3. Logged out / First time user
            else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, 1800);
    }
}
