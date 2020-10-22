package com.pawanhegde.tokindle.data

import android.net.Uri
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.io.File

@TypeConverters
class Converters {
    @TypeConverter
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun stringToUri(string: String?): Uri? {
        return string?.let { Uri.parse(string) }
    }

    @TypeConverter
    fun fileToString(file: File?): String? {
        return file?.toString()
    }

    @TypeConverter
    fun stringToFile(filePath: String?): File? {
        return filePath?.let { File(filePath) }
    }
}