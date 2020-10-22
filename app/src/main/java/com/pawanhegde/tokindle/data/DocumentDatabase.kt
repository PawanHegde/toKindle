package com.pawanhegde.tokindle.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pawanhegde.tokindle.model.Document

@Database(entities = [Document::class], exportSchema = false, version = 1)
@TypeConverters(Converters::class)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun getDocumentDao(): DocumentDao
}