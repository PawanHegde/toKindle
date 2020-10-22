package com.pawanhegde.tokindle.settings

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.postDelayed
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.pawanhegde.tokindle.R
import com.pawanhegde.tokindle.databinding.EmailDialogBinding

class EmailDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
        val binding = EmailDialogBinding.inflate(layoutInflater)

        val dialog = activity?.let {
            AlertDialog.Builder(it)
                .setView(binding.root)
                .setMessage(R.string.email_dialog_message)
                .setPositiveButton(R.string.add) { _, _ ->
                    viewModel.addEmail(binding.emailText.text.toString())
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .create()
        } ?: throw IllegalStateException("Did not find activity for the dialog")

        binding.emailText.requestFocus()
        binding.emailText.postDelayed(200) {
            ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                ?.showSoftInput(
                    binding.emailText,
                    InputMethodManager.SHOW_IMPLICIT,
                )
        }

        binding.emailText.addTextChangedListener { text: Editable? ->
            text?.let {
                val isValidEmail = Patterns.EMAIL_ADDRESS.matcher("$it@kindle.com").matches()
                binding.emailTextLayout.error =
                    if (isValidEmail) null else "This does not look like a valid email id"
            }
        }

        return dialog
    }
}