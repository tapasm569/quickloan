package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

public class MasterActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private RecyclerView rv;
    private TextView tvCount, tvEmpty;
    private EditText etSearch;

    private List<Map<String, Object>> clientList = new ArrayList<>();
    private final Map<String, Double> phoneDueMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        tvCount = findViewById(R.id.tvTotalClientCount);
        tvEmpty = findViewById(R.id.tvEmptyMaster);
        etSearch = findViewById(R.id.etSearchMaster);
        rv = findViewById(R.id.rvMasterCustomers);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadDueBalancesAndClients();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClients(s.toString().toLowerCase(Locale.getDefault()).trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDueBalancesAndClients() {
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?is_paid=eq.0")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchCustomers();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> loans = gson.fromJson(response.body().string(), type);
                    phoneDueMap.clear();
                    if (loans != null) {
                        for (Map<String, Object> l : loans) {
                            String phone = String.valueOf(l.get("phone"));
                            double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                            double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                            double currentDue = total - paid;
                            phoneDueMap.put(phone, phoneDueMap.getOrDefault(phone, 0.0) + currentDue);
                        }
                    }
                }
                fetchCustomers();
            }
        });
    }

    private void fetchCustomers() {
        Request custReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(custReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                clientList = gson.fromJson(body, type);
                if (clientList == null) clientList = new ArrayList<>();

                runOnUiThread(() -> {
                    tvCount.setText("Total Registered Borrowers: " + clientList.size());
                    filterClients(etSearch.getText().toString().trim());
                });
            }
        });
    }

    private void filterClients(String query) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> c : clientList) {
            String name = c.get("name") != null ? c.get("name").toString().toLowerCase() : "";
            String phone = c.get("phone") != null ? c.get("phone").toString() : "";
            if (name.contains(query) || phone.contains(query)) {
                filtered.add(c);
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }

        rv.setAdapter(new RecyclerView.Adapter<ClientVH>() {
            @NonNull
            @Override public ClientVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_master_client, parent, false);
                return new ClientVH(v);
            }

            @Override public void onBindViewHolder(@NonNull ClientVH holder, int position) {
                Map<String, Object> client = filtered.get(position);
                String name = client.get("name") != null ? client.get("name").toString() : "Customer";
                String phone = client.get("phone") != null ? client.get("phone").toString() : "";
                double due = phoneDueMap.getOrDefault(phone, 0.0);

                holder.tvName.setText(name);
                holder.tvPhone.setText("+91 " + phone);
                holder.tvDue.setText(String.format(Locale.getDefault(), "₹%.0f", due));
                holder.tvAvatar.setText(name.isEmpty() ? "C" : String.valueOf(name.charAt(0)).toUpperCase());

                // Click card -> View full profile
                holder.card.setOnClickListener(v -> showProfileDialog(client, due));

                // Call
                holder.btnCall.setOnClickListener(v -> 
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)))
                );

                // WhatsApp
                holder.btnWa.setOnClickListener(v -> {
                    String clean = phone.replaceAll("[^0-9]", "");
                    if (clean.length() == 10) clean = "91" + clean;
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean)));
                });

                // Delete Button on Card
                holder.btnDelete.setOnClickListener(v -> confirmDeleteCustomer(phone, name));
            }

            @Override public int getItemCount() { return filtered.size(); }
        });
    }

    private void showProfileDialog(Map<String, Object> c, double due) {
        String name = c.get("name") != null ? c.get("name").toString() : "Customer";
        String phone = c.get("phone") != null ? c.get("phone").toString() : "";

        String profileDetails = "👤 Name: " + name + "\n"
                + "📞 Mobile: " + phone + "\n"
                + "📅 DOB: " + (c.get("dob") != null ? c.get("dob") : "-") + "\n"
                + "🏡 Village: " + (c.get("village") != null ? c.get("village") : "-") + "\n"
                + "📮 Post Office: " + (c.get("post_office") != null ? c.get("post_office") : "-") + "\n"
                + "👮 Police Station: " + (c.get("police_station") != null ? c.get("police_station") : "-") + "\n"
                + "📍 District: " + (c.get("district") != null ? c.get("district") : "-") + "\n"
                + "📌 PIN: " + (c.get("pin_code") != null ? c.get("pin_code") : "-") + "\n"
                + "👥 Reference: " + (c.get("reference_phone") != null ? c.get("reference_phone") : "-") + "\n"
                + "💳 UPI ID: " + (c.get("upi_id") != null ? c.get("upi_id") : "Not set") + "\n\n"
                + "⚠️ Outstanding Due Balance: ₹" + (int)due;

        new AlertDialog.Builder(this)
                .setTitle("Borrower Profile Details")
                .setMessage(profileDetails)
                .setPositiveButton("Close", null)
                .setNeutralButton("Delete Client", (dialog, which) -> confirmDeleteCustomer(phone, name))
                .show();
    }

    private void confirmDeleteCustomer(String phone, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Customer Profile?")
                .setMessage("Are you sure you want to permanently delete " + name + " (+91 " + phone + ")?\n\nAll loan applications and transaction history for this customer will also be removed.")
                .setPositiveButton("Delete", (dialog, which) -> executeCustomerDeletion(phone))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeCustomerDeletion(String phone) {
        Toast.makeText(this, "Deleting customer...", Toast.LENGTH_SHORT).show();

        // 1. Delete associated transactions
        Request delTxReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?customer_phone=eq." + phone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .delete()
                .build();

        client.newCall(delTxReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                proceedToDeleteLoans(phone);
            }

            @Override public void onResponse(Call call, Response response) {
                proceedToDeleteLoans(phone);
            }
        });
    }

    private void proceedToDeleteLoans(String phone) {
        // 2. Delete associated loans
        Request delLoansReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?phone=eq." + phone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .delete()
                .build();

        client.newCall(delLoansReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                proceedToDeleteCustomerRecord(phone);
            }

            @Override public void onResponse(Call call, Response response) {
                proceedToDeleteCustomerRecord(phone);
            }
        });
    }

    private void proceedToDeleteCustomerRecord(String phone) {
        // 3. Delete customer account record
        Request delCustomerReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?phone=eq." + phone)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .delete()
                .build();

        client.newCall(delCustomerReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MasterActivity.this, "Failed to delete from database", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(MasterActivity.this, "Customer profile deleted successfully", Toast.LENGTH_SHORT).show();
                        loadDueBalancesAndClients();
                    } else {
                        Toast.makeText(MasterActivity.this, "Error deleting (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    static class ClientVH extends RecyclerView.ViewHolder {
        View card;
        TextView tvAvatar, tvName, tvPhone, tvDue;
        Button btnCall, btnWa, btnDelete;

        ClientVH(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.cardClientItem);
            tvAvatar = v.findViewById(R.id.tvAvatarInitial);
            tvName = v.findViewById(R.id.tvClientName);
            tvPhone = v.findViewById(R.id.tvClientPhone);
            tvDue = v.findViewById(R.id.tvClientDueBalance);
            btnCall = v.findViewById(R.id.btnMasterCall);
            btnWa = v.findViewById(R.id.btnMasterWhatsApp);
            btnDelete = v.findViewById(R.id.btnMasterDelete);
        }
    }
}
