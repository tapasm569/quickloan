package com.quickloan.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import okhttp3.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etDob, etVill, etPo, etPs, etDist, etPin, etPhone, etPass, etFamilyPhone, etLenderNo;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.regName);
        etDob = findViewById(R.id.regDob);
        etVill = findViewById(R.id.regVillage);
        etPo = findViewById(R.id.regPO);
        etPs = findViewById(R.id.regPS);
        etDist = findViewById(R.id.regDistrict);
        etPin = findViewById(R.id.regPin);
        etPhone = findViewById(R.id.regPhone);
        etPass = findViewById(R.id.regPassword);
        etFamilyPhone = findViewById(R.id.regFamilyPhone);
        etLenderNo = findViewById(R.id.regLenderNo);

        findViewById(R.id.btnSubmitRegister).setOnClickListener(v -> submitRegistration());
    }

    private void submitRegistration() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pass = etPass.getText().toString().trim();
        String lenderNo = etLenderNo.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || pass.isEmpty() || lenderNo.isEmpty()) {
            Toast.makeText(this, "Name, Phone, Password, and Lender No are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("dob", etDob.getText().toString().trim());
        map.put("village", etVill.getText().toString().trim());
        map.put("post_office", etPo.getText().toString().trim());
        map.put("police_station", etPs.getText().toString().trim());
        map.put("district", etDist.getText().toString().trim());
        map.put("pin_code", etPin.getText().toString().trim());
        map.put("phone", phone);
        map.put("password", pass);
        map.put("family_phone", etFamilyPhone.getText().toString().trim());
        map.put("lender_phone", lenderNo);

        RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                .addHeader("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY")
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Network Error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registered Successfully! You can login now.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed or mobile already registered", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
