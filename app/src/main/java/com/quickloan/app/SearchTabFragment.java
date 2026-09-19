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
import androidx.annotation.Nullable;
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

    private EditText etSearch;
    private RecyclerView rv;
    private TextView tvEmpty;

    private List<Map<String, Object>> allCustomers = new ArrayList<>();
    private List<Map<String, Object>> searchResults = new ArrayList<>();
    private Map<String, Double> phoneToDueMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        etSearch = view.findViewById(R.id.etSearchQuery);
        tvEmpty = view.findViewById(R.id.tvEmptySearch);
        rv = view.findViewById(R.id.rvSearchResults);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        setupSearchInput();
        loadLoansAndBorrowers();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLoansAndBorrowers();
    }

    private double parseDoubleSafe(Object obj) {
        if (obj == null) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void setupSearchInput() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        searchResults.clear();
        if (query.isEmpty()) {
            searchResults.addAll(allCustomers);
        } else {
            for (Map<String, Object> c : allCustomers) {
                String name = c.get("name") != null ? String.valueOf(c.get("name")).toLowerCase() : "";
                String phone = c.get("phone") != null ? String.valueOf(c.get("phone")) : "";
                if (name.contains(query) || phone.contains(query)) {
                    searchResults.add(c);
                }
            }
        }

        if (tvEmpty != null) {
            tvEmpty.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (rv != null && rv.getAdapter() != null) {
            rv.getAdapter().notifyDataSetChanged();
        }
    }

    private void loadLoansAndBorrowers() {
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.DISBURSED&is_paid=eq.0")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchCustomers();
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
                fetchCustomers();
            }
        });
    }

    private void fetchCustomers() {
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> list = gson.fromJson(response.body().string(), type);

                allCustomers.clear();
                searchResults.clear();

                if (list != null) {
                    allCustomers.addAll(list);
                    searchResults.addAll(list);
                }

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);
                        }

                        rv.setAdapter(new RecyclerView.Adapter<SearchVH>() {
                            @NonNull
                            @Override public SearchVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_master_client, parent, false);
                                return new SearchVH(v);
                            }

                            @Override public void onBindViewHolder(@NonNull SearchVH holder, int position) {
                                Map<String, Object> c = searchResults.get(position);
                                String phone = c.get("phone") != null ? String.valueOf(c.get("phone")) : "";
                                String name = c.get("name") != null ? String.valueOf(c.get("name")) : "Borrower";
                                double due = phoneToDueMap.getOrDefault(phone, 0.0);

                                holder.tvName.setText(name);
                                holder.tvPhone.setText("+91 " + phone);
                                holder.tvDue.setText(String.format(Locale.getDefault(), "Due: ₹%.0f", due));

                                // Tap borrower card or "View Profile" button to launch CustomerProfileActivity
                                holder.itemView.setOnClickListener(v -> openCustomerProfile(phone));
                                if (holder.btnView != null) {
                                    holder.btnView.setOnClickListener(v -> openCustomerProfile(phone));
                                }

                                if (holder.btnCall != null) {
                                    holder.btnCall.setOnClickListener(v -> 
                                        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)))
                                    );
                                }
                            }

                            @Override public int getItemCount() { return searchResults.size(); }
                        });
                    });
                }
            }
        });
    }

    private void openCustomerProfile(String phone) {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), CustomerProfileActivity.class);
            intent.putExtra("CUSTOMER_PHONE", phone);
            intent.putExtra("phone", phone);
            startActivity(intent);
        }
    }

    static class SearchVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvDue;
        Button btnView, btnCall;

        SearchVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvClientName);
            tvPhone = v.findViewById(R.id.tvClientPhone);
            tvDue = v.findViewById(R.id.tvClientDueBalance);
            btnView = v.findViewById(R.id.btnViewClientProfile);
            btnCall = v.findViewById(R.id.btnCallClient);
        }
    }
}
