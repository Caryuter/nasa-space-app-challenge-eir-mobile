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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun downloadModel(url: String, fileName: String): Flow<ModelDownloadState> = flow {
        val targetFile = File(context.filesDir, fileName)

        // If file already exists and is non-empty, report success immediately
        if (targetFile.exists() && targetFile.length() > 0) {
            emit(ModelDownloadState.Success(targetFile))
            return@flow
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "EirNASASpaceAppsChallenge/1.0")
            .build()

        var outputStream: FileOutputStream? = null

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                emit(ModelDownloadState.Error("Respuesta de servidor no válida: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body!!
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            outputStream = FileOutputStream(targetFile)
            val buffer = ByteArray(32 * 1024)
            var read: Int

            body.byteStream().use { inputStream ->
                while (inputStream.read(buffer).also { read = it } != -1) {
                    // Check if coroutine was cancelled
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()

                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read

                    val progress = if (totalBytes > 0) {
                        ((downloadedBytes * 100) / totalBytes).toInt()
                    } else 0

                    emit(
                        ModelDownloadState.Progress(
                            percentage = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    )
                }
            }
            outputStream.flush()
            outputStream.close()
            outputStream = null

            emit(ModelDownloadState.Success(targetFile))
        } catch (e: kotlinx.coroutines.CancellationException) {
            try { outputStream?.close() } catch (_: Exception) {}
            if (targetFile.exists()) {
                targetFile.delete()
            }
            emit(ModelDownloadState.Canceled)
            throw e
        } catch (e: Exception) {
            try { outputStream?.close() } catch (_: Exception) {}
            if (targetFile.exists()) {
                targetFile.delete()
            }
            emit(ModelDownloadState.Error("Fallo durante la descarga: ${e.localizedMessage ?: e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
