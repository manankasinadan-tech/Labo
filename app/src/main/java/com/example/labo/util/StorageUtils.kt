package com.example.labo.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream

data class StorageShortcut(
    val label: String,
    val dir: File,
    val isPrimary: Boolean = false
)

object StorageUtils {

    /**
     * Checks if all files access is granted on Android 11+ (API 30+).
     */
    fun isAllFilesAccessGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Creates an Intent to prompt the user to grant MANAGE_ALL_FILES_ACCESS_PERMISSION in Settings.
     */
    fun getAllFilesAccessSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * Returns the base directory for LABO exports and files.
     * Tries:
     * 1. /sdcard/LABO (or Environment.getExternalStorageDirectory()/LABO)
     * 2. context.getExternalFilesDir(null)/LABO
     * 3. context.filesDir/LABO
     * Ensures directories are created safely without throwing ENOENT.
     */
    fun getLaboDirectory(context: Context, subFolder: String = ""): File {
        val folderPath = if (subFolder.isEmpty()) "LABO" else "LABO/$subFolder"

        // 1. Try public storage /sdcard/LABO
        runCatching {
            val extPublic = File(Environment.getExternalStorageDirectory(), folderPath)
            if (extPublic.exists() || extPublic.mkdirs()) {
                if (extPublic.canWrite()) return extPublic
            }
        }

        // 2. Try app external files dir
        runCatching {
            val appExternal = context.getExternalFilesDir(null)
            if (appExternal != null) {
                val dir = File(appExternal, folderPath)
                if (dir.exists() || dir.mkdirs()) {
                    return dir
                }
            }
        }

        // 3. Fallback to app internal storage (always writable)
        val internalDir = File(context.filesDir, folderPath)
        internalDir.mkdirs()
        return internalDir
    }

    /**
     * Safely writes bytes to a target file, creating all parent directories.
     */
    fun safeWriteBytes(targetFile: File, bytes: ByteArray): Boolean {
        return runCatching {
            targetFile.parentFile?.mkdirs()
            targetFile.writeBytes(bytes)
            true
        }.getOrDefault(false)
    }

    /**
     * Gets the default browsable storage directory for file pickers and explorers.
     */
    fun getBrowsableRootDirectory(context: Context): File {
        runCatching {
            val sd = File("/sdcard")
            if (sd.exists() && sd.canRead() && (sd.listFiles() != null)) {
                return sd
            }
        }
        runCatching {
            val ext = Environment.getExternalStorageDirectory()
            if (ext.exists() && ext.canRead() && (ext.listFiles() != null)) {
                return ext
            }
        }
        runCatching {
            val appExt = context.getExternalFilesDir(null)
            if (appExt != null && appExt.exists()) {
                return appExt
            }
        }
        return context.filesDir
    }

    /**
     * Returns list of popular storage shortcuts for quick navigation.
     */
    fun getStorageShortcuts(context: Context): List<StorageShortcut> {
        val list = mutableListOf<StorageShortcut>()

        // 1. LABO Directory (where images and repacks go)
        val laboDir = getLaboDirectory(context)
        list.add(StorageShortcut(label = "Dossier LABO", dir = laboDir, isPrimary = true))

        // 2. Download directory
        runCatching {
            val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dl.exists() || dl.mkdirs()) {
                list.add(StorageShortcut(label = "Download", dir = dl))
            }
        }

        // 3. Root /sdcard
        runCatching {
            val sd = File("/sdcard")
            if (sd.exists() && sd.canRead()) {
                list.add(StorageShortcut(label = "Racine (/sdcard)", dir = sd))
            }
        }

        // 4. Documents directory
        runCatching {
            val doc = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (doc.exists() || doc.mkdirs()) {
                list.add(StorageShortcut(label = "Documents", dir = doc))
            }
        }

        // 5. App sandbox storage
        val appFiles = context.getExternalFilesDir(null) ?: context.filesDir
        list.add(StorageShortcut(label = "App Sandbox", dir = appFiles))

        return list
    }

    /**
     * Resolves a SAF Uri (content://) to a temporary local cache file so engines can read it.
     */
    fun copyUriToLocalCache(context: Context, uri: Uri): File? {
        return runCatching {
            var fileName = "imported_file_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
            }

            val cacheFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        }.getOrNull()
    }

    /**
     * Creates sample files in LABO directory if none exist, so the user has files to test immediately.
     */
    fun createSampleLaboFiles(context: Context): File {
        val laboDir = getLaboDirectory(context)
        val sampleImg = File(laboDir, "gsi_system_sample.img")
        if (!sampleImg.exists()) {
            safeWriteBytes(sampleImg, ByteArray(1024 * 512) { 0x00 })
        }
        val samplePatch = File(laboDir, "labo_build_patch.conf")
        if (!samplePatch.exists()) {
            samplePatch.writeText("# Configuration de patch GSI\nro.labo.patch.version=1.0\nro.labo.optimized=true\n")
        }
        return laboDir
    }
}
