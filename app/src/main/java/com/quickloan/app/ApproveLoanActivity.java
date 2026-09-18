package com.quickloan.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApproveLoanActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private RecyclerView rv;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_loan);

        rv = findViewById(R.id.rvPendingApprovals);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadPendingLoans();
    }

    private void loadPendingLoans() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?disbursement_status=eq.PENDING_APPROVAL&order=id.desc")
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

                runOnUiThread(() -> rv.setAdapter(new RecyclerView.Adapter<ApprovalVH>() {
                    @NonNull
                    @Override
                    public ApprovalVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_approve_loan, parent, false);
                        return new ApprovalVH(v);
                    }

                    @Override
                    public void onBindViewHolder(@NonNull ApprovalVH holder, int position) {
                        Map<String, Object> item = loans.get(position);
                        int id = ((Double) item.get("id")).intValue();
                        String phone = String.valueOf(item.get("phone"));
                        double amt = item.get("principal") != null ? ((Double) item.get("principal")) : 
                                    (item.get("amount") != null ? ((Double) item.get("amount")) : 0);
                        double emi = item.get("daily_emi") != null ? ((Double) item.get("daily_emi")) : 0;
                        int days = item.get("tenure_days") != null ? ((Double) item.get("tenure_days")).intValue() : 0;

                        holder.tvPhone.setText("Phone: " + phone);
                        holder.tvAmount.setText(String.format("₹%.0f", amt));
                        holder.tvDetails.setText("Tenure: " + days + " Days | Daily EMI: ₹" + (int)emi);

                        holder.btnApprove.setOnClickListener(v -> updateStatus(id, "WAITING_FOR_PAYMENT"));
                        holder.btnReject.setOnClickListener(v -> updateStatus(id, "REJECTED"));
                    }

                    @Override public int getItemCount() { return loans != null ? loans.size() : 0; }
                }));
            }
        });
    }

    private void updateStatus(int id, String newStatus) {
        Map<String, Object> map = new HashMap<>();
        map.put("disbursement_status", newStatus);

        RequestBody body = RequestBody.create(gson.toJson(map), MediaType.get("application/json"));
        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?id=eq." + id)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .patch(body)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(ApproveLoanActivity.this, "Loan Status: " + newStatus, Toast.LENGTH_SHORT).show();
                    loadPendingLoans();
                });
            }
        });
    }

    static class ApprovalVH extends RecyclerView.ViewHolder {
        TextView tvPhone, tvAmount, tvDetails;
        Button btnApprove, btnReject;
        public ApprovalVH(@NonNull View v) {
            super(v);
            tvPhone = v.findViewById(R.id.tvApplicantPhone);
            tvAmount = v.findViewById(R.id.tvApplicantAmount);
            tvDetails = v.findViewById(R.id.tvApplicantDetails);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject = v.findViewById(R.id.btnReject);
        }
    }
}
