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

public class PaymentHistoryActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private TextView tvDateBadge, tvTotalDue, tvTodayPaid, tvDisbursementBalance, tvTotalRemaining, tvInterestCollected, tvEmpty;
    private RecyclerView rvTable;
    private String todayIndianDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        todayIndianDate = DateHelper.getTodayDate();

        tvDateBadge = findViewById(R.id.tvSlateCurrentDate);
        tvTotalDue = findViewById(R.id.tvSlateTotalDue);
        tvTodayPaid = findViewById(R.id.tvSlateTodayPaid);
        tvDisbursementBalance = findViewById(R.id.tvSlateDisbursementBalance);
        tvTotalRemaining = findViewById(R.id.tvSlateTotalRemaining);
        tvInterestCollected = findViewById(R.id.tvSlateInterestCollected);
        tvEmpty = findViewById(R.id.tvEmptySlate);

        if (tvDateBadge != null) {
            tvDateBadge.setText(todayIndianDate);
        }

        rvTable = findViewById(R.id.rvSlateTable);
        if (rvTable != null) {
            rvTable.setLayoutManager(new LinearLayoutManager(this));
        }

        loadCustomerNamesAndData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerNamesAndData();
    }

    private double parseDoubleSafe(Object obj) {
        if (obj == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseIntSafe(Object obj) {
        if (obj == null) return 0;
        try {
            return (int) Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadCustomerNamesAndData() {
        Request custReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(custReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchTodayTransactions(new HashMap<>());
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
                fetchTodayTransactions(phoneToName);
            }
        });
    }

    private void fetchTodayTransactions(Map<String, String> phoneToName) {
        Request txReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?payment_type=eq.DAILY_EMI&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(txReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchLoansAndBuildSlate(new HashMap<>(), 0.0, phoneToName);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Map<Integer, Double> loanTodayPaidMap = new HashMap<>();
                double totalTodayPaidSum = 0;

                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> txs = gson.fromJson(response.body().string(), type);
                    if (txs != null) {
                        for (Map<String, Object> t : txs) {
                            String txDate = DateHelper.formatToIndianDate(String.valueOf(t.get("transaction_date")));
                            if (todayIndianDate.equals(txDate)) {
                                int loanId = parseIntSafe(t.get("loan_id"));
                                double amt = parseDoubleSafe(t.get("amount"));
                                loanTodayPaidMap.put(loanId, loanTodayPaidMap.getOrDefault(loanId, 0.0) + amt);
                                totalTodayPaidSum += amt;
                            }
                        }
                    }
                }
                fetchLoansAndBuildSlate(loanTodayPaidMap, totalTodayPaidSum, phoneToName);
            }
        });
    }

    private void fetchLoansAndBuildSlate(Map<Integer, Double> loanTodayPaidMap, double totalTodayPaidSum, Map<String, String> phoneToName) {
        String filterPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        String url = (filterPhone != null && !filterPhone.isEmpty()) ?
                "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + filterPhone + "&disbursement_status=eq.DISBURSED&order=id.desc" :
                "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.DISBURSED&order=id.desc";

        Request loanReq = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, type);
                if (loans == null) loans = new ArrayList<>();

                List<SlateEntry> slateEntries = new ArrayList<>();
                double sumDue = 0;
                double sumDisbursed = 0;
                double sumRemaining = 0;
                double sumInterestProfit = 0;

                int sl = 1;
                for (Map<String, Object> l : loans) {
                    int loanId = parseIntSafe(l.get("id"));
                    String phone = String.valueOf(l.get("phone"));
                    String name = phoneToName.containsKey(phone) ? phoneToName.get(phone) : String.valueOf(l.get("name"));
                    if (name == null || name.isEmpty() || name.startsWith("Borrower (")) {
                        name = phone;
                    }

                    String startDate = l.get("date") != null ? String.valueOf(l.get("date")) : "";
                    double principal = parseDoubleSafe(l.get("principal"));
                    double totalAmount = parseDoubleSafe(l.get("amount"));
                    if (principal <= 0) principal = totalAmount; // Fallback if principal wasn't stored separately

                    double dailyEmi = parseDoubleSafe(l.get("daily_emi"));
                    double totalPaidOverall = parseDoubleSafe(l.get("paid_amount"));

                    double remainingBalance = Math.max(0.0, totalAmount - totalPaidOverall);
                    double accumulatedDue = DateHelper.calculateAccumulatedDue(startDate, dailyEmi, totalAmount, totalPaidOverall);
                    double todayCollection = loanTodayPaidMap.getOrDefault(loanId, 0.0);

                    // Profit calculation: Proportional interest collected from payments
                    double totalInterestExpected = Math.max(0.0, totalAmount - principal);
                    double interestProfit = totalAmount > 0 ? (totalPaidOverall * (totalInterestExpected / totalAmount)) : 0.0;

                    slateEntries.add(new SlateEntry(sl++, name, accumulatedDue, todayCollection, totalPaidOverall, remainingBalance));

                    sumDue += accumulatedDue;
                    sumDisbursed += principal;
                    sumRemaining += remainingBalance;
                    sumInterestProfit += interestProfit;
                }

                double finalSumDue = sumDue;
                double finalDisbursed = sumDisbursed;
                double finalRemaining = sumRemaining;
                double finalProfit = sumInterestProfit;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (tvTotalDue != null) {
                        tvTotalDue.setText(String.format(Locale.getDefault(), "₹%.0f", finalSumDue));
                    }
                    if (tvTodayPaid != null) {
                        tvTodayPaid.setText(String.format(Locale.getDefault(), "₹%.0f", totalTodayPaidSum));
                    }
                    if (tvDisbursementBalance != null) {
                        tvDisbursementBalance.setText(String.format(Locale.getDefault(), "₹%.0f", finalDisbursed));
                    }
                    if (tvTotalRemaining != null) {
                        tvTotalRemaining.setText(String.format(Locale.getDefault(), "₹%.0f", finalRemaining));
                    }
                    if (tvInterestCollected != null) {
                        tvInterestCollected.setText(String.format(Locale.getDefault(), "₹%.0f", finalProfit));
                    }

                    if (slateEntries.isEmpty()) {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                    }

                    if (rvTable != null) {
                        rvTable.setAdapter(new SlateAdapter(slateEntries));
                    }
                });
            }
        });
    }

    static class SlateEntry {
        int slNo;
        String name;
        double todaysDue;
        double todaysCollection;
        double totalPaid;
        double remainingBalance;

        SlateEntry(int slNo, String name, double todaysDue, double todaysCollection, double totalPaid, double remainingBalance) {
            this.slNo = slNo;
            this.name = name;
            this.todaysDue = todaysDue;
            this.todaysCollection = todaysCollection;
            this.totalPaid = totalPaid;
            this.remainingBalance = remainingBalance;
        }
    }

    static class SlateAdapter extends RecyclerView.Adapter<SlateAdapter.SlateVH> {
        private final List<SlateEntry> list;

        SlateAdapter(List<SlateEntry> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public SlateVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slate_ledger_row, parent, false);
            return new SlateVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SlateVH holder, int position) {
            SlateEntry item = list.get(position);

            holder.tvSl.setText(String.valueOf(item.slNo));
            holder.tvName.setText(item.name);
            holder.tvDue.setText(String.format(Locale.getDefault(), "₹%.0f", item.todaysDue));
            holder.tvTodayColl.setText(String.format(Locale.getDefault(), "₹%.0f", item.todaysCollection));
            holder.tvTotalPaid.setText(String.format(Locale.getDefault(), "₹%.0f", item.totalPaid));
            holder.tvRem.setText(String.format(Locale.getDefault(), "₹%.0f", item.remainingBalance));

            if (position % 2 == 1) {
                holder.itemView.setBackgroundColor(Color.parseColor("#141E33"));
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#0F172A"));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class SlateVH extends RecyclerView.ViewHolder {
            TextView tvSl, tvName, tvDue, tvTodayColl, tvTotalPaid, tvRem;

            SlateVH(@NonNull View v) {
                super(v);
                tvSl = v.findViewById(R.id.tvRowSlNo);
                tvName = v.findViewById(R.id.tvRowName);
                tvDue = v.findViewById(R.id.tvRowTodaysDue);
                tvTodayColl = v.findViewById(R.id.tvRowTodaysCollection);
                tvTotalPaid = v.findViewById(R.id.tvRowTotalPaid);
                tvRem = v.findViewById(R.id.tvRowRemainingBalance);
            }
        }
    }
}
