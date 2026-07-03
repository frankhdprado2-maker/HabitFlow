package com.unmsm.habitflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.unmsm.habitflow.BuildConfig
import com.unmsm.habitflow.ui.components.FormField
import com.unmsm.habitflow.ui.components.PrimaryAction
import com.unmsm.habitflow.ui.components.VerticalSpacer
import com.unmsm.habitflow.ui.viewmodel.LoginViewModel
import com.unmsm.habitflow.ui.viewmodel.RegisterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(padding: PaddingValues, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(900)
        onDone()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("HabitFlow", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Habitos universitarios con voz e IA")
    }
}

@Composable
fun OnboardingScreen(padding: PaddingValues, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Organiza tu dia", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Crea rutinas de estudio, salud y productividad con recordatorios claros.")
            Text("No pierdas tu racha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("La app guarda avances offline y sincroniza cuando vuelve la conexion.")
            Text("Habla con HabitFlow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Di ya corri 30 minutos o pregunta que habitos faltan hoy.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Saltar") }
            Button(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Empezar") }
        }
    }
}

@Composable
fun LoginScreen(
    padding: PaddingValues,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onRecover: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLogin()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenido", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Inicia sesion para continuar tu racha.")
        VerticalSpacer()
        FormField(state.email, "Email", viewModel::updateEmail)
        VerticalSpacer()
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Contrasena") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.error != null) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        VerticalSpacer()
        PrimaryAction(if (state.loading) "Entrando..." else "Iniciar sesion", viewModel::login)
        Button(
            onClick = {
                scope.launch {
                    signInWithGoogle(
                        credentialManager = CredentialManager.create(context),
                        context = context,
                        onToken = viewModel::googleLogin,
                        onError = viewModel::showError
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Continuar con Google")
        }
        TextButton(onClick = onRecover) { Text("Recuperar contrasena") }
        TextButton(onClick = onRegister) { Text("Crear cuenta") }
    }
}

private suspend fun signInWithGoogle(
    credentialManager: CredentialManager,
    context: android.content.Context,
    onToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (webClientId.isBlank()) {
        onError("Falta configurar GOOGLE_WEB_CLIENT_ID en local.properties.")
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val credential = credentialManager.getCredential(context, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            onToken(googleCredential.idToken)
        } else {
            onError("No se pudo obtener el token de Google.")
        }
    } catch (error: GetCredentialException) {
        val reason = error.javaClass.simpleName.ifBlank { "GetCredentialException" }
        onError("Google no disponible ($reason). Revisa OAuth Android SHA-1.")
    } catch (error: Throwable) {
        val reason = error.javaClass.simpleName.ifBlank { "Error" }
        onError("No se pudo iniciar sesion con Google ($reason).")
    }
}

@Composable
fun RegisterScreen(
    padding: PaddingValues,
    onDone: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.registered) {
        if (state.registered) onDone()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Registro - paso ${state.step}/3", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        when (state.step) {
            1 -> {
                FormField(state.name, "Nombre completo", viewModel::updateName)
                VerticalSpacer()
                FormField(state.username, "Username", viewModel::updateUsername)
            }
            2 -> {
                FormField(state.email, "Email", viewModel::updateEmail)
                VerticalSpacer()
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Contrasena") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                Text("Cuenta: ${state.email}", style = MaterialTheme.typography.bodyMedium)
                VerticalSpacer()
                FormField(state.goal, "Objetivo principal", viewModel::updateGoal)
            }
        }
        if (state.error != null) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        VerticalSpacer()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = viewModel::previousStep, enabled = state.step > 1, modifier = Modifier.weight(1f)) { Text("Atras") }
            Button(
                onClick = { if (state.step < 3) viewModel.nextStep() else viewModel.register() },
                enabled = !state.loading,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.loading) "Creando..." else if (state.step < 3) "Siguiente" else "Crear") }
        }
    }
}

@Composable
fun RecoverPasswordScreen(padding: PaddingValues, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Recuperar contrasena", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Ingresa tu correo y te enviaremos un enlace de recuperacion.")
        VerticalSpacer()
        FormField("", "Email", {})
        VerticalSpacer()
        PrimaryAction("Enviar enlace", onDone)
    }
}

@Composable
fun VerifyEmailScreen(padding: PaddingValues, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Verifica tu email", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Codigo de 4 digitos")
        VerticalSpacer()
        FormField("", "1234", {})
        VerticalSpacer()
        PrimaryAction("Verificar", onDone)
    }
}
