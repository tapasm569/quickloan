package com.quickloan.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String LENDER_PHONE = "9932655607";
    private static final String LENDER_PASS = "India360@";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";

    private EditText etPhone, etPassword;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.etLoginPhone);
        etPassword = findViewById(R.id.etLoginPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter mobile number and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Owner / Lender verification
        if (phone.equals(LENDER_PHONE) && password.equals(LENDER_PASS)) {
            Intent intent = new Intent(this, LenderMainActivity.class);
            intent.putExtra("LENDER_PHONE", LENDER_PHONE);
            startActivity(intent);
            finish();
            return;
        }

        // 2. Customer verification from Supabase
        String url = "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." 
                + phone + "&password=eq." + password;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
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
                    if (!response.isSuccessful() || body.equals("[]")) {
                        Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> customers = gson.fromJson(body, listType);
                    Map<String, Object> user = customers.get(0);

                    // Save session
                    SharedPreferences prefs = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE);
                    prefs.edit().putString("CUSTOMER_PHONE", phone).apply();

                    double completedVal = user.get("is_profile_completed") == null ? 0 : 
                            Double.parseDouble(user.get("is_profile_completed").toString());

                    if (completedVal == 1) {
                        // Profile ready -> Go to customer panel
                        Intent intent = new Intent(LoginActivity.this, CustomerMainActivity.class);
                        intent.putExtra("CUSTOMER_PHONE", phone);
                        startActivity(intent);
                    } else {
                        // First time login -> Complete profile
                        Intent intent = new Intent(LoginActivity.this, CustomerProfileActivity.class);
                        intent.putExtra("CUSTOMER_PHONE", phone);
                        startActivity(intent);
                    }
                    finish();
                });
            }
        });
    }
}
