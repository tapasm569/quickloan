package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                filter(s.toString().toLowerCase().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
                fullList = gson.fromJson(body, type);
                if (fullList == null) fullList = new ArrayList<>();
                filter("");
            }
        });
    }

    private void filter(String query) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> item : fullList) {
            String name = String.valueOf(item.get("name")).toLowerCase();
            String phone = String.valueOf(item.get("phone")).toLowerCase();
            String id = String.valueOf(item.get("id")).toLowerCase();
            if (name.contains(query) || phone.contains(query) || id.contains(query)) {
                filtered.add(item);
            }
        }

        if (getActivity() != null) {
            requireActivity().runOnUiThread(() -> rv.setAdapter(new RecyclerView.Adapter<SearchVH>() {
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
                    double amt = l.get("amount") != null ? ((Double) l.get("amount")) : 0;

                    holder.tvName.setText(name);
                    holder.tvPhone.setText(phone);
                    holder.tvAmount.setText(String.format("₹%.0f", amt));

                    holder.btnCall.setOnClickListener(v -> 
                        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)))
                    );

                    holder.btnWhatsApp.setOnClickListener(v -> {
                        String clean = phone.replaceAll("[^0-9]", "");
                        if (clean.length() == 10) clean = "91" + clean;
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean)));
                    });
                }

                @Override public int getItemCount() { return filtered.size(); }
            }));
        }
    }

    static class SearchVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAmount;
        View btnCall, btnWhatsApp;
        public SearchVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvAmount = itemView.findViewById(R.id.tvLoanAmount);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnWhatsApp = itemView.findViewById(R.id.btnWhatsApp);
        }
    }
}
