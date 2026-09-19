<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardBackgroundColor="#1E293B"
    app:cardCornerRadius="20dp"
    app:cardElevation="8dp">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="20dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Review &amp; Edit Loan"
                android:textColor="#FFFFFF"
                android:textSize="18sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvEditDialogBorrowerInfo"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="2dp"
                android:text="Borrower: Customer (+91 0000000000)"
                android:textColor="#94A3B8"
                android:textSize="12sp" />

            <!-- Editable Principal Amount -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:text="LOAN AMOUNT / PRINCIPAL (₹)"
                android:textColor="#38BDF8"
                android:textSize="11sp"
                android:textStyle="bold" />

            <EditText
                android:id="@+id/etEditLoanPrincipal"
                android:layout_width="match_parent"
                android:layout_height="46dp"
                android:layout_marginTop="4dp"
                android:background="@drawable/edittext_bg"
                android:inputType="numberDecimal"
                android:paddingHorizontal="12dp"
                android:textColor="#FFFFFF"
                android:textSize="14sp" />

            <!-- Editable Monthly Interest Rate -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="INTEREST RATE (% PER MONTH)"
                android:textColor="#38BDF8"
                android:textSize="11sp"
                android:textStyle="bold" />

            <EditText
                android:id="@+id/etEditLoanRate"
                android:layout_width="match_parent"
                android:layout_height="46dp"
                android:layout_marginTop="4dp"
                android:background="@drawable/edittext_bg"
                android:inputType="numberDecimal"
                android:paddingHorizontal="12dp"
                android:textColor="#FFFFFF"
                android:textSize="14sp" />

            <!-- Editable Tenure -->
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="TENURE (DAYS)"
                android:textColor="#38BDF8"
                android:textSize="11sp"
                android:textStyle="bold" />

            <EditText
                android:id="@+id/etEditLoanTenure"
                android:layout_width="match_parent"
                android:layout_height="46dp"
                android:layout_marginTop="4dp"
                android:background="@drawable/edittext_bg"
                android:inputType="number"
                android:paddingHorizontal="12dp"
                android:textColor="#FFFFFF"
                android:textSize="14sp" />

            <!-- Live Recalculations Container -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="14dp"
                android:background="#0F172A"
                android:orientation="vertical"
                android:padding="12dp">

                <TextView
                    android:id="@+id/tvEditTotalInterest"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Interest: ₹0"
                    android:textColor="#A855F7"
                    android:textSize="12sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvEditTotalPayable"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Total Repayment: ₹0"
                    android:textColor="#F59E0B"
                    android:textSize="13sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvEditDailyEmi"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Daily EMI: ₹0 / day"
                    android:textColor="#10B981"
                    android:textSize="14sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="18dp"
                android:gravity="end"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnCancelLoanEdit"
                    android:layout_width="wrap_content"
                    android:layout_height="42dp"
                    android:backgroundTint="#334155"
                    android:text="Cancel"
                    android:textColor="#CBD5E1"
                    android:textSize="13sp" />

                <Button
                    android:id="@+id/btnConfirmApproveLoan"
                    android:layout_width="wrap_content"
                    android:layout_height="42dp"
                    android:layout_marginStart="10dp"
                    android:backgroundTint="#10B981"
                    android:text="Approve Loan"
                    android:textColor="#FFFFFF"
                    android:textSize="13sp"
                    android:textStyle="bold" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>
</androidx.cardview.widget.CardView>
