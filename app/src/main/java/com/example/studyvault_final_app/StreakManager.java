package com.example.studyvault_final_app;
// StreakManager.java

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StreakManager {

    private static final String PREFS_NAME = "StreakPrefs";
    private SharedPreferences prefs;
    private String userId;

    public StreakManager(Context context) {
        // Get the currently logged-in Firebase user's ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = (user != null) ? user.getUid() : "guest";

        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void recordStudySession() {
        String today = getTodayDate();
        // Each key is now unique per user
        String lastDate = prefs.getString(userId + "_last_date", "");
        int streak = prefs.getInt(userId + "_streak", 0);

        if (today.equals(lastDate)) return;

        String yesterday = getYesterdayDate();

        if (lastDate.equals(yesterday)) {
            streak += 1;
        } else {
            streak = 1;
        }

        prefs.edit()
                .putInt(userId + "_streak", streak)
                .putString(userId + "_last_date", today)
                .apply();
    }

    public int getStreak() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = (user != null) ? user.getUid() : "guest";
        return prefs.getInt(userId + "_streak", 0);
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getYesterdayDate() {
        long oneDayMs = 24 * 60 * 60 * 1000L;
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() - oneDayMs));
    }
}