package com.quickloan.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PayDailyEmiActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private String customerPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) {
            customerPhone = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_PHONE", "");
        }

        promptPaymentDialog();
    }

    private void promptPaymentDialog() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + customerPhone + "&disbursement_status=eq.DISBURSED&order=id.desc&limit=1")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { finish(); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) { finish(); return; }
                String body = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, type);

                if (loans == null || loans.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(PayDailyEmiActivity.this, "No active disbursed loan found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Map<String, Object> activeLoan = loans.get(0);
                int loanId = ((Double) activeLoan.get("id")).intValue();
                double emi = activeLoan.get("daily_emi") != null ? ((Double) activeLoan.get("daily_emi")) : 0;
                double currentPaid = activeLoan.get("paid_amount") != null ? ((Double) activeLoan.get("paid_amount")) : 0;
                double total = activeLoan.get("amount") != null ? ((Double) activeLoan.get("amount")) : 0;

                runOnUiThread(() -> {
                    EditText et = new EditText(PayDailyEmiActivity.this);
                    et.setHint("EMI Amount");
                    et.setText(String.valueOf((int)emi));
                    et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

                    new AlertDialog.Builder(PayDailyEmiActivity.this)
                            .setTitle("Pay Daily EMI")
                            .setMessage("Scheduled EMI: ₹" + (int)emi + "\nTotal Paid So Far: ₹" + (int)currentPaid + " / ₹" + (int)total)
                            .setView(et)
                            .setPositiveButton("Pay via Cash/UPI", (dialog, which) -> {
                                String val = et.getText().toString().trim();
                                if (val.isEmpty()) return;
                                double paidNow = Double.parseDouble(val);
                                recordEmiPayment(loanId, paidNow, currentPaid + paidNow, (currentPaid + paidNow) >= total);
                            })
                            .setNegativeButton("Cancel", (d, w) -> finish())
                            .show();
                });
            }
        });
    }

    private void recordEmiPayment(int loanId, double paidNow, double totalPaid, boolean fullyPaid) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Update loan paid_amount
        Map<String, Object> update = new HashMap<>();
        update.put("paid_amount", totalPaid);
        if (fullyPaid) update.put("is_paid", 1);

        RequestBody b1 = RequestBody.create(gson.toJson(update), MediaType.get("application/json"));
        Request r1 = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?id=eq." + loanId)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .patch(b1)
                .build();

        client.newCall(r1).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {
                // 2. Insert transaction
                Map<String, Object> tx = new HashMap<>();
                tx.put("loan_id", loanId);
                tx.put("customer_phone", customerPhone);
                tx.put("lender_phone", "9932655607");
                tx.put("amount", paidNow);
                tx.put("payment_type", "DAILY_EMI");
                tx.put("payment_mode", "UPI/CASH");
                tx.put("transaction_date", today);

                RequestBody b2 = RequestBody.create(gson.toJson(tx), MediaType.get("application/json"));
                Request r2 = new Request.Builder()
                        .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions")
                        .addHeader("apikey", API_KEY)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .post(b2)
                        .build();

                client.newCall(r2).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {}
                    @Override public void onResponse(Call call, Response resp) {
                        runOnUiThread(() -> {
                            Toast.makeText(PayDailyEmiActivity.this, "Daily EMI payment recorded successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });
            }
        });
    }
}
