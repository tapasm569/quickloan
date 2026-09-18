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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TodaysDueActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private RecyclerView rv;
    private TextView tvTotalDue, tvEmpty;
    private String todayIndianDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_due);

        todayIndianDate = DateHelper.getTodayDate();

        tvTotalDue = findViewById(R.id.tvTotalDueAmount);
        tvEmpty = findViewById(R.id.tvEmptyDue);
        rv = findViewById(R.id.rvTodaysDue);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadTodaysDue();
    }

    private void loadTodaysDue() {
        Request txReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(txReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Set<Integer> paidTodayLoanIds = new HashSet<>();
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> txs = gson.fromJson(response.body().string(), type);
                    if (txs != null) {
                        for (Map<String, Object> t : txs) {
                            String txDate = DateHelper.formatToIndianDate(String.valueOf(t.get("transaction_date")));
                            if (todayIndianDate.equals(txDate) && "DAILY_EMI".equals(t.get("payment_type")) && t.get("loan_id") != null) {
                                paidTodayLoanIds.add(((Double) t.get("loan_id")).intValue());
                            }
                        }
                    }
                }
                fetchActiveLoans(paidTodayLoanIds);
            }
        });
    }

    private void fetchActiveLoans(Set<Integer> paidTodayLoanIds) {
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.DISBURSED&is_paid=eq.0&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> allLoans = gson.fromJson(body, type);

                List<Map<String, Object>> dueTodayLoans = new ArrayList<>();
                double sumDueToday = 0;

                if (allLoans != null) {
                    for (Map<String, Object> l : allLoans) {
                        int loanId = ((Double) l.get("id")).intValue();
                        if (!paidTodayLoanIds.contains(loanId)) {
                            dueTodayLoans.add(l);
                            double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;
                            sumDueToday += emi;
                        }
                    }
                }

                double finalSum = sumDueToday;
                runOnUiThread(() -> {
                    tvTotalDue.setText(String.format(Locale.getDefault(), "₹%.0f", finalSum));
                    if (dueTodayLoans.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("All daily dues for today have been paid!");
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }

                    rv.setAdapter(new RecyclerView.Adapter<DueVH>() {
                        @NonNull
                        @Override public DueVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todays_due, parent, false);
                            return new DueVH(v);
                        }

                        @Override public void onBindViewHolder(@NonNull DueVH holder, int position) {
                            Map<String, Object> l = dueTodayLoans.get(position);
                            int loanId = ((Double) l.get("id")).intValue();
                            String phone = String.valueOf(l.get("phone"));
                            String name = l.get("name") != null ? String.valueOf(l.get("name")) : "Borrower";
                            double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;
                            double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                            double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                            double remaining = total - paid;

                            holder.tvName.setText(name);
                            holder.tvPhone.setText("+91 " + phone);
                            holder.tvEmi.setText("₹" + (int)emi + " / day");
                            holder.tvRemaining.setText("Rem: ₹" + (int)remaining);

                            holder.btnCollect.setOnClickListener(v -> showCollectDialog(loanId, phone, emi, paid, total));

                            holder.btnCall.setOnClickListener(v -> 
                                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)))
                            );

                            holder.btnWa.setOnClickListener(v -> {
                                String clean = phone.replaceAll("[^0-9]", "");
                                if (clean.length() == 10) clean = "91" + clean;
                                String msg = "Hello " + name + ", your daily EMI of ₹" + (int)emi + " is due today. Please pay to keep your account current.";
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean + "&text=" + Uri.encode(msg))));
                            });
                        }

                        @Override public int getItemCount() { return dueTodayLoans.size(); }
                    });
                });
            }
        });
    }

    private void showCollectDialog(int loanId, String phone, double emi, double currentPaid, double total) {
        EditText et = new EditText(this);
        et.setHint("Collected EMI Amount");
        et.setText(String.valueOf((int)emi));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Receive Daily EMI")
                .setMessage("Borrower: " + phone + "\nDue Amount Today: ₹" + (int)emi)
                .setView(et)
                .setPositiveButton("Receive Cash", (dialog, which) -> {
                    String val = et.getText().toString().trim();
                    if (val.isEmpty()) return;
                    double amt = Double.parseDouble(val);
                    recordPayment(loanId, phone, amt, currentPaid + amt, (currentPaid + amt) >= total, "CASH");
                })
                .setNeutralButton("Receive UPI", (dialog, which) -> {
                    String val = et.getText().toString().trim();
                    if (val.isEmpty()) return;
                    double amt = Double.parseDouble(val);
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
            @Override public void onFailure(Call call, IOException e) {}

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
                            Toast.makeText(TodaysDueActivity.this, "Payment recorded for " + today, Toast.LENGTH_SHORT).show();
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
