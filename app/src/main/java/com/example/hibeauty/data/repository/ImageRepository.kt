package com.example.hibeauty.data.repository

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context
import android.provider.OpenableColumns
import org.json.JSONObject

class ImageRepository(private val context: Context) {

    private val cloudName = "dswicy00p"
    private val uploadPreset = "hibeauty"

    suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val url = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                doOutput = true
                requestMethod = "POST"
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            val imageBytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                ?: error("Could not read image")
            val fileName = getFileName(imageUri)

            connection.outputStream.use { output ->
                // upload_preset field
                output.write(("--$boundary\r\n").toByteArray())
                output.write("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n".toByteArray())
                output.write("$uploadPreset\r\n".toByteArray())

                // file field
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n".toByteArray())
                output.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                output.write(imageBytes)
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
            JSONObject(response).getString("secure_url")
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "image_${System.currentTimeMillis()}.jpg"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }
}
