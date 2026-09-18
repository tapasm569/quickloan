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

        setClickListener(view, R.id.cardTransferMoney, v -> startActivity(new Intent(getContext(), TransferMoneyActivity.class)));
        setClickListener(view, R.id.cardCreateAccount, v -> showCreateBorrowerDialog());
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
}
