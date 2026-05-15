package com.example.studyvault_final_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvInitial;
    private TextView tvStatSets, tvStatCards, tvStatStreak;
    private boolean isFirstLoad = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName       = view.findViewById(R.id.tvName);
        tvEmail      = view.findViewById(R.id.tvEmail);
        tvInitial    = view.findViewById(R.id.tvInitial);
        tvStatSets   = view.findViewById(R.id.tvStatSets);
        tvStatCards  = view.findViewById(R.id.tvStatCards);
        tvStatStreak = view.findViewById(R.id.tvStatStreak);

        StreakManager streakManager = new StreakManager(getContext());
        int streak = streakManager.getStreak();
        if (tvStatStreak != null) tvStatStreak.setText(streak + "🔥");

        loadUserData();

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), Edit_Profile.class)));

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logOut());

        return view;
    }

    private void logOut() {
        FirebaseAuth.getInstance().signOut();

        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Intent intent = new Intent(getActivity(), Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            getActivity().startActivity(intent);
            getActivity().finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstLoad) { isFirstLoad = false; return; }
        loadUserData();

        StreakManager streakManager = new StreakManager(getContext());
        int streak = streakManager.getStreak();
        if (tvStatStreak != null) tvStatStreak.setText(streak + "🔥");
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && getView() != null) {
                        String name   = doc.getString("name");
                        String email  = doc.getString("email");
                        String avatar = doc.getString("avatar");

                        tvName.setText(name != null ? name : "User");
                        tvEmail.setText(email != null ? email : "");

                        if (avatar != null && !avatar.isEmpty()) {
                            tvInitial.setText(avatar);
                            tvInitial.setTextSize(38f);
                        } else if (name != null && !name.isEmpty()) {
                            tvInitial.setText(name.substring(0, 1).toUpperCase());
                            tvInitial.setTextSize(38f);
                        }
                    }
                });

        FirebaseHelper.getMyStudySets(
                querySnapshot -> {
                    if (getView() == null) return;
                    int setCount   = querySnapshot.size();
                    int totalCards = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Object cardCount = doc.get("cardCount");
                        if (cardCount instanceof Number) {
                            totalCards += ((Number) cardCount).intValue();
                        }
                    }
                    if (tvStatSets  != null) tvStatSets.setText(String.valueOf(setCount));
                    if (tvStatCards != null) tvStatCards.setText(String.valueOf(totalCards));
                },
                e -> { }
        );
    }
}