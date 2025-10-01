package com.acc_ide.execution

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

/**
 * ANSI escape code parser
 * ANSI转义码解析器 - 将终端颜色代码转换为Android样式文本
 */
object AnsiTextParser {
    
    /**
     * Convert ANSI escape codes to Android Spannable text with colors
     * 将 ANSI 转义码转换为带颜色的 Android Spannable 文本
     */
    fun parseAnsiText(text: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        
        // Pattern matches: \x1B[...any letter or \x1B[...m or standalone [...]m or [K etc.
        val ansiPattern = Regex("""\x1B\[[0-9;]*[a-zA-Z]|\[[0-9;]*m|\[K|\[m""")
        
        var currentColor = Color.WHITE
        var currentBold = false
        var lastIndex = 0
        
        // Find all ANSI codes
        ansiPattern.findAll(text).forEach { match ->
            // Add text before this ANSI code with current style
            val textBeforeCode = text.substring(lastIndex, match.range.first)
            if (textBeforeCode.isNotEmpty()) {
                val start = builder.length
                builder.append(textBeforeCode)
                
                // Apply current color
                if (currentColor != Color.WHITE) {
                    builder.setSpan(
                        ForegroundColorSpan(currentColor),
                        start,
                        builder.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                
                // Apply bold style
                if (currentBold) {
                    builder.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        builder.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            
            // Parse ANSI code to update current style (only for SGR codes ending with 'm')
            val matchText = match.value
            if (matchText.endsWith("m")) {
                // Extract code numbers
                val codeStr = matchText.removePrefix("\u001B[").removePrefix("[").removeSuffix("m")
                if (codeStr.isNotEmpty()) {
                    val codes = codeStr.split(";").mapNotNull { it.toIntOrNull() }
                    
                    for (ansiCode in codes) {
                        when (ansiCode) {
                            0 -> { // Reset
                                currentColor = Color.WHITE
                                currentBold = false
                            }
                            1 -> currentBold = true // Bold
                            22 -> currentBold = false // Normal intensity
                            30 -> currentColor = Color.BLACK
                            31 -> currentColor = Color.rgb(220, 50, 47) // Red
                            32 -> currentColor = Color.rgb(133, 153, 0) // Green
                            33 -> currentColor = Color.rgb(181, 137, 0) // Yellow
                            34 -> currentColor = Color.rgb(38, 139, 210) // Blue
                            35 -> currentColor = Color.rgb(211, 54, 130) // Magenta
                            36 -> currentColor = Color.rgb(42, 161, 152) // Cyan
                            37 -> currentColor = Color.WHITE
                            39 -> currentColor = Color.WHITE // Default foreground
                            90 -> currentColor = Color.GRAY // Bright black
                            91 -> currentColor = Color.rgb(255, 100, 100) // Bright red
                            92 -> currentColor = Color.rgb(100, 255, 100) // Bright green
                            93 -> currentColor = Color.rgb(255, 255, 100) // Bright yellow
                            94 -> currentColor = Color.rgb(100, 100, 255) // Bright blue
                            95 -> currentColor = Color.rgb(255, 100, 255) // Bright magenta
                            96 -> currentColor = Color.rgb(100, 255, 255) // Bright cyan
                            97 -> currentColor = Color.WHITE // Bright white
                        }
                    }
                } else {
                    // Empty code like [m means reset
                    currentColor = Color.WHITE
                    currentBold = false
                }
            }
            
            lastIndex = match.range.last + 1
        }
        
        // Add remaining text
        val remainingText = text.substring(lastIndex)
        if (remainingText.isNotEmpty()) {
            val start = builder.length
            builder.append(remainingText)
            
            if (currentColor != Color.WHITE) {
                builder.setSpan(
                    ForegroundColorSpan(currentColor),
                    start,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            
            if (currentBold) {
                builder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        
        // If no ANSI codes found, return original text
        if (builder.isEmpty()) {
            builder.append(text)
        }
        
        return builder
    }
}

