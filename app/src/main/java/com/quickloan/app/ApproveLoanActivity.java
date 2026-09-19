package com.quickloan.app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ApproveLoanActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private RecyclerView rvPending;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_loan);

        View btnBack = findViewById(R.id.btnBackApprove);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tvEmptyPendingLoans);
        rvPending = findViewById(R.id.rvPendingLoans);
        if (rvPending != null) {
            rvPending.setLayoutManager(new LinearLayoutManager(this));
        }

        loadPendingLoans();
    }

    private void loadPendingLoans() {
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.PENDING&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                String body = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, type);
                if (loans == null) loans = new ArrayList<>();

                List<Map<String, Object>> finalLoans = loans;
                runOnUiThread(() -> {
                    if (finalLoans.isEmpty()) {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                    }
                    if (rvPending != null) {
                        rvPending.setAdapter(new PendingAdapter(finalLoans));
                    }
                });
            }
        });
    }

    private void openEditAndApproveDialog(Map<String, Object> loan) {
        int loanId = (int) Double.parseDouble(String.valueOf(loan.get("id")));
        String phone = String.valueOf(loan.get("customer_phone"));
        double initialPrincipal = loan.get("principal") != null ? Double.parseDouble(String.valueOf(loan.get("principal"))) : Double.parseDouble(String.valueOf(loan.get("amount")));
        double initialRate = loan.get("interest_rate") != null ? Double.parseDouble(String.valueOf(loan.get("interest_rate"))) : 2.0;
        int initialTenure = loan.get("tenure") != null ? (int) Double.parseDouble(String.valueOf(loan.get("tenure"))) : 30;

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_approve_loan, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvInfo = view.findViewById(R.id.tvEditDialogBorrowerInfo);
        EditText etPrincipal = view.findViewById(R.id.etEditLoanPrincipal);
        EditText etRate = view.findViewById(R.id.etEditLoanRate);
        EditText etTenure = view.findViewById(R.id.etEditLoanTenure);

        TextView tvInterest = view.findViewById(R.id.tvEditTotalInterest);
        TextView tvPayable = view.findViewById(R.id.tvEditTotalPayable);
        TextView tvDailyEmi = view.findViewById(R.id.tvEditDailyEmi);

        Button btnCancel = view.findViewById(R.id.btnCancelLoanEdit);
        Button btnApprove = view.findViewById(R.id.btnConfirmApproveLoan);

        tvInfo.setText("Borrower: +91 " + phone);
        etPrincipal.setText(String.valueOf((int) initialPrincipal));
        etRate.setText(String.valueOf(initialRate));
        etTenure.setText(String.valueOf(initialTenure));

        final double[] finalPayable = {0.0};
        final double[] finalEmi = {0.0};

        Runnable recalculate = () -> {
            try {
                double p = Double.parseDouble(etPrincipal.getText().toString().trim());
                double r = Double.parseDouble(etRate.getText().toString().trim());
                int t = Integer.parseInt(etTenure.getText().toString().trim());

                if (p > 0 && t > 0) {
                    double interest = p * (r / 100.0) * (t / 30.0);
                    finalPayable[0] = p + interest;
                    finalEmi[0] = finalPayable[0] / t;

                    tvInterest.setText(String.format(Locale.getDefault(), "Interest: ₹%.0f", interest));
                    tvPayable.setText(String.format(Locale.getDefault(), "Total Repayment: ₹%.0f", finalPayable[0]));
                    tvDailyEmi.setText(String.format(Locale.getDefault(), "Daily EMI: ₹%.0f / day", finalEmi[0]));
                }
            } catch (Exception ignored) {}
        };

        recalculate.run();

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { recalculate.run(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        etPrincipal.addTextChangedListener(watcher);
        etRate.addTextChangedListener(watcher);
        etTenure.addTextChangedListener(watcher);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApprove.setOnClickListener(v -> {
            try {
                double p = Double.parseDouble(etPrincipal.getText().toString().trim());
                double r = Double.parseDouble(etRate.getText().toString().trim());
                int t = Integer.parseInt(etTenure.getText().toString().trim());

                dialog.dismiss();
                executeApproveLoan(loanId, p, finalPayable[0], r, t, finalEmi[0]);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid loan parameters", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void executeApproveLoan(int loanId, double principal, double amount, double rate, int tenure, double dailyEmi) {
        String json = String.format(Locale.US,
                "{\"principal\":%.2f,\"amount\":%.2f,\"interest_rate\":%.2f,\"tenure\":%d,\"daily_emi\":%.2f,\"disbursement_status\":\"DISBURSED\",\"status\":\"APPROVED\"}",
                principal, amount, rate, tenure, dailyEmi
        );

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?id=eq." + loanId)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .patch(body)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApproveLoanActivity.this, "Network error updating loan", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ApproveLoanActivity.this, "Loan approved with modified terms!", Toast.LENGTH_SHORT).show();
                    loadPendingLoans();
                });
            }
        });
    }

    class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.PendingVH> {
        private final List<Map<String, Object>> list;

        PendingAdapter(List<Map<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public PendingVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_loan, parent, false);
            return new PendingVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PendingVH holder, int position) {
            Map<String, Object> l = list.get(position);
            String phone = String.valueOf(l.get("customer_phone"));
            double amt = l.get("principal") != null ? Double.parseDouble(String.valueOf(l.get("principal"))) : Double.parseDouble(String.valueOf(l.get("amount")));
            double rate = l.get("interest_rate") != null ? Double.parseDouble(String.valueOf(l.get("interest_rate"))) : 2.0;
            int tenure = l.get("tenure") != null ? (int) Double.parseDouble(String.valueOf(l.get("tenure"))) : 30;
            String purpose = l.get("purpose") != null ? String.valueOf(l.get("purpose")) : "-";

            holder.tvAmount.setText(String.format(Locale.getDefault(), "Requested: ₹%.0f", amt));
            holder.tvPhone.setText("+91 " + phone);
            holder.tvTerms.setText(String.format(Locale.getDefault(), "Terms: %d Days @ %.1f%% / month", tenure, rate));
            holder.tvPurpose.setText("Purpose: " + purpose);

            holder.btnReview.setOnClickListener(v -> openEditAndApproveDialog(l));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class PendingVH extends RecyclerView.ViewHolder {
            TextView tvAmount, tvPhone, tvTerms, tvPurpose;
            Button btnReview;

            PendingVH(@NonNull View v) {
                super(v);
                tvAmount = v.findViewById(R.id.tvPendingRequestedAmount);
                tvPhone = v.findViewById(R.id.tvPendingPhone);
                tvTerms = v.findViewById(R.id.tvPendingTerms);
                tvPurpose = v.findViewById(R.id.tvPendingPurpose);
                btnReview = v.findViewById(R.id.btnReviewAndApprove);
            }
        }
    }
}
