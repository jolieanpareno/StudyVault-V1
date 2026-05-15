package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class Card_Results extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_results);

        // Record streak when flashcard session is completed
        StreakManager streakManager = new StreakManager(this);
        streakManager.recordStudySession();

        String setId    = getIntent().getStringExtra("setId");
        String setTitle = getIntent().getStringExtra("setTitle");
        int cardCount   = getIntent().getIntExtra("cardCount", 0);

        TextView tvMessage = findViewById(R.id.tvMessage);
        Button btnRestart  = findViewById(R.id.btnRestart);
        Button btnHome     = findViewById(R.id.btnHome);

        tvMessage.setText("You went through all " + cardCount + " cards in\n\"" + setTitle + "\"");

        btnRestart.setOnClickListener(v -> {
            Intent intent = new Intent(this, Flashcards.class);
            intent.putExtra("setId",    setId);
            intent.putExtra("setTitle", setTitle);
            startActivity(intent);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, activity_withnavigation.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }
}