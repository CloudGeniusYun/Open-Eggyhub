package com.eggyhub.android;

import android.content.Context;
import android.content.SharedPreferences;

public class TutorialManager {
    private static final String PREF_NAME = "tutorial_prefs";
    private static final String KEY_CURRENT_TUTORIAL = "current_tutorial";
    private static final String KEY_STEP_INDEX = "step_index";
    
    private static TutorialManager instance;
    private SharedPreferences prefs;
    
    public static final String TUTORIAL_PUBLISH = "publish";
    public static final String TUTORIAL_TASK = "task";
    public static final String TUTORIAL_PROFILE = "profile";
    public static final String TUTORIAL_SUPPLEMENT_CODE = "supplement_code";

    private TutorialManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized TutorialManager getInstance(Context context) {
        if (instance == null) {
            instance = new TutorialManager(context);
        }
        return instance;
    }

    public void startTutorial(String tutorialId) {
        prefs.edit()
            .putString(KEY_CURRENT_TUTORIAL, tutorialId)
            .putInt(KEY_STEP_INDEX, 0)
            .commit(); // 使用 commit 确保状态立即写入，防止页面跳转时的竞态条件
    }

    public String getCurrentTutorial() {
        return prefs.getString(KEY_CURRENT_TUTORIAL, null);
    }

    public int getStepIndex() {
        return prefs.getInt(KEY_STEP_INDEX, -1);
    }

    public void nextStep() {
        int currentIndex = getStepIndex();
        prefs.edit().putInt(KEY_STEP_INDEX, currentIndex + 1).commit();
    }

    public void finishTutorial() {
        prefs.edit().clear().commit();
    }
    
    public boolean isTutorialRunning() {
        return getCurrentTutorial() != null;
    }
}
