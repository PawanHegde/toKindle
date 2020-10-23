package com.pawanhegde.tokindle.guide

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import com.pawanhegde.tokindle.databinding.FragmentUsageGuideBinding

class UsageGuideFragment : Fragment() {
    private lateinit var binding: FragmentUsageGuideBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUsageGuideBinding.inflate(inflater, container, false)

        binding.webview.apply {
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            webViewClient = WebViewClientCompat()
            loadUrl("https://pawanhegde.github.io/toKindle/usage-guide")
        }

        return binding.root

    }

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
}