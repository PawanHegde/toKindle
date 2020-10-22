package com.pawanhegde.tokindle.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emails")
data class EmailEntity(@PrimaryKey val emailAddress: String)