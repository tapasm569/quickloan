package com.quickloan.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class CustomerMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        Toolbar toolbar = findViewById(R.id.toolbarCustomer);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, android.R.string.ok, android.R.string.cancel);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView nav = findViewById(R.id.nav_view_customer);
        nav.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        findViewById(R.id.cardApplyLoan).setOnClickListener(v -> Toast.makeText(this, "Open: Apply Loan", Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardApprovedDetails).setOnClickListener(v -> Toast.makeText(this, "Open: Approved Details", Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardDailyEmi).setOnClickListener(v -> Toast.makeText(this, "Open: Pay Daily EMI", Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardCustHistory).setOnClickListener(v -> Toast.makeText(this, "Open: Payment History", Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardCustLedger).setOnClickListener(v -> Toast.makeText(this, "Open: Ledger Balance", Toast.LENGTH_SHORT).show());
    }
    }
