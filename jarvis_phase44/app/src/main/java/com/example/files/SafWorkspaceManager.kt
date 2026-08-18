package com.example.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Real user-selected workspace backed by Android's Storage Access Framework. */
class SafWorkspaceManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("saf_workspace", Context.MODE_PRIVATE)
    private val key = "tree_uri"

    fun currentTreeUri(): Uri? = prefs.getString(key, null)?.let(Uri::parse)

    fun saveTreeUri(uri: Uri, flags: Int) {
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        try { context.contentResolver.takePersistableUriPermission(uri, takeFlags) } catch (_: SecurityException) { }
        prefs.edit().putString(key, uri.toString()).apply()
    }

    fun clearTreeUri() { prefs.edit().remove(key).apply() }

    suspend fun listFiles(maxItems: Int = 500): List<IndexedFileInfo> = withContext(Dispatchers.IO) {
        val root = currentTreeUri() ?: return@withContext emptyList()
        val result = mutableListOf<IndexedFileInfo>()
        walkTree(root, "", result, maxItems, 0)
        result
    }

    private fun walkTree(treeUri: Uri, relative: String, out: MutableList<IndexedFileInfo>, max: Int, depth: Int) {
        if (out.size >= max || depth > 8) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val name = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mime = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val size = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modified = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (c.moveToNext() && out.size < max) {
                val docId = c.getString(id)
                val display = c.getString(name) ?: "unnamed"
                val isDir = c.getString(mime) == DocumentsContract.Document.MIME_TYPE_DIR
                val path = if (relative.isBlank()) display else "$relative/$display"
                out += IndexedFileInfo(display, path, if (isDir) "" else display.substringAfterLast('.', "").lowercase(),
                    if (c.isNull(size)) 0L else c.getLong(size), if (c.isNull(modified)) 0L else c.getLong(modified), isDir)
                if (isDir) walkDocument(treeUri, docId, path, out, max, depth + 1)
            }
        }
    }

    private fun walkDocument(treeUri: Uri, docId: String, relative: String, out: MutableList<IndexedFileInfo>, max: Int, depth: Int) {
        if (out.size >= max || depth > 8) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { c ->
            val id = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val name = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mime = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val size = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modified = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (c.moveToNext() && out.size < max) {
                val childId = c.getString(id); val display = c.getString(name) ?: "unnamed"; val isDir = c.getString(mime) == DocumentsContract.Document.MIME_TYPE_DIR
                val path = "$relative/$display"
                out += IndexedFileInfo(display, path, if (isDir) "" else display.substringAfterLast('.', "").lowercase(), if (c.isNull(size)) 0L else c.getLong(size), if (c.isNull(modified)) 0L else c.getLong(modified), isDir)
                if (isDir) walkDocument(treeUri, childId, path, out, max, depth + 1)
            }
        }
    }

    suspend fun readFile(path: String): String? = withContext(Dispatchers.IO) {
        val uri = resolveDocument(path) ?: return@withContext null
        context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
    }

    suspend fun writeFile(path: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val clean = cleanPath(path) ?: return@withContext false
        val slash = clean.lastIndexOf('/')
        val parent = if (slash >= 0) clean.substring(0, slash) else ""
        val name = if (slash >= 0) clean.substring(slash + 1) else clean
        if (name.isBlank()) return@withContext false
        val parentUri = resolveDirectory(parent) ?: currentTreeUri() ?: return@withContext false
        val existing = findChild(parentUri, name)
        val uri = existing ?: DocumentsContract.createDocument(context.contentResolver, parentUri, mimeFor(name), name) ?: return@withContext false
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) } ?: return@withContext false
        true
    }

    suspend fun zipProject(outputName: String = "jarvis_project.zip"): ByteArray? = withContext(Dispatchers.IO) {
        val files = listFiles(2000).filter { !it.isDirectory }
        if (files.isEmpty()) return@withContext null
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for (f in files) {
                val data = readFile(f.path)?.toByteArray(Charsets.UTF_8) ?: continue
                zip.putNextEntry(ZipEntry(f.path.trimStart('/')))
                zip.write(data)
                zip.closeEntry()
            }
        }
        bytes.toByteArray()
    }

    suspend fun writeZip(outputName: String = "jarvis_project.zip"): Uri? = withContext(Dispatchers.IO) {
        val root = currentTreeUri() ?: return@withContext null
        val bytes = zipProject(outputName) ?: return@withContext null
        val uri = DocumentsContract.createDocument(context.contentResolver, root, "application/zip", outputName) ?: return@withContext null
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@withContext null
        uri
    }

    private fun resolveDirectory(path: String): Uri? {
        val root = currentTreeUri() ?: return null
        val clean = cleanPath(path) ?: return null
        if (clean.isBlank()) return root
        var current = root
        for (segment in clean.split('/')) {
            val child = findChild(current, segment) ?: return null
            current = child
        }
        return current
    }

    /** Resolve a user-selected workspace file to its document Uri for Android intents. */
    fun resolveFileUri(path: String): Uri? = resolveDocument(path)

    /** Return the MIME type recorded by the Storage Access Framework, falling back to extension. */
    fun mimeType(path: String): String? {
        val uri = resolveDocument(path) ?: return null
        context.contentResolver.query(
            uri, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                val value = c.getString(0)
                if (!value.isNullOrBlank()) return value
            }
        }
        return mimeFor(path)
    }

    private fun resolveDocument(path: String): Uri? {
        val clean = cleanPath(path) ?: return null
        val slash = clean.lastIndexOf('/')
        val parent = if (slash >= 0) clean.substring(0, slash) else ""
        val name = if (slash >= 0) clean.substring(slash + 1) else clean
        return findChild(resolveDirectory(parent) ?: return null, name)
    }

    private fun findChild(parent: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
        context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            val id = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID); val n = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (c.moveToNext()) if (name == c.getString(n)) return DocumentsContract.buildDocumentUriUsingTree(parent, c.getString(id))
        }
        return null
    }

    private fun cleanPath(path: String): String? {
        val clean = path.trim().replace('\\', '/').trim('/')
        if (clean.isBlank() || clean.split('/').any { it == ".." || it.isBlank() }) return if (clean.isBlank()) "" else null
        return clean
    }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "json" -> "application/json"; "py" -> "text/x-python"; "cpp", "h", "hpp" -> "text/x-c"; "js" -> "text/javascript"; "html" -> "text/html"; "css" -> "text/css"; "xml" -> "application/xml"; "md", "txt", "kt", "java", "ts", "csv" -> "text/plain"; else -> "application/octet-stream"
    }
}
