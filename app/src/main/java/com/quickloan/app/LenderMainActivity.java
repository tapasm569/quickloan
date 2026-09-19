package com.quickloan.app;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class LenderMainActivity extends AppCompatActivity {

    public static final String PREF_NAME = "QuickLoanPrefs";
    public static final String KEY_MONTHLY_RATE = "PREF_MONTHLY_INTEREST_RATE";

    private DrawerLayout drawerLayout;
    private TextView tvHeaderInterestBadge;

    private ImageView ivSearch, ivPending, ivPayment, ivAccount;
    private TextView tvSearch, tvPending, tvPayment, tvAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lender_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        tvHeaderInterestBadge = findViewById(R.id.tvHeaderInterestBadge);

        // 3-Line Hamburger Menu Button
        View btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        setupDrawerMenuItems();
        setupTabs();
        updateRateDisplay();

        // Default home tab: SearchTabFragment
        switchTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRateDisplay();
    }

    private void updateRateDisplay() {
        SharedPreferences sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        float currentRate = sp.getFloat(KEY_MONTHLY_RATE, 2.0f);
        if (tvHeaderInterestBadge != null) {
            tvHeaderInterestBadge.setText(String.format(Locale.getDefault(), "%.1f%% / mo", currentRate));
        }
    }

    private void setupDrawerMenuItems() {
        View menuInterest = findViewById(R.id.menuSetInterestRate);
        if (menuInterest != null) {
            menuInterest.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showSetInterestRateDialog();
            });
        }

        View menuMaster = findViewById(R.id.menuMasterDirectory);
        if (menuMaster != null) {
            menuMaster.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, MasterActivity.class));
            });
        }

        View menuLedger = findViewById(R.id.menuLedgerBook);
        if (menuLedger != null) {
            menuLedger.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, PaymentHistoryActivity.class));
            });
        }

        View menuDue = findViewById(R.id.menuTodaysDue);
        if (menuDue != null) {
            menuDue.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, TodaysDueActivity.class));
            });
        }

        View menuPayment = findViewById(R.id.menuTodaysPayment);
        if (menuPayment != null) {
            menuPayment.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, TodaysPaymentActivity.class));
            });
        }
    }

    private void setupTabs() {
        ivSearch = findViewById(R.id.ivTabSearch);
        ivPending = findViewById(R.id.ivTabPending);
        ivPayment = findViewById(R.id.ivTabPayment);
        ivAccount = findViewById(R.id.ivTabAccount);

        tvSearch = findViewById(R.id.tvTabSearch);
        tvPending = findViewById(R.id.tvTabPending);
        tvPayment = findViewById(R.id.tvTabPayment);
        tvAccount = findViewById(R.id.tvTabAccount);

        findViewById(R.id.tabSearch).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.tabPending).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.tabPayment).setOnClickListener(v -> switchTab(2));
        findViewById(R.id.tabAccount).setOnClickListener(v -> switchTab(3));
    }

    private void switchTab(int index) {
        resetTabColors();
        Fragment selectedFragment = null;

        if (index == 0) {
            setTabActive(ivSearch, tvSearch);
            selectedFragment = new SearchTabFragment();
        } else if (index == 1) {
            setTabActive(ivPending, tvPending);
            startActivity(new Intent(this, ApproveLoanActivity.class));
            return;
        } else if (index == 2) {
            setTabActive(ivPayment, tvPayment);
            startActivity(new Intent(this, PaymentHistoryActivity.class));
            return;
        } else if (index == 3) {
            setTabActive(ivAccount, tvAccount);
            // Open the 3-line side menu to access Account & Interest settings
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }
    }

    private void resetTabColors() {
        int inactiveColor = Color.parseColor("#64748B");
        if (ivSearch != null) ivSearch.setColorFilter(inactiveColor);
        if (ivPending != null) ivPending.setColorFilter(inactiveColor);
        if (ivPayment != null) ivPayment.setColorFilter(inactiveColor);
        if (ivAccount != null) ivAccount.setColorFilter(inactiveColor);

        if (tvSearch != null) tvSearch.setTextColor(inactiveColor);
        if (tvPending != null) tvPending.setTextColor(inactiveColor);
        if (tvPayment != null) tvPayment.setTextColor(inactiveColor);
        if (tvAccount != null) tvAccount.setTextColor(inactiveColor);
    }

    private void setTabActive(ImageView iv, TextView tv) {
        int activeColor = Color.parseColor("#38BDF8");
        if (iv != null) iv.setColorFilter(activeColor);
        if (tv != null) tv.setTextColor(activeColor);
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
