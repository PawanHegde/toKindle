package com.pawanhegde.tokindle

import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pawanhegde.tokindle.databinding.ActivityMainBinding
import com.pawanhegde.tokindle.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
//        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        setContentView(binding.root)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val extraText = intent.extras?.get(Intent.EXTRA_TEXT) as String?
                val clipDataUri = intent.extras?.get(Intent.EXTRA_STREAM) as Uri?
                if (extraText != null && URLUtil.isValidUrl(extraText)) {
                    homeViewModel.add(Uri.parse(extraText))
                } else if (clipDataUri != null) {
                    homeViewModel.add(clipDataUri)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            // TODO: Add support for HTML text and URIs on the clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == true) {
                homeViewModel.setClipboardUrl(clipboard.primaryClip?.getItemAt(0)?.text.toString())
            }
        }
    }
}