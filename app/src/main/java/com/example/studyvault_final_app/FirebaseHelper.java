package com.example.studyvault_final_app;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    public static void signUp(String email, String password, String name,
                              OnSuccessListener<Void> onSuccess,
                              OnFailureListener onFailure) {
        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    Map<String, Object> user = new HashMap<>();
                    user.put("name",  name);
                    user.put("email", email);
                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(onSuccess)
                            .addOnFailureListener(onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    public static void signIn(String email, String password,
                              OnSuccessListener<Void> onSuccess,
                              OnFailureListener onFailure) {
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }

    public static String getCurrentUserId() {
        // FIX: Replaced 'var' with explicit FirebaseUser type for Java 8 compatibility
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static void getMyStudySets(OnSuccessListener<QuerySnapshot> onSuccess,
                                      OnFailureListener onFailure) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        FirebaseFirestore.getInstance()
                .collection("studySets")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public static void getFlashcards(String setId,
                                     OnSuccessListener<QuerySnapshot> onSuccess,
                                     OnFailureListener onFailure) {
        FirebaseFirestore.getInstance()
                .collection("studySets").document(setId)
                .collection("flashcards")
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public static void saveQuizResult(String setId, int score, int total,
                                      OnSuccessListener<Void> onSuccess,
                                      OnFailureListener onFailure) {
        String uid = getCurrentUserId();
        if (uid == null) return;
        Map<String, Object> result = new HashMap<>();
        result.put("uid",       uid);
        result.put("setId",     setId);
        result.put("score",     score);
        result.put("total",     total);
        result.put("timestamp", System.currentTimeMillis());
        FirebaseFirestore.getInstance()
                .collection("quizResults")
                .add(result)
                .addOnSuccessListener(r -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }
}