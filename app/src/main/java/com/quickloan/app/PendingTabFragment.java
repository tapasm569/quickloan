package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PendingTabFragment extends Fragment {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private RecyclerView rv;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_simple_list, container, false);

        TextView tvHeader = view.findViewById(R.id.tvListHeaderTitle);
        if (tvHeader != null) {
            tvHeader.setText("Due Payment");
        }

        tvEmpty = view.findViewById(R.id.tvEmptyMessage);
        rv = view.findViewById(R.id.rvSimpleList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        loadDuePayments();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDuePayments();
    }

    private void loadDuePayments() {
        // Fetch real names from customers table
        Request custReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(custReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                fetchDueLoans(new HashMap<>());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Map<String, String> phoneToName = new HashMap<>();
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> customers = gson.fromJson(response.body().string(), type);
                    if (customers != null) {
                        for (Map<String, Object> c : customers) {
                            String p = String.valueOf(c.get("phone"));
                            String n = c.get("name") != null ? String.valueOf(c.get("name")) : "";
                            if (!n.isEmpty()) phoneToName.put(p, n);
                        }
                    }
                }
                fetchDueLoans(phoneToName);
            }
        });
    }

    private void fetchDueLoans(Map<String, String> phoneToName) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Request txReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?transaction_date=eq." + today + "&payment_type=eq.DAILY_EMI")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(txReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Map<Integer, Double> todayPaidMap = new HashMap<>();
                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> txs = gson.fromJson(response.body().string(), type);
                    if (txs != null) {
                        for (Map<String, Object> t : txs) {
                            if (t.get("loan_id") != null && t.get("amount") != null) {
                                int loanId = ((Double) t.get("loan_id")).intValue();
                                double amt = ((Double) t.get("amount"));
                                todayPaidMap.put(loanId, todayPaidMap.getOrDefault(loanId, 0.0) + amt);
                            }
                        }
                    }
                }

                fetchActiveLoans(todayPaidMap, phoneToName);
            }
        });
    }

    private void fetchActiveLoans(Map<Integer, Double> todayPaidMap, Map<String, String> phoneToName) {
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.DISBURSED&is_paid=eq.0&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, type);
                if (loans == null) loans = new ArrayList<>();

                List<Map<String, Object>> dueList = new ArrayList<>();
                for (Map<String, Object> l : loans) {
                    int loanId = ((Double) l.get("id")).intValue();
                    double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;
                    double paidToday = todayPaidMap.getOrDefault(loanId, 0.0);
                    double effectiveDueToday = Math.max(0.0, emi - paidToday);

                    if (effectiveDueToday > 0) {
                        l.put("current_today_due", effectiveDueToday);
                        dueList.add(l);
                    }
                }

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (dueList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("All daily dues for today have been paid!");
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }

                        rv.setAdapter(new RecyclerView.Adapter<DueItemVH>() {
                            @NonNull
                            @Override public DueItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_due_payment, parent, false);
                                return new DueItemVH(v);
                            }

                            @Override public void onBindViewHolder(@NonNull DueItemVH holder, int position) {
                                Map<String, Object> l = dueList.get(position);
                                String phone = String.valueOf(l.get("phone"));

                                // Prioritize real registered customer name
                                String name = phoneToName.containsKey(phone) ? phoneToName.get(phone) : String.valueOf(l.get("name"));
                                if (name == null || name.isEmpty() || name.startsWith("Borrower (")) {
                                    name = phone;
                                }

                                double todaysDue = l.get("current_today_due") != null ? ((Double) l.get("current_today_due")) : 0;
                                double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                                double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                                double remainingBalance = Math.max(0.0, total - paid);

                                holder.tvName.setText(name);
                                holder.tvPhone.setText("+91 " + phone);
                                holder.tvTodayDue.setText(String.format(Locale.getDefault(), "₹%.0f", todaysDue));
                                holder.tvRemaining.setText(String.format(Locale.getDefault(), "₹%.0f", remainingBalance));

                                final String dialPhone = phone;
                                final String finalName = name;
                                holder.btnCall.setOnClickListener(v -> 
                                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dialPhone)))
                                );

                                holder.btnWhatsApp.setOnClickListener(v -> {
                                    String clean = dialPhone.replaceAll("[^0-9]", "");
                                    if (clean.length() == 10) clean = "91" + clean;
                                    String msg = "Hello " + finalName + ", your daily EMI of ₹" + (int)todaysDue + " is due today. Please pay to keep your account current.";
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + clean + "&text=" + Uri.encode(msg))));
                                });
                            }

                            @Override public int getItemCount() { return dueList.size(); }
                        });
                    });
                }
            }
        });
    }

    static class DueItemVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvTodayDue, tvRemaining;
        Button btnCall, btnWhatsApp;

        DueItemVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvDueName);
            tvPhone = v.findViewById(R.id.tvDuePhone);
            tvTodayDue = v.findViewById(R.id.tvDueTodayAmount);
            tvRemaining = v.findViewById(R.id.tvDueRemainingBalance);
            btnCall = v.findViewById(R.id.btnDueCall);
            btnWhatsApp = v.findViewById(R.id.btnDueWhatsApp);
        }
    }
}
