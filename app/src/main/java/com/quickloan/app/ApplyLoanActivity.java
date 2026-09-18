package com.quickloan.app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApplyLoanActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private EditText etAmount, etDays, etNote;
    private TextView tvRate, tvTotal, tvDailyEmi;
    private Button btn5k, btn10k, btn15k, btn20k, btn30k, btn50k;
    private Button btn30, btn60, btn90, btn180;
    private String customerPhone = "";
    private String customerName = "";

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

        customerName = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_NAME", "");
        if (customerName.isEmpty()) {
            fetchRegisteredName();
        }

        etAmount = findViewById(R.id.etCustomAmount);
        etDays = findViewById(R.id.etCustomDays);
        etNote = findViewById(R.id.etApplyNote);
        tvRate = findViewById(R.id.tvCalcRate);
        tvTotal = findViewById(R.id.tvCalcTotal);
        tvDailyEmi = findViewById(R.id.tvCalcDailyEmi);

        btn5k = findViewById(R.id.btnAmt5k);
        btn10k = findViewById(R.id.btnAmt10k);
        btn15k = findViewById(R.id.btnAmt15k);
        btn20k = findViewById(R.id.btnAmt20k);
        btn30k = findViewById(R.id.btnAmt30k);
        btn50k = findViewById(R.id.btnAmt50k);

        btn30 = findViewById(R.id.btnTenure30);
        btn60 = findViewById(R.id.btnTenure60);
        btn90 = findViewById(R.id.btnTenure90);
        btn180 = findViewById(R.id.btnTenure180);

        setupAmountButtons();
        setupTenureButtons();
        setupCustomInputs();

        findViewById(R.id.btnSubmitLoan).setOnClickListener(v -> submitLoanApplication());

        recalculate();
    }

    private void fetchRegisteredName() {
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + customerPhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> list = gson.fromJson(response.body().string(), type);
                    if (list != null && !list.isEmpty()) {
                        Object n = list.get(0).get("name");
                        if (n != null) {
                            customerName = n.toString();
                            getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).edit().putString("CUSTOMER_NAME", customerName).apply();
                        }
                    }
                }
            }
        });
    }

    private void setupAmountButtons() {
        btn5k.setOnClickListener(v -> selectAmount(5000, btn5k));
        btn10k.setOnClickListener(v -> selectAmount(10000, btn10k));
        btn15k.setOnClickListener(v -> selectAmount(15000, btn15k));
        btn20k.setOnClickListener(v -> selectAmount(20000, btn20k));
        btn30k.setOnClickListener(v -> selectAmount(30000, btn30k));
        btn50k.setOnClickListener(v -> selectAmount(50000, btn50k));
    }

    private void selectAmount(double amount, Button selectedBtn) {
        selectedPrincipal = amount;
        etAmount.setText("");
        Button[] btns = {btn5k, btn10k, btn15k, btn20k, btn30k, btn50k};
        for (Button b : btns) b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
        selectedBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        recalculate();
    }

    private void setupTenureButtons() {
        btn30.setOnClickListener(v -> selectTenure(30, btn30));
        btn60.setOnClickListener(v -> selectTenure(60, btn60));
        btn90.setOnClickListener(v -> selectTenure(90, btn90));
        btn180.setOnClickListener(v -> selectTenure(180, btn180));
    }

    private void selectTenure(int days, Button selectedBtn) {
        selectedDays = days;
        etDays.setText("");
        Button[] btns = {btn30, btn60, btn90, btn180};
        for (Button b : btns) b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
        selectedBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        recalculate();
    }

    private void setupCustomInputs() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (!s.toString().trim().isEmpty()) {
                    try {
                        selectedPrincipal = Double.parseDouble(s.toString().trim());
                        Button[] btns = {btn5k, btn10k, btn15k, btn20k, btn30k, btn50k};
                        for (Button b : btns) b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
                        recalculate();
                    } catch (Exception ignored) {}
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etDays.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (!s.toString().trim().isEmpty()) {
                    try {
                        selectedDays = Integer.parseInt(s.toString().trim());
                        Button[] btns = {btn30, btn60, btn90, btn180};
                        for (Button b : btns) b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
                        recalculate();
                    } catch (Exception ignored) {}
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void recalculate() {
        if (selectedPrincipal <= 0 || selectedDays <= 0) return;

        double annualRate = selectedPrincipal <= 10000 ? 10.0 : (selectedPrincipal <= 20000 ? 15.0 : (selectedPrincipal <= 30000 ? 20.0 : 30.0));
        double interestAmt = (selectedPrincipal * annualRate * selectedDays) / (365.0 * 100.0);
        double totalPayable = selectedPrincipal + interestAmt;
        double dailyEmi = totalPayable / selectedDays;

        tvRate.setText(String.format(Locale.getDefault(), "%.0f%% p.a.", annualRate));
        tvTotal.setText(String.format(Locale.getDefault(), "₹%.0f (Interest: ₹%.0f)", totalPayable, interestAmt));
        tvDailyEmi.setText(String.format(Locale.getDefault(), "₹%.0f / day", Math.ceil(dailyEmi)));
    }

    private void submitLoanApplication() {
        if (customerPhone == null || customerPhone.isEmpty()) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPrincipal <= 0 || selectedDays <= 0) {
            Toast.makeText(this, "Please specify an amount and tenure", Toast.LENGTH_SHORT).show();
            return;
        }

        double annualRate = selectedPrincipal <= 10000 ? 10.0 : (selectedPrincipal <= 20000 ? 15.0 : (selectedPrincipal <= 30000 ? 20.0 : 30.0));
        double interestAmt = (selectedPrincipal * annualRate * selectedDays) / (365.0 * 100.0);
        double totalPayable = Math.round(selectedPrincipal + interestAmt);
        double dailyEmi = Math.ceil(totalPayable / selectedDays);

        String today = DateHelper.getTodayDate();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, selectedDays);
        String dueDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.getTime());

        String finalName = (customerName != null && !customerName.isEmpty()) ? customerName : "Customer (" + customerPhone + ")";

        Map<String, Object> map = new HashMap<>();
        map.put("name", finalName);
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
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ApplyLoanActivity.this, "Loan application submitted!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ApplyLoanActivity.this, "Failed (" + response.code() + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
