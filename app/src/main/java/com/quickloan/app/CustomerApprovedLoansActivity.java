package com.quickloan.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;
import java.util.Map;

public class CustomerApprovedLoansActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private RecyclerView rv;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        TextView tvTitle = findViewById(R.id.tvListHeaderTitle);
        tvTitle.setText("Approved Loan Details");
        tvEmpty = findViewById(R.id.tvEmptyMessage);
        rv = findViewById(R.id.rvSimpleList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        String customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) {
            customerPhone = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_PHONE", "");
        }

        loadLoans(customerPhone);
    }

    private void loadLoans(String phone) {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + phone + "&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, listType);
                if (loans == null) loans = new ArrayList<>();

                List<Map<String, Object>> finalLoans = loans;
                runOnUiThread(() -> {
                    if (finalLoans.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvEmpty.setVisibility(View.GONE);
                    rv.setAdapter(new RecyclerView.Adapter<RecordVH>() {
                        @NonNull
                        @Override public RecordVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_record, parent, false);
                            return new RecordVH(v);
                        }

                        @Override public void onBindViewHolder(@NonNull RecordVH holder, int position) {
                            Map<String, Object> l = finalLoans.get(position);
                            String status = String.valueOf(l.get("disbursement_status"));
                            double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                            double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;

                            if ("WAITING_FOR_PAYMENT".equalsIgnoreCase(status)) {
                                holder.t1.setText("⏳ Status: Waiting For Payment");
                            } else if ("DISBURSED".equalsIgnoreCase(status)) {
                                holder.t1.setText("✅ Status: Disbursed (Daily EMI: ₹" + (int)emi + ")");
                            } else {
                                holder.t1.setText("Status: " + status);
                            }

                            holder.t2.setText("Total Payable: ₹" + (int)total + " | Due Date: " + l.get("due_date"));
                        }

                        @Override public int getItemCount() { return finalLoans.size(); }
                    });
                });
            }
        });
    }

    static class RecordVH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        RecordVH(@NonNull View itemView) {
            super(itemView);
            t1 = itemView.findViewById(R.id.tvRecordTitle);
            t2 = itemView.findViewById(R.id.tvRecordSubtitle);
        }
    }
}
