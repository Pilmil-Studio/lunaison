package fr.lunaison

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import fr.lunaison.ui.BiometricUnlock

/**
 * Empreinte ou visage par la demande du système. Seuls les capteurs de classe forte sont acceptés :
 * une reconnaissance faible (photo d'un visage) ne doit pas ouvrir des données de santé.
 * L'appli ne voit jamais l'empreinte : le système répond seulement « reconnu » ou non.
 */
class AndroidBiometricUnlock(private val activity: FragmentActivity) : BiometricUnlock {

    override val available: Boolean
        get() = BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    override fun authenticate(title: String, cancelLabel: String, onSuccess: () -> Unit) {
        if (!available || activity.isFinishing) return
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(cancelLabel)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }
}
