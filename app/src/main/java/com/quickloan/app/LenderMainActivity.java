package com.quickloan.app;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class LenderMainActivity extends AppCompatActivity {

    public static final String PREF_NAME = "QuickLoanPrefs";
    public static final String KEY_MONTHLY_RATE = "PREF_MONTHLY_INTEREST_RATE";

    private TextView tvCurrentRateBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lender_main);

        tvCurrentRateBadge = findViewById(R.id.tvLenderCurrentRateBadge);
        updateRateDisplay();

        View btnSetInterest = findViewById(R.id.btnLenderSetInterest);
        if (btnSetInterest != null) {
            btnSetInterest.setOnClickListener(v -> showSetInterestRateDialog());
        }

        bindNav(R.id.cardMasterDirectory, MasterActivity.class);
        bindNav(R.id.cardApproveLoans, ApproveLoanActivity.class);
        bindNav(R.id.cardLedgerBook, PaymentHistoryActivity.class);
        bindNav(R.id.cardTodaysDue, TodaysDueActivity.class);
        bindNav(R.id.cardTodaysPayment, TodaysPaymentActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRateDisplay();
    }

    private void bindNav(int viewId, Class<?> target) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(view -> startActivity(new Intent(LenderMainActivity.this, target)));
        }
    }

    private void updateRateDisplay() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        float currentRate = sp.getFloat(KEY_MONTHLY_RATE, 2.0f);
        if (tvCurrentRateBadge != null) {
            tvCurrentRateBadge.setText(String.format(Locale.getDefault(), "Current: %.1f%% / month", currentRate));
        }
    }

    private void showSetInterestRateDialog() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        float currentRate = sp.getFloat(KEY_MONTHLY_RATE, 2.0f);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_interest, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.90), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etRate = dialogView.findViewById(R.id.etDialogMonthlyRate);
        TextView tvExample = dialogView.findViewById(R.id.tvDialogRateExample);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelRate);
        Button btnSave = dialogView.findViewById(R.id.btnSaveRate);

        etRate.setText(String.valueOf(currentRate));

        etRate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double r = Double.parseDouble(s.toString().trim());
                    double monthlyInt = 10000.0 * (r / 100.0);
                    tvExample.setText(String.format(Locale.getDefault(), "Calculation: ₹10,000 at %.1f%% for 30 days = ₹%.0f interest", r, monthlyInt));
                } catch (Exception e) {
                    tvExample.setText("Enter a valid percentage");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String val = etRate.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(this, "Please enter a valid interest rate", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                float newRate = Float.parseFloat(val);
                sp.edit().putFloat(KEY_MONTHLY_RATE, newRate).apply();
                updateRateDisplay();
                dialog.dismiss();
                Toast.makeText(this, "Preset interest updated to " + newRate + "% per month", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
