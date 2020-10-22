package com.pawanhegde.tokindle.data

import com.pawanhegde.tokindle.model.EmailEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailRepository @Inject constructor(private val emailDao: EmailDao) {
    val allEmails = emailDao.getAllEmails()

    suspend fun addEmail(emailAddress: String) {
        emailDao.insertEmail(EmailEntity(emailAddress))
    }

    suspend fun deleteEmail(emailAddress: String) {
        emailDao.deleteEmail(EmailEntity(emailAddress))
    }
}