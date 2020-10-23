package com.pawanhegde.tokindle.settings

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pawanhegde.tokindle.R
import com.pawanhegde.tokindle.databinding.EmailCardBinding
import com.pawanhegde.tokindle.databinding.SettingsBinding
import com.pawanhegde.tokindle.model.EmailUiModel

private const val TAG = "SettingsFragment"

class SettingsFragment : Fragment() {
    private lateinit var viewModel: SettingsViewModel
    private lateinit var binding: SettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity()).get(SettingsViewModel::class.java)
        binding = SettingsBinding.inflate(layoutInflater)

        viewModel.emails.observe(viewLifecycleOwner) { emails ->
            binding.emailsRecycler.apply {
                if (emails.isNotEmpty()) {
                    visibility = VISIBLE
                    adapter = EmailsAdapter(emails, onEmailClick)
                    layoutManager = LinearLayoutManager(context)
                } else {
                    visibility = GONE
                }
            }
            binding.noEmails.apply {
                visibility = if (binding.emailsRecycler.visibility == GONE) VISIBLE else GONE
                movementMethod = LinkMovementMethod.getInstance()
                text = SpannableString(getString(R.string.no_emails)).apply {
                    setSpan(
                        object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                val action =
                                    SettingsFragmentDirections.actionSettingsFragmentToUsageGuideFragment()
                                findNavController().navigate(action)
                            }
                        },
                        107,
                        117,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    setSpan(
                        StyleSpan(Typeface.BOLD_ITALIC),
                        40,
                        76,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

        }

        binding.addEmail.setOnClickListener {
            val action = SettingsFragmentDirections.actionSettingsFragmentToEmailDialog()
            findNavController().navigate(action)
        }

        binding.done.setOnClickListener { findNavController().navigateUp() }

        return binding.root
    }

    private val onEmailClick: (EmailUiModel) -> Unit = {
        findNavController().navigate(
            SettingsFragmentDirections.actionSettingsFragmentToSettingsBottomSheetFragment(it.emailId)
        )
    }
}

class EmailsAdapter(
    private val emails: List<EmailUiModel>,
    private val onClick: (EmailUiModel) -> Unit
) : RecyclerView.Adapter<EmailViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailViewHolder =
        EmailViewHolder(
            EmailCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: EmailViewHolder, position: Int) =
        holder.bind(emails[position], onClick)

    override fun getItemCount(): Int = emails.size
}

class EmailViewHolder(private val binding: EmailCardBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(emailUiModel: EmailUiModel, onClick: (EmailUiModel) -> Unit) {
        binding.emailLabel.text = emailUiModel.emailId
        binding.emailLabel.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_outline_mail_outline_24,
            0,
            0,
            0
        )
        itemView.setOnClickListener { onClick(emailUiModel) }
    }
}
