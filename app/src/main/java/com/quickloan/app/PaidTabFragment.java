package com.quickloan.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class PaidTabFragment extends Fragment {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private RecyclerView rv;
    private TextView tvEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_simple_list, container, false);

        // Header Title updated to "Payment Received"
        TextView tvHeader = view.findViewById(R.id.tvListHeaderTitle);
        if (tvHeader != null) {
            tvHeader.setText("Payment Received");
        }

        tvEmpty = view.findViewById(R.id.tvEmptyMessage);
        rv = view.findViewById(R.id.rvSimpleList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        loadTodaysReceivedPayments();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodaysReceivedPayments();
    }

    private void loadTodaysReceivedPayments() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Fetch all loans to map remaining balance by loan_id
        Request loanReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(loanReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                Map<Integer, Double> loanRemainingMap = new HashMap<>();
                Map<Integer, String> loanNameMap = new HashMap<>();

                if (response.isSuccessful() && response.body() != null) {
                    Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                    List<Map<String, Object>> loans = gson.fromJson(response.body().string(), type);
                    if (loans != null) {
                        for (Map<String, Object> l : loans) {
                            int id = ((Double) l.get("id")).intValue();
                            double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                            double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                            loanRemainingMap.put(id, Math.max(0.0, total - paid));
                            if (l.get("name") != null) {
                                loanNameMap.put(id, String.valueOf(l.get("name")));
                            }
                        }
                    }
                }

                // 2. Fetch today's transactions
                fetchTransactions(today, loanRemainingMap, loanNameMap);
            }
        });
    }

    private void fetchTransactions(String today, Map<Integer, Double> loanRemainingMap, Map<Integer, String> loanNameMap) {
        Request txReq = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?transaction_date=eq." + today + "&payment_type=eq.DAILY_EMI&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(txReq).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> txs = gson.fromJson(body, type);
                if (txs == null) txs = new ArrayList<>();

                List<Map<String, Object>> finalTxs = txs;

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (finalTxs.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("No payments received today yet");
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                        }

                        rv.setAdapter(new RecyclerView.Adapter<ReceivedVH>() {
                            @NonNull
                            @Override public ReceivedVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_received, parent, false);
                                return new ReceivedVH(v);
                            }

                            @Override public void onBindViewHolder(@NonNull ReceivedVH holder, int position) {
                                Map<String, Object> item = finalTxs.get(position);
                                int loanId = item.get("loan_id") != null ? ((Double) item.get("loan_id")).intValue() : 0;
                                String phone = item.get("customer_phone") != null ? String.valueOf(item.get("customer_phone")) : "";
                                String name = loanNameMap.containsKey(loanId) ? loanNameMap.get(loanId) : "Borrower";
                                if (name.startsWith("Borrower (")) name = phone;

                                double paidAmt = item.get("amount") != null ? ((Double) item.get("amount")) : 0;
                                double remainingBalance = loanRemainingMap.getOrDefault(loanId, 0.0);
                                String mode = item.get("payment_mode") != null ? String.valueOf(item.get("payment_mode")) : "CASH";

                                holder.tvName.setText(name);
                                holder.tvPhone.setText("+91 " + phone);
                                holder.tvPaid.setText(String.format(Locale.getDefault(), "₹%.0f", paidAmt));
                                holder.tvRemaining.setText(String.format(Locale.getDefault(), "₹%.0f", remainingBalance));

                                // Format Payment Mode Badge
                                if (mode.toUpperCase().contains("UPI")) {
                                    holder.tvMode.setText("UPI");
                                    holder.tvMode.getBackground().setTint(Color.parseColor("#2563EB")); // Blue
                                } else {
                                    holder.tvMode.setText("Cash");
                                    holder.tvMode.getBackground().setTint(Color.parseColor("#10B981")); // Green
                                }
                            }

                            @Override public int getItemCount() { return finalTxs.size(); }
                        });
                    });
                }
            }
        });
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvPaid, tvRemaining, tvMode;

        ReceivedVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvRecBorrowerName);
            tvPhone = v.findViewById(R.id.tvRecBorrowerPhone);
            tvPaid = v.findViewById(R.id.tvRecPaidAmount);
            tvRemaining = v.findViewById(R.id.tvRecRemainingBalance);
            tvMode = v.findViewById(R.id.tvRecPaymentMode);
        }
    }
}
