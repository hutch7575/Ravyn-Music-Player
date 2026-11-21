package app.vitune.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ExtractionRuntimeTest {
    @Test fun installedQuickJsCanExecute() {
        val runtime = Dependencies.quickjsPath
        assertTrue("APK must install the extraction runtime", runtime.isFile)
        assertTrue("QuickJS must be executable", runtime.canExecute())
        val process = ProcessBuilder(runtime.absolutePath, "-e", "console.log(6 * 7)")
            .redirectErrorStream(true).start()
        try {
            assertTrue("QuickJS timed out", process.waitFor(10, TimeUnit.SECONDS))
            val output = process.inputStream.bufferedReader().readText()
            assertTrue("QuickJS could not evaluate JavaScript: $output", output.trim() == "42")
        } finally { process.destroy() }
    }
}
