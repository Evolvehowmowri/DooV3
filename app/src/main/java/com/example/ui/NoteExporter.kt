package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object NoteExporter {
    fun shareNoteAsFile(context: Context, filename: String, content: String, extension: String) {
        try {
            // Remove any characters that aren't letters, digits, underscores, or hyphens
            val safeName = filename.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim()
            val finalName = if (safeName.isBlank()) "note" else safeName
            
            // Create file in the app's cache directory
            val file = File(context.cacheDir, "$finalName.$extension")
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            
            // Generate URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val mimeType = if (extension == "md") "text/markdown" else "text/plain"
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$finalName.$extension")
                putExtra(Intent.EXTRA_TEXT, content) // Fallback plain text
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Export as $extension file"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text sharing if file creation fails
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, filename)
                    putExtra(Intent.EXTRA_TEXT, content)
                }
                context.startActivity(Intent.createChooser(intent, "Export as text"))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
