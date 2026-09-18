package com.quickloan.app;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchTabFragment extends Fragment {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private List<Map<String, Object>> fullLoanList = new ArrayList<>();
    private final Map<String, Map<String, Object>> customerProfileMap = new HashMap<>();
    private RecyclerView rv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        EditText etSearch = view.findViewById(R.id.etSearchQuery);
        rv = view.findViewById(R.id.rvSearchResults);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Guarantee high-contrast visible text and automatic capital letters
        if (etSearch != null) {
            etSearch.setTextColor(Color.WHITE);
            etSearch.setHintTextColor(Color.parseColor("#94A3B8"));
            etSearch.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
                @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                    filter(s.toString().toLowerCase(Locale.getDefault()).trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        loadRegisteredCustomersAndLoans();
        return view;
    }

    private void loadRegisteredCustomersAndLoans() {
        // Step 1: Fetch registered customer records to resolve real names & profile data
        Request custReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(custReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                loadLoans();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> customers = gson.fromJson(response.body().string(), type);
                    customerProfileMap.clear();
                    if (customers != null) {
                        for (Map<String, Object> c : customers) {
                            String phone = String.valueOf(c.get("phone"));
                            customerProfileMap.put(phone, c);
                        }
                    }
                }
                loadLoans();
            }
        });
    }

    private void loadLoans() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> res = gson.fromJson(body, type);
                fullLoanList = res != null ? res : new ArrayList<>();
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> filter(""));
                }
            }
        });
    }

    private void filter(String query) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> loan : fullLoanList) {
            String phone = loan.get("phone") != null ? loan.get("phone").toString() : "";
            
            // Resolve real registered customer name
            String resolvedName = loan.get("name") != null ? loan.get("name").toString() : "";
            if (customerProfileMap.containsKey(phone)) {
                Object realNameObj = customerProfileMap.get(phone).get("name");
                if (realNameObj != null && !realNameObj.toString().trim().isEmpty()) {
                    resolvedName = realNameObj.toString();
                }
            }

            String id = loan.get("id") != null ? loan.get("id").toString() : "";
            if (resolvedName.toLowerCase(Locale.getDefault()).contains(query) 
                    || phone.toLowerCase(Locale.getDefault()).contains(query) 
                    || id.toLowerCase(Locale.getDefault()).contains(query)) {
                filtered.add(loan);
            }
        }

        if (isAdded() && getActivity() != null && rv != null) {
            rv.setAdapter(new RecyclerView.Adapter<SearchVH>() {
                @NonNull
                @Override
                public SearchVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loan, parent, false);
                    return new SearchVH(v);
                }

                @Override
                public void onBindViewHolder(@NonNull SearchVH holder, int position) {
                    Map<String, Object> loan = filtered.get(position);
                    String phone = loan.get("phone") != null ? String.valueOf(loan.get("phone")) : "";

                    // Display actual customer name
                    String displayName = loan.get("name") != null ? String.valueOf(loan.get("name")) : "Borrower";
                    if (customerProfileMap.containsKey(phone)) {
                        Object realNameObj = customerProfileMap.get(phone).get("name");
                        if (realNameObj != null && !realNameObj.toString().trim().isEmpty()) {
                            displayName = realNameObj.toString();
                        }
                    }
                    if (displayName.startsWith("Borrower (") && !phone.isEmpty()) {
                        displayName = phone;
                    }

                    double amount = 0;
                    if (loan.get("amount") != null) {
                        try {
                            amount = Double.parseDouble(String.valueOf(loan.get("amount")));
                        } catch (Exception ignored) {}
                    }

                    holder.tvName.setText(displayName);
                    holder.tvPhone.setText("+91 " + phone);
                    holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amount));

                    // Call Action
                    final String dialPhone = phone;
                    holder.btnCall.setOnClickListener(v -> {
                        if (!dialPhone.isEmpty()) {
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dialPhone)));
                        }
                    });

                    // WhatsApp Action
                    holder.btnWhatsApp.setOnClickListener(v -> {
                        String clean = dialPhone.replaceAll("[^0-9]", "");
                        if (clean.length() == 10) clean = "91" + clean;
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean)));
                    });

                    // View Profile Action
                    final String finalName = displayName;
                    holder.btnProfile.setText("👤 Profile");
                    holder.btnProfile.setOnClickListener(v -> showBorrowerProfileDialog(dialPhone, finalName, loan));
                }

                @Override public int getItemCount() { return filtered.size(); }
            });
        }
    }

    private void showBorrowerProfileDialog(String phone, String name, Map<String, Object> loan) {
        if (getContext() == null) return;

        Map<String, Object> c = customerProfileMap.get(phone);

        double total = loan.get("amount") != null ? Double.parseDouble(String.valueOf(loan.get("amount"))) : 0;
        double paid = loan.get("paid_amount") != null ? Double.parseDouble(String.valueOf(loan.get("paid_amount"))) : 0;
        double emi = loan.get("daily_emi") != null ? Double.parseDouble(String.valueOf(loan.get("daily_emi"))) : 0;

        StringBuilder details = new StringBuilder();
        details.append("👤 Name: ").append(name).append("\n")
               .append("📞 Mobile: +91 ").append(phone).append("\n\n");

        if (c != null) {
            details.append("📅 DOB: ").append(c.get("dob") != null ? c.get("dob") : "Not provided").append("\n")
                   .append("🏡 Village: ").append(c.get("village") != null ? c.get("village") : "Not provided").append("\n")
                   .append("📮 Post Office: ").append(c.get("post_office") != null ? c.get("post_office") : "Not provided").append("\n")
                   .append("👮 Police Station: ").append(c.get("police_station") != null ? c.get("police_station") : "Not provided").append("\n")
                   .append("📍 District: ").append(c.get("district") != null ? c.get("district") : "Not provided").append("\n")
                   .append("📌 PIN Code: ").append(c.get("pin_code") != null ? c.get("pin_code") : "Not provided").append("\n")
                   .append("👥 Reference Contact: ").append(c.get("reference_phone") != null ? c.get("reference_phone") : "Not provided").append("\n")
                   .append("💳 UPI ID: ").append(c.get("upi_id") != null ? c.get("upi_id") : "Not set").append("\n\n");
        }

        details.append("💰 Loan Amount: ₹").append((int) total).append("\n")
               .append("💳 Daily EMI: ₹").append((int) emi).append(" / day\n")
               .append("✅ Total Repaid: ₹").append((int) paid).append("\n")
               .append("⚠️ Remaining Balance: ₹").append((int) Math.max(0.0, total - paid));

        new AlertDialog.Builder(requireContext())
                .setTitle("Borrower Profile")
                .setMessage(details.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    static class SearchVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAmount;
        Button btnCall, btnWhatsApp, btnProfile;
        public SearchVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvAmount = itemView.findViewById(R.id.tvLoanAmount);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnWhatsApp = itemView.findViewById(R.id.btnWhatsApp);
            btnProfile = itemView.findViewById(R.id.btnToggleStatus);
        }
    }
}
