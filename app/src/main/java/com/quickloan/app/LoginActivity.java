package com.quickloan.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private EditText etPhone, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.etLoginPhone);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter mobile number and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("LOGGING IN...");

        // 1. Check if lender credentials match
        if (phone.equals("9932655607") && (password.equals("admin") || password.equals("123456"))) {
            saveSessionAndRedirect("lender", phone, "Lender Admin", true);
            return;
        }

        // 2. Query customer credentials from Supabase
        String url = "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + phone + "&select=*";
        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("LOGIN");
                    Toast.makeText(LoginActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("LOGIN");
                        Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String responseBody = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> users = gson.fromJson(responseBody, type);

                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("LOGIN");

                    if (users != null && !users.isEmpty()) {
                        Map<String, Object> user = users.get(0);
                        String dbPassword = user.get("password") != null ? String.valueOf(user.get("password")) : "";
                        String name = user.get("name") != null ? String.valueOf(user.get("name")) : "Customer";

                        if (dbPassword.equals(password)) {
                            // Direct dashboard access without review or profile update checks
                            saveSessionAndRedirect("customer", phone, name, true);
                        } else {
                            Toast.makeText(LoginActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "No customer found with this phone number", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveSessionAndRedirect(String role, String phone, String name, boolean profileCompleted) {
        SharedPreferences sp = getSharedPreferences("QuickLoanPrefs", MODE_PRIVATE);
        sp.edit()
                .putString("role", role)
                .putString("phone", phone)
                .putString("customer_phone", phone)
                .putString("user_name", name)
                .putBoolean("is_logged_in", true)
                .putBoolean("profile_completed", profileCompleted)
                .putInt("is_profile_updated", 1)
                .apply();

        Intent intent;
        if ("lender".equalsIgnoreCase(role)) {
            intent = new Intent(this, LenderMainActivity.class);
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
