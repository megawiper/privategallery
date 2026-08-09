package com.privategallery.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.privategallery.app.core.navigation.GalleryNavHost
import com.privategallery.app.core.ui.PrivateGalleryTheme

/**
 * FragmentActivity (not plain ComponentActivity) because STEP 48's
 * BiometricPrompt requires a FragmentActivity/Fragment host.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as PrivateGalleryApp).container
        setContent {
            PrivateGalleryTheme {
                GalleryNavHost(appContainer = appContainer)
            }
        }
    }
}
