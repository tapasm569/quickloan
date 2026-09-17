package com.quickloan.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SupabaseHelper supabase;
    private LoanAdapter adapter;
    private TextView tvTotalLent, tvTotalPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        supabase = new SupabaseHelper();
        tvTotalLent = findViewById(R.id.tvTotalLent);
        tvTotalPending = findViewById(R.id.tvTotalPending);

        RecyclerView rvLoans = findViewById(R.id.rvLoans);
        rvLoans.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LoanAdapter(new ArrayList<>(), new LoanAdapter.OnLoanActionListener() {
            @Override
            public void onTogglePaid(LoanModel loan) {
                int newStatus = (loan.getIsPaid() == 1) ? 0 : 1;
                supabase.updateLoanStatus(loan.getId(), newStatus, new SupabaseHelper.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loadData();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, "Update failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onLongClickDelete(LoanModel loan) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Record")
                        .setMessage("Delete loan entry for " + loan.getCustomerName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            supabase.deleteLoan(loan.getId(), new SupabaseHelper.Callback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    loadData();
                                    Toast.makeText(MainActivity.this, "Loan deleted", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(MainActivity.this, "Delete failed: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        rvLoans.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddLoan);
        fab.setOnClickListener(v -> showAddLoanDialog());

        loadData();
    }

    private void showAddLoanDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_loan, null);
        EditText etName = dialogView.findViewById(R.id.etCustomerName);
        EditText etPhone = dialogView.findViewById(R.id.etCustomerPhone);
        EditText etAmount = dialogView.findViewById(R.id.etLoanAmount);
        EditText etNote = dialogView.findViewById(R.id.etNote);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Save Loan", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    String note = etNote.getText().toString().trim();

                    if (name.isEmpty() || amountStr.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Customer name and amount are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount = Double.parseDouble(amountStr);
                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    supabase.addLoan(name, phone, amount, currentDate, note, new SupabaseHelper.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            loadData();
                            Toast.makeText(MainActivity.this, "Saved to Cloud", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadData() {
        supabase.fetchLoans(new SupabaseHelper.Callback<List<LoanModel>>() {
            @Override
            public void onSuccess(List<LoanModel> loans) {
                adapter.updateData(loans);

                double totalLent = 0;
                double totalPending = 0;

                for (LoanModel item : loans) {
                    totalLent += item.getAmount();
                    if (item.getIsPaid() == 0) {
                        totalPending += item.getAmount();
                    }
                }

                tvTotalLent.setText(String.format(Locale.getDefault(), "₹%.0f", totalLent));
                tvTotalPending.setText(String.format(Locale.getDefault(), "₹%.0f", totalPending));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Failed to load: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
                    }
