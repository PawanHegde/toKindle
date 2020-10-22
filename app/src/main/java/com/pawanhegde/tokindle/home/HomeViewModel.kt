package com.pawanhegde.tokindle.home

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.annotation.VisibleForTesting
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.pawanhegde.tokindle.R
import com.pawanhegde.tokindle.data.DocumentRepository
import com.pawanhegde.tokindle.data.EmailRepository
import com.pawanhegde.tokindle.model.Document
import com.pawanhegde.tokindle.model.DocumentUiModel
import com.pawanhegde.tokindle.model.DocumentUiStatus
import com.pawanhegde.tokindle.model.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class HomeViewModel @ViewModelInject constructor(
    private val documentRepository: DocumentRepository,
    emailRepository: EmailRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val documents = documentRepository.allDocuments.combine(documentRepository.downloadStatuses)
    { documents, downloadingStatuses ->
        documents.sortedByDescending { it.addedAt }.map { document ->
            convertToUiModel(
                document,
                downloadingStatuses[document.id]
            )
        }
    }.asLiveData(viewModelScope.coroutineContext)

    private fun convertToUiModel(
        document: Document,
        downloadStatus: DownloadStatus?
    ): DocumentUiModel {
        with(document) {
            return DocumentUiModel(
                id,
                displayName ?: sourcePath.toString(),
                toRelativeDuration(System.currentTimeMillis() - addedAt),
                lastSentAt?.let { "Last sent ${toRelativeDuration(System.currentTimeMillis() - it)}" }
                    ?: context.getString(R.string.never_sent),
                toFileSizeString(localPath?.length())
                    ?: context.getString(R.string.unknown),
                when (downloadStatus) {
                    DownloadStatus.DOWNLOADING -> DocumentUiStatus.DOWNLOADING
                    DownloadStatus.CONVERTING -> DocumentUiStatus.CONVERTING
                    DownloadStatus.SAVING -> DocumentUiStatus.SAVING
                    else -> if (localPath?.exists() == true) DocumentUiStatus.AVAILABLE else DocumentUiStatus.MISSING
                }
            )
        }
    }

    val emails: LiveData<List<String>> =
        emailRepository.allEmails.map { it.map { emailEntity -> emailEntity.emailAddress } }
            .asLiveData()

    private val _clipboardUrl: MutableLiveData<String> = MutableLiveData()
    val clipboardUrl: LiveData<String> = _clipboardUrl
    fun setClipboardUrl(url: String?) {
        if (URLUtil.isValidUrl(url) && hasValidExtension(url)) {
            _clipboardUrl.value = url
        }
    }

    fun add(uri: Uri) {
        viewModelScope.launch { documentRepository.add(uri) }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch { documentRepository.deleteDocument(id) }
    }

    fun getDocument(id: String): LiveData<Document> {
        return liveData { emit(documentRepository.getDocument(id)!!) }
    }

    fun refresh(id: String) {
        viewModelScope.launch { documentRepository.getDocument(id)?.let { add(it.sourcePath) } }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch { documentRepository.cancelDownload(id) }
    }

    fun markAsSent(id: String) {
        viewModelScope.launch { documentRepository.markAsSent(id) }
    }

    companion object {
        @OptIn(ExperimentalTime::class)
        @VisibleForTesting
        fun toRelativeDuration(elapsedTimeInMillis: Long): String {
            val elapsedDurationInMillis = elapsedTimeInMillis.toDuration(DurationUnit.MILLISECONDS)

            val momentsTill = 59.toDuration(DurationUnit.SECONDS)
            val minuteTill = 119.toDuration(DurationUnit.SECONDS)
            val minutesTill = 55.toDuration(DurationUnit.MINUTES)
            val hourTill = 90.toDuration(DurationUnit.MINUTES)
            val hoursTill =
                1.toDuration(DurationUnit.DAYS).minus(1.toDuration(DurationUnit.MILLISECONDS))
            val dayTill =
                2.toDuration(DurationUnit.DAYS).minus(1.toDuration(DurationUnit.MILLISECONDS))
            val daysTill = 60.toDuration(DurationUnit.DAYS)
            return when (elapsedDurationInMillis) {
                in Duration.ZERO..momentsTill -> "a few moments ago"
                in momentsTill..minuteTill -> "about a minute ago"
                in minuteTill..minutesTill -> "about ${elapsedDurationInMillis.inMinutes.roundToInt()} minutes ago"
                in minutesTill..hourTill -> "about an hour ago"
                in hourTill..hoursTill -> "about ${elapsedDurationInMillis.inHours.roundToInt()} hours ago"
                in hoursTill..dayTill -> "about a day ago"
                in dayTill..daysTill -> "about ${elapsedDurationInMillis.inDays.roundToInt()} days ago"
                else -> "quite some time ago"
            }
        }

        private fun toFileSizeString(bytes: Long?): String? {
            return bytes?.let {
                when {
                    bytes > 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
                    bytes > 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
                    bytes > 1_000 -> "%.2f kB".format(bytes / 1_000.0)
                    else -> "$bytes bytes"
                }
            }
        }

        private fun hasValidExtension(url: String?): Boolean {
            return true
        }
    }
}