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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodaysPaymentActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    private TextView tvTotalCollected, tvEmpty;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todays_payment);

        tvTotalCollected = findViewById(R.id.tvTotalCollectedAmount);
        tvEmpty = findViewById(R.id.tvEmptyPayments);
        rv = findViewById(R.id.rvTodaysPayments);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadTodaysPayments();
    }

    private void loadTodaysPayments() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loan_transactions?transaction_date=eq." + today + "&payment_type=eq.DAILY_EMI&order=id.desc")
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

                double total = 0;
                for (Map<String, Object> item : txs) {
                    double amt = item.get("amount") != null ? ((Double) item.get("amount")) : 0;
                    total += amt;
                }

                double finalTotal = total;
                List<Map<String, Object>> finalList = txs;

                runOnUiThread(() -> {
                    tvTotalCollected.setText(String.format(Locale.getDefault(), "₹%.0f", finalTotal));
                    if (finalList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }

                    rv.setAdapter(new RecyclerView.Adapter<RecordVH>() {
                        @NonNull
                        @Override public RecordVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_record, parent, false);
                            return new RecordVH(v);
                        }

                        @Override public void onBindViewHolder(@NonNull RecordVH holder, int position) {
                            Map<String, Object> item = finalList.get(position);
                            double amt = item.get("amount") != null ? ((Double) item.get("amount")) : 0;
                            String mode = String.valueOf(item.get("payment_mode"));
                            String phone = String.valueOf(item.get("customer_phone"));

                            holder.t1.setText("💰 Collected: ₹" + (int)amt);
                            holder.t2.setText("Borrower: " + phone + " | Mode: " + mode);
                        }

                        @Override public int getItemCount() { return finalList.size(); }
                    });
                });
            }
        });
    }

    static class RecordVH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        RecordVH(@NonNull View v) {
            super(v);
            t1 = v.findViewById(R.id.tvRecordTitle);
            t2 = v.findViewById(R.id.tvRecordSubtitle);
        }
    }
}
