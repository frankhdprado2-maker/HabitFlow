package com.unmsm.habitflow.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.ui.components.ClayCard
import com.unmsm.habitflow.ui.components.HabitRow
import com.unmsm.habitflow.ui.components.MetricTile
import com.unmsm.habitflow.ui.components.PrimaryAction
import com.unmsm.habitflow.ui.components.ProgressBar
import com.unmsm.habitflow.ui.components.SectionTitle
import com.unmsm.habitflow.ui.components.StatusBadge
import com.unmsm.habitflow.ui.components.VerticalSpacer
import com.unmsm.habitflow.ui.viewmodel.AchievementsViewModel
import com.unmsm.habitflow.ui.viewmodel.EditProfileViewModel
import com.unmsm.habitflow.ui.viewmodel.HabitDetailViewModel
import com.unmsm.habitflow.ui.viewmodel.HistoryViewModel
import com.unmsm.habitflow.ui.viewmodel.HomeViewModel
import com.unmsm.habitflow.ui.viewmodel.ManualHabitViewModel
import com.unmsm.habitflow.ui.viewmodel.NotificationsViewModel
import com.unmsm.habitflow.ui.viewmodel.ProfileViewModel
import com.unmsm.habitflow.ui.viewmodel.SettingsViewModel
import com.unmsm.habitflow.ui.viewmodel.StatsViewModel
import com.unmsm.habitflow.ui.viewmodel.VoiceViewModel
import java.util.Date

@Composable
fun HomeScreen(
    padding: PaddingValues,
    onHabit: (String) -> Unit,
    onVoice: () -> Unit,
    onManual: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Hola, ${state.userName}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Tu progreso de hoy: ${state.completedToday}/${state.habits.size} hábitos")
                }
                Row {
                    IconButton(onClick = onManual) { Icon(Icons.Default.Add, "Agregar") }
                    IconButton(onClick = onNotifications) { Icon(Icons.Default.Notifications, "Notificaciones") }
                    IconButton(onClick = onVoice) { Icon(Icons.Default.Mic, "Voz") }
                }
            }
            VerticalSpacer()
            LinearProgressIndicator(
                progress = { if (state.habits.isEmpty()) 0f else state.completedToday.toFloat() / state.habits.size },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp))
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Racha", "${state.streak} días", Modifier.weight(1f))
                MetricTile("Hoy", "${state.completedToday}/${state.habits.size}", Modifier.weight(1f))
            }
        }
        if (state.voiceResponse.isNotBlank()) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(14.dp)) {
                        Text(state.voiceText, fontWeight = FontWeight.SemiBold)
                        Text(state.voiceResponse)
                    }
                }
            }
        }
        item { SectionTitle("Hábitos de hoy") }
        if (state.habits.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aun no tienes habitos. Usa el microfono o crea uno manualmente.")
                    Button(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Text("Agregar habito manual") }
                }
            }
        }
        items(state.habits) { habit ->
            HabitRow(
                habit = habit,
                completed = habit.streak > 0,
                onMark = { viewModel.mark(habit) },
                onOpen = { onHabit(habit.id) }
            )
        }
    }
}

@Composable
fun HabitDetailScreen(padding: PaddingValues, viewModel: HabitDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val habit = state.habit
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(habit?.name ?: "Detalle de hábito", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("${habit?.category.orEmpty()} · ${habit?.frequency.orEmpty()}")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Racha", "${habit?.streak ?: 0}", Modifier.weight(1f))
                MetricTile("Mejor", "${habit?.bestStreak ?: 0}", Modifier.weight(1f))
                MetricTile("Mes", "${state.completionPercent}%", Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Heatmap del mes")
            Heatmap()
        }
        item {
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Agregar nota") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Button(onClick = viewModel::markToday, modifier = Modifier.weight(1f)) { Text("Marcar hoy") }
                Button(onClick = viewModel::addNote, modifier = Modifier.weight(1f)) { Text("Agregar nota") }
            }
        }
        item { SectionTitle("Notas recientes") }
        items(state.events) { event ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(event.note.ifBlank { event.habitName }, fontWeight = FontWeight.SemiBold)
                    Text(java.text.DateFormat.getDateTimeInstance().format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall)
                }
                StatusBadge(event.status)
            }
        }
    }
}

