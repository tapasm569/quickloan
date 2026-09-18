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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodaysDueActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private RecyclerView rv;
    private TextView tvTotalDue, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_due);

        tvTotalDue = findViewById(R.id.tvTotalDueAmount);
        tvEmpty = findViewById(R.id.tvEmptyDue);
        rv = findViewById(R.id.rvTodaysDue);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadTodaysDue();
    }

    private void loadTodaysDue() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.DISBURSED&is_paid=eq.0&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, type);
                if (loans == null) loans = new ArrayList<>();

                double sumEmi = 0;
                for (Map<String, Object> l : loans) {
                    double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;
                    sumEmi += emi;
                }

                double finalSum = sumEmi;
                List<Map<String, Object>> finalLoans = loans;

                runOnUiThread(() -> {
                    tvTotalDue.setText(String.format(Locale.getDefault(), "₹%.0f", finalSum));
                    if (finalLoans.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
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
                            Map<String, Object> l = finalLoans.get(position);
                            int loanId = ((Double) l.get("id")).intValue();
                            String phone = String.valueOf(l.get("phone"));
                            String name = l.get("name") != null ? String.valueOf(l.get("name")) : "Borrower";
                            double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;
                            double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                            double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                            double remaining = total - paid;

                            holder.tvName.setText(name);
                            holder.tvPhone.setText(phone);
                            holder.tvEmi.setText("₹" + (int)emi + " / day");
                            holder.tvRemaining.setText("Rem: ₹" + (int)remaining);

                            // Receive EMI Button
                            holder.btnCollect.setOnClickListener(v -> showCollectDialog(loanId, phone, emi, paid, total));

                            // Phone Call Button
                            holder.btnCall.setOnClickListener(v -> 
                                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)))
                            );

                            // WhatsApp Reminder Button
                            holder.btnWa.setOnClickListener(v -> {
                                String clean = phone.replaceAll("[^0-9]", "");
                                if (clean.length() == 10) clean = "91" + clean;
                                String msg = "Hello, reminder for your daily loan EMI of ₹" + (int)emi + ". Please pay today.";
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean + "&text=" + Uri.encode(msg))));
                            });
                        }

                        @Override public int getItemCount() { return finalLoans.size(); }
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
                .setMessage("Phone: " + phone + "\nScheduled Daily EMI: ₹" + (int)emi)
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
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

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
                            Toast.makeText(TodaysDueActivity.this, "Payment of ₹" + (int)paidNow + " recorded!", Toast.LENGTH_SHORT).show();
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
