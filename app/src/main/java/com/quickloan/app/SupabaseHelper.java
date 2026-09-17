package com.quickloan.app;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupabaseHelper {

    private static final String BASE_URL = "https://uzidohuwcebfoovydyak.supabase.co/rest/v1/loans";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private Request.Builder getBaseRequestBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json");
    }

    // Fetch all loans
    public void fetchLoans(Callback<List<LoanModel>> callback) {
        Request request = getBaseRequestBuilder(BASE_URL + "?order=id.desc").get().build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Error code: " + response.code()));
                    return;
                }
                String body = response.body().string();
                Type listType = new TypeToken<List<LoanModel>>(){}.getType();
                List<LoanModel> list = gson.fromJson(body, listType);
                mainHandler.post(() -> callback.onSuccess(list));
            }
        });
    }

    // Insert new loan
    public void addLoan(String name, String phone, double amount, String date, String note, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("amount", amount);
        data.put("date", date);
        data.put("note", note);
        data.put("is_paid", 0);

        RequestBody body = RequestBody.create(gson.toJson(data), MediaType.get("application/json"));
        Request request = getBaseRequestBuilder(BASE_URL)
                .post(body)
                .addHeader("Prefer", "return=minimal")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to insert: " + response.code()));
                }
            }
        });
    }

    // Update paid status
    public void updateLoanStatus(int id, int isPaid, Callback<Void> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("is_paid", isPaid);

        RequestBody body = RequestBody.create(gson.toJson(data), MediaType.get("application/json"));
        Request request = getBaseRequestBuilder(BASE_URL + "?id=eq." + id)
                .patch(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Failed update: " + response.code()));
                }
            }
        });
    }

    // Delete a loan
    public void deleteLoan(int id, Callback<Void> callback) {
        Request request = getBaseRequestBuilder(BASE_URL + "?id=eq." + id)
                .delete()
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    mainHandler.post(() -> callback.onError("Failed delete: " + response.code()));
                }
            }
        });
    }
  }
