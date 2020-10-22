package com.pawanhegde.tokindle.data

import androidx.room.*
import com.pawanhegde.tokindle.model.EmailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails")
    fun getAllEmails(): Flow<List<EmailEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmail(email: EmailEntity)

    @Delete
    suspend fun deleteEmail(email: EmailEntity)
}
