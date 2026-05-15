package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class Signup extends AppCompatActivity {

    private TextInputEditText gmail, pass, confirmpass, users;
    private Button create;
    private TextView go_to_login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        users = findViewById(R.id.signup_name);
        gmail = findViewById(R.id.Singup_Email);
        pass = findViewById(R.id.Signup_Password);
        confirmpass = findViewById(R.id.Signup_Confirm_Password);
        create = findViewById(R.id.Create_Account);
        go_to_login = findViewById(R.id.Go_To_Login);

        create.setOnClickListener(v -> createAccount());

        go_to_login.setOnClickListener(v -> {
            startActivity(new Intent(Signup.this, Login.class));
            finish();
        });
    }

    private void createAccount() {
        String name = users.getText().toString().trim();
        String email = gmail.getText().toString().trim();
        String password = pass.getText().toString().trim();
        String confirmPassword = confirmpass.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmpass.setError("Passwords do not match");
            return;
        }

        create.setEnabled(false);
        create.setText("Creating account...");

        FirebaseHelper.signUp(email, password, name,
                unused -> {
                    Toast.makeText(Signup.this, "Account created! 🎉", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Signup.this, activity_withnavigation.class));
                    finishAffinity(); // Clear stack
                },
                e -> {
                    create.setEnabled(true);
                    create.setText("Create Account");
                    Toast.makeText(Signup.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}