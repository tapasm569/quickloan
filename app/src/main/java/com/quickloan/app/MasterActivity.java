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
import androidx.annotation.NonNull;
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

    private EditText etSearch;
    private RecyclerView rv;
    private TextView tvCount, tvEmpty;

    private List<Map<String, Object>> allCustomers = new ArrayList<>();
    private List<Map<String, Object>> filteredCustomers = new ArrayList<>();
    private Map<String, Double> phoneToDueMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        etSearch = findViewById(R.id.etMasterSearch);
        tvCount = findViewById(R.id.tvMasterClientCount);
        tvEmpty = findViewById(R.id.tvEmptyMaster);
        rv = findViewById(R.id.rvMasterClients);
        rv.setLayoutManager(new LinearLayoutManager(this));

        View btnBack = findViewById(R.id.btnBackMaster);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupSearchFilter();

        // 1. INSTANT LOAD: Load previous data from phone storage immediately
        loadFromCache();

        // 2. NETWORK LOAD: Fetch the latest data from internet in the background
        loadActiveLoansSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActiveLoansSummary();
    }

    /**
     * Reads saved JSON data from the phone's internal storage.
     * This runs in 1 millisecond so the screen never looks empty.
     */
    private void loadFromCache() {
        String cachedCustomers = CacheHelper.getCache(this, "CACHE_MASTER_CUSTOMERS");
        if (cachedCustomers != null && !cachedCustomers.isEmpty()) {
            try {
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> list = gson.fromJson(cachedCustomers, type);
                if (list != null && !list.isEmpty()) {
                    allCustomers.clear();
                    filteredCustomers.clear();
                    allCustomers.addAll(list);
                    filteredCustomers.addAll(list);
                    updateRecyclerView();
                }
            } catch (Exception ignored) {}
        }
    }

    private double parseDoubleSafe(Object obj) {
        if (obj == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void setupSearchFilter() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterList(String query) {
        filteredCustomers.clear();
        if (query.isEmpty()) {
            filteredCustomers.addAll(allCustomers);
        } else {
            for (Map<String, Object> c : allCustomers) {
                String name = c.get("name") != null ? String.valueOf(c.get("name")).toLowerCase() : "";
                String phone = c.get("phone") != null ? String.valueOf(c.get("phone")) : "";
                if (name.contains(query) || phone.contains(query)) {
                    filteredCustomers.add(c);
                }
            }
        }
        updateRecyclerView();
    }

    private void updateRecyclerView() {
        if (tvCount != null) {
            tvCount.setText(filteredCustomers.size() + " Borrowers");
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(filteredCustomers.isEmpty() ? View.VISIBLE : View.GONE);
        }

        if (rv.getAdapter() == null) {
            rv.setAdapter(new RecyclerView.Adapter<ClientVH>() {
                @NonNull
                @Override public ClientVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_master_client, parent, false);
                    return new ClientVH(v);
                }

                @Override public void onBindViewHolder(@NonNull ClientVH holder, int position) {
                    Map<String, Object> c = filteredCustomers.get(position);
                    String phone = c.get("phone") != null ? String.valueOf(c.get("phone")) : "";
                    String name = c.get("name") != null ? String.valueOf(c.get("name")) : "Borrower";
                    double dueBalance = phoneToDueMap.getOrDefault(phone, 0.0);

                    holder.tvName.setText(name);
                    holder.tvPhone.setText("+91 " + phone);
                    holder.tvDue.setText(String.format(Locale.getDefault(), "Due: ₹%.0f", dueBalance));

                    holder.itemView.setOnClickListener(v -> openCustomerProfile(phone));
                    if (holder.btnViewProfile != null) {
                        holder.btnViewProfile.setOnClickListener(v -> openCustomerProfile(phone));
                    }
                    if (holder.btnCall != null) {
                        holder.btnCall.setOnClickListener(v -> 
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)))
                        );
                    }
                }

                @Override public int getItemCount() { return filteredCustomers.size(); }
            });
        } else {
            rv.getAdapter().notifyDataSetChanged();
        }
    }

    private void loadActiveLoansSummary() {
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.DISBURSED&is_paid=eq.0")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                loadCustomersFromNetwork();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                phoneToDueMap.clear();
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> loans = gson.fromJson(response.body().string(), type);
                    if (loans != null) {
                        for (Map<String, Object> l : loans) {
                            String phone = String.valueOf(l.get("customer_phone"));
                            double total = parseDoubleSafe(l.get("amount"));
                            double paid = parseDoubleSafe(l.get("paid_amount"));
                            double rem = Math.max(0.0, total - paid);
                            phoneToDueMap.put(phone, phoneToDueMap.getOrDefault(phone, 0.0) + rem);
                        }
                    }
                }
                loadCustomersFromNetwork();
            }
        });
    }

    private void loadCustomersFromNetwork() {
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                // If offline, user can still view the cached data loaded earlier
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                String rawJson = response.body().string();

                // Save fresh response into phone cache for next time
                CacheHelper.saveCache(MasterActivity.this, "CACHE_MASTER_CUSTOMERS", rawJson);

                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> customers = gson.fromJson(rawJson, type);

                runOnUiThread(() -> {
                    allCustomers.clear();
                    filteredCustomers.clear();
                    if (customers != null) {
                        allCustomers.addAll(customers);
                        filteredCustomers.addAll(customers);
                    }
                    updateRecyclerView();
                });
            }
        });
    }

    private void openCustomerProfile(String phone) {
        Intent intent = new Intent(MasterActivity.this, CustomerProfileActivity.class);
        intent.putExtra("CUSTOMER_PHONE", phone);
        intent.putExtra("phone", phone);
        startActivity(intent);
    }

    static class ClientVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvDue;
        Button btnViewProfile, btnCall;

        ClientVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvClientName);
            tvPhone = v.findViewById(R.id.tvClientPhone);
            tvDue = v.findViewById(R.id.tvClientDueBalance);
            btnViewProfile = v.findViewById(R.id.btnViewClientProfile);
            btnCall = v.findViewById(R.id.btnCallClient);
        }
    }
}
