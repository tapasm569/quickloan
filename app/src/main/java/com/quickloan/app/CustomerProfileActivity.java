package com.quickloan.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
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
    private static final String LENDER_WHATSAPP = "919932655607";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private TextView tvAvatar, tvName, tvPhone;
    private TextView tvApprovedLoan, tvPaidBalance, tvDueBalance;
    private TextView tvUpi, tvVillage, tvPostOffice, tvPoliceStation, tvDistrictPin, tvRefContact;
    private Button btnCall, btnWhatsApp, btnDelete;

    private String customerPhone = "";
    private String currentCustomerName = "";
    private String activeSecretCode = "";

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
        tvDueBalance = findViewById(R.id.tvProfileDueBalance);

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
            Toast.makeText(this, "Customer phone number missing", Toast.LENGTH_SHORT).show();
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

        btnDelete.setOnClickListener(v -> showModernDeleteDialog());
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
                double finalDue = Math.max(0.0, totalApproved - totalPaid);

                runOnUiThread(() -> {
                    tvApprovedLoan.setText(String.format(Locale.getDefault(), "₹%.0f", finalApproved));
                    tvPaidBalance.setText(String.format(Locale.getDefault(), "₹%.0f", finalPaid));
                    tvDueBalance.setText(String.format(Locale.getDefault(), "₹%.0f", finalDue));
                });
            }
        });
    }

    private void showModernDeleteDialog() {
        int code = 100000 + new Random().nextInt(900000);
        activeSecretCode = String.valueOf(code);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_customer, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.90), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogDeleteSubtitle);
        TextView tvNotice = dialogView.findViewById(R.id.tvCodeSentNotice);
        Button btnSendWa = dialogView.findViewById(R.id.btnDialogSendWaCode);
        EditText etCode = dialogView.findViewById(R.id.etDialogSecretCode);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancelDelete);
        Button btnConfirm = dialogView.findViewById(R.id.btnDialogConfirmDelete);

        if (tvSubtitle != null) {
            tvSubtitle.setText("Borrower: " + currentCustomerName + " (+91 " + customerPhone + ")");
        }

        btnSendWa.setOnClickListener(v -> {
            String message = "QuickLoan Security Authorization Code: " + activeSecretCode + "\nAuthorized to delete borrower: " + currentCustomerName + " (" + customerPhone + ").";
            Intent waIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + LENDER_WHATSAPP + "&text=" + Uri.encode(message)));
            startActivity(waIntent);
            if (tvNotice != null) tvNotice.setVisibility(View.VISIBLE);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String enteredCode = etCode.getText().toString().trim();
            if (activeSecretCode.equals(enteredCode)) {
                dialog.dismiss();
                executeDeleteCustomer();
            } else {
                Toast.makeText(CustomerProfileActivity.this, "Incorrect secret code! Verification failed.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void executeDeleteCustomer() {
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
