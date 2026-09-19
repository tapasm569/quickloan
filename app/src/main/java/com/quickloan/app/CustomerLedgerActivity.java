package com.quickloan.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerLedgerActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private TextView tvApprovedLoan, tvPaidBalance, tvRemainingBalance, tvDateBadge, tvEmpty;
    private RecyclerView rvLedger;
    private Button btnViewPdf, btnSendWa;

    private String customerPhone = "";
    private final List<CustLedgerEntry> currentEntries = new ArrayList<>();
    private double curApproved = 0.0, curPaid = 0.0, curRemaining = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_ledger);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) customerPhone = getIntent().getStringExtra("phone");
        if (customerPhone == null || customerPhone.isEmpty()) {
            SharedPreferences sp = getSharedPreferences("QuickLoanPrefs", MODE_PRIVATE);
            customerPhone = sp.getString("phone", sp.getString("customer_phone", ""));
        }

        View btnBack = findViewById(R.id.btnBackCustomerLedger);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvApprovedLoan = findViewById(R.id.tvCustApprovedLoan);
        tvPaidBalance = findViewById(R.id.tvCustPaidBalance);
        tvRemainingBalance = findViewById(R.id.tvCustRemainingBalance);
        tvDateBadge = findViewById(R.id.tvCustomerLedgerDate);
        tvEmpty = findViewById(R.id.tvEmptyCustomerLedger);

        btnViewPdf = findViewById(R.id.btnCustViewPdf);
        btnSendWa = findViewById(R.id.btnCustSendWa);

        if (tvDateBadge != null) tvDateBadge.setText(DateHelper.getTodayDate());

        rvLedger = findViewById(R.id.rvCustomerLedger);
        if (rvLedger != null) rvLedger.setLayoutManager(new LinearLayoutManager(this));

        setupPdfButtons();
        loadCustomerLedgerData();
    }

    private void setupPdfButtons() {
        btnViewPdf.setOnClickListener(v -> {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getFilesDir();
            File expectedPdf = new File(dir, "Customer_Statement_" + customerPhone + ".pdf");

            // Condition: Directly open from storage if file is already created; otherwise generate and open
            if (expectedPdf.exists() && expectedPdf.length() > 0) {
                PdfHelper.openPdfFromStorage(CustomerLedgerActivity.this, expectedPdf);
            } else {
                File generated = PdfHelper.generateCustomerPdf(CustomerLedgerActivity.this, customerPhone, curApproved, curPaid, curRemaining, currentEntries);
                if (generated != null && generated.exists()) {
                    PdfHelper.openPdfFromStorage(CustomerLedgerActivity.this, generated);
                } else {
                    Toast.makeText(CustomerLedgerActivity.this, "Failed to generate statement PDF.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSendWa.setOnClickListener(v -> {
            File generated = PdfHelper.generateCustomerPdf(CustomerLedgerActivity.this, customerPhone, curApproved, curPaid, curRemaining, currentEntries);
            if (generated != null && generated.exists()) {
                PdfHelper.sendPdfToWhatsApp(CustomerLedgerActivity.this, generated);
            } else {
                Toast.makeText(CustomerLedgerActivity.this, "Failed to prepare PDF for WhatsApp.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double parseDoubleSafe(Object obj) {
        if (obj == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void loadCustomerLedgerData() {
        if (customerPhone.isEmpty()) return;

        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + customerPhone + "&disbursement_status=eq.DISBURSED&order=id.asc")
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
                if (!response.isSuccessful() || response.body() == null) return;
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(response.body().string(), type);
                if (loans == null) loans = new ArrayList<>();

                double totalApprovedLoan = 0.0;
                double totalPaidSum = 0.0;

                for (Map<String, Object> l : loans) {
                    totalApprovedLoan += parseDoubleSafe(l.get("amount"));
                    totalPaidSum += parseDoubleSafe(l.get("paid_amount"));
                }

                curApproved = totalApprovedLoan;
                curPaid = totalPaidSum;
                curRemaining = Math.max(0.0, totalApprovedLoan - totalPaidSum);

                runOnUiThread(() -> {
                    if (tvApprovedLoan != null) tvApprovedLoan.setText(String.format(Locale.getDefault(), "₹%.0f", curApproved));
                    if (tvPaidBalance != null) tvPaidBalance.setText(String.format(Locale.getDefault(), "₹%.0f", curPaid));
                    if (tvRemainingBalance != null) tvRemainingBalance.setText(String.format(Locale.getDefault(), "₹%.0f", curRemaining));
                });

                fetchTransactionsAndBuildTable(curApproved);
            }
        });
    }

    private void fetchTransactionsAndBuildTable(double startingTotalLoanAmount) {
        Request txReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?customer_phone=eq." + customerPhone + "&order=id.asc")
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
                if (!response.isSuccessful() || response.body() == null) return;
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> txList = gson.fromJson(response.body().string(), type);
                if (txList == null) txList = new ArrayList<>();

                List<CustLedgerEntry> entries = new ArrayList<>();
                double runningRemaining = startingTotalLoanAmount;

                int sl = 1;
                for (Map<String, Object> t : txList) {
                    double paidAmt = parseDoubleSafe(t.get("amount"));
                    if (paidAmt <= 0) continue;

                    runningRemaining = Math.max(0.0, runningRemaining - paidAmt);
                    String date = DateHelper.formatToIndianDate(String.valueOf(t.get("transaction_date")));

                    entries.add(new CustLedgerEntry(sl++, date, paidAmt, runningRemaining));
                }

                Collections.reverse(entries);
                for (int i = 0; i < entries.size(); i++) entries.get(i).slNo = i + 1;

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    currentEntries.clear();
                    currentEntries.addAll(entries);

                    if (entries.isEmpty()) {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                    }

                    if (rvLedger != null) rvLedger.setAdapter(new CustLedgerAdapter(entries));
                });
            }
        });
    }

    public static class CustLedgerEntry {
        public int slNo;
        public String date;
        public double paid;
        public double remainingBalance;

        public CustLedgerEntry(int slNo, String date, double paid, double remainingBalance) {
            this.slNo = slNo;
            this.date = date;
            this.paid = paid;
            this.remainingBalance = remainingBalance;
        }
    }

    static class CustLedgerAdapter extends RecyclerView.Adapter<CustLedgerAdapter.CustLedgerVH> {
        private final List<CustLedgerEntry> list;
        CustLedgerAdapter(List<CustLedgerEntry> list) { this.list = list; }

        @NonNull @Override
        public CustLedgerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer_ledger_row, parent, false);
            return new CustLedgerVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CustLedgerVH holder, int position) {
            CustLedgerEntry item = list.get(position);
            holder.tvSl.setText(String.valueOf(item.slNo));
            holder.tvDate.setText(item.date);
            holder.tvPaid.setText(String.format(Locale.getDefault(), "₹%.0f", item.paid));
            holder.tvRemaining.setText(String.format(Locale.getDefault(), "₹%.0f", item.remainingBalance));

            holder.itemView.setBackgroundColor(position % 2 == 1 ? Color.parseColor("#141E33") : Color.parseColor("#0F172A"));
        }

        @Override public int getItemCount() { return list.size(); }

        static class CustLedgerVH extends RecyclerView.ViewHolder {
            TextView tvSl, tvDate, tvPaid, tvRemaining;
            CustLedgerVH(@NonNull View v) {
                super(v);
                tvSl = v.findViewById(R.id.tvCustRowSl);
                tvDate = v.findViewById(R.id.tvCustRowDate);
                tvPaid = v.findViewById(R.id.tvCustRowPaid);
                tvRemaining = v.findViewById(R.id.tvCustRowRemaining);
            }
        }
    }
}
