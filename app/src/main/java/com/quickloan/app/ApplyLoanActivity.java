package com.quickloan.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import okhttp3.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ApplyLoanActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private EditText etAmount, etDays, etNote;
    private TextView tvRate, tvTotal, tvDailyEmi;
    private ChipGroup chipGroupAmount, chipGroupTenure;
    private String customerPhone = "";

    private double selectedPrincipal = 10000;
    private int selectedDays = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_loan);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) {
            customerPhone = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_PHONE", "");
        }

        etAmount = findViewById(R.id.etCustomAmount);
        etDays = findViewById(R.id.etCustomDays);
        etNote = findViewById(R.id.etApplyNote);
        tvRate = findViewById(R.id.tvCalcRate);
        tvTotal = findViewById(R.id.tvCalcTotal);
        tvDailyEmi = findViewById(R.id.tvCalcDailyEmi);
        chipGroupAmount = findViewById(R.id.chipGroupAmount);
        chipGroupTenure = findViewById(R.id.chipGroupTenure);

        setupListeners();
        recalculate();
    }

    private void setupListeners() {
        chipGroupAmount.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip5k) selectedPrincipal = 5000;
            else if (checkedId == R.id.chip10k) selectedPrincipal = 10000;
            else if (checkedId == R.id.chip15k) selectedPrincipal = 15000;
            else if (checkedId == R.id.chip20k) selectedPrincipal = 20000;
            else if (checkedId == R.id.chip30k) selectedPrincipal = 30000;
            else if (checkedId == R.id.chip50k) selectedPrincipal = 50000;
            recalculate();
        });

        chipGroupTenure.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip30d) selectedDays = 30;
            else if (checkedId == R.id.chip60d) selectedDays = 60;
            else if (checkedId == R.id.chip90d) selectedDays = 90;
            else if (checkedId == R.id.chip180d) selectedDays = 180;
            recalculate();
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (!s.toString().trim().isEmpty()) {
                    chipGroupAmount.clearCheck();
                    try { selectedPrincipal = Double.parseDouble(s.toString().trim()); } catch (Exception ignored) {}
                    recalculate();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etDays.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (!s.toString().trim().isEmpty()) {
                    chipGroupTenure.clearCheck();
                    try { selectedDays = Integer.parseInt(s.toString().trim()); } catch (Exception ignored) {}
                    recalculate();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnSubmitLoan).setOnClickListener(v -> submitApplication());
    }

    private void recalculate() {
        if (selectedPrincipal <= 0 || selectedDays <= 0) return;

        double annualRate;
        if (selectedPrincipal <= 10000) annualRate = 10.0;
        else if (selectedPrincipal <= 20000) annualRate = 15.0;
        else if (selectedPrincipal <= 30000) annualRate = 20.0;
        else annualRate = 30.0;

        double interestAmt = (selectedPrincipal * annualRate * selectedDays) / (365.0 * 100.0);
        double totalPayable = selectedPrincipal + interestAmt;
        double dailyEmi = totalPayable / selectedDays;

        tvRate.setText(String.format(Locale.getDefault(), "%.0f%% p.a.", annualRate));
        tvTotal.setText(String.format(Locale.getDefault(), "₹%.0f (Interest: ₹%.0f)", totalPayable, interestAmt));
        tvDailyEmi.setText(String.format(Locale.getDefault(), "₹%.0f / day", Math.ceil(dailyEmi)));
    }

    private void submitApplication() {
        if (customerPhone == null || customerPhone.isEmpty()) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedPrincipal <= 0 || selectedDays <= 0) {
            Toast.makeText(this, "Please select an amount and tenure", Toast.LENGTH_SHORT).show();
            return;
        }

        double annualRate = selectedPrincipal <= 10000 ? 10.0 : (selectedPrincipal <= 20000 ? 15.0 : (selectedPrincipal <= 30000 ? 20.0 : 30.0));
        double interestAmt = (selectedPrincipal * annualRate * selectedDays) / (365.0 * 100.0);
        double totalPayable = Math.round(selectedPrincipal + interestAmt);
        double dailyEmi = Math.ceil(totalPayable / selectedDays);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, selectedDays);
        String dueDate = sdf.format(cal.getTime());

        Map<String, Object> map = new HashMap<>();
        map.put("name", "Borrower (" + customerPhone + ")");
        map.put("phone", customerPhone);
        map.put("customer_phone", customerPhone);
        map.put("lender_phone", "9932655607");
        map.put("principal", selectedPrincipal);
        map.put("interest_rate", annualRate);
        map.put("amount", totalPayable);
        map.put("total_amount", totalPayable);
        map.put("paid_amount", 0);
        map.put("daily_emi", dailyEmi);
        map.put("tenure_days", selectedDays);
        map.put("date", today);
        map.put("due_date", dueDate);
        map.put("note", etNote.getText().toString().trim());
        map.put("disbursement_status", "PENDING_APPROVAL");
        map.put("is_paid", 0);

        RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Prefer", "return=representation")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApplyLoanActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                final String resBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ApplyLoanActivity.this, "Loan application sent to lender!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ApplyLoanActivity.this, "Failed (" + response.code() + "): " + resBody, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
