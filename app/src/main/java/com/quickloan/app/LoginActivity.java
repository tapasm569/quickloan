package com.quickloan.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private static final String LENDER_PHONE = "9932655607";
    private static final String LENDER_PASS = "India360@";

    private EditText etPhone, etPassword;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.etLoginPhone);
        etPassword = findViewById(R.id.etLoginPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> performLogin());
        findViewById(R.id.btnRegisterCustomer).setOnClickListener(v -> 
            startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void performLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter phone and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Check Owner/Lender Credentials
        if (phone.equals(LENDER_PHONE) && password.equals(LENDER_PASS)) {
            Toast.makeText(this, "Welcome, Lender!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LenderMainActivity.class);
            intent.putExtra("LENDER_PHONE", LENDER_PHONE);
            startActivity(intent);
            finish();
            return;
        }

        // 2. Otherwise verify borrower in Supabase customers table
        String url = "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." 
                + phone + "&password=eq." + password;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                runOnUiThread(() -> {
                    if (response.isSuccessful() && !body.equals("[]")) {
                        Toast.makeText(LoginActivity.this, "Customer Login Success!", Toast.LENGTH_SHORT).show();
                        // Open Customer Area (can link to Customer Dashboard)
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
                                       }
