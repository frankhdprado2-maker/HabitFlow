package com.unmsm.habitflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.ui.theme.HabitFlowDimensions
import com.unmsm.habitflow.ui.theme.HabitFlowElevation
import com.unmsm.habitflow.ui.theme.HabitFlowIconSizes
import com.unmsm.habitflow.ui.theme.HabitFlowMotion
import com.unmsm.habitflow.ui.theme.HabitFlowShapes
import com.unmsm.habitflow.ui.theme.HabitFlowSpacing

data class HabitFlowNavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun HabitFlowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(HabitFlowShapes.Large)
    val cardModifier = modifier
        .shadow(
            elevation = HabitFlowElevation.Soft,
            shape = shape,
            clip = false,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)

    if (onClick == null) {
        Card(
            modifier = cardModifier,
            colors = colors,
            shape = shape,
            elevation = elevation
        ) { content() }
    } else {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = colors,
            shape = shape,
            elevation = elevation
        ) { content() }
    }
}

@Composable
fun HabitFlowPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(HabitFlowDimensions.ButtonHeight),
        shape = RoundedCornerShape(HabitFlowShapes.Medium),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = HabitFlowSpacing.Large)
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )

            icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(HabitFlowIconSizes.Small))
        }
        if (!loading) {
            if (icon != null) Spacer(Modifier.size(HabitFlowSpacing.Small))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun HabitFlowSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(HabitFlowDimensions.ButtonHeight),
        shape = RoundedCornerShape(HabitFlowShapes.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(HabitFlowIconSizes.Small))
            Spacer(Modifier.size(HabitFlowSpacing.Small))
        }
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun HabitFlowTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = HabitFlowDimensions.MinTouchTarget)) {
        Text(label)
    }
}

@Composable
fun HabitFlowOutlinedField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null) }
        },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { text ->
            { Text(text) }
        },
        singleLine = singleLine,
        shape = RoundedCornerShape(HabitFlowShapes.Medium),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
fun HabitFlowSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Buscar"
) {
    HabitFlowOutlinedField(
        value = value,
        label = label,
        onValueChange = onValueChange,
        modifier = modifier,
        leadingIcon = Icons.Default.Search
    )
}

@Composable
fun HabitFlowTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionIcon != null && onAction != null) {
            IconButton(onClick = onAction, modifier = Modifier.size(HabitFlowDimensions.MinTouchTarget)) {
                Icon(actionIcon, contentDescription = actionDescription)
            }
        }
    }
}

@Composable
fun HabitFlowNavigationBar(
    items: List<HabitFlowNavigationItem>,
    selectedRoute: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(HabitFlowDimensions.NavigationHeight),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = selectedRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelected(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, maxLines = 1) }
            )
        }
    }
}

