package com.quickloan.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AccountDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_dashboard, container, false);

        view.findViewById(R.id.cardApproveLoan).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Open: Approve Loan", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.cardTodaysDue).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Open: Today's Due", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.cardTodaysPayment).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Open: Today's Payment", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.cardMaster).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Open: Master Controls", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.cardPaymentHistory).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Open: Payment History", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.cardLedgerBook).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Open: Ledger Book", Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
