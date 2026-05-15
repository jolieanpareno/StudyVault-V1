package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class activity_withnavigation extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_withnavigation);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            loadFragment(new VaultFragment());
            bottomNav.setSelectedItemId(R.id.Vault);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int itemId = item.getItemId();

            if (itemId == R.id.Vault) {
                selected = new VaultFragment();
            } else if (itemId == R.id.Create) {
                selected = new CreateFragment();
            } else if (itemId == R.id.Profile) {
                selected = new ProfileFragment();
            }

            if (selected != null) {
                loadFragment(selected);
                return true;
            }
            return false;
        });
    }

    // When activity_withnavigation resumes (e.g. after Edit Profile),
    // check login state. But we SKIP this check when coming back from
    // a logout because FLAG_ACTIVITY_CLEAR_TASK will have already
    // destroyed this activity before onResume ever fires.
    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}