@Composable
fun HabitFlowMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    HabitFlowCard(modifier = modifier, containerColor = containerColor) {
        Column(
            Modifier.padding(HabitFlowSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Small)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Small)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!supportingText.isNullOrBlank()) {
                Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HabitFlowHabitCard(
    habit: Habit,
    completed: Boolean,
    onComplete: () -> Unit,
    onSkip: (() -> Unit)? = null,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(HabitFlowMotion.Micro),
        label = "habit-check"
    )
    HabitFlowCard(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(HabitFlowSpacing.Large)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Medium)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(HabitFlowShapes.Medium))
                    .background(if (completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(habit.icon.take(2).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${habit.category} · ${habit.reminderTime} · racha ${habit.streak}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onSkip != null) {
                IconButton(onClick = onSkip, modifier = Modifier.size(HabitFlowDimensions.MinTouchTarget)) {
                    Icon(Icons.Default.Close, contentDescription = "Saltar hábito", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(
                onClick = onComplete,
                modifier = Modifier
                    .size(HabitFlowDimensions.MinTouchTarget)
                    .scale(1f + progress * 0.06f)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (completed) "Hábito completado" else "Completar hábito"
                    }
            ) {
                Icon(
                    imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HabitFlowEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    suggestions: List<String> = emptyList(),
    onSuggestion: (String) -> Unit = {}
) {
    HabitFlowCard(modifier = modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier.padding(HabitFlowSpacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WellnessIllustration()
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (suggestions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Small)) {
                    suggestions.take(3).forEach { suggestion ->
                        AssistChip(onClick = { onSuggestion(suggestion) }, label = { Text(suggestion) })
                    }
                }
            }
            if (actionLabel != null && onAction != null) {
                HabitFlowPrimaryButton(label = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun HabitFlowErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    HabitFlowCard(modifier = modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.padding(HabitFlowSpacing.Large), verticalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Small)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                HabitFlowSecondaryButton(label = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun HabitFlowLoadingState(modifier: Modifier = Modifier, label: String = "Cargando") {
    Column(
        modifier = modifier.fillMaxWidth().padding(HabitFlowSpacing.XLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Medium)
    ) {
        CircularProgressIndicator()
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HabitFlowAvatar(
    name: String,
    modifier: Modifier = Modifier,
    avatarKey: String? = null,
    size: Int = 64
) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "HF" }
    val background = when (avatarKey) {
        "avatar_coral" -> MaterialTheme.colorScheme.secondaryContainer
        "avatar_amber" -> Color(0xFFFFECC1)
        "avatar_mint" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun HabitFlowProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(HabitFlowMotion.Content),
        label = "progress-ring"
    )
    Box(modifier = modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color.Gray.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = Color(0xFF2F7D6D),
                startAngle = -90f,
                sweepAngle = animated * 360f,
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!label.isNullOrBlank()) Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun HabitFlowStreakBadge(streak: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(HabitFlowShapes.Pill)
    ) {
        Text(
            text = "$streak días",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun HabitFlowCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(HabitFlowShapes.Pill)
    )
}

@Composable
fun HabitFlowSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = HabitFlowSpacing.Large, bottom = HabitFlowSpacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (actionLabel != null && onAction != null) {
            HabitFlowTextButton(label = actionLabel, onClick = onAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFlowConfirmationSheet(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.padding(HabitFlowSpacing.XLarge),
            verticalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Medium)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(HabitFlowSpacing.Medium)) {
                HabitFlowSecondaryButton(label = dismissLabel, onClick = onDismiss, modifier = Modifier.weight(1f))
                HabitFlowPrimaryButton(label = confirmLabel, onClick = onConfirm, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(HabitFlowSpacing.Small))
        }
    }
}

@Composable
fun HabitFlowVoiceOrb(
    listening: Boolean,
    processing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "voice-orb")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(HabitFlowMotion.Content),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice-pulse"
    )
    val scale = if (listening) pulse else 1f
    Surface(
        onClick = onClick,
        enabled = !processing,
        modifier = modifier
            .size(HabitFlowIconSizes.Orb)
            .scale(scale)
            .semantics {
                role = Role.Button
                contentDescription = if (listening) "Detener escucha" else "Iniciar escucha por voz"
            },
        shape = CircleShape,
        color = if (listening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        contentColor = if (listening) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (processing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(HabitFlowIconSizes.Large),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(HabitFlowIconSizes.Large))
            }
        }
    }
}

@Composable
fun HabitFlowMessageBubble(
    author: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val isUser = author == "user"
    Box(modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.86f),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = HabitFlowShapes.Large,
                topEnd = HabitFlowShapes.Large,
                bottomStart = if (isUser) HabitFlowShapes.Large else HabitFlowShapes.Small,
                bottomEnd = if (isUser) HabitFlowShapes.Small else HabitFlowShapes.Large
            )
        ) {
            Column(Modifier.padding(HabitFlowSpacing.Medium), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isUser) "Tú" else "HabitFlow", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(text)
            }
        }
    }
}

@Composable
fun HabitFlowSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(HabitFlowShapes.Medium),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

@Composable
private fun WellnessIllustration() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.size(width = 156.dp, height = 96.dp)) {
        drawCircle(color = primary.copy(alpha = 0.16f), radius = 42.dp.toPx(), center = Offset(58.dp.toPx(), 48.dp.toPx()))
        drawCircle(color = secondary.copy(alpha = 0.18f), radius = 26.dp.toPx(), center = Offset(105.dp.toPx(), 34.dp.toPx()))
        drawCircle(color = tertiary.copy(alpha = 0.16f), radius = 22.dp.toPx(), center = Offset(108.dp.toPx(), 70.dp.toPx()))
        drawLine(
            color = primary,
            start = Offset(36.dp.toPx(), 64.dp.toPx()),
            end = Offset(74.dp.toPx(), 38.dp.toPx()),
            strokeWidth = 7.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = secondary,
            start = Offset(74.dp.toPx(), 38.dp.toPx()),
            end = Offset(116.dp.toPx(), 58.dp.toPx()),
            strokeWidth = 7.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    HabitFlowSectionHeader(title = title, modifier = modifier)
}

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    HabitFlowCard(modifier = modifier, onClick = onClick, containerColor = containerColor, content = content)
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    HabitFlowMetricCard(label = label, value = value, modifier = modifier)
}

@Composable
fun HabitRow(habit: Habit, completed: Boolean = false, onMark: () -> Unit, onOpen: () -> Unit) {
    HabitFlowHabitCard(habit = habit, completed = completed, onComplete = onMark, onOpen = onOpen)
}

@Composable
fun StatusBadge(status: HabitStatus) {
    val color = when (status) {
        HabitStatus.Completed -> MaterialTheme.colorScheme.primary
        HabitStatus.Skipped -> Color(0xFFE2A226)
        HabitStatus.Failed -> MaterialTheme.colorScheme.error
        HabitStatus.Pending -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(HabitFlowShapes.Pill)) {
        Text(
            status.label(),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ProgressBar(label: String, value: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${(value.coerceIn(0f, 1f) * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(HabitFlowDimensions.ProgressHeight)
                .clip(RoundedCornerShape(HabitFlowShapes.Pill))
        )
    }
}

@Composable
fun FormField(value: String, label: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    HabitFlowOutlinedField(value = value, label = label, onValueChange = onChange, modifier = modifier)
}

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HabitFlowPrimaryButton(label = label, onClick = onClick, modifier = modifier)
}

@Composable
fun VerticalSpacer() {
    Spacer(Modifier.height(12.dp))
}

private fun HabitStatus.label(): String =
    when (this) {
        HabitStatus.Completed -> "Completado"
        HabitStatus.Skipped -> "Saltado"
        HabitStatus.Failed -> "No completado"
        HabitStatus.Pending -> "Por hacer"
    }
