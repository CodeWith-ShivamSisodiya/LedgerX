package com.ledgerx.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ledgerx.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import android.Manifest
import android.content.pm.PackageManager

class OnboardingNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun privacyThemeAndAccountStepsAreReachableOffline() {
        compose.onNodeWithText("LedgerX").assertIsDisplayed()
        compose.onNodeWithText("Get started").performClick()
        compose.onNodeWithText("Choose your appearance").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Create your first account").assertIsDisplayed()
    }

    @Test fun offlineEditionHasNoInternetPermission() {
        val activity = compose.activity
        assertEquals(PackageManager.PERMISSION_DENIED, activity.packageManager.checkPermission(Manifest.permission.INTERNET, activity.packageName))
    }
}
