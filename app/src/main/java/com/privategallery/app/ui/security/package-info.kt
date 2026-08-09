/**
 * Reserved for future security-setup-specific screens if the flow
 * grows beyond what SecuritySettingsScreen (ui/settings) covers.
 * Currently, PIN/password/pattern/biometric configuration lives in
 * ui/settings/SecuritySettingsScreen.kt since it is reached only from
 * Settings (STEP 62) and shares SettingsViewModel — keeping it there
 * avoided a second ViewModel for the same preference reads/writes
 * (RULE C, RULE N).
 */
package com.privategallery.app.ui.security
