package com.pawanhegde.tokindle.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.chimbori.crux.articles.Article
import com.chimbori.crux.articles.ArticleExtractor
import com.pawanhegde.tokindle.model.Document
import com.pawanhegde.tokindle.model.DownloadStatus
import com.pawanhegde.tokindle.model.DownloadStatus.*
import dagger.hilt.android.qualifiers.ApplicationContext
import j2html.TagCreator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DocumentRepository"
private const val DOCUMENTS_FOLDER = "documents"

@Singleton
class DocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentDao: DocumentDao,
) {
    companion object {
        private fun generateRandomId() = UUID.randomUUID().toString()
    }

    /**
     * Download jobs that are currently in progress
     */
    private val ongoingDownloads = mutableMapOf<String, Job>()

    /**
     * Statuses of documents. Only contains those documents that were added during this app session
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _downloadStatuses = MutableStateFlow(mapOf<String, DownloadStatus>())
    val downloadStatuses: Flow<Map<String, DownloadStatus>> get() = _downloadStatuses

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun updateDownloadStatus(documentId: String, status: DownloadStatus) {
        _downloadStatuses.value += mapOf(Pair(documentId, status))
    }

    val allDocuments: Flow<List<Document>> get() = documentDao.getAll()

    suspend fun getDocument(id: String): Document? {
        return documentDao.getById(id)
    }

    suspend fun add(source: Uri) {
        val existingDocument = deleteIfExisting(source)
        val id = existingDocument?.id ?: generateRandomId()

        val job = GlobalScope.launch { download(id, source) }
        ongoingDownloads[id]?.cancel("Document is being downloaded again")
        ongoingDownloads[id] = job
    }

    suspend fun deleteDocument(id: String) {
        val document = documentDao.getById(id)

        GlobalScope.launch(Dispatchers.IO) {
            document?.localPath?.delete()
        }

        // Remove the reference in our database
        documentDao.delete(id)
    }

    private suspend fun deleteIfExisting(sourceUri: Uri): Document? {
        // If a document by the same path exists, clear it
        return documentDao.getBySourcePath(sourceUri)?.let {
            it.localPath?.delete()
            it
        }
    }

    private suspend fun download(documentId: String, source: Uri) = coroutineScope {
        when (source.scheme) {
            "content" -> downloadLocal(documentId, source)
            "http", "https", "ftp", "sftp" -> downloadRemote(documentId, source)
            else -> throw IllegalStateException("Unexpected source type ${source.scheme}")
        }
    }

    private suspend fun downloadRemote(id: String, source: Uri) = coroutineScope {
        val entity = Document(id = id, sourcePath = source)
        documentDao.upsert(entity)

        launch(Dispatchers.IO) {
            try {
                updateDownloadStatus(id, DOWNLOADING)
                val url = URL(source.toString())
                val mimeType = guessContentTypeRemote(url)
                var displayName = getDisplayName(source)

                // We're also updating the display name as a side-effect in the below statement
                val inputStream: InputStream = url.openStream()?.let {
                    if (mimeType == "text/html") {
                        updateDownloadStatus(id, CONVERTING)
                        val article = extractArticle(it)

                        displayName = article.title ?: displayName

                        val html = createHtml(article, article.canonicalUrl.toString())

                        return@let html.byteInputStream()
                    } else {
                        return@let it
                    }
                } ?: throw IllegalStateException("Could not resolve the content at $source")

                // Upsert to update the display name in the store
                entity.displayName = displayName
                documentDao.upsert(entity)

                val fileName =
                    createLocalFileName(source.lastPathSegment ?: source.host ?: id, mimeType)
                val localFile = context.getExternalFilesDir(DOCUMENTS_FOLDER)?.resolve(fileName)
                    ?: return@launch  // We can't find the external files directory. This is likely transient

                entity.localPath = localFile
                documentDao.upsert(entity)

                val outputStream = localFile.outputStream()
                updateDownloadStatus(id, SAVING)
                inputStream.copyTo(outputStream)
                updateDownloadStatus(id, FINISHED)
            } catch (e: Exception) {
                Log.e(TAG, "downloadRemote: Failed to download from the remote URL $source", e)
                updateDownloadStatus(id, ERRORED)
            }
        }
    }

    private suspend fun downloadLocal(id: String, source: Uri) = coroutineScope {
        val displayName = guessDisplayNameLocal(source)
        val entity = Document(id = id, sourcePath = source, displayName = displayName)
        documentDao.upsert(entity)

        launch(Dispatchers.IO) {
            try {
                val mimeType: String = guessContentTypeLocal(source)
                updateDownloadStatus(id, DOWNLOADING)

                val inputStream: InputStream =
                    context.contentResolver.openInputStream(source)?.let {
                        if (mimeType == "text/html") {
                            updateDownloadStatus(id, CONVERTING)
                            val article = extractArticle(it)

                            entity.displayName = article.title
                            documentDao.upsert(entity)

                            val html = createHtml(article, article.canonicalUrl.toString())
                            return@let html.byteInputStream()
                        } else {
                            return@let it
                        }
                    } ?: throw IllegalStateException("Could not resolve the content at $source")

                val fileName = createLocalFileName(displayName, mimeType)
                val localFile = context.getExternalFilesDir(DOCUMENTS_FOLDER)?.resolve(fileName)
                    ?: return@launch  // We can't find the external files directory. This is likely transient

                entity.localPath = localFile
                documentDao.upsert(entity)

                val outputStream = localFile.outputStream()
                updateDownloadStatus(id, SAVING)
                inputStream.copyTo(outputStream)
                updateDownloadStatus(id, FINISHED)
            } catch (e: Exception) {
                Log.e(TAG, "downloadLocal: Failed to download a local file", e)
                updateDownloadStatus(id, ERRORED)
            }
        }
    }

    private fun createLocalFileName(displayName: String, mimeType: String): String {
        val baseName = displayName.substringBeforeLast(".")
        val currentTime = System.currentTimeMillis()
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

        return "${baseName}_$currentTime${if (extension == null) "" else ".$extension"}"
    }

    private fun guessContentTypeRemote(url: URL): String {
        val reported = url.openConnection().contentType?.substringBefore(";")
        return reported ?: URLConnection.guessContentTypeFromName(url.toString()) ?: "text/html"
    }

    private fun guessContentTypeLocal(uri: Uri): String {
        return when (val reportedType = context.contentResolver.getType(uri)) {
            "multipart/related" -> "text/html"
            null -> "text/plain"
            else -> reportedType
        }
    }

    private fun extractArticle(input: InputStream): Article {
        val document = Jsoup.parse(input, null, "https://example.com")
        return ArticleExtractor(
            document.baseUri().toHttpUrl(),
            document
        ).extractMetadata().extractContent().article
    }

    private fun guessDisplayNameLocal(source: Uri): String {
        return context.contentResolver.query(source, null, null, null, null)?.use {
            it.moveToFirst()
            it.getString(it.getColumnIndex("_display_name")) ?: source.toString()
        } // For the really unlikely case that we can't get the display name for a local file
            ?: source.toString()
    }

    suspend fun markAsSent(id: String) {
        documentDao.getById(id)?.let {
            it.lastSentAt = System.currentTimeMillis()
            documentDao.update(it)
        }
    }

    fun cancelDownload(id: String) {
        ongoingDownloads[id]?.cancel("Cancelled by user")
        ongoingDownloads.remove(id)
    }

    private fun getDisplayName(uri: Uri): String {
        val host = uri.host?.substringAfter("www.")

        return if (uri.pathSegments.isNotEmpty()) {
            "$host > ${uri.pathSegments.joinToString(separator = " > ")}"
        } else {
            uri.toString()
        }
    }

    private fun createHtml(article: Article, url: String?): String {
        val encodedImage = article.imageUrl?.let {
            if (it.scheme == "data") {
                it.toString()
            }
            "data:image/gif;base64,${getByteArrayFromImageURL(it)}"
        }

        val title = TagCreator.title(article.title)
        val charset = TagCreator.meta().withCharset("UTF-8")
        val meta = TagCreator.meta()
            .attr("http-equiv", "Content-Type")
            .attr("content", "text/html; charset=utf-8")
            .attr("viewport", "width=device-width, initial-scale=1")


        val heading = TagCreator.h1(article.title)
        val description = TagCreator.em(article.description)
        val coverImage = TagCreator.img().withSrc(encodedImage).withStyle("width: 100%")
        val content = TagCreator.rawHtml(article.document.toString())
        val linkToOriginal = url?.let {
            TagCreator.p(
                TagCreator.span("To load the original page in the browser "),
                TagCreator.a("click here").withHref(it),
            )
        }

        val head = TagCreator.head(
            title,
            charset,
            meta
        )

        val body = TagCreator.body()
            .with(heading)
            .with(linkToOriginal)
            .condWith(!article.description.isNullOrBlank(), description)
            .condWith(!encodedImage.isNullOrBlank(), coverImage)
            .with(content)

        return TagCreator.html(head, body).renderFormatted()
    }

    private fun getByteArrayFromImageURL(url: HttpUrl): String? {
        try {
            val imageUrl = url.toUrl()
            val ucon = imageUrl.openConnection()
            val `is` = ucon.getInputStream()
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var read: Int
            do {
                read = `is`.read(buffer, 0, buffer.size)
                if (read != -1)
                    baos.write(buffer, 0, read)
            } while (read != -1)
            baos.flush()
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            println("When trying to fetch the image, found the exception: $e")
        }

        return null
    }
}