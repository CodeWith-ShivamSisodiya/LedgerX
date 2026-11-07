package com.ledgerx.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.ledgerx.app.ui.LedgerXApp
import com.ledgerx.app.ui.LedgerXViewModel
import com.ledgerx.app.ui.theme.LedgerXTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: LedgerXViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            LedgerXTheme {
                Surface(Modifier.fillMaxSize()) {
                    LedgerXApp(viewModel)
                }
            }
        }
    }
}
