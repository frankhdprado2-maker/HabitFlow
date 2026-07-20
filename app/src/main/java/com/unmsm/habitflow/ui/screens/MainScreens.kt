package com.unmsm.habitflow.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.domain.habit.HabitHeatmap
import com.unmsm.habitflow.domain.habit.HeatmapDayState
import com.unmsm.habitflow.domain.habit.HabitFrequencyType
import com.unmsm.habitflow.domain.habit.AggregationMode
import com.unmsm.habitflow.domain.habit.MeasurementType
import com.unmsm.habitflow.ui.components.ClayCard
import com.unmsm.habitflow.ui.components.HabitFlowAvatar
import com.unmsm.habitflow.ui.components.HabitFlowCategoryChip
import com.unmsm.habitflow.ui.components.HabitFlowEmptyState
import com.unmsm.habitflow.ui.components.HabitFlowHabitCard
import com.unmsm.habitflow.ui.components.HabitFlowMessageBubble
import com.unmsm.habitflow.ui.components.HabitFlowMetricCard
import com.unmsm.habitflow.ui.components.HabitFlowOutlinedField
import com.unmsm.habitflow.ui.components.HabitFlowPrimaryButton
import com.unmsm.habitflow.ui.components.HabitFlowProgressRing
import com.unmsm.habitflow.ui.components.HabitFlowSecondaryButton
import com.unmsm.habitflow.ui.components.HabitFlowSectionHeader
import com.unmsm.habitflow.ui.components.HabitFlowStreakBadge
import com.unmsm.habitflow.ui.components.HabitFlowTopBar
import com.unmsm.habitflow.ui.components.HabitFlowVoiceOrb
import com.unmsm.habitflow.ui.components.ProgressBar
import com.unmsm.habitflow.ui.components.StatusBadge
import com.unmsm.habitflow.ui.state.VoiceAssistantPhase
import com.unmsm.habitflow.ui.state.HabitAssociationOptionUi
import com.unmsm.habitflow.ui.state.InterpretedHabitUi
import com.unmsm.habitflow.ui.theme.HabitFlowAccent
import com.unmsm.habitflow.ui.theme.HabitFlowShapes
import com.unmsm.habitflow.ui.theme.HabitFlowTextScale
import com.unmsm.habitflow.ui.theme.habitFlowAccentColor
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
import com.unmsm.habitflow.voice.VoiceErrorType
import com.unmsm.habitflow.voice.MicrophonePermissionState
import com.unmsm.habitflow.voice.microphonePermissionState
import androidx.core.app.ActivityCompat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val dateText = remember {
        DateFormat.getDateInstance(DateFormat.FULL, Locale("es", "PE")).format(Date())
    }
    val progress = if (state.habits.isEmpty()) 0f else state.completedToday.toFloat() / state.habits.size
    val nextHabit = state.habits.firstOrNull { it.id !in state.completedHabitIds }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HabitFlowTopBar(
                title = "Hola, ${state.userName}",
                subtitle = dateText,
                actionIcon = Icons.Default.Notifications,
                actionDescription = "Notificaciones",
                onAction = onNotifications
            )
        }
        item {
            ClayCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HabitFlowProgressRing(progress = progress, label = "hoy")
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Vas ${state.completedToday}/${state.habits.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Un avance pequeño también cuenta. Mantén el ritmo sin saturarte.")
                        HabitFlowStreakBadge(state.streak)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitFlowMetricCard("Mejor racha", "${state.bestStreak} días", Modifier.weight(1f))
                HabitFlowMetricCard("Completados", "${state.totalCompleted}", Modifier.weight(1f))
            }
        }
        if (state.lastActionMessage != null) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(state.lastActionMessage.orEmpty(), modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::undoLastAction) { Text("Deshacer") }
                    }
                }
            }
        }
        item {
            ClayCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Asistente de voz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Registra, consulta o crea hábitos con confirmación antes de guardar.")
                    }
                    IconButton(onClick = onVoice, modifier = Modifier.size(58.dp)) {
                        Icon(Icons.Default.Mic, contentDescription = "Abrir asistente de voz", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            HabitFlowSectionHeader(title = "Próxima actividad", actionLabel = "Agregar", onAction = onManual)
        }
        item {
            if (nextHabit == null) {
                Text("Todo listo por ahora. Puedes crear otra rutina o revisar tu progreso.")
            } else {
                ClayCard {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(nextHabit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${nextHabit.category} · ${nextHabit.reminderTime}")
                        }
                        Button(onClick = { viewModel.mark(nextHabit) }) { Text("Completar") }
                    }
                }
            }
        }
        item { HabitFlowSectionHeader("Hábitos de hoy") }
        if (state.habits.isEmpty()) {
            item {
                HabitFlowEmptyState(
                    title = "Aún no tienes hábitos",
                    message = "Empieza con una rutina pequeña. Puedes crearla manualmente o dictarla con voz.",
                    actionLabel = "Crear mi primer hábito",
                    onAction = onManual,
                    suggestions = listOf("Leer 20 min", "Beber agua", "Caminar")
                )
            }
        }
        items(state.habits) { habit ->
            HabitFlowHabitCard(
                habit = habit,
                completed = habit.id in state.completedHabitIds,
                onComplete = { viewModel.mark(habit) },
                onSkip = { viewModel.mark(habit, HabitStatus.Skipped) },
                onOpen = { onHabit(habit.id) }
            )
        }
    }
}

