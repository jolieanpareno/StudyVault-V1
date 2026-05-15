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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sets);

        setId    = getIntent().getStringExtra("setId");
        setTitle = getIntent().getStringExtra("setTitle");

        TextView tvTitle    = findViewById(R.id.tvSetTitle);
        TextView tvDesc     = findViewById(R.id.tvSetDescription);
        Button btnFlash     = findViewById(R.id.btnFlashcards);
        Button btnQuiz      = findViewById(R.id.btnQuiz);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnDelete = findViewById(R.id.btnDelete);

        tvTitle.setText(setTitle);

        // Load description + card count from Firestore
        FirebaseFirestore.getInstance()
                .collection("studySets").document(setId).get()
                .addOnSuccessListener(doc -> {
                    String desc = doc.getString("description");
                    tvDesc.setText(desc != null && !desc.isEmpty() ? desc : "No description");
                    Long count = doc.getLong("cardCount");
                    totalCards = count != null ? count.intValue() : 0;
                });

        btnBack.setOnClickListener(v -> finish());

        btnFlash.setOnClickListener(v -> showCountPicker(false));
        btnQuiz.setOnClickListener(v -> showCountPicker(true));

        btnDelete.setOnClickListener(v ->
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Delete Set")
                        .setMessage("Are you sure? This cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> deleteSetCompletely(setId))
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    private void showCountPicker(boolean isQuiz) {
        String type = isQuiz ? "questions" : "cards";
        List<String> options = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        // Only show options that fit within the actual card count
        if (totalCards == 0 || totalCards >= 5)  { options.add("5 " + type);  counts.add(5); }
        if (totalCards == 0 || totalCards >= 10) { options.add("10 " + type); counts.add(10); }
        if (totalCards == 0 || totalCards >= 15) { options.add("15 " + type); counts.add(15); }
        if (totalCards == 0 || totalCards >= 20) { options.add("20 " + type); counts.add(20); }

        // Always add "All" option
        String allLabel = totalCards > 0 ? "All " + totalCards + " " + type : "All " + type;
        options.add(allLabel);
        counts.add(totalCards > 0 ? totalCards : -1);

        new android.app.AlertDialog.Builder(this)
                .setTitle("How many " + type + "?")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    int chosen = counts.get(which);
                    launchMode(isQuiz, chosen);
                })
                .show();
    }

    private void launchMode(boolean isQuiz, int count) {
        Intent intent = new Intent(this, isQuiz ? Quiz.class : Flashcards.class);
        intent.putExtra("setId",     setId);
        intent.putExtra("setTitle",  setTitle);
        intent.putExtra("cardCount", count);
        startActivity(intent);
    }

    /**
     * Deletes flashcards subcollection, then quizQuestions subcollection,
     * then the parent document — in that order to avoid orphaned data.
     */
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