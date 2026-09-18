package com.quickloan.app;

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
import java.util.ArrayList;
import java.util.List;
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
        ((TextView) view.findViewById(R.id.tvListHeaderTitle)).setText("Pending & Due Loans");
        tvEmpty = view.findViewById(R.id.tvEmptyMessage);
        rv = view.findViewById(R.id.rvSimpleList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        loadPending();
        return view;
    }

    private void loadPending() {
        Request request = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans?is_paid=eq.0&order=id.desc")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (getActivity() != null) requireActivity().runOnUiThread(() -> tvEmpty.setVisibility(View.VISIBLE));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> list = gson.fromJson(body, type);
                if (list == null) list = new ArrayList<>();

                List<Map<String, Object>> finalList = list;
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (finalList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvEmpty.setVisibility(View.GONE);
                        rv.setAdapter(new RecyclerView.Adapter<RecordVH>() {
                            @NonNull @Override public RecordVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_record, parent, false);
                                return new RecordVH(v);
                            }

                            @Override public void onBindViewHolder(@NonNull RecordVH holder, int position) {
                                Map<String, Object> l = finalList.get(position);
                                double total = l.get("amount") != null ? ((Double) l.get("amount")) : 0;
                                double paid = l.get("paid_amount") != null ? ((Double) l.get("paid_amount")) : 0;
                                double emi = l.get("daily_emi") != null ? ((Double) l.get("daily_emi")) : 0;

                                holder.t1.setText(l.get("phone") + " | Due: ₹" + (int)(total - paid));
                                holder.t2.setText("Daily EMI: ₹" + (int)emi + " | Due Date: " + l.get("due_date"));
                            }

                            @Override public int getItemCount() { return finalList.size(); }
                        });
                    });
                }
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
