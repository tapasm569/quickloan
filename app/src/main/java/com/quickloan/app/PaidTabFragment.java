package com.quickloan.app;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
public class PaidTabFragment extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView tv = new TextView(getContext());
        tv.setText("Paid Loans Tab (Swipe left/right)");
        tv.setPadding(30, 50, 30, 30);
        return tv;
    }
}