@Composable
fun HabitDetailScreen(padding: PaddingValues, viewModel: HabitDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val habit = state.habit
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HabitFlowTopBar(
                title = habit?.name ?: "Detalle de hábito",
                subtitle = "${habit?.category.orEmpty()} · ${habit?.frequency.orEmpty()}"
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitFlowMetricCard("Racha", "${habit?.streak ?: 0}", Modifier.weight(1f))
                HabitFlowMetricCard("Mejor", "${habit?.bestStreak ?: 0}", Modifier.weight(1f))
                HabitFlowMetricCard("Mes", "${state.completionPercent}%", Modifier.weight(1f))
            }
        }
        item {
            HabitFlowSectionHeader("Heatmap del mes")
            Heatmap(state.heatmap)
        }
        item {
            ClayCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (habit != null && habit.measurement.type != MeasurementType.BOOLEAN) {
                        Text("Meta: ${habit.measurement.targetValue} ${habit.measurement.unit}")
                        HabitFlowOutlinedField(
                            state.progressValue,
                            "Progreso en ${habit.measurement.unit}",
                            viewModel::updateProgressValue
                        )
                        HabitFlowPrimaryButton("Registrar progreso", viewModel::recordProgress)
                    }
                    HabitFlowOutlinedField(state.note, "Agregar nota", viewModel::updateNote, singleLine = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (habit?.measurement?.type == MeasurementType.BOOLEAN) {
                            HabitFlowPrimaryButton("Marcar hoy", viewModel::markToday, modifier = Modifier.weight(1f))
                        }
                        HabitFlowSecondaryButton("Guardar nota", viewModel::addNote, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item { HabitFlowSectionHeader("Notas recientes") }
        items(state.events) { event ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(event.note.ifBlank { event.habitName }, fontWeight = FontWeight.SemiBold)
                    Text(DateFormat.getDateTimeInstance().format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall)
                }
                StatusBadge(event.status)
            }
        }
    }
}

