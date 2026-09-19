package com.quickloan.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingLoans();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private double parseDoubleSafe(Object obj, double defaultVal) {
        if (obj == null) return defaultVal;
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private int parseIntSafe(Object obj, int defaultVal) {
        if (obj == null) return defaultVal;
        try {
            return (int) Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private void loadPendingLoans() {
        // Query loans where disbursement_status is PENDING or status is PENDING
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?or=(disbursement_status.eq.PENDING,status.eq.PENDING)&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    if (tvEmpty != null) {
                        tvEmpty.setText("Network error loading loans: " + e.getMessage());
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        if (tvEmpty != null) {
                            tvEmpty.setText("Failed to load records from Supabase");
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    });
                    return;
                }

                String body = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, type);
                if (loans == null) loans = new ArrayList<>();

                List<Map<String, Object>> finalLoans = loans;
                runOnUiThread(() -> {
                    if (finalLoans.isEmpty()) {
                        if (tvEmpty != null) {
                            tvEmpty.setText("No pending loan applications found.");
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
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
        int loanId = parseIntSafe(loan.get("id"), 0);
        String phone = String.valueOf(loan.get("customer_phone"));
        String purpose = loan.get("purpose") != null ? String.valueOf(loan.get("purpose")) : "Personal";
        
        double initialPrincipal = loan.get("principal") != null ? 
                parseDoubleSafe(loan.get("principal"), 10000) : 
                parseDoubleSafe(loan.get("amount"), 10000);
        
        double initialRate = parseDoubleSafe(loan.get("interest_rate"), 2.0);
        int initialTenure = parseIntSafe(loan.get("tenure"), 30);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1E293B"));
        bg.setCornerRadius(dpToPx(16));
        container.setBackground(bg);

        // Header
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Review & Approve Loan");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        container.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Applicant: +91 " + phone + " (" + purpose + ")");
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSub.setPadding(0, dpToPx(2), 0, dpToPx(12));
        container.addView(tvSub);

        // Inputs
        container.addView(createLabel("LOAN PRINCIPAL (₹)"));
        EditText etPrincipal = createInput(String.valueOf((int) initialPrincipal), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(etPrincipal);

        container.addView(createLabel("INTEREST RATE (% PER MONTH)"));
        EditText etRate = createInput(String.valueOf(initialRate), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(etRate);

        container.addView(createLabel("LOAN TENURE (DAYS)"));
        EditText etTenure = createInput(String.valueOf(initialTenure), InputType.TYPE_CLASS_NUMBER);
        container.addView(etTenure);

        // Real-Time Calculation Box
        LinearLayout calcBox = new LinearLayout(this);
        calcBox.setOrientation(LinearLayout.VERTICAL);
        calcBox.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        LinearLayout.LayoutParams calcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        calcParams.setMargins(0, dpToPx(14), 0, dpToPx(14));
        calcBox.setLayoutParams(calcParams);

        GradientDrawable calcBg = new GradientDrawable();
        calcBg.setColor(Color.parseColor("#0F172A"));
        calcBg.setCornerRadius(dpToPx(10));
        calcBox.setBackground(calcBg);

        TextView tvInterest = new TextView(this);
        tvInterest.setTextColor(Color.parseColor("#A855F7"));
        tvInterest.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvInterest.setTypeface(null, Typeface.BOLD);
        calcBox.addView(tvInterest);

        TextView tvPayable = new TextView(this);
        tvPayable.setTextColor(Color.parseColor("#F59E0B"));
        tvPayable.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvPayable.setTypeface(null, Typeface.BOLD);
        tvPayable.setPadding(0, dpToPx(4), 0, dpToPx(4));
        calcBox.addView(tvPayable);

        TextView tvDailyEmi = new TextView(this);
        tvDailyEmi.setTextColor(Color.parseColor("#10B981"));
        tvDailyEmi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvDailyEmi.setTypeface(null, Typeface.BOLD);
        calcBox.addView(tvDailyEmi);

        container.addView(calcBox);

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

                    tvInterest.setText(String.format(Locale.getDefault(), "Total Interest: ₹%.0f", interest));
                    tvPayable.setText(String.format(Locale.getDefault(), "Total Payable: ₹%.0f", finalPayable[0]));
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

        // Buttons: Reject & Approve
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);

        Button btnReject = new Button(this);
        btnReject.setText("Reject");
        btnReject.setTextColor(Color.WHITE);
        btnReject.setBackgroundColor(Color.parseColor("#E11D48"));
        btnReject.setOnClickListener(v -> {
            dialog.dismiss();
            executeRejectLoan(loanId);
        });

        Button btnApprove = new Button(this);
        btnApprove.setText("Approve Loan");
        btnApprove.setTextColor(Color.WHITE);
        btnApprove.setTypeface(null, Typeface.BOLD);
        btnApprove.setBackgroundColor(Color.parseColor("#10B981"));

        LinearLayout.LayoutParams btnApproveParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnApproveParams.setMargins(dpToPx(10), 0, 0, 0);
        btnApprove.setLayoutParams(btnApproveParams);

        btnApprove.setOnClickListener(v -> {
            try {
                double p = Double.parseDouble(etPrincipal.getText().toString().trim());
                double r = Double.parseDouble(etRate.getText().toString().trim());
                int t = Integer.parseInt(etTenure.getText().toString().trim());

                dialog.dismiss();
                executeApproveLoan(loanId, p, finalPayable[0], r, t, finalEmi[0]);
            } catch (Exception e) {
                Toast.makeText(this, "Please verify all numerical fields", Toast.LENGTH_SHORT).show();
            }
        });

        btnRow.addView(btnReject);
        btnRow.addView(btnApprove);
        container.addView(btnRow);

        scrollView.addView(container);
        dialog.setContentView(scrollView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#38BDF8"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dpToPx(8), 0, dpToPx(4));
        return tv;
    }

    private EditText createInput(String value, int inputType) {
        EditText et = new EditText(this);
        et.setText(value);
        et.setInputType(inputType);
        et.setTextColor(Color.WHITE);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        et.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#0F172A"));
        inputBg.setCornerRadius(dpToPx(8));
        inputBg.setStroke(dpToPx(1), Color.parseColor("#334155"));
        et.setBackground(inputBg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(46));
        params.setMargins(0, dpToPx(2), 0, dpToPx(6));
        et.setLayoutParams(params);
        return et;
    }

    private void executeApproveLoan(int loanId, double principal, double amount, double rate, int tenure, double dailyEmi) {
        String today = DateHelper.getTodayDate();
        String json = String.format(Locale.US,
                "{\"principal\":%.2f,\"amount\":%.2f,\"interest_rate\":%.2f,\"tenure\":%d,\"daily_emi\":%.2f,\"disbursement_status\":\"DISBURSED\",\"status\":\"APPROVED\",\"date\":\"%s\"}",
                principal, amount, rate, tenure, dailyEmi, today
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
                runOnUiThread(() -> Toast.makeText(ApproveLoanActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ApproveLoanActivity.this, "Loan approved successfully!", Toast.LENGTH_SHORT).show();
                        loadPendingLoans();
                    });
                } else {
                    String err = response.body() != null ? response.body().string() : "Error " + response.code();
                    runOnUiThread(() -> Toast.makeText(ApproveLoanActivity.this, "Approval failed: " + err, Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void executeRejectLoan(int loanId) {
        String json = "{\"disbursement_status\":\"REJECTED\",\"status\":\"REJECTED\"}";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?id=eq." + loanId)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .patch(body)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ApproveLoanActivity.this, "Network error rejecting loan", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ApproveLoanActivity.this, "Loan application rejected", Toast.LENGTH_SHORT).show();
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_master_client, parent, false);
            return new PendingVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PendingVH holder, int position) {
            Map<String, Object> l = list.get(position);
            String phone = String.valueOf(l.get("customer_phone"));
            double amt = l.get("principal") != null ? 
                    parseDoubleSafe(l.get("principal"), 0) : 
                    parseDoubleSafe(l.get("amount"), 0);

            holder.tvName.setText(String.format(Locale.getDefault(), "Requested: ₹%.0f", amt));
            holder.tvPhone.setText("+91 " + phone);
            holder.tvDue.setText("PENDING");

            holder.btnAction.setText("Review & Approve");
            holder.btnAction.setBackgroundColor(Color.parseColor("#10B981"));
            holder.btnAction.setOnClickListener(v -> openEditAndApproveDialog(l));

            if (holder.btnCall != null) {
                holder.btnCall.setVisibility(View.VISIBLE);
                holder.btnCall.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phone));
                    startActivity(callIntent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class PendingVH extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvDue;
            Button btnAction, btnCall;

            PendingVH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvClientName);
                tvPhone = v.findViewById(R.id.tvClientPhone);
                tvDue = v.findViewById(R.id.tvClientDueBalance);
                btnAction = v.findViewById(R.id.btnViewClientProfile);
                btnCall = v.findViewById(R.id.btnCallClient);
            }
        }
    }
}
