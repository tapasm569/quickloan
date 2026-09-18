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

        // Show splash screen for 2 seconds before redirecting
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE);
            String savedCustomerPhone = prefs.getString("CUSTOMER_PHONE", "");

            Intent intent;
            if (!savedCustomerPhone.isEmpty()) {
                // Auto-login existing customer
                intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
                intent.putExtra("CUSTOMER_PHONE", savedCustomerPhone);
            } else {
                // First-time or logged-out user
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, 2000);
    }
                                    }
