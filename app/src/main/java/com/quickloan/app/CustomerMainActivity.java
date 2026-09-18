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
    private TextView tvDrawerName, tvDrawerPhone;
    private String customerPhone = "";
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
        tvDrawerName = findViewById(R.id.tvDrawerCustomerName);
        tvDrawerPhone = findViewById(R.id.tvDrawerCustomerPhone);

        // Right Hamburger Button
        View btnHamburger = findViewById(R.id.btnRightHamburger);
        if (btnHamburger != null && drawerLayout != null) {
            btnHamburger.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
        }

        // Profile Card Click inside Drawer
        View btnProfileHeader = findViewById(R.id.btnDrawerProfileHeader);
        if (btnProfileHeader != null) {
            btnProfileHeader.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.END);
                showPersonalDetailsDialog();
            });
        }

        // Drawer Menu Actions
        View menuUpdate = findViewById(R.id.menuUpdateDetails);
        if (menuUpdate != null) {
            menuUpdate.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.END);
                showUpdateDetailsDialog();
            });
        }

        View menuReset = findViewById(R.id.menuResetPassword);
        if (menuReset != null) {
            menuReset.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.END);
                showResetPasswordDialog();
            });
        }

        View menuHelp = findViewById(R.id.menuHelp);
        if (menuHelp != null) {
            menuHelp.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.END);
                showHelpDialog();
            });
        }

        View menuLogout = findViewById(R.id.menuLogout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> performLogout());
        }

        // Dashboard Feature Cards
        View cardApply = findViewById(R.id.cardApplyLoan);
        if (cardApply != null) {
            cardApply.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerMainActivity.this, ApplyLoanActivity.class);
                intent.putExtra("CUSTOMER_PHONE", customerPhone);
                startActivity(intent);
            });
        }

        View cardApproved = findViewById(R.id.cardApprovedDetails);
        if (cardApproved != null) {
            cardApproved.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerMainActivity.this, CustomerApprovedLoansActivity.class);
                intent.putExtra("CUSTOMER_PHONE", customerPhone);
                startActivity(intent);
            });
        }

        View cardDaily = findViewById(R.id.cardDailyEmi);
        if (cardDaily != null) {
            cardDaily.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerMainActivity.this, PayDailyEmiActivity.class);
                intent.putExtra("CUSTOMER_PHONE", customerPhone);
                startActivity(intent);
            });
        }

        View cardHist = findViewById(R.id.cardCustHistory);
        if (cardHist != null) {
            cardHist.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerMainActivity.this, PaymentHistoryActivity.class);
                intent.putExtra("CUSTOMER_PHONE", customerPhone);
                startActivity(intent);
            });
        }

        View cardLedger = findViewById(R.id.cardCustLedger);
        if (cardLedger != null) {
            cardLedger.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerMainActivity.this, CustomerLedgerActivity.class);
                intent.putExtra("CUSTOMER_PHONE", customerPhone);
                startActivity(intent);
            });
        }

        fetchCustomerProfile();
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
                String body = response.body() != null ? response.body().string() : "";
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> list = gson.fromJson(body, listType);

                if (list != null && !list.isEmpty()) {
                    customerData = list.get(0);
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        String name = customerData.get("name") != null ? String.valueOf(customerData.get("name")) : "Customer";
                        if (tvToolbarName != null) tvToolbarName.setText(name);
                        if (tvDrawerName != null) tvDrawerName.setText(name);
                        if (tvDrawerPhone != null) tvDrawerPhone.setText("+91 " + customerPhone);
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