@Composable
fun StatsScreen(padding: PaddingValues, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val maxWeek = state.weekly.maxOrNull()?.coerceAtLeast(1) ?: 1
    val weekLabels = remember {
        val formatter = SimpleDateFormat("EEE", Locale("es", "PE"))
        (6 downTo 0).map { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                .time
                .let(formatter::format)
                .take(2)
                .replaceFirstChar { it.uppercase() }
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HabitFlowTopBar("Progreso", subtitle = "Rachas, semana y cumplimiento") }
        item {
            ClayCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Coach inteligente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Pregunta por tu progreso. Las respuestas usan tus registros reales y no modifican hábitos.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.coach.suggestions) { suggestion ->
                            HabitFlowCategoryChip(
                                suggestion,
                                selected = false,
                                onClick = { viewModel.askCoach(suggestion) }
                            )
                        }
                    }
                    HabitFlowOutlinedField(
                        value = state.coach.question,
                        label = "¿Qué quieres saber de tu rutina?",
                        onValueChange = viewModel::updateCoachQuestion,
                        singleLine = false
                    )
                    HabitFlowPrimaryButton(
                        label = if (state.coach.loading) "Analizando..." else "Analizar mi progreso",
                        onClick = { viewModel.askCoach() },
                        loading = state.coach.loading,
                        enabled = state.coach.question.isNotBlank() && !state.coach.loading
                    )
                    if (state.coach.answer.isNotBlank()) {
                        Text(
                            state.coach.title.ifBlank { "Insight de HabitFlow" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(state.coach.answer)
                        state.coach.evidence.forEach { evidence ->
                            Text("• $evidence", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    state.coach.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitFlowMetricCard("Racha actual", "${state.currentStreak} días", Modifier.weight(1f))
                HabitFlowMetricCard("Mejor racha", "${state.bestStreak} días", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitFlowMetricCard("Mes", "${state.monthPercent}%", Modifier.weight(1f))
                HabitFlowMetricCard("Semana", signedCount(state.weeklyComparison), Modifier.weight(1f))
            }
        }
        item {
            HabitFlowSectionHeader("Semana")
            ClayCard {
                Row(
                    Modifier.fillMaxWidth().height(156.dp).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    state.weekly.forEachIndexed { index, value ->
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((108 * (value.toFloat() / maxWeek).coerceIn(0.08f, 1f)).dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(weekLabels[index], style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        if (!state.hasData) {
            item {
                HabitFlowEmptyState(
                    title = "Tus estadísticas aparecerán pronto",
                    message = "Completa un hábito para ver rachas, comparaciones e insights reales.",
                    suggestions = listOf("Completar hábito", "Crear rutina")
                )
            }
        }
        item { HabitFlowSectionHeader("Cumplimiento por hábito") }
        items(state.habits) { habit ->
            ProgressBar(habit.name, state.habitCompletionRates[habit.id] ?: 0f)
        }
    }
}

@Composable
fun HistoryScreen(padding: PaddingValues, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HabitFlowTopBar("Historial", subtitle = "Registros agrupados por fecha") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Todos", "Completados", "Saltados")) { filter ->
                    HabitFlowCategoryChip(filter, selected = state.filter == filter, onClick = { viewModel.setFilter(filter) })
                }
            }
        }
        if (state.events.isEmpty()) {
            item {
                HabitFlowEmptyState(
                    title = "Sin registros todavía",
                    message = "Cuando completes o saltes hábitos, aparecerán aquí con fecha, hora y estado.",
                    suggestions = listOf("Completar uno", "Usar voz")
                )
            }
        }
        items(state.events) { event ->
            ClayCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.habitName, fontWeight = FontWeight.SemiBold)
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall)
                        if (event.note.isNotBlank()) Text(event.note, style = MaterialTheme.typography.bodySmall)
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
        item { HabitFlowTopBar("Notificaciones") }
        if (state.notifications.isEmpty()) {
            item { HabitFlowEmptyState("Sin notificaciones", "Te avisaremos solo cuando haya algo útil para tu rutina.") }
        }
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
fun ProfileScreen(
    padding: PaddingValues,
    onEdit: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HabitFlowTopBar(
                title = "Perfil",
                actionIcon = Icons.Default.Settings,
                actionDescription = "Ajustes",
                onAction = onSettings
            )
        }
        item {
            ClayCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        HabitFlowAvatar(state.user.name, avatarKey = state.user.avatarKey, size = 72)
                        Column(Modifier.weight(1f)) {
                            Text(state.user.name.ifBlank { "Estudiante" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("@${state.user.username} · nivel ${state.user.level}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.user.goal.ifBlank { "Ser constante" })
                        }
                    }
                    if (state.user.categories.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.user.categories.take(6)) { category ->
                                HabitFlowCategoryChip(category, selected = true, onClick = {})
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HabitFlowPrimaryButton("Editar", onEdit, modifier = Modifier.weight(1f), icon = Icons.Default.Edit)
                        HabitFlowSecondaryButton("Logros", onAchievements, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitFlowMetricCard("Insignias", "${state.achievements.count { it.unlocked }}", Modifier.weight(1f))
                HabitFlowMetricCard("Racha", "0 días", Modifier.weight(1f))
            }
        }
        item { HabitFlowSectionHeader("Insignias") }
        val unlocked = state.achievements.filter { it.unlocked }
        if (unlocked.isEmpty()) {
            item { HabitFlowEmptyState("Sin insignias aún", "Completa hábitos para desbloquear logros reales.") }
        }
        items(unlocked) { achievement ->
            ClayCard {
                Column(Modifier.padding(14.dp)) {
                    Text(achievement.title, fontWeight = FontWeight.SemiBold)
                    Text(achievement.description)
                }
            }
        }
        item { HabitFlowSectionHeader("Amigos activos") }
        if (state.friends.isEmpty()) {
            item { Text("Aún no agregaste amigos. Esta sección queda lista para una futura función social.") }
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
        item { HabitFlowTopBar("Editar perfil") }
        item {
            ClayCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HabitFlowOutlinedField(value = state.name, label = "Nombre visible", onValueChange = viewModel::updateName)
                    HabitFlowOutlinedField(value = state.username, label = "Username", onValueChange = viewModel::updateUsername)
                    HabitFlowOutlinedField(value = state.goal, label = "Objetivo principal", onValueChange = viewModel::updateGoal, singleLine = false)
                }
            }
        }
        item { HabitFlowSectionHeader("Avatar") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("avatar_lavender" to "Lavanda", "avatar_mint" to "Menta", "avatar_coral" to "Coral", "avatar_amber" to "Ámbar")) { (key, label) ->
                    HabitFlowCategoryChip(label, selected = state.avatarKey == key, onClick = { viewModel.updateAvatar(key) })
                }
            }
        }
        item { HabitFlowSectionHeader("Categorías") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Estudio", "Salud", "Bienestar", "Productividad", "Lectura", "Sueño")) { category ->
                    HabitFlowCategoryChip(category, selected = category in state.categories, onClick = { viewModel.toggleCategory(category) })
                }
            }
        }
        if (state.error != null) {
            item { Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item { HabitFlowPrimaryButton(if (state.loading) "Guardando..." else "Guardar perfil", viewModel::save, loading = state.loading) }
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
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HabitFlowTopBar("Ajustes", subtitle = "Apariencia, voz, privacidad y cuenta") }
        item { HabitFlowSectionHeader("Apariencia") }
        item {
            ClayCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Usa el tema del teléfono o fija una apariencia.", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("system" to "Sistema", "light" to "Claro", "dark" to "Oscuro")) { option ->
                            HabitFlowCategoryChip(
                                option.second,
                                selected = state.settings.themeMode == option.first,
                                onClick = { viewModel.setThemeMode(option.first) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SettingSwitch(
                "Colores dinámicos",
                state.settings.dynamicColor,
                viewModel::toggleDynamicColor,
                "Adapta la paleta a tu fondo de pantalla en Android 12 o superior."
            )
        }
        item { Text("Color de acento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HabitFlowAccent.entries) { accent ->
                    AccentChoiceChip(
                        accent = accent,
                        selected = state.settings.accentColor == accent.key,
                        onClick = { viewModel.setAccentColor(accent.key) }
                    )
                }
            }
        }
        item { Text("Tamaño de texto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HabitFlowTextScale.entries) { scale ->
                    HabitFlowCategoryChip(
                        scale.label,
                        selected = state.settings.textScale == scale.key,
                        onClick = { viewModel.setTextScale(scale.key) }
                    )
                }
            }
        }
        item { HabitFlowSectionHeader("Notificaciones") }
        item {
            SettingSwitch(
                "Recordatorios",
                state.settings.notifications,
                viewModel::toggleNotifications,
                "Recibe avisos útiles para sostener tu rutina."
            )
        }
        item { HabitFlowSectionHeader("Voz y asistente") }
        item {
            SettingSwitch(
                "Respuesta hablada",
                state.settings.voiceResponseEnabled,
                viewModel::toggleVoiceResponse,
                "Permite que HabitFlow lea en voz alta confirmaciones e insights."
            )
        }
        item { HabitFlowSectionHeader("Privacidad y seguridad") }
        item { SettingSwitch("Biometría", state.settings.biometric, viewModel::toggleBiometric) }
        item { SettingSwitch("Perfil público", state.settings.publicProfile, viewModel::togglePublicProfile) }
        item { Text("Idioma: ${state.settings.language}") }
        item { HabitFlowSectionHeader("Cuenta") }
        item { Button(onClick = {}) { Text("Cambiar contraseña") } }
        item {
            HabitFlowSecondaryButton(
                label = if (state.loggingOut) "Cerrando..." else "Cerrar sesión",
                onClick = { viewModel.logout(onLogout) },
                enabled = !state.loggingOut
            )
        }
        state.logoutError?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        item {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Eliminar cuenta", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AchievementsScreen(padding: PaddingValues, viewModel: AchievementsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            HabitFlowTopBar("Logros", subtitle = "Nivel ${state.level} · ${state.xp} XP")
            LinearProgressIndicator(progress = { state.xp / 1000f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)))
        }
        if (state.plans.isNotEmpty()) {
            item { HabitFlowSectionHeader("Planes de IA") }
            items(state.plans.take(3)) { plan ->
                ClayCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(plan.title, fontWeight = FontWeight.SemiBold)
                        Text(plan.summary)
                        plan.actions.take(3).forEach { action -> Text("• $action") }
                    }
                }
            }
        }
        if (state.cosmetics.isNotEmpty()) {
            item { HabitFlowSectionHeader("Recompensas") }
            items(state.cosmetics) { reward ->
                ClayCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(reward.name, fontWeight = FontWeight.SemiBold)
                            Text(reward.description)
                        }
                        Text(if (reward.unlocked) "OK" else "${(reward.cost - state.xp).coerceAtLeast(0)} XP")
                    }
                }
            }
        }
        item { HabitFlowSectionHeader("Insignias") }
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
    var confirmation by remember { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Eliminar cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Text("Perderás tus hábitos, historial, rachas, logros, archivos y datos de perfil. Esta acción no debe ejecutarse accidentalmente.")
        }
        item { HabitFlowOutlinedField(confirmation, "Escribe ELIMINAR", { confirmation = it }) }
        item {
            Button(onClick = onDeleted, enabled = confirmation == "ELIMINAR") {
                Text("Confirmar eliminación")
            }
        }
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
    val lifecycleOwner = remember(context) { context.findActivity() as? LifecycleOwner }
    var typedText by rememberSaveable { mutableStateOf("") }
    var lastTranscript by rememberSaveable { mutableStateOf("") }
    var permissionRequestedBefore by rememberSaveable { mutableStateOf(false) }
    var permissionRequestWasRepeat by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.startConversation() }
    LaunchedEffect(state.transcript) {
        if (state.transcript.isNotBlank() && state.transcript != lastTranscript) {
            if (typedText.isBlank() || typedText == lastTranscript) {
                typedText = state.transcript
            }
            lastTranscript = state.transcript
        }
    }
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.handleAppBackgrounded()
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true) {
                viewModel.cancelListening()
            }
        }
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.toggleRecording()
        } else {
            val activity = context.findActivity()
            val permissionState = microphonePermissionState(
                granted = false,
                requestedBefore = permissionRequestWasRepeat,
                shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
                } ?: false
            )
            val permanentlyDenied = permissionState == MicrophonePermissionState.PermanentlyDenied
            val message = if (permanentlyDenied) {
                "El permiso de micrófono quedó bloqueado. Ábrelo en ajustes para registrar con voz."
            } else {
                "Debes permitir el acceso al micrófono para dictar por voz."
            }
            viewModel.showError(
                message = message,
                type = VoiceErrorType.InsufficientPermissions,
                permissionPermanentlyDenied = permanentlyDenied
            )
        }
    }
    val openSettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }
    val onMicClick: () -> Unit = onMicClick@{
        if (state.phase == VoiceAssistantPhase.PreparingModel ||
            state.phase == VoiceAssistantPhase.Transcribing ||
            state.phase == VoiceAssistantPhase.Processing
        ) return@onMicClick
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.toggleRecording()
        } else {
            viewModel.requestMicrophonePermission()
            permissionRequestWasRepeat = permissionRequestedBefore
            permissionRequestedBefore = true
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
                Column(Modifier.weight(1f)) {
                    Text("Asistente inteligente", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(voiceStatusLabel(state.phase), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Registra hábitos o pregunta por tu progreso usando voz o texto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HabitFlowVoiceOrb(
                    listening = state.phase is VoiceAssistantPhase.Recording,
                    processing = state.phase == VoiceAssistantPhase.Processing ||
                        state.phase == VoiceAssistantPhase.Transcribing ||
                        state.phase == VoiceAssistantPhase.PreparingModel,
                    onClick = onMicClick
                )
            }
        }
        if (state.phase == VoiceAssistantPhase.ModelNotPrepared || state.phase == VoiceAssistantPhase.PreparingModel) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Preparando el modelo local de voz…", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (state.phase is VoiceAssistantPhase.Recording) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Grabando…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${state.recordingDurationMillis / 1_000}.${(state.recordingDurationMillis % 1_000) / 100} s / 15 s")
                        LinearProgressIndicator(
                            progress = { state.audioLevel.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HabitFlowPrimaryButton("Detener", viewModel::stopRecording, modifier = Modifier.weight(1f))
                            HabitFlowSecondaryButton("Cancelar", viewModel::cancelListening, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (state.phase == VoiceAssistantPhase.Transcribing) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Whisper está convirtiendo tu voz en texto…", fontWeight = FontWeight.SemiBold)
                        Text("La voz se procesa localmente en tu dispositivo.")
                        TextButton(onClick = viewModel::cancelListening) { Text("Cancelar") }
                    }
                }
            }
        }
        if (state.phase == VoiceAssistantPhase.Processing) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(state.response.ifBlank { "Analizando..." })
                    }
                }
            }
        }
        items(state.messages) { message ->
            HabitFlowMessageBubble(author = message.author, text = message.text)
        }
        if (state.coachTitle.isNotBlank()) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.coachTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        state.coachHighlights.forEach { highlight ->
                            Text("• $highlight", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "Es una recomendación: no se aplicó ningún cambio automáticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        if (state.interpretedHabits.isNotEmpty()) {
            item {
                VoiceInterpretationConfirmationCard(
                    originalText = state.interpretationText.ifBlank { state.transcript },
                    habits = state.interpretedHabits,
                    options = state.habitAssociationOptions,
                    saving = state.savingInterpretation,
                    onUpdate = viewModel::updateInterpretedHabit,
                    onAssociate = viewModel::updateInterpretedHabitAssociation,
                    onCancel = { viewModel.cancelInterpretedHabits() },
                    onConfirm = viewModel::confirmInterpretedHabits
                )
            }
        } else if (state.pendingSummary != null) {
            item {
                ClayCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Confirmación requerida", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(state.pendingSummary.orEmpty())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HabitFlowSecondaryButton("Cancelar", { viewModel.cancelPendingAction() }, modifier = Modifier.weight(1f))
                            HabitFlowPrimaryButton("Confirmar", viewModel::confirmPendingAction, modifier = Modifier.weight(1f), icon = Icons.Default.Check)
                        }
                    }
                }
            }
        }
        if (state.quickReplies.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.quickReplies) { reply ->
                        HabitFlowCategoryChip(reply, selected = false, onClick = { viewModel.sendText(reply) })
                    }
                }
            }
        }
        if (state.error != null) {
            item {
                val permissionError = (state.phase as? VoiceAssistantPhase.Error)?.type == VoiceErrorType.InsufficientPermissions &&
                    state.permissionPermanentlyDenied
                ClayCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onErrorContainer)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HabitFlowSecondaryButton("Reintentar", onClick = onMicClick, modifier = Modifier.weight(1f))
                                HabitFlowSecondaryButton(
                                    "Escribir",
                                    onClick = { typedText = state.partialTranscript.ifBlank { state.transcript } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HabitFlowSecondaryButton("Manual", onClick = onManual)
                        }
                        if (permissionError) {
                            HabitFlowSecondaryButton("Abrir ajustes", onClick = openSettings)
                        }
                    }
                }
            }
        }
        item {
            ClayCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "La voz se procesa localmente con Whisper. HabitFlow enviará únicamente el texto cuando pulses Enviar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HabitFlowOutlinedField(typedText, "Transcripción editable o texto manual", { typedText = it }, singleLine = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HabitFlowSecondaryButton(
                            if (state.transcript.isBlank()) "Manual" else "Volver a grabar",
                            if (state.transcript.isBlank()) onManual else onMicClick,
                            modifier = Modifier.weight(1f)
                        )
                        HabitFlowPrimaryButton(
                            "Enviar",
                            onClick = {
                                viewModel.sendText(typedText)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = typedText.isNotBlank() &&
                                state.phase != VoiceAssistantPhase.Processing &&
                                state.phase != VoiceAssistantPhase.Transcribing &&
                                state.phase != VoiceAssistantPhase.PreparingModel &&
                                state.phase !is VoiceAssistantPhase.Recording &&
                                !state.savingInterpretation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceInterpretationConfirmationCard(
    originalText: String,
    habits: List<InterpretedHabitUi>,
    options: List<HabitAssociationOptionUi>,
    saving: Boolean,
    onUpdate: (Int, String, String) -> Unit,
    onAssociate: (Int, String?) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    ClayCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Revisa antes de guardar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (originalText.isNotBlank()) {
                Text("Texto original", style = MaterialTheme.typography.labelLarge)
                Text(originalText, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            habits.forEachIndexed { index, habit ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(HabitFlowShapes.Medium))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Hábito ${index + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    HabitFlowOutlinedField(habit.name, "Nombre", { onUpdate(index, "name", it) })
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            listOf(
                                "completed" to "Completado",
                                "planned" to "Planeado",
                                "created" to "Crear",
                                "unknown" to "Pendiente"
                            )
                        ) { (value, label) ->
                            HabitFlowCategoryChip(
                                label = label,
                                selected = habit.action == value,
                                onClick = { onUpdate(index, "action", value) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HabitFlowOutlinedField(
                            value = habit.quantity,
                            label = "Cantidad",
                            onValueChange = { onUpdate(index, "quantity", it) },
                            modifier = Modifier.weight(1f)
                        )
                        HabitFlowOutlinedField(
                            value = habit.unit,
                            label = "Unidad",
                            onValueChange = { onUpdate(index, "unit", it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HabitFlowOutlinedField(habit.date, "Fecha (YYYY-MM-DD)", { onUpdate(index, "date", it) })
                    HabitFlowOutlinedField(habit.notes, "Notas", { onUpdate(index, "notes", it) }, singleLine = false)
                    Text(
                        habit.existingHabitName?.let { "Relacionado con: $it" }
                            ?: "Se creará un hábito nuevo si no existe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            HabitFlowCategoryChip(
                                label = "Nuevo",
                                selected = habit.existingHabitId == null,
                                onClick = { onAssociate(index, null) }
                            )
                        }
                        items(options.take(12)) { option ->
                            HabitFlowCategoryChip(
                                label = option.name,
                                selected = habit.existingHabitId == option.id,
                                onClick = { onAssociate(index, option.id) }
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitFlowSecondaryButton("Cancelar", onCancel, modifier = Modifier.weight(1f), enabled = !saving)
                HabitFlowPrimaryButton(
                    "Confirmar y registrar",
                    onConfirm,
                    modifier = Modifier.weight(1f),
                    loading = saving,
                    icon = Icons.Default.Check
                )
            }
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
            Text("Crea un hábito sin usar dictado de voz.")
        }
        item { HabitFlowOutlinedField(state.name, "Nombre del hábito", viewModel::updateName) }
        item { HabitFlowOutlinedField(state.category, "Categoría", viewModel::updateCategory) }
        item { HabitFlowSectionHeader("Frecuencia") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        HabitFrequencyType.DAILY to "Diario",
                        HabitFrequencyType.SPECIFIC_WEEKDAYS to "Días",
                        HabitFrequencyType.TIMES_PER_WEEK to "Por semana",
                        HabitFrequencyType.INTERVAL_DAYS to "Intervalo",
                        HabitFrequencyType.MONTHLY_DATES to "Mensual",
                        HabitFrequencyType.ONE_TIME to "Una vez"
                    )
                ) { (type, label) ->
                    HabitFlowCategoryChip(
                        label,
                        selected = state.frequencyType == type.name,
                        onClick = { viewModel.updateFrequencyType(type.name) }
                    )
                }
            }
        }
        when (state.frequencyType) {
            HabitFrequencyType.SPECIFIC_WEEKDAYS.name -> item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            "MONDAY" to "Lun", "TUESDAY" to "Mar", "WEDNESDAY" to "Mié",
                            "THURSDAY" to "Jue", "FRIDAY" to "Vie", "SATURDAY" to "Sáb", "SUNDAY" to "Dom"
                        )
                    ) { (day, label) ->
                        HabitFlowCategoryChip(
                            label = label,
                            selected = day in state.weekdays,
                            onClick = { viewModel.toggleWeekday(day) }
                        )
                    }
                }
            }
            HabitFrequencyType.TIMES_PER_WEEK.name -> item {
                HabitFlowOutlinedField(state.timesPerWeek, "Veces por semana (1-7)", viewModel::updateTimesPerWeek)
            }
            HabitFrequencyType.INTERVAL_DAYS.name -> item {
                HabitFlowOutlinedField(state.intervalDays, "Cada cuántos días", viewModel::updateIntervalDays)
            }
            HabitFrequencyType.MONTHLY_DATES.name -> item {
                HabitFlowOutlinedField(state.monthlyDays, "Días del mes, separados por coma", viewModel::updateMonthlyDays)
            }
        }
        if (state.frequencyType == HabitFrequencyType.INTERVAL_DAYS.name ||
            state.frequencyType == HabitFrequencyType.ONE_TIME.name
        ) {
            item { HabitFlowOutlinedField(state.startDate, "Fecha inicial (YYYY-MM-DD)", viewModel::updateStartDate) }
        }
        if (state.frequencyType != HabitFrequencyType.ONE_TIME.name) {
            item { HabitFlowOutlinedField(state.endDate, "Fecha final opcional (YYYY-MM-DD)", viewModel::updateEndDate) }
        }
        item { HabitFlowOutlinedField(state.timezone, "Zona horaria", viewModel::updateTimezone) }
        item { HabitFlowSectionHeader("Objetivo") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(
                    MeasurementType.BOOLEAN to "Sí/No", MeasurementType.COUNT to "Conteo",
                    MeasurementType.DURATION to "Duración", MeasurementType.QUANTITY to "Cantidad"
                )) { (type, label) ->
                    HabitFlowCategoryChip(
                        label = label,
                        selected = state.measurementType == type.name,
                        onClick = { viewModel.updateMeasurementType(type.name) }
                    )
                }
            }
        }
        if (state.measurementType != MeasurementType.BOOLEAN.name) {
            item { HabitFlowOutlinedField(state.targetValue, "Meta", viewModel::updateTargetValue) }
            item { HabitFlowOutlinedField(state.measurementUnit, "Unidad", viewModel::updateMeasurementUnit) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Permitir progreso parcial", modifier = Modifier.weight(1f))
                    Switch(state.allowPartialProgress, viewModel::updateAllowPartial)
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(
                        AggregationMode.ADD to "Sumar", AggregationMode.SET_TOTAL to "Establecer total",
                        AggregationMode.REPLACE to "Reemplazar"
                    )) { (mode, label) ->
                        HabitFlowCategoryChip(
                            label = label,
                            selected = state.aggregationMode == mode.name,
                            onClick = { viewModel.updateAggregationMode(mode.name) }
                        )
                    }
                }
            }
        }
        item { HabitFlowOutlinedField(state.reminderTime, "Hora o nota", viewModel::updateReminderTime) }
        if (state.error != null) {
            item { Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error) }
        }
        item {
            HabitFlowPrimaryButton(
                label = if (state.loading) "Guardando..." else "Guardar hábito",
                onClick = viewModel::save,
                loading = state.loading
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    description: String? = null
) {
    ClayCard {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun AccentChoiceChip(
    accent: HabitFlowAccent,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(HabitFlowShapes.Pill))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(habitFlowAccentColor(accent.key, dark = false))
        )
        Text(accent.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun Heatmap(heatmap: HabitHeatmap) {
    if (!heatmap.hasActivity) {
        Text(
            "Todavía no tienes actividad registrada.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val leadingEmptyDays = heatmap.days.firstOrNull()?.date?.dayOfWeek?.value?.minus(1) ?: 0
    val cells = List(leadingEmptyDays) { null } + heatmap.days
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Box(Modifier.size(32.dp))
                        return@forEach
                    }
                    val color = when (day.state) {
                        HeatmapDayState.Completed -> MaterialTheme.colorScheme.tertiary
                        HeatmapDayState.Partial -> MaterialTheme.colorScheme.secondary
                        HeatmapDayState.Skipped -> MaterialTheme.colorScheme.errorContainer
                        HeatmapDayState.ScheduledEmpty -> MaterialTheme.colorScheme.surfaceVariant
                        HeatmapDayState.NotScheduled -> MaterialTheme.colorScheme.surfaceContainer
                        HeatmapDayState.Future -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f)
                    }
                    Box(
                        Modifier.size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun signedCount(value: Int): String =
    when {
        value > 0 -> "+$value"
        else -> value.toString()
    }

private fun voiceStatusLabel(phase: VoiceAssistantPhase): String =
    when (phase) {
        VoiceAssistantPhase.Idle,
        VoiceAssistantPhase.Ready -> "Listo para registrar con voz"
        VoiceAssistantPhase.ModelNotPrepared -> "Modelo no preparado"
        VoiceAssistantPhase.PreparingModel -> "Preparando Whisper local"
        VoiceAssistantPhase.RequestingPermission -> "Solicitando permiso"
        is VoiceAssistantPhase.Recording -> "Grabando"
        VoiceAssistantPhase.Transcribing -> "Transcribiendo localmente"
        VoiceAssistantPhase.Processing -> "Pensando"
        VoiceAssistantPhase.AwaitingConfirmation -> "Confirmando"
        VoiceAssistantPhase.Speaking -> "Respondiendo"
        VoiceAssistantPhase.Completed -> "Listo"
        is VoiceAssistantPhase.Error -> "Necesita atención"
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
