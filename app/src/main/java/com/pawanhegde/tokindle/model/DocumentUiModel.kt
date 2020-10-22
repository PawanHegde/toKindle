package com.pawanhegde.tokindle.model

data class DocumentUiModel(
    val id: String,
    val displayName: String,
    val addedAt: String,
    val lastSentAt: String,
    val fileSize: String,
    val documentUiStatus: DocumentUiStatus
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentUiModel

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (addedAt != other.addedAt) return false
        if (lastSentAt != other.lastSentAt) return false
        if (fileSize != other.fileSize) return false
        if (documentUiStatus != other.documentUiStatus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + addedAt.hashCode()
        result = 31 * result + lastSentAt.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + documentUiStatus.hashCode()
        return result
    }
}

enum class DocumentUiStatus {
    AVAILABLE,
    MISSING,
    DOWNLOADING,
    CONVERTING,
    SAVING
}