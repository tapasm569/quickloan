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
import java.util.List;
import java.util.Map;

public class CustomerApprovedLoansActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private RecyclerView rv;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private String customerPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_loan);

        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        if (customerPhone == null || customerPhone.isEmpty()) {
            customerPhone = getSharedPreferences("QUICK_LOAN_PREFS", MODE_PRIVATE).getString("CUSTOMER_PHONE", "");
        }

        ((TextView) findViewById(android.R.id.text1 != 0 ? android.R.id.text1 : R.id.rvPendingApprovals)).setText("Approved Loan Details");
        rv = findViewById(R.id.rvPendingApprovals);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadCustomerApprovedLoans();
    }

    private void loadCustomerApprovedLoans() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?customer_phone=eq." + customerPhone + "&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> loans = gson.fromJson(body, listType);

                runOnUiThread(() -> rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        Map<String, Object> l = loans.get(position);
                        TextView t1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView t2 = holder.itemView.findViewById(android.R.id.text2);

                        String status = String.valueOf(l.get("disbursement_status"));
                        double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                        double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;

                        if ("WAITING_FOR_PAYMENT".equals(status)) {
                            t1.setText("⏳ Status: Waiting For Payment");
                        } else if ("DISBURSED".equals(status)) {
                            t1.setText("✅ Status: Active Disbursed (EMI: ₹" + (int)emi + "/day)");
                        } else {
                            t1.setText("Loan Status: " + status);
                        }

                        t2.setText("Total Payable: ₹" + (int)total + " | Due Date: " + l.get("due_date"));
                    }

                    @Override public int getItemCount() { return loans != null ? loans.size() : 0; }
                }));
            }
        });
    }
}
