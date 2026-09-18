package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashMap;
import java.util.Map;

public class AccountDashboardFragment extends Fragment {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_dashboard, container, false);

        // 1. Transfer Money
        view.findViewById(R.id.cardTransferMoney).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), TransferMoneyActivity.class))
        );

        // 2. Approve Loan
        view.findViewById(R.id.cardApproveLoan).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), ApproveLoanActivity.class))
        );

        // 3. Create Account
        view.findViewById(R.id.cardCreateAccount).setOnClickListener(v -> showCreateBorrowerDialog());

        // 4. Master
        view.findViewById(R.id.cardMaster).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), MasterActivity.class))
        );

        // 5. Payment History & Ledger Book
        view.findViewById(R.id.cardPaymentHistory).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), PaymentHistoryActivity.class))
        );
        view.findViewById(R.id.cardLedgerBook).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), PaymentHistoryActivity.class))
        );

        // 6. Today's Due & Payment
        view.findViewById(R.id.cardTodaysDue).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Viewing Today's Due in Pending Tab", Toast.LENGTH_SHORT).show()
        );
        view.findViewById(R.id.cardTodaysPayment).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Viewing Today's Collections in Payment Tab", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void showCreateBorrowerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_borrower, null);
        EditText etPhone = dialogView.findViewById(R.id.etBorrowerPhone);
        EditText etPass = dialogView.findViewById(R.id.etBorrowerPassword);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        dialogView.findViewById(R.id.btnCreateId).setOnClickListener(v -> {
            String rawPhone = etPhone.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (rawPhone.isEmpty() || pass.isEmpty()) return;

            String tempPhone = rawPhone.replaceAll("[^0-9]", "");
            if (tempPhone.length() > 10 && tempPhone.startsWith("91")) tempPhone = tempPhone.substring(tempPhone.length() - 10);
            final String finalPhone = tempPhone;

            Map<String, Object> map = new HashMap<>();
            map.put("name", "New Borrower");
            map.put("phone", finalPhone);
            map.put("password", pass);
            map.put("lender_phone", "9932655607");
            map.put("is_profile_completed", 0);

            RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
            Request req = new Request.Builder()
                    .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .post(body)
                    .build();

            client.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) {
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "ID created for " + finalPhone, Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });

        dialogView.findViewById(R.id.btnSendWhatsApp).setOnClickListener(v -> {
            String phone = etPhone.getText().toString().replaceAll("[^0-9]", "");
            if (phone.length() == 10) phone = "91" + phone;
            String msg = "Hello, your Quick Loan account has been created!\nMobile: " + etPhone.getText() + "\nPassword: " + etPass.getText();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(msg)));
            startActivity(intent);
        });

        dialog.show();
    }
}
