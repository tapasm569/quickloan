package com.quickloan.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import java.io.IOException;
import java.util.Locale;

public class ApplyLoanActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();

    private EditText etAmount, etInterestRate, etTenure, etPurpose;
    private TextView tvTotalInterest, tvTotalPayable, tvDailyEmi;
    private Button btnSubmit;

    private float presetMonthlyRate = 2.0f;
    private double calculatedTotalAmount = 0;
    private double calculatedDailyEmi = 0;
    private String customerPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_loan);

        SharedPreferences sp = getSharedPreferences("QuickLoanPrefs", MODE_PRIVATE);
        presetMonthlyRate = sp.getFloat("PREF_MONTHLY_INTEREST_RATE", 2.0f);
        customerPhone = sp.getString("phone", sp.getString("customer_phone", ""));

        View btnBack = findViewById(R.id.btnBackApply);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        etAmount = findViewById(R.id.etApplyAmount);
        etInterestRate = findViewById(R.id.etApplyInterestRate);
        etTenure = findViewById(R.id.etApplyTenure);
        etPurpose = findViewById(R.id.etApplyPurpose);

        tvTotalInterest = findViewById(R.id.tvApplyTotalInterest);
        tvTotalPayable = findViewById(R.id.tvApplyTotalPayable);
        tvDailyEmi = findViewById(R.id.tvApplyDailyEmi);
        btnSubmit = findViewById(R.id.btnSubmitLoanApplication);

        etInterestRate.setText(String.format(Locale.getDefault(), "%.1f%% per month", presetMonthlyRate));

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateMonthlyRepayment();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        etAmount.addTextChangedListener(watcher);
        etTenure.addTextChangedListener(watcher);

        btnSubmit.setOnClickListener(v -> submitApplication());
    }

    private void calculateMonthlyRepayment() {
        try {
            double principal = Double.parseDouble(etAmount.getText().toString().trim());
            int days = Integer.parseInt(etTenure.getText().toString().trim());

            if (principal > 0 && days > 0) {
                // Per month interest formula: Principal * (Rate / 100) * (Days / 30)
                double totalInterest = principal * (presetMonthlyRate / 100.0) * (days / 30.0);
                calculatedTotalAmount = principal + totalInterest;
                calculatedDailyEmi = calculatedTotalAmount / days;

                tvTotalInterest.setText(String.format(Locale.getDefault(), "₹%.0f", totalInterest));
                tvTotalPayable.setText(String.format(Locale.getDefault(), "₹%.0f", calculatedTotalAmount));
                tvDailyEmi.setText(String.format(Locale.getDefault(), "₹%.0f / day", calculatedDailyEmi));
            }
        } catch (Exception e) {
            tvTotalInterest.setText("₹0");
            tvTotalPayable.setText("₹0");
            tvDailyEmi.setText("₹0 / day");
        }
    }

    private void submitApplication() {
        String amtStr = etAmount.getText().toString().trim();
        String tenureStr = etTenure.getText().toString().trim();
        String purpose = etPurpose.getText().toString().trim();

        if (amtStr.isEmpty() || tenureStr.isEmpty()) {
            Toast.makeText(this, "Please fill in amount and tenure", Toast.LENGTH_SHORT).show();
            return;
        }

        double principal = Double.parseDouble(amtStr);
        int days = Integer.parseInt(tenureStr);

        String json = String.format(Locale.US,
                "{\"customer_phone\":\"%s\",\"principal\":%.2f,\"amount\":%.2f,\"interest_rate\":%.2f,\"tenure\":%d,\"daily_emi\":%.2f,\"purpose\":\"%s\",\"disbursement_status\":\"PENDING\",\"is_paid\":0,\"date\":\"%s\"}",
                customerPhone, principal, calculatedTotalAmount, presetMonthlyRate, days, calculatedDailyEmi, purpose, DateHelper.getTodayDate()
        );

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApplyLoanActivity.this, "Network error submitting application", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ApplyLoanActivity.this, "Loan application submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
}