@Composable
fun StatsScreen(padding: PaddingValues, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Estadísticas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Racha actual", "${state.currentStreak}", Modifier.weight(1f))
                MetricTile("% del mes", "${state.monthPercent}%", Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Semana")
            Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                state.weekly.forEach { value ->
                    Box(Modifier.weight(1f).fillMaxHeight(value / 5f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
        item { SectionTitle("Cumplimiento por hábito") }
        items(state.habits) { habit -> ProgressBar(habit.name, (habit.streak.coerceAtMost(10) / 10f)) }
    }
}

@Composable
fun HistoryScreen(padding: PaddingValues, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Historial", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Completados", "Saltados").forEach { filter ->
                    AssistChip(onClick = { viewModel.setFilter(filter) }, label = { Text(filter) })
                }
            }
        }
        items(state.events) { event ->
            ClayCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.habitName, fontWeight = FontWeight.SemiBold)
                        Text(java.text.DateFormat.getDateInstance().format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall)
                    }
                    StatusBadge(event.status)
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(padding: PaddingValues, viewModel: NotificationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Notificaciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        items(state.notifications) { item ->
            ClayCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold)
                    Text(item.message)
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(padding: PaddingValues, onEdit: () -> Unit, onAchievements: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ClayCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.user.avatarKey?.takeLast(2)?.uppercase() ?: state.user.name.take(2).uppercase(), fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(state.user.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("@${state.user.username} - nivel ${state.user.level} - racha 0")
                        }
                    }
                    if (state.user.categories.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.user.categories.take(4).forEach { category ->
                                AssistChip(onClick = {}, label = { Text(category) })
                            }
                        }
                    }
                    Text(state.user.goal.ifBlank { "Ser constante" })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Button(onClick = onEdit) { Text("Editar perfil") }
                        Button(onClick = onAchievements) { Text("Logros") }
                    }
                }
            }
        }
        item { SectionTitle("Insignias") }
        items(state.achievements.filter { it.unlocked }) { Text("• ${it.title}: ${it.description}") }
        if (state.achievements.isEmpty()) {
            item { Text("Todavia no tienes insignias.") }
        }
        item { SectionTitle("Amigos activos") }
        if (state.friends.isEmpty()) {
            item { Text("Todavia no agregaste amigos.") }
        }
        items(state.friends) { friend -> Text("${friend.first}: racha ${friend.second}") }
    }
}

@Composable
fun EditProfileScreen(
    padding: PaddingValues,
    onDone: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Editar perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            ClayCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(state.name, viewModel::updateName, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(state.username, viewModel::updateUsername, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(state.goal, viewModel::updateGoal, label = { Text("Objetivo principal") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { SectionTitle("Avatar") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("avatar_lavender" to "LV", "avatar_mint" to "MT", "avatar_coral" to "CR", "avatar_amber" to "AM").forEach { (key, label) ->
                    AssistChip(
                        onClick = { viewModel.updateAvatar(key) },
                        label = { Text(if (state.avatarKey == key) "$label OK" else label) }
                    )
                }
            }
        }
        item { SectionTitle("Categorias") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Estudio", "Salud", "Bienestar", "Productividad").forEach { category ->
                    AssistChip(
                        onClick = { viewModel.toggleCategory(category) },
                        label = { Text(if (category in state.categories) "$category OK" else category) }
                    )
                }
            }
        }
        if (state.error != null) {
            item { Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item {
            PrimaryAction(if (state.loading) "Guardando..." else "Guardar perfil", viewModel::save)
        }
    }
}

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    onDelete: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Configuración", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SettingSwitch("Notificaciones", state.settings.notifications, viewModel::toggleNotifications)
        SettingSwitch("Modo oscuro", state.settings.darkMode, viewModel::toggleDarkMode)
        Text("Color de acento")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("violet" to "Violeta", "mint" to "Menta", "coral" to "Coral", "amber" to "Ambar").forEach { (key, label) ->
                AssistChip(
                    onClick = { viewModel.setAccentColor(key) },
                    label = { Text(if (state.settings.accentColor == key) "$label OK" else label) }
                )
            }
        }
        SettingSwitch("Biometría", state.settings.biometric, viewModel::toggleBiometric)
        SettingSwitch("Perfil público", state.settings.publicProfile, viewModel::togglePublicProfile)
        Text("Idioma: ${state.settings.language}")
        Button(onClick = {}) { Text("Cambiar contraseña") }
        Button(
            onClick = { viewModel.logout(onLogout) },
            enabled = !state.loggingOut
        ) { Text(if (state.loggingOut) "Cerrando..." else "Cerrar sesion") }
        TextButton(onClick = onDelete) { Text("Eliminar cuenta", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun AchievementsScreen(padding: PaddingValues, viewModel: AchievementsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Logros", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Nivel ${state.level} · ${state.xp} XP")
            LinearProgressIndicator(progress = { state.xp / 1000f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)))
        }
        items(state.achievements) { achievement ->
            ClayCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(achievement.title, fontWeight = FontWeight.SemiBold)
                        Text(if (achievement.unlocked) achievement.description else achievement.requirement)
                    }
                    Text(if (achievement.unlocked) "OK" else "${achievement.xp} XP")
                }
            }
        }
    }
}

