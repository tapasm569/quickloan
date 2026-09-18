package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.gson.Gson;
import okhttp3.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AccountDashboardFragment extends Fragment {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_dashboard, container, false);

        setClickListener(view, R.id.cardTransferMoney, v -> startActivity(new Intent(getContext(), TransferMoneyActivity.class)));
        setClickListener(view, R.id.cardCreateAccount, v -> showCreateBorrowerDialog());
        setClickListener(view, R.id.cardAddOldLoan, v -> showAddOldLoanDialog());
        setClickListener(view, R.id.cardApproveLoan, v -> startActivity(new Intent(getContext(), ApproveLoanActivity.class)));
        setClickListener(view, R.id.cardTodaysDue, v -> startActivity(new Intent(getContext(), TodaysDueActivity.class)));
        setClickListener(view, R.id.cardTodaysPayment, v -> startActivity(new Intent(getContext(), TodaysPaymentActivity.class)));
        setClickListener(view, R.id.cardMaster, v -> startActivity(new Intent(getContext(), MasterActivity.class)));
        setClickListener(view, R.id.cardLedgerBook, v -> startActivity(new Intent(getContext(), PaymentHistoryActivity.class)));

        return view;
    }

    private void setClickListener(View root, int viewId, View.OnClickListener listener) {
        View target = root.findViewById(viewId);
        if (target != null) {
            target.setOnClickListener(listener);
        }
    }

    private void showCreateBorrowerDialog() {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_borrower, null);
        EditText etPhone = dialogView.findViewById(R.id.etBorrowerPhone);
        EditText etPass = dialogView.findViewById(R.id.etBorrowerPassword);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        Button btnCreate = dialogView.findViewById(R.id.btnCreateId);
        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                String rawPhone = etPhone.getText().toString().trim();
                String pass = etPass.getText().toString().trim();

                if (rawPhone.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(getContext(), "Enter phone and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
                if (cleanPhone.length() > 10 && cleanPhone.startsWith("91")) {
                    cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
                }
                final String finalPhone = cleanPhone;

                Map<String, Object> map = new HashMap<>();
                map.put("name", "New Borrower");
                map.put("phone", finalPhone);
                map.put("password", pass);
                map.put("lender_phone", "9932655607");
                map.put("is_profile_completed", 0);

                RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
                Request request = new Request.Builder()
                        .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                        .addHeader("apikey", API_KEY)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show());
                        }
                    }
                    @Override public void onResponse(Call call, Response response) {
                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                if (response.isSuccessful()) {
                                    Toast.makeText(getContext(), "ID created successfully", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(getContext(), "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            });
        }

        Button btnWa = dialogView.findViewById(R.id.btnSendWhatsApp);
        if (btnWa != null) {
            btnWa.setOnClickListener(v -> {
                String rawPhone = etPhone.getText().toString().trim();
                String pass = etPass.getText().toString().trim();
                if (rawPhone.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(getContext(), "Fill phone and password first", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    String cleanPhone = rawPhone.replaceAll("[^0-9]", "");
                    if (cleanPhone.length() == 10) cleanPhone = "91" + cleanPhone;
                    String msg = "Hello, your Quick Loan account has been created!\nMobile: " + rawPhone + "\nPassword: " + pass;
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + URLEncoder.encode(msg, "UTF-8")));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "WhatsApp error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialog.show();
    }

    private void showAddOldLoanDialog() {
        if (!isAdded() || getContext() == null) return;

        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_old_loan, null);
        EditText etName = v.findViewById(R.id.etOldCustName);
        EditText etPhone = v.findViewById(R.id.etOldCustPhone);
        EditText etTotal = v.findViewById(R.id.etOldTotalAmount);
        EditText etPaid = v.findViewById(R.id.etOldPaidAmount);
        EditText etEmi = v.findViewById(R.id.etOldDailyEmi);
        EditText etDue = v.findViewById(R.id.etOldDueDate);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();

        v.findViewById(R.id.btnSaveOldLoan).setOnClickListener(btn -> {
            String name = etName.getText().toString().trim();
            String rawPhone = etPhone.getText().toString().trim();
            String sTotal = etTotal.getText().toString().trim();
            String sPaid = etPaid.getText().toString().trim();
            String sEmi = etEmi.getText().toString().trim();
            String dueDate = etDue.getText().toString().trim();

            if (name.isEmpty() || rawPhone.isEmpty() || sTotal.isEmpty() || sEmi.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = rawPhone.replaceAll("[^0-9]", "");
            if (phone.length() > 10 && phone.startsWith("91")) {
                phone = phone.substring(phone.length() - 10);
            }

            double total = Double.parseDouble(sTotal);
            double paid = sPaid.isEmpty() ? 0.0 : Double.parseDouble(sPaid);
            double emi = Double.parseDouble(sEmi);

            Map<String, Object> custMap = new HashMap<>();
            custMap.put("name", name);
            custMap.put("phone", phone);
            custMap.put("password", "123456");
            custMap.put("is_profile_completed", 1);
            custMap.put("lender_phone", "9932655607");

            String finalPhone = phone;
            RequestBody b1 = RequestBody.create(gson.toJson(custMap), MediaType.get("application/json"));
            Request r1 = new Request.Builder()
                    .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .post(b1)
                    .build();

            client.newCall(r1).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    insertLoanDirectly(name, finalPhone, total, paid, emi, dueDate, dialog);
                }
                @Override public void onResponse(Call call, Response response) {
                    insertLoanDirectly(name, finalPhone, total, paid, emi, dueDate, dialog);
                }
            });
        });

        dialog.show();
    }

    private void insertLoanDirectly(String name, String phone, double total, double paid, double emi, String dueDate, AlertDialog dialog) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> loanMap = new HashMap<>();
        loanMap.put("name", name);
        loanMap.put("phone", phone);
        loanMap.put("customer_phone", phone);
        loanMap.put("lender_phone", "9932655607");
        loanMap.put("principal", total);
        loanMap.put("amount", total);
        loanMap.put("total_amount", total);
        loanMap.put("paid_amount", paid);
        loanMap.put("daily_emi", emi);
        loanMap.put("disbursement_status", "DISBURSED");
        loanMap.put("disbursement_mode", "CASH");
        loanMap.put("is_paid", paid >= total ? 1 : 0);
        loanMap.put("date", today);
        loanMap.put("due_date", dueDate.isEmpty() ? today : dueDate);

        RequestBody b2 = RequestBody.create(gson.toJson(loanMap), MediaType.get("application/json"));
        Request r2 = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(b2)
                .build();

        client.newCall(r2).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(Call call, Response response) {
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Old loan added! Ledger adjusted.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    });
                }
            }
        });
    }
}
