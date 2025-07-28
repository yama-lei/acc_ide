package com.acc_ide.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.Locale

/**
 * Helper class for changing app language dynamically
 * 语言助手类 - 用于动态更改应用程序语言
 */
class LocaleHelper {
    companion object {
        private const val SELECTED_LANGUAGE = "selected_language"
        private const val TAG = "LocaleHelper"

        /**
         * Set locale for app with specified language
         * 更新应用程序的语言
         */
        fun setLocale(context: Context, language: String): Context {
            saveLanguage(context, language)
            val languageToApply = if (language.isEmpty()) {
                Locale.getDefault().language
            } else {
                language
            }
            
            Log.d(TAG, "Setting language: $languageToApply")
            return updateResources(context, languageToApply)
        }

        /**
         * Get saved language code from preferences
         * 获取保存的语言代码
         */
        fun getLanguage(context: Context): String {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val language = prefs.getString(SELECTED_LANGUAGE, "") ?: ""
            Log.d(TAG, "Retrieved saved language: $language")
            return language
        }

        /**
         * Save language setting to SharedPreferences
         * 保存语言设置到SharedPreferences
         */
        private fun saveLanguage(context: Context, language: String) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putString(SELECTED_LANGUAGE, language)
            editor.apply()
            Log.d(TAG, "Saved language setting: $language")
        }

        /**
         * Update resource configuration to apply new language
         * 更新资源配置以应用新语言
         */
        private fun updateResources(context: Context, language: String): Context {
            Log.d(TAG, "Updating resource configuration, language: $language")
            
            val locale = when (language) {
                "en" -> Locale.ENGLISH
                "zh" -> Locale.CHINESE
                "zh-CN" -> Locale.SIMPLIFIED_CHINESE
                "zh-TW" -> Locale.TRADITIONAL_CHINESE
                else -> Locale(language)
            }
            
            Locale.setDefault(locale)
            
            val resources = context.resources
            val configuration = Configuration(resources.configuration)
            
            // Set locale
            configuration.setLocale(locale)
            Log.d(TAG, "Locale set: $locale")
            
            // Use createConfigurationContext on API 17+
            val updatedContext = context.createConfigurationContext(configuration)
            
            // Check again if configuration has been applied correctly
            val updatedLocale = updatedContext.resources.configuration.locales.get(0)
            Log.d(TAG, "Updated Locale: $updatedLocale, language: ${updatedLocale.language}, country: ${updatedLocale.country}")
            
            return updatedContext
        }
    }
} 