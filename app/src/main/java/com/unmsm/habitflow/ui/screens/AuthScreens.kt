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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unmsm.habitflow.ui.components.FormField
import com.unmsm.habitflow.ui.components.PrimaryAction
import com.unmsm.habitflow.ui.components.VerticalSpacer
import com.unmsm.habitflow.ui.viewmodel.LoginViewModel
import com.unmsm.habitflow.ui.viewmodel.RegisterViewModel
import kotlinx.coroutines.delay

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
        Text("Hábitos universitarios con voz e IA")
    }
}

@Composable
fun OnboardingScreen(padding: PaddingValues, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Organiza tu día", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Crea rutinas de estudio, salud y productividad con recordatorios claros.")
            Text("No pierdas tu racha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("La app guarda avances offline y sincroniza cuando vuelve la conexión.")
            Text("Habla con HabitFlow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Di “ya corrí 30 minutos” o pregunta qué hábitos faltan hoy.")
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
    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLogin()
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bienvenido", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Inicia sesión para continuar tu racha.")
        VerticalSpacer()
        FormField(state.email, "Email", viewModel::updateEmail)
        VerticalSpacer()
        androidx.compose.material3.OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        if (state.error != null) Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        VerticalSpacer()
        PrimaryAction(if (state.loading) "Entrando..." else "Iniciar sesión", viewModel::login)
        Button(onClick = { viewModel.googleLogin("TODO_WEB_ID_TOKEN") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Continuar con Google")
        }
        TextButton(onClick = onRecover) { Text("Recuperar contraseña") }
        TextButton(onClick = onRegister) { Text("Crear cuenta") }
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
        Text("Registro · paso ${state.step}/3", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        when (state.step) {
            1 -> {
                FormField(state.name, "Nombre completo", viewModel::updateName)
                VerticalSpacer()
                FormField(state.username, "Username", viewModel::updateUsername)
            }
            2 -> {
                FormField(state.email, "Email universitario", viewModel::updateEmail)
                VerticalSpacer()
                androidx.compose.material3.OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
            else -> FormField(state.goal, "Objetivo principal", viewModel::updateGoal)
        }
        if (state.error != null) Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        VerticalSpacer()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = viewModel::previousStep, enabled = state.step > 1, modifier = Modifier.weight(1f)) { Text("Atrás") }
            Button(
                onClick = { if (state.step < 3) viewModel.nextStep() else viewModel.register() },
                modifier = Modifier.weight(1f)
            ) { Text(if (state.step < 3) "Siguiente" else "Crear") }
        }
    }
}

@Composable
fun RecoverPasswordScreen(padding: PaddingValues, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Ingresa tu correo y te enviaremos un enlace de recuperación.")
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
        Text("Código de 4 dígitos")
        VerticalSpacer()
        FormField("", "1234", {})
        VerticalSpacer()
        PrimaryAction("Verificar", onDone)
    }
}
