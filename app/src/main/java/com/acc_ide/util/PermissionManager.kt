package com.acc_ide.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.acc_ide.R

/**
 * Permission manager for handling various permission requests and management
 * 权限管理器 - 负责处理应用所需的各种权限请求和管理
 */
class PermissionManager(private val activity: AppCompatActivity) {
    
    companion object {
        const val REQUEST_STORAGE_PERMISSION = 100
        private const val TAG = "PermissionManager"
    }
    
    /**
     * Storage permission callback interface
     * 请求存储权限回调接口
     */
    interface StoragePermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied()
    }
    
    private var storagePermissionCallback: StoragePermissionCallback? = null
    
    /**
     * Request storage permission with callback
     * 请求存储权限
     */
    fun requestStoragePermission(callback: StoragePermissionCallback) {
        this.storagePermissionCallback = callback
        
        val permissionCheck = when {
            // Android 13+ (API 33+) - app-specific directory doesn't need permission
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                true // Access to app-specific directory doesn't need permission
            }

            // Android 10 (API 29) - 12 (API 32) use scoped storage
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Access to app-specific directory doesn't need special permission, but keep check just in case
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

            // Android 9 (API 28) and below use legacy storage permissions
            // Since WRITE_EXTERNAL_STORAGE permission is removed, return true directly
            // App-specific directory is accessible on all Android versions
            else -> {
                true
            }
        }

        // Request permission or initialize based on permission check result
        if (!permissionCheck) {
            // Show permission explanation dialog
            showPermissionExplanationDialog()
        } else {
            // Already have permission, callback success directly
            callback.onPermissionGranted()
        }
    }
    
    /**
     * Show permission explanation dialog to user
     * 显示权限解释对话框
     */
    private fun showPermissionExplanationDialog() {
        val alertBuilder = AlertDialog.Builder(activity)
        alertBuilder.setTitle(R.string.storage_permission_title)
        alertBuilder.setMessage(R.string.storage_permission_message)
        alertBuilder.setPositiveButton("OK") { _, _ ->
            // Request corresponding permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                // Android 10-12: request READ_EXTERNAL_STORAGE permission
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_STORAGE_PERMISSION
                )
            } else {
                // Android 13+ or Android 9-: permission not needed or deprecated
                storagePermissionCallback?.onPermissionGranted()
            }
        }
        alertBuilder.setNegativeButton(android.R.string.cancel) { _, _ ->
            storagePermissionCallback?.onPermissionDenied()
        }
        alertBuilder.show()
    }
    
    /**
     * Handle permission request result - should be called in Activity's onRequestPermissionsResult
     * 处理权限请求结果 - 应该在Activity的onRequestPermissionsResult中调用
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d(TAG, "Storage permission granted")
                storagePermissionCallback?.onPermissionGranted()
            } else {
                // Permission denied, but app-specific directory can still be used
                Log.d(TAG, "Storage permission denied, but app-specific directory can still be used")
                storagePermissionCallback?.onPermissionGranted() // Can still continue as using app-specific directory
            }
            
            // Clear callback reference
            storagePermissionCallback = null
        }
    }
    
    /**
     * Check if has storage permission
     * 检查是否有存储权限
     */
    fun hasStoragePermission(): Boolean {
        return when {
            // Android 13+ (API 33+) - app-specific directory doesn't need permission
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> true
            
            // Android 10 (API 29) - 12 (API 32) use scoped storage
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
            
            // Android 9 (API 28) and below
            else -> true // App-specific directory is always available
        }
    }
    
    /**
     * Check if should show permission rationale
     * 检查权限是否应该显示说明
     */
    fun shouldShowPermissionRationale(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            else -> false
        }
    }
} 