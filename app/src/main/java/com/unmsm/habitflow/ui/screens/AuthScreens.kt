package com.unmsm.habitflow.ui.screens

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.unmsm.habitflow.BuildConfig
import com.unmsm.habitflow.R
import com.unmsm.habitflow.ui.components.HabitFlowAvatar
import com.unmsm.habitflow.ui.components.HabitFlowCard
import com.unmsm.habitflow.ui.components.HabitFlowCategoryChip
import com.unmsm.habitflow.ui.components.HabitFlowOutlinedField
import com.unmsm.habitflow.ui.components.HabitFlowPrimaryButton
import com.unmsm.habitflow.ui.components.HabitFlowSecondaryButton
import com.unmsm.habitflow.ui.components.HabitFlowTextButton
import com.unmsm.habitflow.ui.theme.HabitFlowAccent
import com.unmsm.habitflow.ui.theme.HabitFlowShapes
import com.unmsm.habitflow.ui.theme.HabitFlowSpacing
import com.unmsm.habitflow.ui.viewmodel.LoginViewModel
import com.unmsm.habitflow.ui.viewmodel.ProfileSetupViewModel
import com.unmsm.habitflow.ui.viewmodel.RegisterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(padding: PaddingValues, onDone: suspend () -> Unit) {
    LaunchedEffect(Unit) {
        delay(350)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HabitFlowMark(Modifier.size(112.dp))
            Text("HabitFlow", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Rutinas claras, aun cuando tu día no lo sea.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OnboardingScreen(padding: PaddingValues, onFinish: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                HabitFlowMark(Modifier.size(148.dp))
                Text("HabitFlow", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    "Organiza hábitos de estudio, salud y productividad con una app local-first, cálida y fácil de sostener.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HabitFlowCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tu rutina empieza pequeña", style = MaterialTheme.typography.titleMedium)
                        Text("El primer inicio de sesión te guiará para elegir objetivos, hábitos iniciales, apariencia y recordatorios.")
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitFlowPrimaryButton("Comenzar", onFinish)
                HabitFlowTextButton("Ya tengo cuenta", onFinish, modifier = Modifier.fillMaxWidth())
            }
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
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()
    val missingWebClientMessage = stringResource(R.string.google_missing_web_client_id)
    var passwordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.loggedIn, state.needsProfile) {
        if (state.needsProfile) onProfileSetup()
        if (state.loggedIn) onLogin()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitFlowMark(Modifier.size(104.dp))
                Text("Bienvenido a HabitFlow", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Retoma tu rutina con calma. Tus hábitos locales se mantienen visibles incluso si la conexión falla.")
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitFlowOutlinedField(
                    value = state.email,
                    label = "Correo electrónico",
                    onValueChange = viewModel::updateEmail,
                    leadingIcon = Icons.Default.Email,
                    isError = state.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                HabitFlowOutlinedField(
                    value = state.password,
                    label = "Contraseña",
                    onValueChange = viewModel::updatePassword,
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = state.error != null,
                    supportingText = state.error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.login() })
                )
                HabitFlowPrimaryButton(
                    label = if (state.loading) "Iniciando..." else "Iniciar sesión",
                    onClick = viewModel::login,
                    loading = state.loading
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(1.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
                    Text("o continúa con", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(1.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant))
                }
                HabitFlowSecondaryButton(
                    label = stringResource(R.string.continue_with_google),
                    onClick = {
                        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                        if (webClientId.isBlank()) {
                            viewModel.showError(missingWebClientMessage)
                            return@HabitFlowSecondaryButton
                        }
                        viewModel.beginExternalLogin()
                        coroutineScope.launch {
                            runGoogleCredentialFlow(
                                credentialManager = credentialManager,
                                context = context,
                                webClientId = webClientId,
                                onToken = viewModel::googleLogin,
                                onError = viewModel::showError
                            )
                        }
                    }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onRecover) { Text("Recuperar contraseña") }
                    TextButton(onClick = onRegister) { Text("Crear cuenta") }
                }
            }
        }
    }
}

