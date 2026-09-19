package com.quickloan.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class AccountDashboardFragment extends Fragment {

    public static final String PREF_NAME = "QuickLoanPrefs";
    public static final String KEY_MONTHLY_RATE = "PREF_MONTHLY_INTEREST_RATE";

    private TextView tvInterestBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvInterestBadge = view.findViewById(R.id.tvAccountInterestBadge);
        updateInterestBadge();

        // 1. Set Monthly Interest Rate Card
        view.findViewById(R.id.cardSetInterestRate).setOnClickListener(v -> showSetInterestRateDialog());

        // 2. Slate Ledger Book Card
        view.findViewById(R.id.cardAccountLedgerBook).setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), PaymentHistoryActivity.class));
            }
        });

        // 3. Today's Due & Pending Card
        view.findViewById(R.id.cardAccountTodaysDue).setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), TodaysDueActivity.class));
            }
        });

        // 4. Today's Payment Card
        view.findViewById(R.id.cardAccountTodaysPayment).setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), TodaysPaymentActivity.class));
            }
        });

        // 5. Master Client Directory Card
        view.findViewById(R.id.cardAccountMasterDirectory).setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), MasterActivity.class));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInterestBadge();
    }

    private void updateInterestBadge() {
        if (getContext() == null || tvInterestBadge == null) return;
        SharedPreferences sp = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        float currentRate = sp.getFloat(KEY_MONTHLY_RATE, 2.0f);
        tvInterestBadge.setText(String.format(Locale.getDefault(), "Current: %.1f%% / month", currentRate));
    }

    private void showSetInterestRateDialog() {
        if (getContext() == null) return;
        SharedPreferences sp = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        float currentRate = sp.getFloat(KEY_MONTHLY_RATE, 2.0f);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Set Monthly Interest Rate");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 30, 60, 10);

        TextView tvDesc = new TextView(getContext());
        tvDesc.setText("Set default interest rate (% per month) applied to all borrower loans:");
        tvDesc.setTextSize(13);
        tvDesc.setTextColor(Color.parseColor("#94A3B8"));
        layout.addView(tvDesc);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.US, "%.1f", currentRate));
        input.setHint("e.g. 2.0");
        input.setTextSize(16);
        layout.addView(input);

        TextView tvNote = new TextView(getContext());
        tvNote.setText("Formula: Principal × (Rate% / month) × (Days / 30)");
        tvNote.setTextSize(11);
        tvNote.setPadding(0, 16, 0, 0);
        tvNote.setTextColor(Color.parseColor("#38BDF8"));
        layout.addView(tvNote);

        builder.setView(layout);

        builder.setPositiveButton("Save Preset", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (!val.isEmpty()) {
                try {
                    float newRate = Float.parseFloat(val);
                    sp.edit().putFloat(KEY_MONTHLY_RATE, newRate).apply();
                    updateInterestBadge();
                    Toast.makeText(getContext(), "Interest preset updated to " + newRate + "% / month", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
