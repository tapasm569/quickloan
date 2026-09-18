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

public class PaymentHistoryActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        ((TextView) findViewById(android.R.id.text1 != 0 ? android.R.id.text1 : R.id.rvMasterCustomers)).setText("Transaction Ledger & History");
        rv = findViewById(R.id.rvMasterCustomers);
        rv.setLayoutManager(new LinearLayoutManager(this));

        String phone = getIntent().getStringExtra("CUSTOMER_PHONE");
        String url = (phone != null && !phone.isEmpty()) ?
                "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?customer_phone=eq." + phone + "&order=id.desc" :
                "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?order=id.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> txs = gson.fromJson(body, type);

                runOnUiThread(() -> rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    @NonNull
                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new RecyclerView.ViewHolder(v) {};
                    }

                    @Override
                    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                        Map<String, Object> item = txs.get(position);
                        TextView t1 = holder.itemView.findViewById(android.R.id.text1);
                        TextView t2 = holder.itemView.findViewById(android.R.id.text2);

                        String type = String.valueOf(item.get("payment_type"));
                        double amt = item.get("amount") != null ? ((Double) item.get("amount")) : 0;
                        String date = String.valueOf(item.get("transaction_date"));
                        String mode = String.valueOf(item.get("payment_mode"));

                        t1.setText((type.equals("DISBURSEMENT") ? "💸 Received Loan: ₹" : "💳 Paid EMI: ₹") + (int)amt);
                        t2.setText("Date: " + date + " | Mode: " + mode + " | Phone: " + item.get("customer_phone"));
                    }

                    @Override public int getItemCount() { return txs != null ? txs.size() : 0; }
                }));
            }
        });
    }
}
