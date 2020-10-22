package com.pawanhegde.tokindle.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pawanhegde.tokindle.R
import com.pawanhegde.tokindle.databinding.SettingsBottomSheetBinding

class SettingsBottomSheetFragment : BottomSheetDialogFragment() {
    private val arguments: SettingsBottomSheetFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
        with(SettingsBottomSheetBinding.inflate(LayoutInflater.from(context), container, false)) {
            settingsBottomSheetHeader.text = arguments.emailId
            settingsBottomSheetOption.apply {
                text = getString(R.string.delete)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_delete_24, 0, 0, 0)
                setOnClickListener {
                    viewModel.deleteEmail(arguments.emailId)
                    dismiss()
                }
            }
            return root
        }
    }
}