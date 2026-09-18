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

public class PaymentHistoryActivity extends AppCompatActivity {

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
        tvTitle.setText("Transaction Ledger & History");
        tvEmpty = findViewById(R.id.tvEmptyMessage);
        rv = findViewById(R.id.rvSimpleList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        String customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");

        String url = (customerPhone != null && !customerPhone.isEmpty()) ?
                "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?customer_phone=eq." + customerPhone + "&order=id.desc" :
                "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?order=id.desc";

        loadHistory(url);
    }

    private void loadHistory(String url) {
        Request request = new Request.Builder()
                .url(url)
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
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> txs = gson.fromJson(body, type);
                if (txs == null) txs = new ArrayList<>();

                List<Map<String, Object>> finalList = txs;
                runOnUiThread(() -> {
                    if (finalList.isEmpty()) {
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
                            Map<String, Object> item = finalList.get(position);
                            String type = String.valueOf(item.get("payment_type"));
                            double amt = item.get("amount") != null ? ((Double) item.get("amount")) : 0;
                            String date = String.valueOf(item.get("transaction_date"));
                            String mode = String.valueOf(item.get("payment_mode"));

                            holder.t1.setText(("DISBURSEMENT".equals(type) ? "💸 Received Loan: ₹" : "💳 Paid EMI: ₹") + (int)amt);
                            holder.t2.setText("Date: " + date + " | Mode: " + mode + " | Phone: " + item.get("customer_phone"));
                        }

                        @Override public int getItemCount() { return finalList.size(); }
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
