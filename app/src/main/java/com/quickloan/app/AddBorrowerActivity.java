package com.quickloan.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class AddBorrowerActivity extends AppCompatActivity {

    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InV6aWRvaHV3Y2ViZm9vdnlkeWFrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MDUzNjEsImV4cCI6MjEwNTE4MTM2MX0.2yFWPMXFK_UxTZMuv0J9XIPAPomyxP96MwCo9S2VQYY";
    private final OkHttpClient client = new OkHttpClient();

    private EditText etName, etPhone, etPassword, etVillage, etPostOffice, etPoliceStation, etDist, etAadhar, etPan;
    private ImageView ivPhotoPreview;
    private TextView tvPhotoStatus, tvAadharStatus, tvPanStatus;
    private Button btnSubmit;

    private String base64Photo = "";
    private String base64Aadhar = "";
    private String base64Pan = "";

    private ActivityResultLauncher<Intent> photoPickerLauncher;
    private ActivityResultLauncher<Intent> aadharPickerLauncher;
    private ActivityResultLauncher<Intent> panPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_borrower);

        View btnBack = findViewById(R.id.btnBackAddBorrower);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        etName = findViewById(R.id.etBorrowerName);
        etPhone = findViewById(R.id.etBorrowerPhone);
        etPassword = findViewById(R.id.etBorrowerPassword);
        etVillage = findViewById(R.id.etBorrowerVillage);
        etPostOffice = findViewById(R.id.etBorrowerPostOffice);
        etPoliceStation = findViewById(R.id.etBorrowerPoliceStation);
        etDist = findViewById(R.id.etBorrowerDist);
        etAadhar = findViewById(R.id.etBorrowerAadhar);
        etPan = findViewById(R.id.etBorrowerPan);

        ivPhotoPreview = findViewById(R.id.ivCustomerPhotoPreview);
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus);
        tvAadharStatus = findViewById(R.id.tvAadharStatus);
        tvPanStatus = findViewById(R.id.tvPanStatus);
        btnSubmit = findViewById(R.id.btnSubmitBorrower);

        setupImagePickers();

        findViewById(R.id.btnUploadCustomerPhoto).setOnClickListener(v -> launchFilePicker(photoPickerLauncher));
        findViewById(R.id.btnUploadAadhar).setOnClickListener(v -> launchFilePicker(aadharPickerLauncher));
        findViewById(R.id.btnUploadPan).setOnClickListener(v -> launchFilePicker(panPickerLauncher));

        btnSubmit.setOnClickListener(v -> submitBorrowerProfile());
    }

    private void launchFilePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        launcher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void setupImagePickers() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri uri = result.getData().getData();
                        base64Photo = uriToBase64(uri, 400);
                        if (!base64Photo.isEmpty()) {
                            ivPhotoPreview.setImageURI(uri);
                            tvPhotoStatus.setText("✓ Photo selected");
                            tvPhotoStatus.setTextColor(0xFF10B981);
                        }
                    }
                }
        );

        aadharPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri uri = result.getData().getData();
                        base64Aadhar = uriToBase64(uri, 800);
                        if (!base64Aadhar.isEmpty()) {
                            tvAadharStatus.setText("✓ Aadhaar Card selected");
                            tvAadharStatus.setTextColor(0xFF10B981);
                        }
                    }
                }
        );

        panPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri uri = result.getData().getData();
                        base64Pan = uriToBase64(uri, 800);
                        if (!base64Pan.isEmpty()) {
                            tvPanStatus.setText("✓ PAN Card selected");
                            tvPanStatus.setTextColor(0xFF10B981);
                        }
                    }
                }
        );
    }

    private String uriToBase64(Uri uri, int maxDim) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap == null) return "";

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float ratio = (float) width / (float) height;

            if (width > maxDim || height > maxDim) {
                if (ratio > 1) {
                    width = maxDim;
                    height = (int) (maxDim / ratio);
                } else {
                    height = maxDim;
                    width = (int) (maxDim * ratio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private void submitBorrowerProfile() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String village = etVillage.getText().toString().trim();
        String po = etPostOffice.getText().toString().trim();
        String ps = etPoliceStation.getText().toString().trim();
        String dist = etDist.getText().toString().trim();
        String aadhar = etAadhar.getText().toString().trim();
        String pan = etPan.getText().toString().trim();

        if (name.isEmpty() || phone.length() != 10 || password.isEmpty()) {
            Toast.makeText(this, "Please enter name, 10-digit mobile, and password", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullAddress = String.format(Locale.getDefault(), "Vill/City: %s, P.O: %s, P.S: %s, Dist: %s",
                village.isEmpty() ? "N/A" : village,
                po.isEmpty() ? "N/A" : po,
                ps.isEmpty() ? "N/A" : ps,
                dist.isEmpty() ? "N/A" : dist
        );

        btnSubmit.setEnabled(false);
        btnSubmit.setText("CREATING & ACTIVATING...");

        // All booleans sent as true/false rather than 1/0
        String json = String.format(Locale.US,
                "{" +
                        "\"name\":\"%s\"," +
                        "\"phone\":\"%s\"," +
                        "\"password\":\"%s\"," +
                        "\"village\":\"%s\"," +
                        "\"post_office\":\"%s\"," +
                        "\"police_station\":\"%s\"," +
                        "\"district\":\"%s\"," +
                        "\"address\":\"%s\"," +
                        "\"aadhar_no\":\"%s\"," +
                        "\"aadhar_image\":\"%s\"," +
                        "\"pan_no\":\"%s\"," +
                        "\"pan_image\":\"%s\"," +
                        "\"photo\":\"%s\"," +
                        "\"status\":\"ACTIVE\"," +
                        "\"is_verified\":true," +
                        "\"is_approved\":true," +
                        "\"profile_completed\":true," +
                        "\"is_profile_updated\":true," +
                        "\"role\":\"customer\"" +
                        "}",
                escapeJson(name),
                escapeJson(phone),
                escapeJson(password),
                escapeJson(village),
                escapeJson(po),
                escapeJson(ps),
                escapeJson(dist),
                escapeJson(fullAddress),
                escapeJson(aadhar),
                escapeJson(base64Aadhar),
                escapeJson(pan),
                escapeJson(base64Pan),
                escapeJson(base64Photo)
        );

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request req = new Request.Builder()
                .url("https://uzidohuwcebfoovydyak.supabase.co/rest/v1/customers")
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("CREATE BORROWER PROFILE");
                    Toast.makeText(AddBorrowerActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                final boolean success = response.isSuccessful();
                final String responseBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("CREATE BORROWER PROFILE");
                    if (success) {
                        Toast.makeText(AddBorrowerActivity.this, "Borrower activated! Customer can now login directly.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(AddBorrowerActivity.this, "Error (" + response.code() + "): " + responseBody, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
                        }
                        
