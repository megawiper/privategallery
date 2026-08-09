package com.privategallery.app.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.privategallery.app.core.security.BiometricAuthHelper
import com.privategallery.app.core.security.BiometricAuthResult
import com.privategallery.app.core.ui.Dimens
import com.privategallery.app.core.ui.GalleryIcons
import com.privategallery.app.core.ui.components.GalleryPrimaryButton
import com.privategallery.app.data.preferences.AuthMethod

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    isPrivate: Boolean,
    onUnlocked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var credentialInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) onUnlocked()
    }

    LaunchedEffect(uiState.biometricEnabled, uiState.authMethod) {
        val activity = context as? FragmentActivity
        if (uiState.biometricEnabled && activity != null && BiometricAuthHelper.isBiometricAvailable(activity)) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = if (isPrivate) "Unlock Private Gallery" else "Unlock Gallery",
                subtitle = "Use your biometric credential",
                negativeButtonText = "Use ${uiState.authMethod.name.lowercase()} instead"
            ) { result ->
                when (result) {
                    is BiometricAuthResult.Success -> viewModel.onBiometricSuccess()
                    is BiometricAuthResult.Error -> viewModel.onBiometricError(result.message)
                    is BiometricAuthResult.Cancelled -> Unit
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceXL),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = GalleryIcons.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
            Text(
                text = if (isPrivate) "Private Gallery" else "Unlock Gallery",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.padding(top = Dimens.SpaceXL))

            when (uiState.authMethod) {
                AuthMethod.PIN, AuthMethod.PASSWORD, AuthMethod.PATTERN -> {
                    OutlinedTextField(
                        value = credentialInput,
                        onValueChange = { credentialInput = it },
                        label = { Text(if (uiState.authMethod == AuthMethod.PIN) "PIN" else "Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (uiState.authMethod == AuthMethod.PIN) {
                                KeyboardType.NumberPassword
                            } else {
                                KeyboardType.Password
                            }
                        ),
                        isError = uiState.errorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.padding(top = Dimens.SpaceL))
                    GalleryPrimaryButton(
                        text = "Unlock",
                        onClick = { viewModel.verifyCredential(credentialInput.toCharArray()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AuthMethod.BIOMETRIC -> {
                    Text(
                        text = "Waiting for biometric authentication…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AuthMethod.NONE -> {
                    LaunchedEffect(Unit) { onUnlocked() }
                }
            }
        }
    }
}
