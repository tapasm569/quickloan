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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchTabFragment extends Fragment {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private List<Map<String, Object>> fullList = new ArrayList<>();
    private RecyclerView rv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        EditText etSearch = view.findViewById(R.id.etSearchQuery);
        rv = view.findViewById(R.id.rvSearchResults);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        loadAllData();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
                @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                    filter(s.toString().toLowerCase(Locale.getDefault()).trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        return view;
    }

    private void loadAllData() {
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
                fullList = res != null ? res : new ArrayList<>();
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> filter(""));
                }
            }
        });
    }

    private void filter(String query) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> item : fullList) {
            String name = item.get("name") != null ? item.get("name").toString().toLowerCase() : "";
            String phone = item.get("phone") != null ? item.get("phone").toString().toLowerCase() : "";
            String id = item.get("id") != null ? item.get("id").toString().toLowerCase() : "";
            if (name.contains(query) || phone.contains(query) || id.contains(query)) {
                filtered.add(item);
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
                    Map<String, Object> l = filtered.get(position);
                    String phone = l.get("phone") != null ? String.valueOf(l.get("phone")) : "";
                    String name = l.get("name") != null ? String.valueOf(l.get("name")) : "Borrower";
                    
                    double amt = 0;
                    if (l.get("amount") != null) {
                        try {
                            amt = Double.parseDouble(String.valueOf(l.get("amount")));
                        } catch (Exception ignored) {}
                    }

                    holder.tvName.setText(name);
                    holder.tvPhone.setText("+91 " + phone);
                    holder.tvAmount.setText(String.format(Locale.getDefault(), "₹%.0f", amt));

                    final String dialPhone = phone;
                    holder.btnCall.setOnClickListener(v -> {
                        if (!dialPhone.isEmpty()) {
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dialPhone)));
                        }
                    });

                    holder.btnWhatsApp.setOnClickListener(v -> {
                        String clean = dialPhone.replaceAll("[^0-9]", "");
                        if (clean.length() == 10) clean = "91" + clean;
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean)));
                    });
                }

                @Override public int getItemCount() { return filtered.size(); }
            });
        }
    }

    static class SearchVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAmount;
        Button btnCall, btnWhatsApp;
        public SearchVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvCustomerName);
            tvPhone = v.findViewById(R.id.tvCustomerPhone);
            tvAmount = v.findViewById(R.id.tvLoanAmount);
            btnCall = v.findViewById(R.id.btnCall);
            btnWhatsApp = v.findViewById(R.id.btnWhatsApp);
        }
    }
}
