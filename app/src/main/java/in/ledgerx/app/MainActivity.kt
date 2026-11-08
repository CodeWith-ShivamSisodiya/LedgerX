package com.ledgerx.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import com.ledgerx.app.ui.LedgerXApp
import com.ledgerx.app.ui.LedgerXViewModel
import com.ledgerx.app.ui.theme.LedgerXTheme
import com.ledgerx.app.core.model.MoneyDisplayConfig

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: LedgerXViewModel by viewModels()
    private var sessionUnlocked by mutableStateOf(false)
    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val textScale by viewModel.textScale.collectAsState()
            val localeTag by viewModel.localeTag.collectAsState()
            MoneyDisplayConfig.setLocaleTag(localeTag)
            LedgerXTheme(themeMode, textScale) {
                Surface(Modifier.fillMaxSize()) {
                    LedgerXApp(
                        viewModel = viewModel,
                        sessionUnlocked = sessionUnlocked,
                        deviceAuthenticationAvailable = canAuthenticate(),
                        onAuthenticate = ::authenticate,
                        onLocalDataDeleted = { finishAndRemoveTask() },
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sessionUnlocked = false
    }

    private fun canAuthenticate(): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    private fun authenticate() {
        if (!canAuthenticate()) return
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    sessionUnlocked = true
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LedgerX")
            .setSubtitle("Use biometrics or your device credential")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}
