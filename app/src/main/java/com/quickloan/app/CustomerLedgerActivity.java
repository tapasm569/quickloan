package com.quickloan.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class CustomerLedgerActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private TextView tvTotalBorrowed, tvTotalPaid, tvRemainingDue;
    private String customerPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_ledger);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) {
            customerPhone = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_PHONE", "");
        }

        tvTotalBorrowed = findViewById(R.id.tvLedgerBorrowed);
        tvTotalPaid = findViewById(R.id.tvLedgerPaid);
        tvRemainingDue = findViewById(R.id.tvLedgerRemaining);

        loadCustomerLedger();
    }

    private void loadCustomerLedger() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + customerPhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(CustomerLedgerActivity.this, "Network error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, listType);

                double totalBorrowed = 0;
                double totalPaid = 0;

                if (loans != null) {
                    for (Map<String, Object> l : loans) {
                        double amt = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                        double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                        totalBorrowed += amt;
                        totalPaid += paid;
                    }
                }

                double outstanding = totalBorrowed - totalPaid;
                double finalBorrowed = totalBorrowed;
                double finalPaid = totalPaid;

                runOnUiThread(() -> {
                    if (tvTotalBorrowed != null) tvTotalBorrowed.setText(String.format("₹%.0f", finalBorrowed));
                    if (tvTotalPaid != null) tvTotalPaid.setText(String.format("₹%.0f", finalPaid));
                    if (tvRemainingDue != null) tvRemainingDue.setText(String.format("₹%.0f", outstanding));
                });
            }
        });
    }
}
