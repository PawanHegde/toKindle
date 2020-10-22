package com.pawanhegde.tokindle.home

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.postDelayed
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.pawanhegde.tokindle.R
import com.pawanhegde.tokindle.databinding.UrlDialogBinding


private const val HTTPS_PREFIX = "https://"

class UrlDialog : DialogFragment() {
    private lateinit var binding: UrlDialogBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        binding = UrlDialogBinding.inflate(layoutInflater)

        val dialog = activity?.let {
            AlertDialog.Builder(it)
                .setView(binding.root)
                .setMessage(R.string.source)
                .setPositiveButton(R.string.add) { _, _ ->
                    viewModel.add(Uri.parse("$HTTPS_PREFIX${binding.urlText.text.toString()}"))
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .create()
        } ?: throw IllegalStateException("Did not find activity for the dialog")

        binding.urlText.requestFocus()
        binding.urlText.postDelayed(200) {
            getSystemService(requireContext(), InputMethodManager::class.java)?.showSoftInput(
                binding.urlText,
                SHOW_IMPLICIT,
            )
        }

        binding.urlText.addTextChangedListener { text: Editable? ->
            text?.let {
                val isValidUrl = Patterns.WEB_URL.matcher("$HTTPS_PREFIX$it").matches()
                binding.urlTextLayout.error =
                    if (isValidUrl) null else "This does not look like a valid URL"
            }
        }

        return dialog
    }
}