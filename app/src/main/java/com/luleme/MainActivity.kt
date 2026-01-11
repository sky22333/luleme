package com.luleme

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.luleme.ui.navigation.NavGraph
import com.luleme.ui.theme.LulemeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LulemeTheme {
                NavGraph()
            }
        }
    }
}
