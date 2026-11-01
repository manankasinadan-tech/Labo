package com.example.labo.util

import android.content.Context
import android.os.Environment
import java.io.File

object StorageUtils {
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
}
