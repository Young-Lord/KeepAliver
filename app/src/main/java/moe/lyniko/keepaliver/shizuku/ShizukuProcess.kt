package moe.lyniko.keepaliver.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuProcess {

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = reader.readText()
            val errorOutput = errorReader.readText()

            val exitCode = process.waitFor()
            process.destroy()

            if (exitCode != 0) {
                throw RuntimeException("Shell exited $exitCode: $errorOutput")
            }
            output
        }
    }
}
