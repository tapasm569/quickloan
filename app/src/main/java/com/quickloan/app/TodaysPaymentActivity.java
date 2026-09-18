package com.quickloan.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodaysPaymentActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private TextView tvTotalCollected, tvEmpty, tvDateBadge;
    private RecyclerView rv;
    private String todayIndianDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_payment);

        todayIndianDate = DateHelper.getTodayDate();

        tvTotalCollected = findViewById(R.id.tvTotalCollectedAmount);
        tvEmpty = findViewById(R.id.tvEmptyPayments);
        tvDateBadge = findViewById(R.id.tvPaymentDateBadge);
        rv = findViewById(R.id.rvTodaysPayments);

        if (tvDateBadge != null) {
            tvDateBadge.setText(todayIndianDate);
        }

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
        }

        loadRegisteredCustomersAndPayments();
    }

    private void loadRegisteredCustomersAndPayments() {
        Request custReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(custReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchLoansAndTransactions(new HashMap<>());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Map<String, String> phoneToName = new HashMap<>();
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> customers = gson.fromJson(response.body().string(), type);
                    if (customers != null) {
                        for (Map<String, Object> c : customers) {
                            String p = String.valueOf(c.get("phone"));
                            String n = c.get("name") != null ? String.valueOf(c.get("name")) : "";
                            if (!n.isEmpty()) phoneToName.put(p, n);
                        }
                    }
                }
                fetchLoansAndTransactions(phoneToName);
            }
        });
    }

    private void fetchLoansAndTransactions(Map<String, String> phoneToName) {
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchTransactions(phoneToName, new HashMap<>());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Map<Integer, Double> loanRemainingMap = new HashMap<>();
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> loans = gson.fromJson(response.body().string(), type);
                    if (loans != null) {
                        for (Map<String, Object> l : loans) {
                            int id = ((Double) l.get("id")).intValue();
                            double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                            double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                            loanRemainingMap.put(id, Math.max(0.0, total - paid));
                        }
                    }
                }
                fetchTransactions(phoneToName, loanRemainingMap);
            }
        });
    }

    private void fetchTransactions(Map<String, String> phoneToName, Map<Integer, Double> loanRemainingMap) {
        Request txReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?payment_type=eq.DAILY_EMI&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(txReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> txs = gson.fromJson(body, type);
                if (txs == null) txs = new ArrayList<>();

                List<Map<String, Object>> todayTxs = new ArrayList<>();
                double total = 0;

                for (Map<String, Object> item : txs) {
                    String date = DateHelper.formatToIndianDate(String.valueOf(item.get("transaction_date")));
                    if (todayIndianDate.equals(date)) {
                        todayTxs.add(item);
                        double amt = item.get("amount") != null ? ((Double) item.get("amount")) : 0;
                        total += amt;
                    }
                }

                double finalTotal = total;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (tvTotalCollected != null) {
                        tvTotalCollected.setText(String.format(Locale.getDefault(), "₹%.0f", finalTotal));
                    }

                    if (todayTxs.isEmpty()) {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                    }

                    if (rv != null) {
                        rv.setAdapter(new RecyclerView.Adapter<PaymentVH>() {
                            @NonNull
                            @Override
                            public PaymentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_received, parent, false);
                                return new PaymentVH(v);
                            }

                            @Override
                            public void onBindViewHolder(@NonNull PaymentVH holder, int position) {
                                Map<String, Object> item = todayTxs.get(position);
                                int loanId = item.get("loan_id") != null ? ((Double) item.get("loan_id")).intValue() : 0;
                                String phone = item.get("customer_phone") != null ? String.valueOf(item.get("customer_phone")) : "";

                                String name = phoneToName.containsKey(phone) ? phoneToName.get(phone) : phone;
                                if (name == null || name.isEmpty() || name.startsWith("Borrower (")) {
                                    name = phone;
                                }

                                double paidAmt = item.get("amount") != null ? ((Double) item.get("amount")) : 0;
                                double remainingBalance = loanRemainingMap.getOrDefault(loanId, 0.0);
                                String mode = item.get("payment_mode") != null ? String.valueOf(item.get("payment_mode")) : "CASH";

                                holder.tvName.setText(name);
                                holder.tvPhone.setText("+91 " + phone);
                                holder.tvPaid.setText(String.format(Locale.getDefault(), "₹%.0f", paidAmt));
                                holder.tvRemaining.setText(String.format(Locale.getDefault(), "₹%.0f", remainingBalance));

                                if (mode.toUpperCase().contains("UPI")) {
                                    holder.tvMode.setText("UPI");
                                    holder.tvMode.getBackground().setTint(Color.parseColor("#2563EB"));
                                } else {
                                    holder.tvMode.setText("CASH");
                                    holder.tvMode.getBackground().setTint(Color.parseColor("#10B981"));
                                }
                            }

                            @Override
                            public int getItemCount() {
                                return todayTxs.size();
                            }
                        });
                    }
                });
            }
        });
    }

    static class PaymentVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvPaid, tvRemaining, tvMode;

        PaymentVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvRecBorrowerName);
            tvPhone = v.findViewById(R.id.tvRecBorrowerPhone);
            tvPaid = v.findViewById(R.id.tvRecPaidAmount);
            tvRemaining = v.findViewById(R.id.tvRecRemainingBalance);
            tvMode = v.findViewById(R.id.tvRecPaymentMode);
        }
    }
}
