package com.quickloan.app;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class CustomerProfileActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private static final String LENDER_PHONE = "9932655607";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private TextView tvAvatar, tvName, tvPhone;
    private TextView tvApprovedLoan, tvPaidBalance;
    private TextView tvUpi, tvVillage, tvPostOffice, tvPoliceStation, tvDistrictPin, tvRefContact;
    private Button btnCall, btnWhatsApp, btnDelete;

    private String customerPhone = "";
    private String currentCustomerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) {
            customerPhone = getIntent().getStringExtra("phone");
        }

        View btnBack = findViewById(R.id.btnBackProfile);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvAvatar = findViewById(R.id.tvProfileAvatar);
        tvName = findViewById(R.id.tvProfileName);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvApprovedLoan = findViewById(R.id.tvProfileApprovedLoan);
        tvPaidBalance = findViewById(R.id.tvProfilePaidBalance);

        tvUpi = findViewById(R.id.tvProfileUpi);
        tvVillage = findViewById(R.id.tvProfileVillage);
        tvPostOffice = findViewById(R.id.tvProfilePostOffice);
        tvPoliceStation = findViewById(R.id.tvProfilePoliceStation);
        tvDistrictPin = findViewById(R.id.tvProfileDistrictPin);
        tvRefContact = findViewById(R.id.tvProfileRefContact);

        btnCall = findViewById(R.id.btnProfileCall);
        btnWhatsApp = findViewById(R.id.btnProfileWhatsApp);
        btnDelete = findViewById(R.id.btnDeleteCustomerProfile);

        if (customerPhone == null || customerPhone.trim().isEmpty()) {
            Toast.makeText(this, "Customer information missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupButtons();
        loadCustomerProfile();
        loadCustomerLoans();
    }

    private double parseDoubleSafe(Object obj) {
        if (obj == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void setupButtons() {
        btnCall.setOnClickListener(v -> {
            if (!customerPhone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customerPhone)));
            }
        });

        btnWhatsApp.setOnClickListener(v -> {
            if (!customerPhone.isEmpty()) {
                String clean = customerPhone.replaceAll("[^0-9]", "");
                if (clean.length() == 10) clean = "91" + clean;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean)));
            }
        });

        btnDelete.setOnClickListener(v -> initiateSecureDeletion());
    }

    private void loadCustomerProfile() {
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + customerPhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                String body = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> list = gson.fromJson(body, type);

                if (list != null && !list.isEmpty()) {
                    Map<String, Object> c = list.get(0);
                    currentCustomerName = c.get("name") != null ? String.valueOf(c.get("name")) : "Borrower";
                    String upi = c.get("upi_id") != null ? String.valueOf(c.get("upi_id")) : "Not Provided";
                    String vill = c.get("village") != null ? String.valueOf(c.get("village")) : "-";
                    String po = c.get("post_office") != null ? String.valueOf(c.get("post_office")) : "-";
                    String ps = c.get("police_station") != null ? String.valueOf(c.get("police_station")) : "-";
                    String dist = c.get("district") != null ? String.valueOf(c.get("district")) : "-";
                    String pin = c.get("pin_code") != null ? String.valueOf(c.get("pin_code")) : "-";
                    String ref = c.get("ref_contact") != null ? String.valueOf(c.get("ref_contact")) : "-";

                    runOnUiThread(() -> {
                        tvName.setText(currentCustomerName);
                        tvPhone.setText("+91 " + customerPhone);
                        if (!currentCustomerName.isEmpty()) {
                            tvAvatar.setText(String.valueOf(currentCustomerName.charAt(0)).toUpperCase());
                        }

                        tvUpi.setText(upi);
                        tvVillage.setText(vill);
                        tvPostOffice.setText(po);
                        tvPoliceStation.setText(ps);
                        tvDistrictPin.setText(dist + (pin.equals("-") ? "" : " - " + pin));
                        tvRefContact.setText(ref);
                    });
                }
            }
        });
    }

    private void loadCustomerLoans() {
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + customerPhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                String body = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, type);
                if (loans == null) loans = new ArrayList<>();

                double totalApproved = 0.0;
                double totalPaid = 0.0;

                for (Map<String, Object> l : loans) {
                    String status = l.get("disbursement_status") != null ? String.valueOf(l.get("disbursement_status")) : "";
                    if ("DISBURSED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
                        double amt = parseDoubleSafe(l.get("amount"));
                        double paid = parseDoubleSafe(l.get("paid_amount"));
                        totalApproved += amt;
                        totalPaid += paid;
                    }
                }

                double finalApproved = totalApproved;
                double finalPaid = totalPaid;

                runOnUiThread(() -> {
                    tvApprovedLoan.setText(String.format(Locale.getDefault(), "₹%.0f", finalApproved));
                    tvPaidBalance.setText(String.format(Locale.getDefault(), "₹%.0f", finalPaid));
                });
            }
        });
    }

    private void initiateSecureDeletion() {
        // Generate random 6-digit verification code
        int randomCode = 100000 + new Random().nextInt(900000);
        String secretOtp = String.valueOf(randomCode);

        // Attempt SMS dispatch to Lender Mobile Number (9932655607)
        String smsMessage = "QuickLoan Security Code: " + secretOtp + " to authorize deletion of customer " + currentCustomerName + " (" + customerPhone + ").";
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(LENDER_PHONE, null, smsMessage, null, null);
        } catch (Exception ignored) {}

        // Prompt Dialog
        EditText etInput = new EditText(this);
        etInput.setHint("Enter 6-digit Secret Code");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etInput.setTextColor(Color.WHITE);
        etInput.setBackgroundResource(R.drawable.edittext_bg);
        etInput.setPadding(35, 35, 35, 35);

        new AlertDialog.Builder(this)
                .setTitle("Lender Authorization Required")
                .setMessage("A 6-digit secret code has been sent to lender mobile: +91 " + LENDER_PHONE + "\n\nSecret Code: " + secretOtp + "\n\nEnter code to permanently delete this customer profile:")
                .setView(etInput)
                .setPositiveButton("CONFIRM DELETE", (dialog, which) -> {
                    String enteredCode = etInput.getText().toString().trim();
                    if (secretOtp.equals(enteredCode)) {
                        executeDeleteCustomer();
                    } else {
                        Toast.makeText(CustomerProfileActivity.this, "Invalid code! Deletion aborted.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void executeDeleteCustomer() {
        // Delete Customer Loans
        Request delLoans = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + customerPhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .delete()
                .build();

        client.newCall(delLoans).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {}
        });

        // Delete Customer Profile
        Request delCustomer = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + customerPhone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .delete()
                .build();

        client.newCall(delCustomer).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(CustomerProfileActivity.this, "Network error deleting profile", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(CustomerProfileActivity.this, "Customer profile deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
