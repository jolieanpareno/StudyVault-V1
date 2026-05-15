package com.example.studyvault_final_app;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class Edit_Profile extends AppCompatActivity {

    // All available animal avatars
    private static final String[] ANIMALS = {
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐸", "🐵", "🐔", "🐧",
            "🐦", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴",
            "🦄", "🐝", "🐛", "🦋", "🐢", "🐍", "🦎", "🦖",
            "🦕", "🐙", "🦑", "🦀", "🐡", "🐬", "🐳", "🦈"
    };

    private String selectedAnimal = "🐶"; // default
    private TextView tvAvatarPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate((Bundle) savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        EditText etName            = findViewById(R.id.etDisplayName);
        TextView tvEmail           = findViewById(R.id.tvEmail);
        tvAvatarPreview            = findViewById(R.id.tvInitial);
        EditText etPassword        = findViewById(R.id.etPassword);
        EditText etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        Button btnSave             = findViewById(R.id.btnSave);
        RecyclerView rvAnimals     = findViewById(R.id.rvAnimals);
        String uid = FirebaseHelper.getCurrentUserId();

        // Set up animal grid picker
        rvAnimals.setLayoutManager(new GridLayoutManager(this, 5));
        AnimalPickerAdapter animalAdapter = new AnimalPickerAdapter(
                Arrays.asList(ANIMALS),
                selectedAnimal,
                animal -> {
                    selectedAnimal = animal;
                    tvAvatarPreview.setText(animal);
                }
        );
        rvAnimals.setAdapter(animalAdapter);

        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name   = doc.getString("name");
                            String avatar = doc.getString("avatar");

                            etName.setText(name);
                            tvEmail.setText(doc.getString("email"));

                            // Load saved avatar or fall back to first letter
                            if (avatar != null && !avatar.isEmpty()) {
                                selectedAnimal = avatar;
                                tvAvatarPreview.setText(avatar);
                                animalAdapter.setSelected(avatar);
                            } else if (name != null && !name.isEmpty()) {
                                tvAvatarPreview.setText(name.substring(0, 1).toUpperCase());
                            }
                        }
                    });

            // Keep avatar preview in sync while typing name (only if no animal chosen yet)
            etName.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Once an animal is selected, the name no longer controls the avatar
                }
                public void afterTextChanged(android.text.Editable s) {}
            });

            btnSave.setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newPassword     = etPassword.getText().toString();
                String confirmPassword = etPasswordConfirm.getText().toString();
                boolean hasPasswordInput = !newPassword.isEmpty() || !confirmPassword.isEmpty();

                if (hasPasswordInput) {
                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                // Save name + chosen animal to Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", newName);
                updates.put("avatar", selectedAnimal);

                FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .update(updates)
                        .addOnSuccessListener(u -> {
                            if (hasPasswordInput) {
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null) {
                                    user.updatePassword(newPassword)
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(this, "Profile and password updated!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this,
                                                        "Profile saved, but password update failed: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                                finish();
                                            });
                                }
                            } else {
                                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}