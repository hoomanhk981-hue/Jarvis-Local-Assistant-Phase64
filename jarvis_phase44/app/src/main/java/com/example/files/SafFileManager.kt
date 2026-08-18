package com.example.files

import android.content.Context
import android.net.Uri
import com.example.data.models.CodeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class IndexRefreshMode {
    MANUAL,
    AUTOMATIC,
    CUSTOM
}

data class IndexedFileInfo(
    val name: String,
    val path: String,
    val extension: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isDirectory: Boolean = false
)

class SafFileManager(private val context: Context) {

    private val supportedExtensions = setOf(
        "txt", "json", "csv", "py", "cpp", "c", "h", "hpp",
        "java", "kt", "html", "css", "js", "ts", "xml", "md", "zip"
    )

    fun getProjectDirectory(): File {
        val dir = File(context.filesDir, "user_workspace")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Scans and indexes files inside the authorized directory.
     */
    suspend fun indexDirectory(dir: File = getProjectDirectory()): List<IndexedFileInfo> = withContext(Dispatchers.IO) {
        val indexed = mutableListOf<IndexedFileInfo>()
        if (!dir.exists()) return@withContext emptyList()

        dir.walkTopDown().maxDepth(5).forEach { file ->
            val ext = file.extension.lowercase()
            if (file.isFile && (supportedExtensions.contains(ext) || ext.isEmpty())) {
                indexed.add(
                    IndexedFileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        extension = ext,
                        sizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        isDirectory = false
                    )
                )
            }
        }
        indexed
    }

    /**
     * Saves a code file to local disk workspace.
     */
    suspend fun saveCodeFile(codeFile: CodeFile, targetDir: File = getProjectDirectory()): File = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) targetDir.mkdirs()
        val file = File(targetDir, codeFile.name)
        file.writeText(codeFile.content)
        file
    }

    /**
     * Reads a code file from disk.
     */
    suspend fun readCodeFile(fileName: String, targetDir: File = getProjectDirectory()): CodeFile? = withContext(Dispatchers.IO) {
        val file = File(targetDir, fileName)
        if (!file.exists()) return@withContext null
        val ext = file.extension.lowercase()
        val lang = when (ext) {
            "py" -> "python"
            "cpp", "c", "h", "hpp" -> "cpp"
            "json" -> "json"
            "sh", "bash" -> "bash"
            "js", "ts" -> "javascript"
            "html" -> "html"
            "css" -> "css"
            "kt" -> "kotlin"
            "java" -> "java"
            else -> "text"
        }
        CodeFile(name = file.name, content = file.readText(), language = lang, filePath = file.absolutePath)
    }

    /**
     * Creates a real ZIP archive from workspace files.
     */
    suspend fun createZipArchive(
        sourceDir: File = getProjectDirectory(),
        zipFileName: String = "project_export.zip"
    ): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val outputFile = File(exportDir, zipFileName)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { out ->
                sourceDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.absolutePath != outputFile.absolutePath) {
                        val relativePath = sourceDir.toPath().relativize(file.toPath()).toString()
                        val entry = ZipEntry(relativePath)
                        out.putNextEntry(entry)
                        file.inputStream().use { input -> input.copyTo(out) }
                        out.closeEntry()
                    }
                }
            }
            outputFile
        } catch (e: Exception) {
            null
        }
    }
}