@Composable
fun DeleteAccountScreen(padding: PaddingValues, onDeleted: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text("Eliminar cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Text("Perderás tus hábitos, historial, rachas, logros, archivos y datos de perfil.")
        VerticalSpacer()
        OutlinedTextField("", {}, label = { Text("Escribe ELIMINAR") }, modifier = Modifier.fillMaxWidth())
        VerticalSpacer()
        Button(onClick = onDeleted) { Text("Confirmar eliminación") }
    }
}

@Composable
fun VoiceScreen(
    padding: PaddingValues,
    onManual: () -> Unit,
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.toggleRecording()
        } else {
            viewModel.showError("Activa el permiso de microfono para dictar por voz.")
        }
    }
    val onMicClick = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.toggleRecording()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Voz", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            state.recording -> "Grabando..."
                            state.transcribing -> "Transcribiendo..."
                            else -> "Conversacion activa"
                        }
                    )
                }
                IconButton(onClick = onMicClick, modifier = Modifier.size(68.dp)) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Grabar voz",
                        modifier = Modifier.size(42.dp),
                        tint = if (state.recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (state.messages.isEmpty()) {
            item {
                ClayCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Prueba con una frase", fontWeight = FontWeight.SemiBold)
                        Text("Toca el microfono para grabar y vuelve a tocarlo para enviar.")
                        Text("Complete leer 20 paginas")
                        Text("Salte tomar agua")
                        Text("No pude estudiar algoritmos")
                    }
                }
            }
        }
        items(state.messages) { message ->
            Box(Modifier.fillMaxWidth(), contentAlignment = if (message.author == "user") Alignment.CenterEnd else Alignment.CenterStart) {
                ClayCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (message.author == "user") "Tu" else "HabitFlow", fontWeight = FontWeight.SemiBold)
                        Text(message.text)
                    }
                }
            }
        }
        if (state.response == "Procesando..." || state.response == "Transcribiendo...") {
            item { Text(state.response, color = MaterialTheme.colorScheme.tertiary) }
        }
        if (state.quickReplies.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.quickReplies) { reply ->
                        AssistChip(onClick = { viewModel.sendText(reply) }, label = { Text(reply) })
                    }
                }
            }
        }
        if (state.error != null) {
            item { Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Text("Registrar manualmente") }
        }
    }
}

@Composable
fun ManualHabitScreen(
    padding: PaddingValues,
    onDone: () -> Unit,
    viewModel: ManualHabitViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Registro manual", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Crea un habito sin usar dictado de voz.")
        }
        item { OutlinedTextField(state.name, viewModel::updateName, label = { Text("Nombre del habito") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(state.category, viewModel::updateCategory, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(state.frequency, viewModel::updateFrequency, label = { Text("Frecuencia") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(state.reminderTime, viewModel::updateReminderTime, label = { Text("Hora o nota") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        if (state.error != null) {
            item { Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(onClick = viewModel::save, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.loading) "Guardando..." else "Guardar habito")
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Heatmap() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(5) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { col ->
                    val active = (row + col) % 3 != 0
                    Box(
                        Modifier.size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}
