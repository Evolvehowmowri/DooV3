package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object NoteExporter {
    fun shareNoteAsFile(context: Context, filename: String, content: String, extension: String) {
        try {
            // Remove any characters that aren't Unicode letters, digits, spaces, hyphens, underscores, or periods
            val safeName = filename.replace(Regex("[^\\p{L}\\p{N}_\\-\\s.]"), "_")
                .replace(Regex("_+"), "_")
                .trim()
            val finalName = if (safeName.isBlank() || safeName == "_") "note" else safeName
            
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

    /**
     * Creates a ZIP archive containing multiple files in custom folder directories and shares it.
     * files is a List of Pair (RelativePathWithinZip, FileContentString)
     */
    fun shareZipFile(context: Context, zipFileName: String, files: List<Pair<String, String>>) {
        try {
            val safeName = zipFileName.replace(Regex("[^\\p{L}\\p{N}_\\-\\s.]"), "_")
                .replace(Regex("_+"), "_")
                .trim()
            val finalName = if (safeName.isBlank() || safeName == "_") "archive" else safeName
            
            val file = File(context.cacheDir, "$finalName.zip")
            FileOutputStream(file).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    files.forEach { (path, content) ->
                        try {
                            val entry = ZipEntry(path)
                            zos.putNextEntry(entry)
                            zos.write(content.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$finalName.zip")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Export folder-structured ZIP"))
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                // Ultimate text fallback
                val combinedText = files.joinToString("\n\n---\n\n") { "${it.first}:\n${it.second}" }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, zipFileName)
                    putExtra(Intent.EXTRA_TEXT, combinedText)
                }
                context.startActivity(Intent.createChooser(intent, "Export compiled text"))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
