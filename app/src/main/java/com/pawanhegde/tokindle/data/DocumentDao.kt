package com.pawanhegde.tokindle.data

import android.net.Uri
import androidx.room.*
import com.pawanhegde.tokindle.model.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query(value = "SELECT * FROM documents ORDER BY addedAt DESC")
    fun getAll(): Flow<List<Document>>

    @Query(value = "SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): Document?

    @Query(value = "SELECT * from documents WHERE sourcePath = :sourcePath")
    suspend fun getBySourcePath(sourcePath: Uri): Document?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: Document)

    @Update
    suspend fun update(document: Document): Int

    @Transaction
    suspend fun upsert(document: Document) {
        val rowsUpdated = update(document)
        if (rowsUpdated == 0) insert(document)
    }

    @Delete
    suspend fun delete(document: Document): Int

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String): Int
}