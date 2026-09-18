package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMainActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";

    private DrawerLayout drawerLayout;
    private TextView tvToolbarName;
    private TextView tvHeaderName, tvHeaderPhone;
    private String customerPhone;
    private Map<String, Object> customerData = new HashMap<>();

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) {
            customerPhone = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_PHONE", "");
        }

        drawerLayout = findViewById(R.id.drawer_layout_customer);
        tvToolbarName = findViewById(R.id.tvToolbarCustomerName);

        // Hamburger button listener
        View btnHamburger = findViewById(R.id.btnRightHamburger);
        if (btnHamburger != null && drawerLayout != null) {
            btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        }

        NavigationView navView = findViewById(R.id.nav_view_customer_right);
        if (navView != null) {
            View headerView = navView.getHeaderCount() > 0 ? navView.getHeaderView(0) : navView.inflateHeaderView(R.layout.nav_header_customer);
            if (headerView != null) {
                tvHeaderName = headerView.findViewById(R.id.tvHeaderCustomerName);
                tvHeaderPhone = headerView.findViewById(R.id.tvHeaderCustomerPhone);

                View headerClickTarget = headerView.findViewById(R.id.headerCustomerProfile);
                if (headerClickTarget != null) {
                    headerClickTarget.setOnClickListener(v -> {
                        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.END);
                        showPersonalDetailsDialog();
                    });
                }
            }

            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.END);

                if (id == R.id.nav_update_details) {
                    showUpdateDetailsDialog();
                } else if (id == R.id.nav_reset_password) {
                    showResetPasswordDialog();
                } else if (id == R.id.nav_help) {
                    showHelpDialog();
                } else if (id == R.id.nav_logout) {
                    performLogout();
                }
                return true;
            });
        }

        // Dashboard Card listeners
        setCardListener(R.id.cardApplyLoan, "Open: Apply Loan");
        setCardListener(R.id.cardApprovedDetails, "Open: Approved Loan Details");
        setCardListener(R.id.cardDailyEmi, "Open: Pay Daily EMI");
        setCardListener(R.id.cardCustHistory, "Open: Payment History");
        setCardListener(R.id.cardCustLedger, "Open: Ledger Balance");

        fetchCustomerProfile();
    }

    private void setCardListener(int viewId, String message) {
        View card = findViewById(viewId);
        if (card != null) {
            card.setOnClickListener(v -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
        }
    }

    private void fetchCustomerProfile() {
        if (customerPhone == null || customerPhone.isEmpty()) return;

        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + customerPhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> list = gson.fromJson(body, listType);

                if (list != null && !list.isEmpty()) {
                    customerData = list.get(0);
                    runOnUiThread(() -> {
                        String name = customerData.get("name") != null ? String.valueOf(customerData.get("name")) : "Customer";
                        if (tvToolbarName != null) tvToolbarName.setText(name);
                        if (tvHeaderName != null) tvHeaderName.setText(name);
                        if (tvHeaderPhone != null) tvHeaderPhone.setText("+91 " + customerPhone);
                    });
                }
            }
        });
    }

    private void showPersonalDetailsDialog() {
        if (isFinishing() || isDestroyed()) return;

        String details = "👤 Name: " + (customerData.get("name") != null ? customerData.get("name") : "-") + "\n"
                + "📅 DOB: " + (customerData.get("dob") != null ? customerData.get("dob") : "-") + "\n"
                + "🏡 Village: " + (customerData.get("village") != null ? customerData.get("village") : "-") + "\n"
                + "📮 Post Office: " + (customerData.get("post_office") != null ? customerData.get("post_office") : "-") + "\n"
                + "👮 Police Station: " + (customerData.get("police_station") != null ? customerData.get("police_station") : "-") + "\n"
                + "📍 District: " + (customerData.get("district") != null ? customerData.get("district") : "-") + "\n"
                + "📌 PIN: " + (customerData.get("pin_code") != null ? customerData.get("pin_code") : "-") + "\n"
                + "📞 Reference: " + (customerData.get("reference_phone") != null ? customerData.get("reference_phone") : "-") + "\n"
                + "💳 UPI ID: " + (customerData.get("upi_id") != null ? customerData.get("upi_id") : "Not set");

        new AlertDialog.Builder(this)
                .setTitle("Personal Details")
                .setMessage(details)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showUpdateDetailsDialog() {
        if (isFinishing() || isDestroyed()) return;

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_update_customer_details, null);
        EditText etUpi = v.findViewById(R.id.etUpdateUpi);
        EditText etVill = v.findViewById(R.id.etUpdateVillage);
        EditText etPo = v.findViewById(R.id.etUpdatePO);
        EditText etPs = v.findViewById(R.id.etUpdatePS);
        EditText etDist = v.findViewById(R.id.etUpdateDistrict);
        EditText etPin = v.findViewById(R.id.etUpdatePin);

        if (customerData.get("upi_id") != null) etUpi.setText(String.valueOf(customerData.get("upi_id")));
        if (customerData.get("village") != null) etVill.setText(String.valueOf(customerData.get("village")));
        if (customerData.get("post_office") != null) etPo.setText(String.valueOf(customerData.get("post_office")));
        if (customerData.get("police_station") != null) etPs.setText(String.valueOf(customerData.get("police_station")));
        if (customerData.get("district") != null) etDist.setText(String.valueOf(customerData.get("district")));
        if (customerData.get("pin_code") != null) etPin.setText(String.valueOf(customerData.get("pin_code")));

        new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton("Save Updates", (dialog, which) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("upi_id", etUpi.getText().toString().trim());
                    map.put("village", etVill.getText().toString().trim());
                    map.put("post_office", etPo.getText().toString().trim());
                    map.put("police_station", etPs.getText().toString().trim());
                    map.put("district", etDist.getText().toString().trim());
                    map.put("pin_code", etPin.getText().toString().trim());

                    RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
                    Request req = new Request.Builder()
                            .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + customerPhone)
                            .addHeader("apikey", API_KEY)
                            .addHeader("Authorization", "Bearer " + API_KEY)
                            .patch(body)
                            .build();

                    client.newCall(req).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {}

                        @Override
                        public void onResponse(Call call, Response response) {
                            runOnUiThread(() -> {
                                if (response.isSuccessful()) {
                                    Toast.makeText(CustomerMainActivity.this, "Details updated successfully", Toast.LENGTH_SHORT).show();
                                    fetchCustomerProfile();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResetPasswordDialog() {
        if (isFinishing() || isDestroyed()) return;

        EditText etNewPass = new EditText(this);
        etNewPass.setHint("Enter new password");
        etNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your new password:")
                .setView(etNewPass)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = etNewPass.getText().toString().trim();
                    if (newPass.isEmpty()) return;

                    Map<String, Object> map = new HashMap<>();
                    map.put("password", newPass);

                    RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
                    Request req = new Request.Builder()
                            .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + customerPhone)
                            .addHeader("apikey", API_KEY)
                            .addHeader("Authorization", "Bearer " + API_KEY)
                            .patch(body)
                            .build();

                    client.newCall(req).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {}

                        @Override
                        public void onResponse(Call call, Response response) {
                            runOnUiThread(() -> {
                                if (response.isSuccessful()) {
                                    Toast.makeText(CustomerMainActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHelpDialog() {
        if (isFinishing() || isDestroyed()) return;

        String lenderNo = customerData.get("lender_phone") != null ? 
                String.valueOf(customerData.get("lender_phone")) : "9932655607";

        new AlertDialog.Builder(this)
                .setTitle("Lender Help & Support")
                .setMessage("Contact your lender for questions:\n\n📞 " + lenderNo)
                .setPositiveButton("Call Now", (dialog, which) -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + lenderNo));
                    startActivity(callIntent);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void performLogout() {
        getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
                                                                                }