private suspend fun runGoogleCredentialFlow(
    credentialManager: CredentialManager,
    context: Context,
    webClientId: String,
    onToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            onError("Google no devolvio una credencial compatible.")
            return
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleCredential.idToken
        if (idToken.isBlank()) {
            onError("Google no devolvio token. Revisa el Web Client ID.")
            return
        }
        onToken(idToken)
    } catch (_: GetCredentialCancellationException) {
        onError("Inicio con Google cancelado.")
    } catch (_: NoCredentialException) {
        onError("No hay cuentas de Google disponibles en este dispositivo.")
    } catch (_: GoogleIdTokenParsingException) {
        onError("Google devolvio un token que no se pudo leer.")
    } catch (error: GetCredentialException) {
        onError("No se pudo abrir Google (${error.type}).")
    } catch (error: Throwable) {
        onError("No se pudo iniciar sesion con Google (${error.javaClass.simpleName}).")
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Personaliza HabitFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { state.step / state.maxSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(HabitFlowShapes.Pill))
                )
                Text("Paso ${state.step} de ${state.maxSteps}", style = MaterialTheme.typography.labelMedium)
            }
        }
        item {
            when (state.step) {
                1 -> WelcomeStep()
                2 -> ProfileStep(state = state, viewModel = viewModel)
                3 -> CategoryStep(state = state, viewModel = viewModel)
                4 -> StarterHabitsStep(state = state, viewModel = viewModel)
                5 -> AppearanceStep(state = state, viewModel = viewModel)
                else -> SummaryStep(state = state)
            }
        }
        if (state.error != null) {
            item { Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitFlowSecondaryButton(
                    label = if (state.step == 1) "Ahora no" else "Atrás",
                    onClick = { if (state.step == 1) onDone() else viewModel.previousStep() },
                    modifier = Modifier.weight(1f),
                    icon = if (state.step == 1) null else Icons.Default.ArrowBack
                )
                HabitFlowPrimaryButton(
                    label = if (state.step == state.maxSteps) "Empezar mi rutina" else "Continuar",
                    onClick = { if (state.step == state.maxSteps) viewModel.save() else viewModel.nextStep() },
                    modifier = Modifier.weight(1f),
                    loading = state.loading,
                    icon = if (state.step == state.maxSteps) null else Icons.Default.ArrowForward
                )
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
    var passwordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.registered) {
        if (state.registered) onDone()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Te tomará menos de un minuto. Después podrás personalizar tu rutina.")
        }
        item {
            HabitFlowCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paso ${state.step}/3", style = MaterialTheme.typography.titleMedium)
                    when (state.step) {
                        1 -> {
                            HabitFlowOutlinedField(state.name, "Nombre completo", viewModel::updateName, leadingIcon = Icons.Default.Person)
                            HabitFlowOutlinedField(state.username, "Username", viewModel::updateUsername, leadingIcon = Icons.Default.AccountCircle)
                        }
                        2 -> {
                            HabitFlowOutlinedField(
                                state.email,
                                "Correo electrónico",
                                viewModel::updateEmail,
                                leadingIcon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                            )
                            HabitFlowOutlinedField(
                                state.password,
                                "Contraseña",
                                viewModel::updatePassword,
                                leadingIcon = Icons.Default.Lock,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                            )
                        }
                        else -> {
                            Text("Cuenta: ${state.email}", style = MaterialTheme.typography.bodyMedium)
                            HabitFlowOutlinedField(state.goal, "Objetivo principal", viewModel::updateGoal, leadingIcon = Icons.Default.Flag)
                        }
                    }
                    if (state.error != null) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HabitFlowSecondaryButton("Atrás", viewModel::previousStep, enabled = state.step > 1, modifier = Modifier.weight(1f))
                        HabitFlowPrimaryButton(
                            label = if (state.loading) "Creando..." else if (state.step < 3) "Siguiente" else "Crear",
                            onClick = { if (state.step < 3) viewModel.nextStep() else viewModel.register() },
                            loading = state.loading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecoverPasswordScreen(padding: PaddingValues, onDone: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Recuperar contraseña", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Ingresa tu correo y te enviaremos un enlace de recuperación.")
        }
        item { HabitFlowOutlinedField("", "Correo electrónico", {}, leadingIcon = Icons.Default.Email) }
        item { HabitFlowPrimaryButton("Enviar enlace", onDone) }
    }
}

@Composable
fun VerifyEmailScreen(padding: PaddingValues, onDone: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Verifica tu correo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Ingresa el código de 4 dígitos que enviamos a tu correo.")
        }
        item { HabitFlowOutlinedField("", "Código", {}) }
        item { HabitFlowPrimaryButton("Verificar", onDone) }
    }
}

@Composable
private fun WelcomeStep() {
    HabitFlowCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            HabitFlowMark(Modifier.size(136.dp))
            Text("Una rutina hecha a tu medida", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Elegiremos objetivos, hábitos iniciales, tema y recordatorios. Puedes ajustar todo después desde Perfil.")
        }
    }
}

