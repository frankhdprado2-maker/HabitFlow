package com.unmsm.habitflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.unmsm.habitflow.BuildConfig
import com.unmsm.habitflow.ui.components.ClayCard
import com.unmsm.habitflow.ui.components.FormField
import com.unmsm.habitflow.ui.components.PrimaryAction
import com.unmsm.habitflow.ui.components.VerticalSpacer
import com.unmsm.habitflow.ui.viewmodel.LoginViewModel
import com.unmsm.habitflow.ui.viewmodel.ProfileSetupViewModel
import com.unmsm.habitflow.ui.viewmodel.RegisterViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(padding: PaddingValues, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(350)
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
    onProfileSetup: () -> Unit,
    onRegister: () -> Unit,
    onRecover: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                viewModel.showError("Google no devolvio token. Revisa el Web Client ID.")
            } else {
                viewModel.googleLogin(idToken)
            }
        } catch (error: ApiException) {
            viewModel.showError("Google fallo (${error.statusCode}). Revisa OAuth Android SHA-1.")
        } catch (error: Throwable) {
            viewModel.showError("No se pudo iniciar sesion con Google (${error.javaClass.simpleName}).")
        }
    }
    LaunchedEffect(state.loggedIn, state.needsProfile) {
        if (state.needsProfile) onProfileSetup()
        if (state.loggedIn) onLogin()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ClayCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
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
                        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                        if (webClientId.isBlank()) {
                            viewModel.showError("Falta configurar GOOGLE_WEB_CLIENT_ID en local.properties.")
                            return@Button
                        }
                        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestIdToken(webClientId)
                            .build()
                        val client = GoogleSignIn.getClient(context, options)
                        client.signOut().addOnCompleteListener {
                            googleLauncher.launch(client.signInIntent)
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
    }
}

@Composable
fun ProfileSetupScreen(
    padding: PaddingValues,
    onDone: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        ClayCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text("Como te llamas?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Guardaremos tu perfil para personalizar HabitFlow.")
                VerticalSpacer()
                FormField(state.name, "Nombre", viewModel::updateName)
                VerticalSpacer()
                FormField(state.username, "Username", viewModel::updateUsername)
                VerticalSpacer()
                FormField(state.goal, "Objetivo principal", viewModel::updateGoal)
                VerticalSpacer()
                Text("Avatar", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("avatar_lavender" to "LV", "avatar_mint" to "MT", "avatar_coral" to "CR").forEach { (key, label) ->
                        AssistChip(
                            onClick = { viewModel.updateAvatar(key) },
                            label = { Text(if (state.avatarKey == key) "$label OK" else label) }
                        )
                    }
                }
                VerticalSpacer()
                Text("Categorias", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Estudio", "Salud", "Bienestar").forEach { category ->
                        AssistChip(
                            onClick = { viewModel.toggleCategory(category) },
                            label = { Text(if (category in state.categories) "$category OK" else category) }
                        )
                    }
                }
                if (state.error != null) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                VerticalSpacer()
                PrimaryAction(if (state.loading) "Guardando..." else "Guardar perfil", viewModel::save)
            }
        }
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
        ClayCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
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
