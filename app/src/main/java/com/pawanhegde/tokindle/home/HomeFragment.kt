package com.pawanhegde.tokindle.home

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.pawanhegde.tokindle.R
import com.pawanhegde.tokindle.databinding.DocumentCardBinding
import com.pawanhegde.tokindle.databinding.HomeBinding
import com.pawanhegde.tokindle.model.DocumentUiModel
import com.pawanhegde.tokindle.model.DocumentUiStatus
import com.pawanhegde.tokindle.model.DocumentUiStatus.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val TAG = "HomeFragment"

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class HomeFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: HomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)
        binding = HomeBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        with(binding) {
            // Invisible until the data becomes available
            documents.visibility = INVISIBLE
            documents.adapter = DocumentsAdapter(onDocumentClick, emptyList())
            documents.layoutManager = LinearLayoutManager(context)
            viewModel.documents.observe(viewLifecycleOwner) {
                if (it.isEmpty()) {
                    noDocuments.visibility = VISIBLE
                    documents.visibility = INVISIBLE
                } else {
                    noDocuments.visibility = INVISIBLE
                    documents.visibility = VISIBLE
                }
                (documents.adapter as DocumentsAdapter).update(it)
            }

            initialiseFab(speedDial)
            optionHelp.setOnClickListener {
                val openHelp =
                    com.pawanhegde.tokindle.home.HomeFragmentDirections.actionHomeFragmentToSupportFragment()
                findNavController().navigate(openHelp)
            }
            optionSettings.setOnClickListener {
                val openSettings =
                    com.pawanhegde.tokindle.home.HomeFragmentDirections.actionHomeFragmentToSettingsFragment()
                findNavController().navigate(openSettings)
            }

            return root
        }
    }


    private fun getDisplayName(uri: Uri): String {
        val host = uri.host?.substringAfter("www.")

        return if (uri.pathSegments.isNotEmpty()) {
            "$host > ${uri.pathSegments.joinToString(separator = " > ")}"
        } else {
            uri.toString()
        }
    }

    private fun initialiseFab(speedDial: SpeedDialView) {
        val addUrl = SpeedDialActionItem.Builder(
            R.id.add_url,
            R.drawable.ic_baseline_link_24
        )
            .setFabSize(FloatingActionButton.SIZE_NORMAL)
            .setLabel(R.string.add_from_url)
            .create()

        val addFile = SpeedDialActionItem.Builder(
            R.id.add_document,
            R.drawable.ic_baseline_text_snippet_24
        )
            .setFabSize(FloatingActionButton.SIZE_NORMAL)
            .setFabBackgroundColor(Color.RED)
            .setFabImageTintColor(Color.WHITE)
            .setLabel(R.string.add_file)
            .create()

        // Register for activity result now. We'll launch this at the trigger of addFile
        val addFileAction =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) viewModel.add(uri)
            }

        viewModel.clipboardUrl.observe(viewLifecycleOwner) {
            val addClipboardUrl = SpeedDialActionItem.Builder(
                R.id.add_clipboard_url,
                R.drawable.ic_baseline_link_24
            )
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setLabel(getString(R.string.add_from_clipboard, getDisplayName(Uri.parse(it))))
                .create()

            speedDial.addActionItem(addClipboardUrl)
        }

        speedDial.addActionItem(addFile)
        speedDial.addActionItem(addUrl)

        speedDial.setOnActionSelectedListener {
            when (it.id) {
                R.id.add_document -> addFileAction.launch(SUPPORTED_MIMETYPES)
                R.id.add_clipboard_url -> viewModel.add(Uri.parse(viewModel.clipboardUrl.value))
                R.id.add_url -> onUrlAddClick()
            }

            speedDial.close()
            return@setOnActionSelectedListener true
        }
    }

    private val onUrlAddClick = {
        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToUrlDialog())
    }

    private val onDocumentClick = { id: String, status: DocumentUiStatus ->
        Log.i(TAG, "$id clicked. It has status $status")
        with(findNavController()) {
            if (currentDestination is FragmentNavigator.Destination
                && (currentDestination as FragmentNavigator.Destination).className == HomeFragment::class.java.name
            ) {
                navigate(
                    HomeFragmentDirections.actionHomeFragmentToHomeBottomSheetFragment(
                        id,
                        status
                    )
                )
            }
        }
    }

    companion object {
        // Source: https://www.amazon.com/gp/sendtokindle/email
        val SUPPORTED_MIMETYPES = arrayOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  // .doc & .docx
            "text/html",
            "application/rtf",
            "image/jpeg",
            "application/vnd.amazon.ebook",
            "application/x-mobipocket-ebook",
            "image/gif",
            "image/png",
            "image/bmp",
            "application/pdf",
            "text/plain",
        )
    }
}

class DocumentsAdapter(
    private val onClick: (String, DocumentUiStatus) -> Unit,
    private var documents: List<DocumentUiModel>
) : RecyclerView.Adapter<DocumentViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder =
        DocumentViewHolder(
            DocumentCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(documents[position], onClick)
    }

    override fun getItemCount(): Int = documents.size

    override fun getItemId(position: Int): Long {
        return this.documents[position].id.hashCode().toLong()
    }

    fun update(new: List<DocumentUiModel>) {
        val old = documents
        val diff = DiffUtil.calculateDiff(DiffCallback(old, new))
        diff.dispatchUpdatesTo(this)
        documents = new
    }

    inner class DiffCallback constructor(
        private val oldList: List<DocumentUiModel>,
        private val newList: List<DocumentUiModel>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old == new
        }
    }
}

class DocumentViewHolder(
    private val binding: DocumentCardBinding
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(document: DocumentUiModel, onClick: (String, DocumentUiStatus) -> Unit) {
        with(binding) {
            cardTitle.text = document.displayName
            itemView.setOnClickListener { onClick(document.id, document.documentUiStatus) }

            // Only one of subtitle1 and progressbar should show up at one time
            cardSubtitle1.visibility = when (document.documentUiStatus) {
                AVAILABLE, MISSING -> VISIBLE
                else -> GONE
            }
            cardProgressBar.visibility = if (cardSubtitle1.isVisible) GONE else VISIBLE


            when (document.documentUiStatus) {
                AVAILABLE -> {
                    cardSubtitle1.text = document.lastSentAt
                    cardSubtitle2.text = document.fileSize
                }
                MISSING -> {
                    cardSubtitle1.setText(R.string.download_missing)
                    cardSubtitle2.setText(R.string.try_refreshing)
                }
                DOWNLOADING -> {
                    cardSubtitle2.setText(R.string.downloading_from_internet)
                }
                CONVERTING -> {
                    cardSubtitle2.setText(R.string.converting)
                }
                SAVING -> {
                    cardSubtitle2.setText(R.string.saving_locally)
                }
            }
        }
    }
}