@Composable
private fun ProfileStep(state: com.unmsm.habitflow.ui.state.ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HabitFlowAvatar(name = state.name.ifBlank { "HabitFlow" }, avatarKey = state.avatarKey, size = 82)
        HabitFlowOutlinedField(state.name, "Nombre visible", viewModel::updateName, leadingIcon = Icons.Default.Person)
        HabitFlowOutlinedField(state.username, "Username", viewModel::updateUsername, leadingIcon = Icons.Default.AccountCircle)
        HabitFlowOutlinedField(state.goal, "Objetivo principal", viewModel::updateGoal, leadingIcon = Icons.Default.Flag)
        HabitFlowOutlinedField(state.bio, "Descripción breve (opcional)", viewModel::updateBio, singleLine = false)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("avatar_lavender" to "Lavanda", "avatar_mint" to "Menta", "avatar_coral" to "Coral", "avatar_amber" to "Ámbar")) { item ->
                HabitFlowCategoryChip(item.second, selected = state.avatarKey == item.first, onClick = { viewModel.updateAvatar(item.first) })
            }
        }
    }
}

@Composable
private fun CategoryStep(state: com.unmsm.habitflow.ui.state.ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Elige tus categorías", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Usaremos esto para sugerir hábitos y ordenar tu progreso.")
        val categories = listOf("Estudio", "Salud", "Ejercicio", "Lectura", "Sueño", "Productividad", "Bienestar", "Finanzas personales", "Personalizado")
        categories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    HabitFlowCategoryChip(category, selected = category in state.categories, onClick = { viewModel.toggleCategory(category) })
                }
            }
        }
    }
}

@Composable
private fun StarterHabitsStep(state: com.unmsm.habitflow.ui.state.ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Elige de uno a tres hábitos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Podrás editarlos después. La idea es empezar con algo sostenible.")
        val templates = listOf(
            "Estudiar 25 minutos",
            "Leer 20 minutos",
            "Beber agua",
            "Dormir temprano",
            "Caminar o correr",
            "Preparar tareas del día",
            "Meditar"
        )
        templates.forEach { template ->
            HabitFlowCard(
                onClick = { viewModel.toggleTemplate(template) },
                containerColor = if (template in state.selectedTemplates) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(template, style = MaterialTheme.typography.titleMedium)
                    Text(if (template in state.selectedTemplates) "Elegido" else "Agregar", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AppearanceStep(state: com.unmsm.habitflow.ui.state.ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Apariencia y recordatorios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Modo oscuro")
            Switch(checked = state.darkMode, onCheckedChange = viewModel::toggleDarkMode)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recordatorios")
            Switch(checked = state.remindersEnabled, onCheckedChange = viewModel::toggleReminders)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Respuesta hablada")
            Switch(checked = state.voiceResponseEnabled, onCheckedChange = viewModel::toggleVoiceResponse)
        }
        Text("Color de acento", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(HabitFlowAccent.entries) { accent ->
                HabitFlowCategoryChip(accent.label, selected = state.accentColor == accent.key, onClick = { viewModel.setAccentColor(accent.key) })
            }
        }
    }
}

@Composable
private fun SummaryStep(state: com.unmsm.habitflow.ui.state.ProfileSetupUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Resumen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        HabitFlowCard {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HabitFlowAvatar(name = state.name, avatarKey = state.avatarKey, size = 58)
                    Column {
                        Text(state.name.ifBlank { "Estudiante" }, style = MaterialTheme.typography.titleMedium)
                        Text("@${state.username.ifBlank { "usuario" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Objetivo: ${state.goal.ifBlank { "Ser constante" }}")
                Text("Hábitos: ${state.selectedTemplates.joinToString(", ")}")
                Text("Acento: ${HabitFlowAccent.entries.firstOrNull { it.key == state.accentColor }?.label ?: "Menta"}")
            }
        }
    }
}

@Composable
private fun HabitFlowMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(primary.copy(alpha = 0.15f), radius = size.minDimension * 0.46f, center = center)
            drawCircle(secondary.copy(alpha = 0.22f), radius = size.minDimension * 0.19f, center = Offset(size.width * 0.68f, size.height * 0.28f))
            drawLine(
                color = primary,
                start = Offset(size.width * 0.28f, size.height * 0.58f),
                end = Offset(size.width * 0.48f, size.height * 0.40f),
                strokeWidth = size.minDimension * 0.08f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tertiary,
                start = Offset(size.width * 0.48f, size.height * 0.40f),
                end = Offset(size.width * 0.74f, size.height * 0.60f),
                strokeWidth = size.minDimension * 0.08f,
                cap = StrokeCap.Round
            )
        }
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text("HF", fontWeight = FontWeight.Bold, color = primary)
        }
    }
}
