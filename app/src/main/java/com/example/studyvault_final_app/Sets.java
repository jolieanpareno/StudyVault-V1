package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;
import java.util.*;

public class Sets extends AppCompatActivity {

    private String setId, setTitle;
    private int totalCards = 0;
    private int selectedCount = 10; // default

    private TextView tvCardCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        setId    = getIntent().getStringExtra("setId");
        setTitle = getIntent().getStringExtra("setTitle");

        TextView tvTitle      = findViewById(R.id.tvSetTitle);
        TextView tvDesc       = findViewById(R.id.tvSetDescription);
        Button btnFlash       = findViewById(R.id.btnFlashcards);
        Button btnQuiz        = findViewById(R.id.btnQuiz);
        ImageButton btnBack   = findViewById(R.id.btnBack);
        ImageButton btnDelete = findViewById(R.id.btnDelete);
        Button btnMinus       = findViewById(R.id.btnMinus);
        Button btnPlus        = findViewById(R.id.btnPlus);
        Button btnAll         = findViewById(R.id.btnAll);
        tvCardCount           = findViewById(R.id.tvCardCount);

        tvTitle.setText(setTitle);
        updateCountDisplay();

        // Load description + card count from Firestore
        FirebaseFirestore.getInstance()
                .collection("studySets").document(setId).get()
                .addOnSuccessListener(doc -> {
                    String desc = doc.getString("description");
                    tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "No description");
                    Long count = doc.getLong("cardCount");
                    totalCards = count != null ? count.intValue() : 0;
                    // Clamp selectedCount to totalCards if needed
                    if (totalCards > 0 && selectedCount > totalCards) {
                        selectedCount = totalCards;
                        updateCountDisplay();
                    }
                });

        btnBack.setOnClickListener(v -> finish());

        btnMinus.setOnClickListener(v -> {
            if (selectedCount > 1) {
                selectedCount--;
                updateCountDisplay();
            }
        });

        btnPlus.setOnClickListener(v -> {
            int max = totalCards > 0 ? totalCards : 999;
            if (selectedCount < max) {
                selectedCount++;
                updateCountDisplay();
            } else {
                toast("That's all the cards in this set!");
            }
        });

        btnAll.setOnClickListener(v -> {
            if (totalCards > 0) {
                selectedCount = totalCards;
                updateCountDisplay();
            } else {
                toast("Card count not loaded yet, try again.");
            }
        });

        btnFlash.setOnClickListener(v -> launchMode(false));
        btnQuiz.setOnClickListener(v  -> launchMode(true));

        btnDelete.setOnClickListener(v ->
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Delete Set")
                        .setMessage("Are you sure? This cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> deleteSetCompletely(setId))
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    private void updateCountDisplay() {
        tvCardCount.setText(String.valueOf(selectedCount));
    }

    private void launchMode(boolean isQuiz) {
        if (totalCards > 0 && selectedCount > totalCards) {
            toast("You only have " + totalCards + " cards in this set.");
            return;
        }
        Intent intent = new Intent(this, isQuiz ? Quiz.class : Flashcards.class);
        intent.putExtra("setId",     setId);
        intent.putExtra("setTitle",  setTitle);
        intent.putExtra("cardCount", selectedCount);
        startActivity(intent);
    }

    private void deleteSetCompletely(String setId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference setRef = db.collection("studySets").document(setId);

        setRef.collection("flashcards").get().addOnSuccessListener(flashSnap -> {
            WriteBatch batch1 = db.batch();
            for (DocumentSnapshot d : flashSnap.getDocuments()) batch1.delete(d.getReference());

            batch1.commit().addOnSuccessListener(u1 -> {
                setRef.collection("quizQuestions").get().addOnSuccessListener(quizSnap -> {
                    WriteBatch batch2 = db.batch();
                    for (DocumentSnapshot d : quizSnap.getDocuments()) batch2.delete(d.getReference());

                    batch2.commit().addOnSuccessListener(u2 ->
                            setRef.delete().addOnSuccessListener(u3 -> {
                                Toast.makeText(this, "Set deleted!", Toast.LENGTH_SHORT).show();
                                finish();
                            }).addOnFailureListener(e ->
                                    toast("Error deleting set: " + e.getMessage()))
                    ).addOnFailureListener(e -> toast("Error deleting quiz data: " + e.getMessage()));
                });
            }).addOnFailureListener(e -> toast("Error deleting cards: " + e.getMessage()));
        }).addOnFailureListener(e -> toast("Error: " + e.getMessage()));
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}