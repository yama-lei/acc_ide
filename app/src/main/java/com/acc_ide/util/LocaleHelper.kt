package com.acc_ide.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.Locale

/**
 * Helper class to change the app language dynamically
 */
class LocaleHelper {
    companion object {
        private const val SELECTED_LANGUAGE = "selected_language"
        private const val TAG = "LocaleHelper"

        /**
         * 更新应用程序的语言
         */
        fun setLocale(context: Context, language: String): Context {
            saveLanguage(context, language)
            val languageToApply = if (language.isEmpty()) {
                Locale.getDefault().language
            } else {
                language
            }
            
            Log.d(TAG, "设置语言: $languageToApply")
            return updateResources(context, languageToApply)
        }

        /**
         * 获取保存的语言代码
         */
        fun getLanguage(context: Context): String {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val language = prefs.getString(SELECTED_LANGUAGE, "") ?: ""
            Log.d(TAG, "获取保存的语言: $language")
            return language
        }

        /**
         * 保存语言设置到 SharedPreferences
         */
        private fun saveLanguage(context: Context, language: String) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = prefs.edit()
            editor.putString(SELECTED_LANGUAGE, language)
            editor.apply()
            Log.d(TAG, "保存语言设置: $language")
        }

        /**
         * 更新资源配置以应用新语言
         */
        private fun updateResources(context: Context, language: String): Context {
            Log.d(TAG, "更新资源配置，语言: $language")
            
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
            
            // 设置区域
            configuration.setLocale(locale)
            Log.d(TAG, "已设置Locale: $locale")

            // 在应用级别更新配置
            context.resources.updateConfiguration(configuration, resources.displayMetrics)
            
            // 在API 17+上使用createConfigurationContext
            val updatedContext = context.createConfigurationContext(configuration)
            
            // 再次检查配置是否已正确应用
            val updatedLocale = updatedContext.resources.configuration.locales.get(0)
            Log.d(TAG, "更新后的Locale: $updatedLocale, 语言: ${updatedLocale.language}, 国家: ${updatedLocale.country}")
            
            return updatedContext
        }
    }
} 