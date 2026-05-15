package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;

public class Quiz_Result extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        // Record streak when quiz is completed
        StreakManager streakManager = new StreakManager(this);
        streakManager.recordStudySession();


        int score       = getIntent().getIntExtra("score", 0);
        int total       = getIntent().getIntExtra("total", 0);
        String setId    = getIntent().getStringExtra("setId");
        String setTitle = getIntent().getStringExtra("setTitle");

        TextView tvScore   = findViewById(R.id.tvScore);
        TextView tvMessage = findViewById(R.id.tvMessage);
        Button btnRetry    = findViewById(R.id.btnRetry);
        Button btnHome     = findViewById(R.id.btnHome);

        tvScore.setText(score + " / " + total);

        double pct = total > 0 ? (double) score / total : 0;
        String msg;
        if (pct == 1.0)      msg = "Perfect score! 🏆";
        else if (pct >= 0.8) msg = "Great job! 🎉";
        else if (pct >= 0.5) msg = "Good effort! Keep studying 📚";
        else                 msg = "Keep practicing, you'll get it! 💪";
        tvMessage.setText(msg);

        btnRetry.setOnClickListener(v -> {
            // Disable button while deleting to prevent double-taps
            btnRetry.setEnabled(false);
            btnRetry.setText("Loading...");

            // FIX: Wait for deletion to complete BEFORE launching Quiz
            // Previously, Quiz could start before deletion finished (race condition),
            // causing it to load old questions instead of regenerating fresh ones.
            FirebaseFirestore.getInstance()
                    .collection("studySets").document(setId)
                    .collection("quizQuestions").get()
                    .addOnSuccessListener(snap -> {
                        WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        for (DocumentSnapshot d : snap.getDocuments())
                            batch.delete(d.getReference());
                        batch.commit().addOnSuccessListener(unused -> {
                            // Only launch Quiz AFTER deletion is confirmed done
                            Intent intent = new Intent(this, Quiz.class);
                            intent.putExtra("setId",    setId);
                            intent.putExtra("setTitle", setTitle);
                            startActivity(intent);
                            finish();
                        }).addOnFailureListener(e -> {
                            btnRetry.setEnabled(true);
                            btnRetry.setText("Try Again");
                            Toast.makeText(this, "Error resetting quiz. Try again.", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        btnRetry.setEnabled(true);
                        btnRetry.setText("Try Again");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, activity_withnavigation.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}