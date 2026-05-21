package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.*;
import java.util.*;

public class VaultFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private final List<Map<String, Object>> allSets      = new ArrayList<>();
    private final List<Map<String, Object>> filteredSets = new ArrayList<>();
    private StudySetAdapter adapter;
    private boolean isFirstLoad = true;
    // Set to true by CreateFragment after a successful save so Vault always refreshes
    public static boolean pendingRefresh = false;

    // FIX #2: Keep references to stat TextViews so they can be updated after load
    private TextView tvTotalSets, tvTotalCards, tvStreakStat, tvStreak;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vault, container, false);

        // FIX #1 & #2: Bind all stat views
        tvStreak     = view.findViewById(R.id.tvStreak);
        tvTotalSets  = view.findViewById(R.id.tvTotalSets);
        tvTotalCards = view.findViewById(R.id.tvTotalCards);
        tvStreakStat = view.findViewById(R.id.tvStreakStat);

        // Update streak pill and streak stat card
        StreakManager streakManager = new StreakManager(getContext());
        int streak = streakManager.getStreak();
        updateStreakUI(streak);

        recyclerView = view.findViewById(R.id.recyclerView);
        etSearch     = view.findViewById(R.id.searchBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new StudySetAdapter(filteredSets, set -> {
            Intent intent = new Intent(getActivity(), Sets.class);
            intent.putExtra("setId",    (String) set.get("id"));
            intent.putExtra("setTitle", (String) set.get("title"));
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // FIX #3: Wire up the Add Set button
//        view.findViewById(R.id.btnAddSet).setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Iterm_Study_Set.class);
//            startActivity(intent);
//        });

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSets(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        loadStudySets();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstLoad) { isFirstLoad = false; return; }
        // Always reload if navigated here after a save
        loadStudySets();
        pendingRefresh = false;
    }

    private void loadStudySets() {
        FirebaseHelper.getMyStudySets(
                querySnapshot -> {
                    allSets.clear();
                    int totalCards = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> s = new HashMap<>(doc.getData());
                        s.put("id", doc.getId());
                        allSets.add(s);

                        // FIX #2: Tally card counts if stored per set
                        Object cardCount = doc.get("cardCount");
                        if (cardCount instanceof Number) {
                            totalCards += ((Number) cardCount).intValue();
                        }
                    }

                    // FIX #2: Update the stats header cards
                    int setCount = allSets.size();
                    if (tvTotalSets  != null) tvTotalSets.setText(String.valueOf(setCount));
                    if (tvTotalCards != null) tvTotalCards.setText(String.valueOf(totalCards));

                    filterSets(etSearch != null ? etSearch.getText().toString() : "");
                },
                e -> toast("Error: " + e.getMessage())
        );
    }

    // FIX #1: Correctly color the streak pill — orange for active, gray tint for zero
    private void updateStreakUI(int streak) {
        if (tvStreakStat != null) tvStreakStat.setText(String.valueOf(streak));

        if (tvStreak != null) {
            tvStreak.setText(streak == 1 ? "1 day" : streak + " days");
            // The pill TextView already has textColor="#F4A261" in XML.
            // Only override to gray when streak is 0 so it looks inactive.
            if (streak == 0) {
                tvStreak.setTextColor(android.graphics.Color.parseColor("#5A7A9A"));
            } else {
                // Restore the orange defined in the XML
                tvStreak.setTextColor(android.graphics.Color.parseColor("#F4A261"));
            }
        }
    }

    private void filterSets(String query) {
        filteredSets.clear();
        for (Map<String, Object> set : allSets) {
            String title = (String) set.getOrDefault("title", "");
            if (title.toLowerCase().contains(query.toLowerCase())) filteredSets.add(set);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void toast(String msg) {
        if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}