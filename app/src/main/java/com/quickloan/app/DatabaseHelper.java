package com.quickloan.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "quick_loan.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_LOANS = "loans";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_IS_PAID = "is_paid";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_LOANS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_PHONE + " TEXT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_NOTE + " TEXT, " +
                COLUMN_IS_PAID + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOANS);
        onCreate(db);
    }

    public long insertLoan(String name, String phone, double amount, String date, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NAME, name);
        cv.put(COLUMN_PHONE, phone);
        cv.put(COLUMN_AMOUNT, amount);
        cv.put(COLUMN_DATE, date);
        cv.put(COLUMN_NOTE, note);
        cv.put(COLUMN_IS_PAID, 0);
        return db.insert(TABLE_LOANS, null, cv);
    }

    public void updateLoanStatus(int id, int isPaid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_IS_PAID, isPaid);
        db.update(TABLE_LOANS, cv, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteLoan(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOANS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<LoanModel> getAllLoans() {
        List<LoanModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LOANS + " ORDER BY " + COLUMN_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE));
                int isPaid = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PAID));

                list.add(new LoanModel(id, name, phone, amount, date, note, isPaid));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
                   }
