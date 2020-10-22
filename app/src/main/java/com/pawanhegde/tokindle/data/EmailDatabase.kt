package com.pawanhegde.tokindle.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pawanhegde.tokindle.model.EmailEntity

@Database(entities = [EmailEntity::class], exportSchema = false, version = 1)
abstract class EmailDatabase : RoomDatabase() {
    abstract fun getEmailDao(): EmailDao
}