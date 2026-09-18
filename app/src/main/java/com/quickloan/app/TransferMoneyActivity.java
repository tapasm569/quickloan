package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransferMoneyActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private RecyclerView rv;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_money);

        rv = findViewById(R.id.rvTransferLoans);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadWaitingLoans();
    }

    private void loadWaitingLoans() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.WAITING_FOR_PAYMENT&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> list = gson.fromJson(body, listType);

                runOnUiThread(() -> rv.setAdapter(new RecyclerView.Adapter<TransferVH>() {
                    @NonNull
                    @Override
                    public TransferVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer_money, parent, false);
                        return new TransferVH(v);
                    }

                    @Override
                    public void onBindViewHolder(@NonNull TransferVH holder, int position) {
                        Map<String, Object> item = list.get(position);
                        int loanId = ((Double) item.get("id")).intValue();
                        String phone = String.valueOf(item.get("phone"));
                        double principal = item.get("principal") != null ? ((Double) item.get("principal")) : 
                                          (item.get("amount") != null ? ((Double) item.get("amount")) : 0);

                        holder.tvNamePhone.setText("Phone: " + phone);
                        holder.tvAmount.setText(String.format("₹%.0f", principal));

                        // Fetch customer UPI ID
                        fetchCustomerUpi(phone, holder.tvUpi);

                        // UPI intent trigger
                        holder.btnUpi.setOnClickListener(v -> {
                            String upiId = holder.tvUpi.getText().toString().replace("UPI: ", "").trim();
                            if (upiId.isEmpty() || upiId.equalsIgnoreCase("Not set")) {
                                Toast.makeText(TransferMoneyActivity.this, "Customer has not configured a UPI ID yet!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Uri uri = Uri.parse("upi://pay?pa=" + upiId + "&pn=Borrower&am=" + (int)principal + "&cu=INR");
                            Intent upiIntent = new Intent(Intent.ACTION_VIEW, uri);
                            try {
                                startActivity(Intent.createChooser(upiIntent, "Pay via UPI App"));
                            } catch (Exception e) {
                                Toast.makeText(TransferMoneyActivity.this, "No UPI App found", Toast.LENGTH_SHORT).show();
                            }
                        });

                        // Mark as Paid (Cash / UPI confirmation)
                        holder.btnCash.setOnClickListener(v -> new AlertDialog.Builder(TransferMoneyActivity.this)
                                .setTitle("Confirm Disbursement")
                                .setMessage("Confirm loan money of ₹" + (int)principal + " transferred to customer?")
                                .setPositiveButton("Confirm (Paid)", (d, w) -> recordDisbursement(loanId, phone, principal, "CASH"))
                                .setNegativeButton("Cancel", null)
                                .show());
                    }

                    @Override public int getItemCount() { return list != null ? list.size() : 0; }
                }));
            }
        });
    }

    private void fetchCustomerUpi(String phone, TextView target) {
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + phone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> res = gson.fromJson(body, type);
                if (res != null && !res.isEmpty()) {
                    Object upi = res.get(0).get("upi_id");
                    runOnUiThread(() -> target.setText("UPI: " + (upi != null ? upi.toString() : "Not set")));
                }
            }
        });
    }

    private void recordDisbursement(int loanId, String phone, double amount, String mode) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Update loan status to DISBURSED
        Map<String, Object> update = new HashMap<>();
        update.put("disbursement_status", "DISBURSED");
        update.put("disbursement_mode", mode);

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
                // 2. Insert ledger transaction entry
                Map<String, Object> tx = new HashMap<>();
                tx.put("loan_id", loanId);
                tx.put("customer_phone", phone);
                tx.put("lender_phone", "9932655607");
                tx.put("amount", amount);
                tx.put("payment_type", "DISBURSEMENT");
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
                            Toast.makeText(TransferMoneyActivity.this, "Loan money marked as PAID!", Toast.LENGTH_SHORT).show();
                            loadWaitingLoans();
                        });
                    }
                });
            }
        });
    }

    static class TransferVH extends RecyclerView.ViewHolder {
        TextView tvNamePhone, tvUpi, tvAmount;
        Button btnUpi, btnCash;
        public TransferVH(@NonNull View v) {
            super(v);
            tvNamePhone = v.findViewById(R.id.tvTransferNamePhone);
            tvUpi = v.findViewById(R.id.tvCustomerUpiId);
            tvAmount = v.findViewById(R.id.tvDisburseAmount);
            btnUpi = v.findViewById(R.id.btnPayUpi);
            btnCash = v.findViewById(R.id.btnPayCash);
        }
    }
}
