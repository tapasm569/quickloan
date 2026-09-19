package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class TodaysDueActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private RecyclerView rv;
    private TextView tvTotalDue, tvEmpty, tvDateBadge;
    private String todayIndianDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_due);

        todayIndianDate = DateHelper.getTodayDate();

        tvTotalDue = findViewById(R.id.tvTotalDueAmount);
        tvEmpty = findViewById(R.id.tvEmptyDue);
        tvDateBadge = findViewById(R.id.tvDueCurrentDateBadge);
        rv = findViewById(R.id.rvTodaysDue);

        if (tvDateBadge != null) {
            tvDateBadge.setText(todayIndianDate);
        }

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
        }

        loadTodaysDue();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodaysDue();
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

    private void loadTodaysDue() {
        Request custReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(custReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchActiveLoans(new HashMap<>());
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
                fetchActiveLoans(phoneToName);
            }
        });
    }

    private void fetchActiveLoans(Map<String, String> phoneToName) {
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.DISBURSED&is_paid=eq.0&order=id.desc")
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
                List<Map<String, Object>> allLoans = gson.fromJson(body, type);
                if (allLoans == null) allLoans = new ArrayList<>();

                List<Map<String, Object>> dueTodayLoans = new ArrayList<>();
                double sumDueToday = 0;

                for (Map<String, Object> l : allLoans) {
                    String startDate = l.get("date") != null ? String.valueOf(l.get("date")) : "";
                    double dailyEmi = parseDoubleSafe(l.get("daily_emi"));
                    double totalAmount = parseDoubleSafe(l.get("amount"));
                    double paidAmount = parseDoubleSafe(l.get("paid_amount"));

                    double accumulatedDue = DateHelper.calculateAccumulatedDue(startDate, dailyEmi, totalAmount, paidAmount);

                    if (accumulatedDue > 0) {
                        l.put("calculated_due_today", accumulatedDue);
                        dueTodayLoans.add(l);
                        sumDueToday += accumulatedDue;
                    }
                }

                double finalSum = sumDueToday;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (tvTotalDue != null) {
                        tvTotalDue.setText(String.format(Locale.getDefault(), "₹%.0f", finalSum));
                    }

                    if (dueTodayLoans.isEmpty()) {
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("No pending dues for today! All borrowers are up to date.");
                        }
                    } else {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                    }

                    if (rv != null) {
                        rv.setAdapter(new RecyclerView.Adapter<DueVH>() {
                            @NonNull
                            @Override public DueVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todays_due, parent, false);
                                return new DueVH(v);
                            }

                            @Override public void onBindViewHolder(@NonNull DueVH holder, int position) {
                                Map<String, Object> l = dueTodayLoans.get(position);
                                int loanId = parseIntSafe(l.get("id"));
                                String phone = String.valueOf(l.get("phone"));

                                String name = phoneToName.containsKey(phone) ? phoneToName.get(phone) : String.valueOf(l.get("name"));
                                if (name == null || name.isEmpty() || name.startsWith("Borrower (")) {
                                    name = phone;
                                }

                                double accumulatedDue = parseDoubleSafe(l.get("calculated_due_today"));
                                double total = parseDoubleSafe(l.get("amount"));
                                double paid = parseDoubleSafe(l.get("paid_amount"));
                                double remaining = Math.max(0.0, total - paid);

                                holder.tvName.setText(name);
                                holder.tvPhone.setText("+91 " + phone);
                                holder.tvEmi.setText(String.format(Locale.getDefault(), "₹%.0f", accumulatedDue));
                                holder.tvRemaining.setText(String.format(Locale.getDefault(), "₹%.0f", remaining));

                                final String borrowerName = name;
                                final String borrowerPhone = phone;

                                holder.btnCollect.setOnClickListener(v -> 
                                    showCollectDialog(loanId, borrowerPhone, borrowerName, accumulatedDue, paid, total)
                                );

                                holder.btnCall.setOnClickListener(v -> 
                                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + borrowerPhone)))
                                );

                                holder.btnWa.setOnClickListener(v -> {
                                    String clean = borrowerPhone.replaceAll("[^0-9]", "");
                                    if (clean.length() == 10) clean = "91" + clean;
                                    String msg = "Hello " + borrowerName + ", your total due amount of ₹" + (int)accumulatedDue + " is due today. Please pay to keep your account current.";
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean + "&text=" + Uri.encode(msg))));
                                });
                            }

                            @Override public int getItemCount() { return dueTodayLoans.size(); }
                        });
                    }
                });
            }
        });
    }

    private void showCollectDialog(int loanId, String phone, String name, double dueToday, double currentPaid, double total) {
        EditText et = new EditText(this);
        et.setHint("Amount to Collect");
        et.setText(String.valueOf((int)dueToday));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(android.graphics.Color.WHITE);
        et.setBackgroundResource(R.drawable.edittext_bg);
        et.setPadding(30, 30, 30, 30);

        new AlertDialog.Builder(this)
                .setTitle("Receive Daily EMI")
                .setMessage("Borrower: " + name + " (+91 " + phone + ")\nTotal Due Today: ₹" + (int)dueToday)
                .setView(et)
                .setPositiveButton("Receive Cash", (dialog, which) -> {
                    String val = et.getText().toString().trim();
                    if (val.isEmpty()) return;
                    double amt = parseDoubleSafe(val);
                    recordPayment(loanId, phone, amt, currentPaid + amt, (currentPaid + amt) >= total, "CASH");
                })
                .setNeutralButton("Receive UPI", (dialog, which) -> {
                    String val = et.getText().toString().trim();
                    if (val.isEmpty()) return;
                    double amt = parseDoubleSafe(val);
                    recordPayment(loanId, phone, amt, currentPaid + amt, (currentPaid + amt) >= total, "UPI");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void recordPayment(int loanId, String phone, double paidNow, double totalPaid, boolean fullyPaid, String mode) {
        String today = DateHelper.getTodayDate();

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
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(TodaysDueActivity.this, "Network error updating loan", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                Map<String, Object> tx = new HashMap<>();
                tx.put("loan_id", loanId);
                tx.put("customer_phone", phone);
                tx.put("lender_phone", "9932655607");
                tx.put("amount", paidNow);
                tx.put("payment_type", "DAILY_EMI");
                tx.put("payment_mode", mode);
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
                            Toast.makeText(TodaysDueActivity.this, "Payment recorded: ₹" + (int)paidNow + " (" + mode + ")", Toast.LENGTH_SHORT).show();
                            loadTodaysDue();
                        });
                    }
                });
            }
        });
    }

    static class DueVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvEmi, tvRemaining;
        Button btnCollect, btnCall, btnWa;

        DueVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvDueCustomerName);
            tvPhone = v.findViewById(R.id.tvDueCustomerPhone);
            tvEmi = v.findViewById(R.id.tvDueEmiAmount);
            tvRemaining = v.findViewById(R.id.tvDueRemaining);
            btnCollect = v.findViewById(R.id.btnCollectEmi);
            btnCall = v.findViewById(R.id.btnCallBorrower);
            btnWa = v.findViewById(R.id.btnWaBorrower);
        }
    }
}
