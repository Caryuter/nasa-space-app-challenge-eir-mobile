package mx.edu.utez.eir_nasa_space_apps_challenge.domain.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    data class Progress(
        val percentage: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ModelDownloadState()
    data class Success(val file: File) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
    object Canceled : ModelDownloadState()
}

class ModelDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        const val EXPECTED_MODEL_BYTES = 613_722_264L // Qwen3-0.6B.litertlm size
    }

    fun downloadModel(url: String, fileName: String): Flow<ModelDownloadState> = flow {
        val targetFile = File(context.filesDir, fileName)
        val tempFile = File(context.filesDir, "$fileName.tmp")

        // If target file already exists and is complete (> 500 MB), report success immediately
        if (targetFile.exists() && targetFile.length() > 500_000_000L) {
            emit(ModelDownloadState.Success(targetFile))
            return@flow
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "EirNASASpaceAppsChallenge/1.0")
            .header("Accept-Encoding", "identity") // Avoid gzip stripping Content-Length
            .build()

        var outputStream: FileOutputStream? = null

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                emit(ModelDownloadState.Error("Respuesta de servidor no válida: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body!!
            val rawLength = body.contentLength()
            val headerLength = response.header("Content-Length")?.toLongOrNull()
            val xLinkedSize = response.header("x-linked-size")?.toLongOrNull()
            val totalBytes = when {
                rawLength > 0 -> rawLength
                headerLength != null && headerLength > 0 -> headerLength
                xLinkedSize != null && xLinkedSize > 0 -> xLinkedSize
                else -> EXPECTED_MODEL_BYTES
            }

            // Clean up old temp file if it exists
            if (tempFile.exists()) {
                tempFile.delete()
            }

            outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(64 * 1024)
            var read: Int
            var downloadedBytes = 0L
            var lastEmittedPercent = -1
            var lastEmittedTime = 0L

            // Emit initial 0% progress
            emit(
                ModelDownloadState.Progress(
                    percentage = 0,
                    downloadedBytes = 0L,
                    totalBytes = totalBytes
                )
            )

            body.byteStream().use { inputStream ->
                while (inputStream.read(buffer).also { read = it } != -1) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()

                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read

                    val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                    val now = System.currentTimeMillis()

                    if (progress != lastEmittedPercent || (now - lastEmittedTime) >= 200L) {
                        lastEmittedPercent = progress
                        lastEmittedTime = now
                        emit(
                            ModelDownloadState.Progress(
                                percentage = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                        )
                    }
                }
            }
            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Atomically replace target file
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            emit(
                ModelDownloadState.Progress(
                    percentage = 100,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes
                )
            )
            emit(ModelDownloadState.Success(targetFile))
        } catch (e: kotlinx.coroutines.CancellationException) {
            try { outputStream?.close() } catch (_: Exception) {}
            if (tempFile.exists()) {
                try { tempFile.delete() } catch (_: Exception) {}
            }
            emit(ModelDownloadState.Canceled)
            throw e
        } catch (e: Exception) {
            try { outputStream?.close() } catch (_: Exception) {}
            if (tempFile.exists()) {
                try { tempFile.delete() } catch (_: Exception) {}
            }
            emit(ModelDownloadState.Error("Fallo durante la descarga: ${e.localizedMessage ?: e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
