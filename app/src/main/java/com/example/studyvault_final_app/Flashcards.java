package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;

public class Flashcards extends AppCompatActivity {

    private TextView tvTitle, tvCardCount, tvQuestion, tvAnswer, tvTapHint;
    private Button btnPrev, btnNext;
    private ProgressBar progressBar;
    private CardView flashcard;

    private final List<Map<String, Object>> cards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isAnswerShown = false;
    private String setId, setTitle;
    private int requestedCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        setId          = getIntent().getStringExtra("setId");
        setTitle       = getIntent().getStringExtra("setTitle");
        requestedCount = getIntent().getIntExtra("cardCount", -1);

        tvTitle     = findViewById(R.id.tvTitle);
        tvCardCount = findViewById(R.id.tvCardCount);
        tvQuestion  = findViewById(R.id.tvQuestion);
        tvAnswer    = findViewById(R.id.tvAnswer);
        tvTapHint   = findViewById(R.id.tvTapHint);
        btnPrev     = findViewById(R.id.btnPrevious);
        btnNext     = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
        flashcard   = findViewById(R.id.flashcard);

        tvTitle.setText(setTitle);
        tvAnswer.setVisibility(View.GONE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        flashcard.setOnClickListener(v -> {
            isAnswerShown = !isAnswerShown;
            tvAnswer.setVisibility(isAnswerShown ? View.VISIBLE : View.GONE);
            tvTapHint.setVisibility(isAnswerShown ? View.GONE : View.VISIBLE);
        });

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) { currentIndex--; showCard(); }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < cards.size() - 1) {
                currentIndex++;
                showCard();
            } else {
                Intent intent = new Intent(this, Card_Results.class);
                intent.putExtra("setId",     setId);
                intent.putExtra("setTitle",  setTitle);
                intent.putExtra("cardCount", cards.size());
                startActivity(intent);
                finish();
            }
        });

        loadCards();
    }

    private void loadCards() {
        FirebaseHelper.getFlashcards(setId,
                snapshot -> {
                    cards.clear();
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());
                    Collections.shuffle(docs);

                    // Limit to requested count
                    int limit = (requestedCount > 0 && requestedCount < docs.size())
                            ? requestedCount : docs.size();

                    for (int i = 0; i < limit; i++) {
                        if (docs.get(i).getData() != null) cards.add(docs.get(i).getData());
                    }

                    if (!cards.isEmpty()) {
                        progressBar.setMax(cards.size());
                        showCard();
                    } else {
                        Toast.makeText(this, "No cards found", Toast.LENGTH_SHORT).show();
                    }
                },
                e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void showCard() {
        isAnswerShown = false;
        Map<String, Object> card = cards.get(currentIndex);
        tvQuestion.setText(safeString(card.get("question")));
        tvAnswer.setText(safeString(card.get("answer")));
        tvAnswer.setVisibility(View.GONE);
        tvTapHint.setVisibility(View.VISIBLE);
        tvCardCount.setText("Card " + (currentIndex + 1) + " of " + cards.size());
        progressBar.setProgress(currentIndex + 1);
        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setText(currentIndex == cards.size() - 1 ? "Finish" : "Next");
    }

    private String safeString(Object val) {
        return val != null ? val.toString() : "";
    }
}