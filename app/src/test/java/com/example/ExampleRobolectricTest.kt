package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.labo.engine.CoreEngine
import com.example.labo.engine.FilesystemEngine
import com.example.labo.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LABO", appName)
    }

    @Test
    fun `test core engine initialization and log emission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coreEngine = CoreEngine.getInstance(context)
        assertNotNull(coreEngine)

        coreEngine.log(LogLevel.INFO, "TestEngine", "Test log entry")
        val logs = coreEngine.logs.value
        assertEquals(true, logs.any { it.message == "Test log entry" })
    }

    @Test
    fun `test filesystem engine bootable permissions inference`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coreEngine = CoreEngine.getInstance(context)
        val fsEngine = FilesystemEngine(coreEngine)

        val binConfig = fsEngine.inferBootableFsConfig("/system/bin/sh", false)
        assertEquals("0755", binConfig.mode)
        assertEquals(0, binConfig.uid)
        assertEquals(2000, binConfig.gid)

        val apkConfig = fsEngine.inferBootableFsConfig("/system/app/Core/Core.apk", false)
        assertEquals("0644", apkConfig.mode)
        assertEquals(0, apkConfig.uid)
        assertEquals(0, apkConfig.gid)
    }
}
