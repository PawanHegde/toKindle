package com.pawanhegde.tokindle.support

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pawanhegde.tokindle.databinding.SupportBinding

class SupportFragment : Fragment() {
    private lateinit var binding: SupportBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SupportBinding.inflate(layoutInflater, container, false)

        with(binding) {
            close.setOnClickListener { findNavController().popBackStack() }

            feedback.setOnClickListener {
                startActivity(
                    Intent(Intent.ACTION_SENDTO)
                        .setType("message/rfc822")
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf("tokindle@pawanhegde.com"))
                        .putExtra(Intent.EXTRA_SUBJECT, "Feedback about toKindle")
                )
            }

            usageGuide.setOnClickListener {
                val action = SupportFragmentDirections.actionSupportFragmentToUsageGuideFragment()
                findNavController().navigate(action)
            }
        }

        return binding.root
    }
}