package com.pawanhegde.tokindle.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.pawanhegde.tokindle.R
import com.pawanhegde.tokindle.databinding.HomeBottomSheetBinding
import com.pawanhegde.tokindle.databinding.HomeBottomSheetOptionBinding
import com.pawanhegde.tokindle.model.Document
import com.pawanhegde.tokindle.model.DocumentUiStatus.*
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import java.lang.IllegalStateException


@AndroidEntryPoint
class HomeBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var homeViewModel: HomeViewModel
    private val arguments: HomeBottomSheetFragmentArgs by navArgs()

    private val cancelOption = { document: Document ->
        BottomSheetOption(
            getString(R.string.cancel),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_cancel_24)
        ) {
            homeViewModel.cancelDownload(document.id)
            dismiss()
        }
    }

    private val deleteOption = { document: Document ->
        BottomSheetOption(
            getString(R.string.delete),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_delete_24)
        ) {
            homeViewModel.deleteDocument(document.id)
            dismiss()
        }
    }

    private val addEmailOption = {
        BottomSheetOption(
            getString(R.string.send_to_unknown),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_outline_settings_24)
        ) {
            Toast.makeText(requireContext(), R.string.add_first_email, Toast.LENGTH_LONG).show()
            val action =
                HomeBottomSheetFragmentDirections.actionHomeBottomSheetFragmentToSettingsFragment()
            findNavController().navigate(action)
        }
    }

    private val sendOption = { document: Document, email: String ->
        BottomSheetOption(
            getString(R.string.send_to, email),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_send_24)
        ) {
            document.localPath?.let {
                startActivity(
                    Intent(Intent.ACTION_SEND)
                        .setType("message/rfc822")
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf("$email@kindle.com"))
                        .putExtra(Intent.EXTRA_SUBJECT, "ToKindle")
                        .putExtra(
                            Intent.EXTRA_STREAM,
                            FileProvider.getUriForFile(
                                requireActivity(),
                                "com.pawanhegde.tokindle.provider",
                                document.localPath!!
                            )
                        )
                )
                homeViewModel.markAsSent(document.id)

            } ?: Toast.makeText(
                requireContext(),
                getString(R.string.download_missing),
                Toast.LENGTH_SHORT
            ).show()

            dismiss()
        }
    }

    private val refreshOption = { document: Document ->
        BottomSheetOption(
            getString(R.string.refresh),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_refresh_24)
        ) {
            if (document.isLocalContent()) {
                try {
                    val queryResults =
                        context?.contentResolver?.query(document.sourcePath, null, null, null, null)
                            ?.use {
                                it.count
                            }

                    if (queryResults != 1) {
                        throw IllegalStateException("Could not get the content metadata")
                    }
                } catch (e: Exception) {
                    dismiss()

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.content_non_fetchable),
                        Toast.LENGTH_LONG
                    ).show()

                    return@BottomSheetOption
                }
            }
            homeViewModel.refresh(document.id)
            dismiss()
        }
    }

    private val previewOption = { document: Document ->
        BottomSheetOption(
            getString(R.string.preview),
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_preview_24)
        ) {
            try {
                val authority = "com.pawanhegde.tokindle.provider"
                val data = FileProvider.getUriForFile(
                    requireActivity(),
                    authority,
                    document.localPath!!
                )
                val intent = Intent(Intent.ACTION_VIEW)
                    .setData(data)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    getString(R.string.no_activity_for_preview),
                    Toast.LENGTH_SHORT
                ).show()
            }
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        with(HomeBottomSheetBinding.inflate(inflater, container, false)) {
            homeViewModel.emails.observe(viewLifecycleOwner) { emails ->
                homeViewModel.getDocument(arguments.selectedDocumentId)
                    .observe(viewLifecycleOwner) { document ->
                        val cancel = cancelOption(document)
                        val delete = deleteOption(document)
                        val preview = previewOption(document)
                        val refresh = refreshOption(document)
                        val send = emails.map { sendOption(document, it) }
                            .ifEmpty { listOf(addEmailOption()) }

                        val options: List<BottomSheetOption> = when (arguments.status) {
                            MISSING -> listOf(refresh, delete)
                            CONVERTING, DOWNLOADING, SAVING -> listOf(cancel)
                            AVAILABLE -> send.plus(listOf(preview, refresh, delete))
                        }

                        homeBottomSheetOptions.adapter = HomeBottomSheetOptionsAdapter(options)
                    }
                homeBottomSheetOptions.layoutManager = LinearLayoutManager(context)
            }

            homeViewModel.documents.observe(viewLifecycleOwner) { documents ->
                homeBottomSheetHeader.text =
                    documents.firstOrNull { doc -> doc.id == arguments.selectedDocumentId }?.displayName
            }

            return root
        }
    }
}

class HomeBottomSheetOptionsAdapter(
    private val bottomSheetOptions: List<BottomSheetOption>
) :
    RecyclerView.Adapter<HomeBottomSheetOptionsViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HomeBottomSheetOptionsViewHolder =
        HomeBottomSheetOptionsViewHolder(
            HomeBottomSheetOptionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )


    override fun onBindViewHolder(holder: HomeBottomSheetOptionsViewHolder, position: Int) {
        holder.bind(bottomSheetOptions[position])
    }

    override fun getItemCount(): Int = bottomSheetOptions.size
}

class HomeBottomSheetOptionsViewHolder(private val binding: HomeBottomSheetOptionBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(bottomSheetOption: BottomSheetOption) {
        with(binding) {
            with(bottomSheetOption) {
                homeBottomSheetOption.text = title
                homeBottomSheetOption.setOnClickListener(onClick)
                homeBottomSheetOption.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawable,
                    null,
                    null,
                    null
                )
            }
        }
    }
}

data class BottomSheetOption(
    val title: String,
    val drawable: Drawable?,
    val onClick: View.OnClickListener
)