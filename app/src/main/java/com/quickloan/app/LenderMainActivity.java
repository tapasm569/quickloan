package com.quickloan.app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LenderMainActivity extends AppCompatActivity {

    private final String[] tabTitles = new String[]{"Pending", "Paid", "Search", "Account"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lender_main);

        String lenderPhone = getIntent().getStringExtra("LENDER_PHONE");
        if (lenderPhone != null) {
            ((TextView) findViewById(R.id.tvLenderId)).setText("ID: " + lenderPhone);
        }

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

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
                return tabTitles.length;
            }
        });

        // Sync tabs with smooth horizontal swiping
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> 
            tab.setText(tabTitles[position])
        ).attach();
    }
                                           }
