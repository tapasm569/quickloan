package com.quickloan.app;

import com.google.gson.annotations.SerializedName;

public class LoanModel {
    private int id;
    
    @SerializedName("name")
    private String customerName;
    
    private String phone;
    private double amount;
    private String date;
    private String note;
    
    @SerializedName("is_paid")
    private int isPaid;

    public LoanModel(int id, String customerName, String phone, double amount, String date, String note, int isPaid) {
        this.id = id;
        this.customerName = customerName;
        this.phone = phone;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.isPaid = isPaid;
    }

    public int getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public double getAmount() { return amount; }
    public String getDate() { return date; }
    public String getNote() { return note; }
    public int getIsPaid() { return isPaid; }
    public void setIsPaid(int isPaid) { this.isPaid = isPaid; }
}
