package com.pawanhegde.tokindle.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pawanhegde.tokindle.data.EmailRepository
import com.pawanhegde.tokindle.model.EmailUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val emailRepository: EmailRepository
) : ViewModel() {
    val emails: LiveData<List<EmailUiModel>> =
        emailRepository.allEmails.map { it.map { entity -> EmailUiModel(entity.emailAddress) } }
            .asLiveData()

    fun deleteEmail(emailId: String) = viewModelScope.launch {
        emailRepository.deleteEmail(emailId)
    }

    fun addEmail(emailId: String) = viewModelScope.launch {
        emailRepository.addEmail(emailId)
    }
}