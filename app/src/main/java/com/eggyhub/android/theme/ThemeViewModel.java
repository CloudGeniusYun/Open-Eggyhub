package com.eggyhub.android.theme;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;

/**
 * 主题ViewModel（单例）
 */
public class ThemeViewModel extends AndroidViewModel {

    private static final String PREF_NAME = "user_theme";
    private static final String KEY_CARDVIEW_THEME = "cardview_theme";

    private MutableLiveData<CardViewTheme> cardViewTheme = new MutableLiveData<>();
    private Gson gson = new Gson();
    private SharedPreferences sharedPreferences;

    public ThemeViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadThemeFromPreferences();
    }

    public LiveData<CardViewTheme> getCardViewTheme() {
        return cardViewTheme;
    }

    public void updateCardViewTheme(CardViewTheme theme) {
        cardViewTheme.setValue(theme);
        saveThemeToPreferences();
    }

    private void saveThemeToPreferences() {
        CardViewTheme theme = cardViewTheme.getValue();
        if (theme != null) {
            String json = gson.toJson(theme);
            sharedPreferences.edit().putString(KEY_CARDVIEW_THEME, json).apply();
        }
    }

    private void loadThemeFromPreferences() {
        String json = sharedPreferences.getString(KEY_CARDVIEW_THEME, null);
        if (json != null) {
            try {
                CardViewTheme theme = gson.fromJson(json, CardViewTheme.class);
                if (theme != null) {
                    cardViewTheme.setValue(theme);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 默认主题
        cardViewTheme.setValue(new CardViewTheme());
    }
}