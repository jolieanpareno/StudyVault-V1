package com.example.studyvault_final_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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

public class ForgotPassword extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private TextInputEditText emailInput;
    private Button sendResetButton;
    private TextView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailInput      = findViewById(R.id.forgotEmailInput);
        sendResetButton = findViewById(R.id.sendResetButton);
        backButton      = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        sendResetButton.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = emailInput.getText() != null
                ? emailInput.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "If that email is registered, a reset link has been sent.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Failed to send reset email. Try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        sendResetButton.setEnabled(!isLoading);
        sendResetButton.setText(isLoading ? "Sending…" : "Send Reset Link");
    }
}