package com.example.studyvault_final_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateFragment extends Fragment {

    private EditText etTitle, etDescription, etAiInput, etCardCount;
    private LinearLayout cardContainer;
    private Button btnSave, btnGenerateAi;
    private ProgressBar aiProgressBar;
    private final List<View> cardViews = new ArrayList<>();
    private LayoutInflater myInflater;

    private static final String GROQ_URL      = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL    = "llama-3.1-8b-instant";
    private static final int    DAILY_AI_LIMIT = 10;

    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create, container, false);
        myInflater = inflater;

        etAiInput     = view.findViewById(R.id.etAiInput);
        etCardCount   = view.findViewById(R.id.etCardCount);
        btnGenerateAi = view.findViewById(R.id.btnGenerateAi);
        aiProgressBar = view.findViewById(R.id.aiProgressBar);
        etTitle       = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        cardContainer = view.findViewById(R.id.cardContainer);
        Button btnAddCard = view.findViewById(R.id.btnAddCard);
        btnSave       = view.findViewById(R.id.btnSave);

        addCardView();
        btnAddCard.setOnClickListener(v -> addCardView());
        btnSave.setOnClickListener(v -> saveStudySet());
        btnGenerateAi.setOnClickListener(v -> generateWithAi());

        return view;
    }

    /** Read the typed number from etCardCount; returns -1 if invalid */
    private int getSelectedCardCount() {
        String raw = etCardCount.getText().toString().trim();
        if (raw.isEmpty()) {
            toast("Please enter how many flashcards to generate.");
            return -1;
        }
        int count;
        try {
            count = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            toast("Please enter a valid number.");
            return -1;
        }
        if (count < 1 || count > 50) {
            toast("Please enter a number between 1 and 50.");
            return -1;
        }
        return count;
    }

    private void generateWithAi() {
        String input = etAiInput.getText().toString().trim();
        if (input.isEmpty()) {
            toast("Please paste some notes first!");
            return;
        }

        int cardCount = getSelectedCardCount();
        if (cardCount == -1) return;

        String uid = FirebaseHelper.getCurrentUserId();
        if (uid == null) {
            toast("User not logged in.");
            return;
        }

        DocumentReference userRef = FirebaseFirestore.getInstance()
                .collection("users").document(uid);

        final int finalCardCount = cardCount;
        userRef.get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            String today = todayString();
            long count = 0;
            if (today.equals(doc.getString("lastAiUsageDate"))) {
                Long c = doc.getLong("aiUsageCount");
                count = c != null ? c : 0;
            }
            if (count >= DAILY_AI_LIMIT) {
                toast("Daily AI limit reached (10/10). Try again tomorrow!");
            } else {
                callGroqApi(input, userRef, today, count, finalCardCount);
            }
        }).addOnFailureListener(e -> callGroqApi(input, userRef, todayString(), 0, finalCardCount));
    }

    private void callGroqApi(String notes, DocumentReference userRef,
                             String today, long currentCount, int cardCount) {
        if (!isAdded()) return;
        setAiLoading(true);

        executor.execute(() -> {
            try {
                String prompt =
                        "Create exactly " + cardCount + " flashcards from the notes below.\n" +
                                "Format each line exactly like this: Question | Answer\n" +
                                "Output ONLY the " + cardCount + " lines. No numbers, no extra text, no explanations.\n\n" +
                                "Notes: " + notes;

                JSONObject message = new JSONObject()
                        .put("role", "user")
                        .put("content", prompt);
                JSONArray messages = new JSONArray().put(message);
                JSONObject body = new JSONObject()
                        .put("model", GROQ_MODEL)
                        .put("messages", messages)
                        .put("temperature", 0.4)
                        .put("max_tokens", 1024);

                HttpURLConnection conn = (HttpURLConnection) new URL(GROQ_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream inputStream = (code == 200)
                        ? conn.getInputStream() : conn.getErrorStream();

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String ln;
                    while ((ln = br.readLine()) != null) sb.append(ln);
                }

                if (code != 200) {
                    String errorBody = sb.toString();
                    mainHandler.post(() -> {
                        setAiLoading(false);
                        toast("AI Error " + code + ": " + errorBody);
                    });
                    return;
                }

                String text = new JSONObject(sb.toString())
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    setAiLoading(false);
                    if (parseAiResponse(text)) {
                        Map<String, Object> upd = new HashMap<>();
                        upd.put("aiUsageCount", currentCount + 1);
                        upd.put("lastAiUsageDate", today);
                        userRef.set(upd, SetOptions.merge());
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    setAiLoading(false);
                    toast("AI Error: " + e.getMessage());
                });
            }
        });
    }

    private void setAiLoading(boolean on) {
        btnGenerateAi.setEnabled(!on);
        aiProgressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) toast("AI is generating flashcards...");
    }

    private boolean parseAiResponse(String response) {
        boolean anyAdded = false;
        for (String line : response.split("\n")) {
            String clean = line.trim();
            if (clean.isEmpty() || clean.startsWith("```")) continue;
            if (clean.contains("|")) {
                String[] parts = clean.split("\\|", 2);
                if (parts.length == 2
                        && !parts[0].trim().isEmpty()
                        && !parts[1].trim().isEmpty()) {
                    if (!anyAdded) {
                        cardContainer.removeAllViews();
                        cardViews.clear();
                        anyAdded = true;
                    }
                    addAiCardView(parts[0].trim(), parts[1].trim());
                }
            }
        }
        if (!anyAdded) toast("Could not parse AI response. Try again.");
        return anyAdded;
    }

    private void addAiCardView(String question, String answer) {
        View cardView = myInflater.inflate(R.layout.activity_item_card_input, cardContainer, false);
        ((EditText) cardView.findViewById(R.id.etQuestion)).setText(question);
        ((EditText) cardView.findViewById(R.id.etAnswer)).setText(answer);
        ((TextView) cardView.findViewById(R.id.tvCardNumber)).setText(String.format(Locale.getDefault(), "Card %d", cardViews.size() + 1));
        cardView.findViewById(R.id.btnRemoveCard).setOnClickListener(v -> {
            cardContainer.removeView(cardView);
            cardViews.remove(cardView);
            renumberCards();
        });
        cardContainer.addView(cardView);
        cardViews.add(cardView);
    }

    private void addCardView() {
        View cardView = myInflater.inflate(R.layout.activity_item_card_input, cardContainer, false);
        ((TextView) cardView.findViewById(R.id.tvCardNumber)).setText(String.format(Locale.getDefault(), "Card %d", cardViews.size() + 1));
        cardView.findViewById(R.id.btnRemoveCard).setOnClickListener(v -> {
            cardContainer.removeView(cardView);
            cardViews.remove(cardView);
            renumberCards();
        });
        cardContainer.addView(cardView);
        cardViews.add(cardView);
    }

    private void renumberCards() {
        for (int i = 0; i < cardViews.size(); i++) {
            ((TextView) cardViews.get(i).findViewById(R.id.tvCardNumber))
                    .setText(String.format(Locale.getDefault(), "Card %d", i + 1));
        }
    }

    private void saveStudySet() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) { toast("Please enter a title"); return; }

        List<Map<String, String>> cards = new ArrayList<>();
        for (View v : cardViews) {
            String q = ((EditText) v.findViewById(R.id.etQuestion)).getText().toString().trim();
            String a = ((EditText) v.findViewById(R.id.etAnswer)).getText().toString().trim();
            if (!q.isEmpty() && !a.isEmpty()) {
                Map<String, String> card = new HashMap<>();
                card.put("question", q);
                card.put("answer", a);
                cards.add(card);
            }
        }
        if (cards.isEmpty()) { toast("Add at least one complete card"); return; }

        btnSave.setEnabled(false);
        btnSave.setText(R.string.saving);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("studySets").document();

        Map<String, Object> setData = new HashMap<>();
        setData.put("title",       title);
        setData.put("description", etDescription.getText().toString().trim());
        setData.put("uid",         FirebaseHelper.getCurrentUserId());
        setData.put("cardCount",   cards.size());
        setData.put("createdAt",   System.currentTimeMillis());

        ref.set(setData).addOnSuccessListener(unused -> {
            WriteBatch batch = db.batch();
            for (Map<String, String> card : cards) {
                batch.set(ref.collection("flashcards").document(), card);
            }
            batch.commit()
                    .addOnSuccessListener(u -> {
                        if (!isAdded()) return;
                        toast("Saved to Vault! 🎉");
                        clearScreen();
                        // Navigate to the Vault tab and trigger a refresh
                        if (getActivity() != null) {
                            VaultFragment.pendingRefresh = true;
                            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                                    getActivity().findViewById(R.id.bottomNavigationView);
                            if (nav != null) nav.setSelectedItemId(R.id.Vault);
                        }
                    })
                    .addOnFailureListener(e -> resetSaveButton(e.getMessage()));
        }).addOnFailureListener(e -> resetSaveButton(e.getMessage()));
    }

    private void clearScreen() {
        etTitle.setText("");
        etDescription.setText("");
        etAiInput.setText("");
        etCardCount.setText("");
        cardContainer.removeAllViews();
        cardViews.clear();
        addCardView();
        btnSave.setEnabled(true);
        btnSave.setText(R.string.save_study_set);
    }

    private void resetSaveButton(String error) {
        if (!isAdded()) return;
        btnSave.setEnabled(true);
        btnSave.setText(R.string.save_study_set);
        toast("Error: " + error);
    }

    private void toast(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}