// Inside onCreate() in CustomerMainActivity.java:
findViewById(R.id.cardApplyLoan).setOnClickListener(v -> {
    Intent intent = new Intent(this, ApplyLoanActivity.class);
    intent.putExtra("CUSTOMER_PHONE", customerPhone);
    startActivity(intent);
});

findViewById(R.id.cardApprovedDetails).setOnClickListener(v -> {
    Intent intent = new Intent(this, CustomerApprovedLoansActivity.class);
    intent.putExtra("CUSTOMER_PHONE", customerPhone);
    startActivity(intent);
});

findViewById(R.id.cardDailyEmi).setOnClickListener(v -> {
    Intent intent = new Intent(this, PayDailyEmiActivity.class);
    intent.putExtra("CUSTOMER_PHONE", customerPhone);
    startActivity(intent);
});

findViewById(R.id.cardCustHistory).setOnClickListener(v -> {
    Intent intent = new Intent(this, PaymentHistoryActivity.class);
    intent.putExtra("CUSTOMER_PHONE", customerPhone);
    startActivity(intent);
});

findViewById(R.id.cardCustLedger).setOnClickListener(v -> {
    Intent intent = new Intent(this, CustomerLedgerActivity.class);
    intent.putExtra("CUSTOMER_PHONE", customerPhone);
    startActivity(intent);
});
