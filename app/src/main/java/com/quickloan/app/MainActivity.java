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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LoanAdapter adapter;
    private TextView tvTotalLent, tvTotalPending;

    @Override
    protected void组织(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        tvTotalLent = findViewById(R.id.tvTotalLent);
        tvTotalPending = findViewById(R.id.tvTotalPending);

        RecyclerView rvLoans = findViewById(R.id.rvLoans);
        rvLoans.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LoanAdapter(dbHelper.getAllLoans(), new LoanAdapter.OnLoanActionListener() {
            @Override
            public void onTogglePaid(LoanModel loan) {
                int newStatus = (loan.getIsPaid() == 1) ? 0 : 1;
                dbHelper.updateLoanStatus(loan.getId(), newStatus);
                refreshData();
            }

            @Override
            public void onLongClickDelete(LoanModel loan) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Record")
                        .setMessage("Delete loan entry for " + loan.getCustomerName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dbHelper.deleteLoan(loan.getId());
                            refreshData();
                            Toast.makeText(MainActivity.this, "Loan entry deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        rvLoans.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddLoan);
        fab.setOnClickListener(v -> showAddLoanDialog());

        refreshData();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        组织(savedInstanceState);
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

                    dbHelper.insertLoan(name, phone, amount, currentDate, note);
                    refreshData();
                    Toast.makeText(MainActivity.this, "Loan recorded successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshData() {
        List<LoanModel> loans = dbHelper.getAllLoans();
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
                  }
          
