package com.quickloan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LenderMainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ViewPager2 viewPager;

    private final int[] tabIcons = new int[]{
            R.drawable.ic_tab_pending,
            R.drawable.ic_tab_payment,
            R.drawable.ic_tab_search,
            R.drawable.ic_tab_account
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lender_main);

        drawerLayout = findViewById(R.id.lender_drawer_layout);

        // Click right hamburger icon -> open right-hand drawer
        findViewById(R.id.btnLenderRightHamburger).setOnClickListener(v -> 
            drawerLayout.openDrawer(GravityCompat.END)
        );

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return new PendingTabFragment();
                    case 1: return new PaidTabFragment();
                    case 2: return new SearchTabFragment();
                    case 3:
                    default: return new AccountDashboardFragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabIcons.length;
            }
        });

        // Set up tab logos
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> 
            tab.setIcon(tabIcons[position])
        ).attach();

        // Right Drawer Menu actions
        NavigationView navView = findViewById(R.id.nav_view_lender);
        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.END);

            if (id == R.id.nav_lender_home) {
                viewPager.setCurrentItem(0, true);
            } else if (id == R.id.nav_lender_contact) {
                showContactDialog();
            } else if (id == R.id.nav_lender_logout) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            return true;
        });
    }

    private void showContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Support & Contact")
                .setMessage("Helpline: +91 9932655607\nEmail: support@quickloan.com")
                .setPositiveButton("Call Now", (dialog, which) -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:9932655607"));
                    startActivity(callIntent);
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
