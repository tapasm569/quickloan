package com.quickloan.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.LoanViewHolder> {

    public interface OnLoanActionListener {
        void onTogglePaid(LoanModel loan);
        void onLongClickDelete(LoanModel loan);
    }

    private List<LoanModel> loanList;
    private final OnLoanActionListener listener;

    public LoanAdapter(List<LoanModel> loanList, OnLoanActionListener listener) {
        this.loanList = loanList;
        this.listener = listener;
    }

    public void updateData(List<LoanModel> newList) {
        this.loanList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loan, parent, false);
        return new LoanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
        LoanModel loan = loanList.get(position);
        holder.tvName.setText(loan.getCustomerName());
        holder.tvPhone.setText("Phone: " + loan.getPhone());
        holder.tvAmount.setText(String.format("₹%.2f", loan.getAmount()));
        holder.tvDate.setText("Date: " + loan.getDate());

        if (loan.getIsPaid() == 1) {
            holder.btnStatus.setText("Paid");
            holder.btnStatus.setTextColor(Color.parseColor("#16A34A"));
            holder.btnStatus.setStrokeColorResource(android.R.color.transparent);
        } else {
            holder.btnStatus.setText("Mark Paid");
            holder.btnStatus.setTextColor(Color.parseColor("#DC2626"));
        }

        holder.btnStatus.setOnClickListener(v -> listener.onTogglePaid(loan));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongClickDelete(loan);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return loanList.size();
    }

    static class LoanViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAmount, tvDate;
        MaterialButton btnStatus;

        public LoanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvAmount = itemView.findViewById(R.id.tvLoanAmount);
            tvDate = itemView.findViewById(R.id.tvLoanDate);
            btnStatus = itemView.findViewById(R.id.btnToggleStatus);
        }
    }
}
