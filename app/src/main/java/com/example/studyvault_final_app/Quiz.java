package com.example.studyvault_final_app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;
import java.util.*;

public class Quiz extends AppCompatActivity {

    private TextView tvTitle, tvQuestionCount, tvQuestion, tvScore;
    private Button btnA, btnB, btnC, btnD;
    private ProgressBar progressBar;

    private final List<Map<String, Object>> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private String setId, setTitle;
    private int requestedCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        setId          = getIntent().getStringExtra("setId");
        setTitle       = getIntent().getStringExtra("setTitle");
        requestedCount = getIntent().getIntExtra("cardCount", -1);

        tvTitle         = findViewById(R.id.tvTitle);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvQuestion      = findViewById(R.id.tvQuestion);
        tvScore         = findViewById(R.id.tvScore);
        progressBar     = findViewById(R.id.progressBar);
        btnA = findViewById(R.id.btnOptionA);
        btnB = findViewById(R.id.btnOptionB);
        btnC = findViewById(R.id.btnOptionC);
        btnD = findViewById(R.id.btnOptionD);

        tvTitle.setText(setTitle);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnA.setOnClickListener(v -> checkAnswer("A"));
        btnB.setOnClickListener(v -> checkAnswer("B"));
        btnC.setOnClickListener(v -> checkAnswer("C"));
        btnD.setOnClickListener(v -> checkAnswer("D"));

        generateQuizFromFlashcards();
    }

    private void generateQuizFromFlashcards() {
        FirebaseHelper.getFlashcards(setId,
                snapshot -> {
                    List<DocumentSnapshot> docs = new ArrayList<>(snapshot.getDocuments());

                    if (docs.size() < 2) {
                        Toast.makeText(this, "Need at least 2 cards to generate a quiz",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Shuffle and limit to requested count
                    Collections.shuffle(docs);
                    int limit = (requestedCount > 0 && requestedCount < docs.size())
                            ? requestedCount : docs.size();
                    docs = docs.subList(0, limit);

                    // Collect all answers for use as distractors
                    List<String> allAnswers = new ArrayList<>();
                    for (DocumentSnapshot d : docs) {
                        String ans = d.getString("answer");
                        if (ans != null) allAnswers.add(ans);
                    }

                    String[] letters = {"A", "B", "C", "D"};
                    List<Map<String, Object>> generated = new ArrayList<>();

                    for (DocumentSnapshot doc : docs) {
                        String question = doc.getString("question");
                        String correct  = doc.getString("answer");
                        if (question == null || correct == null) continue;

                        // Build wrong answers pool
                        List<String> wrong = new ArrayList<>(allAnswers);
                        wrong.remove(correct);
                        Collections.shuffle(wrong);

                        List<String> options = new ArrayList<>();
                        for (int i = 0; i < Math.min(3, wrong.size()); i++) options.add(wrong.get(i));
                        options.add(correct);

                        // Pad to 4 if not enough distractors
                        while (options.size() < 4) options.add("—");

                        Collections.shuffle(options);

                        String correctLetter = "A";
                        for (int i = 0; i < options.size(); i++) {
                            if (options.get(i).equals(correct)) {
                                correctLetter = letters[i];
                                break;
                            }
                        }

                        Map<String, Object> q = new HashMap<>();
                        q.put("question",      question);
                        q.put("optionA",       options.get(0));
                        q.put("optionB",       options.get(1));
                        q.put("optionC",       options.get(2));
                        q.put("optionD",       options.get(3));
                        q.put("correctAnswer", correctLetter);
                        generated.add(q);
                    }

                    questions.addAll(generated);
                    runOnUiThread(() -> {
                        progressBar.setMax(questions.size());
                        showQuestion();
                    });
                },
                e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void showQuestion() {
        Map<String, Object> q = questions.get(currentIndex);
        tvQuestion.setText(safeString(q.get("question")));
        btnA.setText("A.  " + safeString(q.get("optionA")));
        btnB.setText("B.  " + safeString(q.get("optionB")));
        btnC.setText("C.  " + safeString(q.get("optionC")));
        btnD.setText("D.  " + safeString(q.get("optionD")));
        tvQuestionCount.setText("Question " + (currentIndex + 1) + " of " + questions.size());
        tvScore.setText("Score: " + score);
        progressBar.setProgress(currentIndex + 1);
        setButtonsEnabled(true);
        resetButtonColors();
    }

    private void checkAnswer(String chosen) {
        setButtonsEnabled(false);
        String correct = safeString(questions.get(currentIndex).get("correctAnswer"));
        if (chosen.equals(correct)) score++;
        highlightAnswer(chosen, correct);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            currentIndex++;
            if (currentIndex < questions.size()) showQuestion();
            else finishQuiz();
        }, 1200);
    }

    private void highlightAnswer(String chosen, String correct) {
        Button[] btns   = {btnA, btnB, btnC, btnD};
        String[] labels = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            if (labels[i].equals(correct))
                btns[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2ECC71")));
            else if (labels[i].equals(chosen))
                btns[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E74C3C")));
        }
    }

    private void resetButtonColors() {
        ColorStateList purple = ColorStateList.valueOf(Color.parseColor("#7F77DD"));
        btnA.setBackgroundTintList(purple);
        btnB.setBackgroundTintList(purple);
        btnC.setBackgroundTintList(purple);
        btnD.setBackgroundTintList(purple);
    }

    private void setButtonsEnabled(boolean on) {
        btnA.setEnabled(on); btnB.setEnabled(on);
        btnC.setEnabled(on); btnD.setEnabled(on);
    }

    private void finishQuiz() {
        FirebaseHelper.saveQuizResult(setId, score, questions.size(), u -> {}, e -> {});
        Intent intent = new Intent(this, Quiz_Result.class);
        intent.putExtra("score",    score);
        intent.putExtra("total",    questions.size());
        intent.putExtra("setId",    setId);
        intent.putExtra("setTitle", setTitle);
        startActivity(intent);
        finish();
    }

    private String safeString(Object val) {
        return val != null ? val.toString() : "";
    }
}