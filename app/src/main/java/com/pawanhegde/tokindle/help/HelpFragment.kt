package com.pawanhegde.tokindle.help

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import com.pawanhegde.tokindle.databinding.HelpBinding

class HelpFragment : Fragment() {
    private lateinit var binding: HelpBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            val shouldForceDark =
                when (context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                    Configuration.UI_MODE_NIGHT_YES -> true
                    Configuration.UI_MODE_NIGHT_NO -> false
                    else -> false
                }
            if (shouldForceDark) {
                WebSettingsCompat.setForceDark(
                    binding.webview.settings,
                    WebSettingsCompat.FORCE_DARK_ON
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = HelpBinding.inflate(layoutInflater, container, false)
        with(binding) {
            close.setOnClickListener { findNavController().popBackStack() }
            webview.apply {
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                setBackgroundColor(Color.TRANSPARENT)
                webViewClient = WebViewClientCompat()
                loadUrl("https://pawanhegde.github.io/toKindle/usage-guide")
            }

            return root
        }
    }
}