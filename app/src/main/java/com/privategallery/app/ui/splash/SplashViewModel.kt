package com.privategallery.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.data.preferences.AppPreferences
import com.privategallery.app.data.preferences.AuthMethod
import com.privategallery.app.data.preferences.SecurityPreferences
import com.privategallery.app.data.preferences.SecurityScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * STEP 5 — SPLASH SCREEN
 *
 * Determines where the user should land after the short splash delay:
 *  - FIRST_TIME  -> Welcome (onboarding, STEP 6)
 *  - LOCKED      -> Lock screen (normal gallery lock enabled, STEP 46)
 *  - RETURNING   -> Home
 *
 * Reads real state now that the security foundation (STEP 45-47) and
 * AppPreferences (onboarding flag) exist.
 */
sealed interface SplashDestination {
    data object FirstTime : SplashDestination
    data object Locked : SplashDestination
    data object Returning : SplashDestination
}

class SplashViewModel(
    private val appPreferences: AppPreferences,
    private val securityPreferences: SecurityPreferences
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            delay(SPLASH_MIN_DURATION_MS)
            _destination.value = resolveStartDestination()
        }
    }

    private suspend fun resolveStartDestination(): SplashDestination {
        val onboardingComplete = appPreferences.isOnboardingComplete.first()
        if (!onboardingComplete) return SplashDestination.FirstTime

        val normalAuthMethod = securityPreferences.observeAuthMethod(SecurityScope.NORMAL_GALLERY).first()
        return if (normalAuthMethod != AuthMethod.NONE) {
            SplashDestination.Locked
        } else {
            SplashDestination.Returning
        }
    }

    private companion object {
        const val SPLASH_MIN_DURATION_MS = 600L
    }
}
