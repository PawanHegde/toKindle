package com.pawanhegde.tokindle.hilt

import android.content.Context
import androidx.room.Room
import com.pawanhegde.tokindle.data.DocumentDao
import com.pawanhegde.tokindle.data.DocumentDatabase
import com.pawanhegde.tokindle.data.EmailDao
import com.pawanhegde.tokindle.data.EmailDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun providesDatabase(@ApplicationContext context: Context): DocumentDatabase {
        return Room.databaseBuilder(context, DocumentDatabase::class.java, "to_kindle.db").build()
    }

    @Provides
    fun providesDocumentDao(documentDatabase: DocumentDatabase): DocumentDao {
        return documentDatabase.getDocumentDao()
    }

    @Provides
    fun providesEmailDatabase(@ApplicationContext context: Context): EmailDatabase {
        return Room.databaseBuilder(context, EmailDatabase::class.java, "to_kindle_emails.db")
            .build()
    }

    @Provides
    fun providesEmailDao(emailDatabase: EmailDatabase): EmailDao {
        return emailDatabase.getEmailDao()
    }
}