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

public class MasterActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private RecyclerView rv;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        rv = findViewById(R.id.rvMasterCustomers);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadCustomers();
    }

    private void loadCustomers() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers?lender_phone=eq.9932655607&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body().string();
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> customers = gson.fromJson(body, listType);

                runOnUiThread(() -> rv.setAdapter(new RecyclerView.Adapter<CustomerVH>() {
                    @NonNull
                    @Override
                    public CustomerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                        return new CustomerVH(v);
                    }

                    @Override
                    public void onBindViewHolder(@NonNull CustomerVH holder, int position) {
                        Map<String, Object> c = customers.get(position);
                        holder.t1.setText(String.valueOf(c.get("name")));
                        holder.t2.setText("Phone: " + c.get("phone") + " | " + (c.get("village") == null ? "Pending profile" : c.get("village")));
                    }

                    @Override
                    public int getItemCount() { return customers != null ? customers.size() : 0; }
                }));
            }
        });
    }

    static class CustomerVH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        public CustomerVH(@NonNull View itemView) {
            super(itemView);
            t1 = itemView.findViewById(android.R.id.text1);
            t2 = itemView.findViewById(android.R.id.text2);
        }
    }
            }
