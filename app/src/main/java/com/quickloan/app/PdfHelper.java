package com.quickloan.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;

public class PdfHelper {

    /**
     * Checks if the PDF exists in device storage and directly opens it.
     */
    public static void openPdfFromStorage(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0) {
            Toast.makeText(context, "PDF file not found in storage. Please generate it first.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(Intent.createChooser(intent, "Open Ledger PDF"));
        } catch (Exception e) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sends the PDF file directly to WhatsApp.
     */
    public static void sendPdfToWhatsApp(Context context, File pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0) {
            Toast.makeText(context, "PDF not found. Please regenerate.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setPackage("com.whatsapp");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(shareIntent);
        } catch (Exception e) {
            // Fallback to standard app chooser if direct WhatsApp package fails
            try {
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);
                Intent fallback = new Intent(Intent.ACTION_SEND);
                fallback.setType("application/pdf");
                fallback.putExtra(Intent.EXTRA_STREAM, uri);
                fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(fallback, "Share via"));
            } catch (Exception ex) {
                Toast.makeText(context, "Failed to share PDF: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Generates Lender Slate Ledger PDF (Multi-column with metrics).
     */
    public static File generateLenderPdf(Context context, String date, double due, double paid, double disbursed, double remaining, double profit, List<PaymentHistoryActivity.SlateEntry> list) {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        int pageWidth = 595; // A4 standard width
        int pageHeight = 842; // A4 standard height

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Header Background
        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawRect(0, 0, pageWidth, 70, paint);

        // Header Titles
        paint.setColor(Color.WHITE);
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("QUICKLOAN - LENDER SLATE LEDGER", 20, 38, paint);

        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawText("Generated on: " + date, 20, 56, paint);

        // Summary Metric Box
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRoundRect(20, 85, pageWidth - 20, 145, 8, 8, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(String.format(Locale.getDefault(), "TODAY DUE: Rs.%.0f   |   TODAY PAID: Rs.%.0f   |   DISBURSED: Rs.%.0f", due, paid, disbursed), 30, 110, paint);
        canvas.drawText(String.format(Locale.getDefault(), "TOTAL REMAINING: Rs.%.0f   |   PROFIT EARNED: Rs.%.0f", remaining, profit), 30, 130, paint);

        // Table Header
        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawRect(20, 160, pageWidth - 20, 185, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SL", 25, 176, paint);
        canvas.drawText("BORROWER NAME", 55, 176, paint);
        canvas.drawText("DUE TODAY", 230, 176, paint);
        canvas.drawText("TODAY COLL", 320, 176, paint);
        canvas.drawText("TOTAL PAID", 410, 176, paint);
        canvas.drawText("REMAINING", 500, 176, paint);

        // Rows
        int y = 205;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (int i = 0; i < list.size(); i++) {
            PaymentHistoryActivity.SlateEntry item = list.get(i);

            // Row zebra background
            if (i % 2 == 1) {
                paint.setColor(Color.parseColor("#F8FAFC"));
                canvas.drawRect(20, y - 14, pageWidth - 20, y + 6, paint);
            }

            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTextSize(8);
            canvas.drawText(String.valueOf(item.slNo), 25, y, paint);
            
            String n = item.name.length() > 22 ? item.name.substring(0, 22) + ".." : item.name;
            canvas.drawText(n, 55, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs.%.0f", item.todaysDue), 230, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs.%.0f", item.todaysCollection), 320, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs.%.0f", item.totalPaid), 410, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs.%.0f", item.remainingBalance), 500, y, paint);

            y += 20;
            if (y > pageHeight - 40) break; // Avoid overflow on single page
        }

        document.finishPage(page);

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        File file = new File(dir, "Lender_Ledger_" + date.replace("-", "_") + ".pdf");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            fos.close();
            document.close();
            return file;
        } catch (Exception e) {
            document.close();
            return null;
        }
    }

    /**
     * Generates Customer Statement PDF.
     */
    public static File generateCustomerPdf(Context context, String customerPhone, double approved, double paid, double remaining, List<CustomerLedgerActivity.CustLedgerEntry> list) {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Header Background
        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawRect(0, 0, pageWidth, 70, paint);

        // Header Text
        paint.setColor(Color.WHITE);
        paint.setTextSize(18);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("QUICKLOAN - CUSTOMER STATEMENT", 20, 38, paint);

        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawText("Borrower Mobile: +91 " + customerPhone + "  |  Date: " + DateHelper.getTodayDate(), 20, 56, paint);

        // Metrics Summary
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRoundRect(20, 85, pageWidth - 20, 135, 8, 8, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(String.format(Locale.getDefault(), "APPROVED LOAN: Rs.%.0f    |    TOTAL PAID: Rs.%.0f    |    REMAINING: Rs.%.0f", approved, paid, remaining), 30, 115, paint);

        // Table Header
        paint.setColor(Color.parseColor("#1E293B"));
        canvas.drawRect(20, 150, pageWidth - 20, 175, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SL NO", 35, 166, paint);
        canvas.drawText("PAYMENT DATE", 140, 166, paint);
        canvas.drawText("AMOUNT PAID", 290, 166, paint);
        canvas.drawText("REMAINING BALANCE", 430, 166, paint);

        // Table Rows
        int y = 195;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        for (int i = 0; i < list.size(); i++) {
            CustomerLedgerActivity.CustLedgerEntry item = list.get(i);

            if (i % 2 == 1) {
                paint.setColor(Color.parseColor("#F8FAFC"));
                canvas.drawRect(20, y - 14, pageWidth - 20, y + 6, paint);
            }

            paint.setColor(Color.parseColor("#0F172A"));
            paint.setTextSize(9);
            canvas.drawText(String.valueOf(item.slNo), 40, y, paint);
            canvas.drawText(item.date, 140, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs.%.0f", item.paid), 290, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs.%.0f", item.remainingBalance), 430, y, paint);

            y += 22;
            if (y > pageHeight - 40) break;
        }

        document.finishPage(page);

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        File file = new File(dir, "Customer_Statement_" + customerPhone + ".pdf");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            fos.close();
            document.close();
            return file;
        } catch (Exception e) {
            document.close();
            return null;
        }
    }
}
