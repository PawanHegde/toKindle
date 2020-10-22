package com.pawanhegde.tokindle.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "documents", indices = [Index(value = ["sourcePath"], unique = true)])
data class Document(
    @PrimaryKey
    var id: String,
    var displayName: String? = null,
    val sourcePath: Uri,
    var localPath: File? = null,
    val addedAt: Long = System.currentTimeMillis(),
    var lastSentAt: Long? = null,
) {
    fun isLocalContent(): Boolean {
        return sourcePath.scheme == "content"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Document

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (sourcePath != other.sourcePath) return false
        if (localPath != other.localPath) return false
        if (addedAt != other.addedAt) return false
        if (lastSentAt != other.lastSentAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + sourcePath.hashCode()
        result = 31 * result + (localPath?.hashCode() ?: 0)
        result = 31 * result + addedAt.hashCode()
        result = 31 * result + (lastSentAt?.hashCode() ?: 0)
        return result
    }


}