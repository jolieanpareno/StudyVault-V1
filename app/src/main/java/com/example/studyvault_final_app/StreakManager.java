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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = (user != null) ? user.getUid() : "guest";
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Call this whenever the user studies (e.g. opens a set, flips a card).
     * - Same day → no-op (streak already counted today)
     * - Consecutive day → increment streak
     * - Gap of 2+ days → reset streak to 1
     */
    public void recordStudySession() {
        String today = getTodayDate();
        String lastDate = prefs.getString(userId + "_last_date", "");
        int streak = prefs.getInt(userId + "_streak", 0);

        // Already recorded today — nothing to do
        if (today.equals(lastDate)) return;

        String yesterday = getYesterdayDate();

        if (lastDate.equals(yesterday)) {
            // Studied yesterday → keep the chain going
            streak += 1;
        } else {
            // Skipped at least one day (or very first session) → reset
            streak = 1;
        }

        prefs.edit()
                .putInt(userId + "_streak", streak)
                .putString(userId + "_last_date", today)
                .apply();
    }

    /**
     * Returns the current streak, but first checks whether it has expired
     * (i.e. the user didn't study yesterday or today). This ensures the UI
     * always shows 0 after a missed day even if recordStudySession() wasn't
     * called yet.
     */
    public int getStreak() {
        // Re-resolve userId in case auth state changed
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.userId = (user != null) ? user.getUid() : "guest";

        String lastDate = prefs.getString(userId + "_last_date", "");
        int streak = prefs.getInt(userId + "_streak", 0);

        // If there's no recorded session yet, streak is 0
        if (lastDate.isEmpty()) return 0;

        String today = getTodayDate();
        String yesterday = getYesterdayDate();

        // Streak is still alive if the last session was today or yesterday
        if (lastDate.equals(today) || lastDate.equals(yesterday)) {
            return streak;
        }

        // Missed more than one day — expire the streak
        prefs.edit()
                .putInt(userId + "_streak", 0)
                .apply();
        return 0;
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