package com.quickloan.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import okhttp3.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerProfileActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private String phone;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        phone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (phone == null || phone.isEmpty()) {
            phone = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_PHONE", "");
        }

        findViewById(R.id.btnSubmitProfile).setOnClickListener(v -> submitProfile());
    }

    private void submitProfile() {
        String name = ((EditText) findViewById(R.id.etName)).getText().toString().trim();
        String dob = ((EditText) findViewById(R.id.etDob)).getText().toString().trim();
        String vill = ((EditText) findViewById(R.id.etVillage)).getText().toString().trim();
        String po = ((EditText) findViewById(R.id.etPO)).getText().toString().trim();
        String ps = ((EditText) findViewById(R.id.etPS)).getText().toString().trim();
        String dist = ((EditText) findViewById(R.id.etDistrict)).getText().toString().trim();
        String pin = ((EditText) findViewById(R.id.etPin)).getText().toString().trim();
        String ref = ((EditText) findViewById(R.id.etRefContact)).getText().toString().trim();

        if (name.isEmpty() || vill.isEmpty() || pin.isEmpty()) {
            Toast.makeText(this, "Name, Village, and PIN Code are required", Toast.LENGTH_SHORT).show();
            return;
        }

        final String activePhone = phone;
        final String customerName = name;

        Map<String, Object> map = new HashMap<>();
        map.put("name", customerName);
        map.put("dob", dob);
        map.put("village", vill);
        map.put("post_office", po);
        map.put("police_station", ps);
        map.put("district", dist);
        map.put("pin_code", pin);
        map.put("reference_phone", ref);
        map.put("is_profile_completed", 1);

        RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + activePhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(CustomerProfileActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        // 1. Save customer name in local session
                        SharedPreferences prefs = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE);
                        prefs.edit()
                                .putString("USER_ROLE", "CUSTOMER")
                                .putString("USER_PHONE", activePhone)
                                .putString("CUSTOMER_PHONE", activePhone)
                                .putString("CUSTOMER_NAME", customerName)
                                .putBoolean("IS_PROFILE_COMPLETED", true)
                                .apply();

                        // 2. Cascade update name into loans table
                        syncCustomerNameToLoans(activePhone, customerName);

                        // 3. Cascade update name into loan_transactions table
                        syncCustomerNameToTransactions(activePhone, customerName);

                        Toast.makeText(CustomerProfileActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CustomerProfileActivity.this, CustomerMainActivity.class);
                        intent.putExtra("CUSTOMER_PHONE", activePhone);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(CustomerProfileActivity.this, "Failed (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void syncCustomerNameToLoans(String phone, String name) {
        Map<String, Object> update = new HashMap<>();
        update.put("name", name);
        RequestBody body = RequestBody.create(gson.toJson(update), MediaType.get("application/json"));
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + phone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .patch(body)
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }

    private void syncCustomerNameToTransactions(String phone, String name) {
        Map<String, Object> update = new HashMap<>();
        update.put("customer_name", name);
        RequestBody body = RequestBody.create(gson.toJson(update), MediaType.get("application/json"));
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?customer_phone=eq." + phone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .patch(body)
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });
    }
}
