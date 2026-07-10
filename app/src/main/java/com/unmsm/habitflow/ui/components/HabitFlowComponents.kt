package com.unmsm.habitflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitStatus
import com.unmsm.habitflow.ui.theme.ClayTokens

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(ClayTokens.Radius.Large)
    val cardModifier = modifier
        .shadow(
            elevation = ClayTokens.Elevation.Resting,
            shape = shape,
            clip = false,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        )
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = ClayTokens.Elevation.Pressed)

    if (onClick == null) {
        Card(
            modifier = cardModifier,
            colors = colors,
            shape = shape,
            border = border,
            elevation = elevation
        ) { content() }
    } else {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            colors = colors,
            shape = shape,
            border = border,
            elevation = elevation
        ) { content() }
    }
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    ClayCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(ClayTokens.Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HabitRow(habit: Habit, completed: Boolean = false, onMark: () -> Unit, onOpen: () -> Unit) {
    ClayCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(ClayTokens.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(ClayTokens.Size.IconTile)
                    .clip(RoundedCornerShape(ClayTokens.Radius.Small))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(habit.icon.take(2), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(habit.name, fontWeight = FontWeight.SemiBold)
                Text("${habit.category} · ${habit.reminderTime} · racha ${habit.streak}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onMark) {
                Icon(
                    imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Marcar hábito",
                    tint = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: HabitStatus) {
    val color = when (status) {
        HabitStatus.Completed -> MaterialTheme.colorScheme.tertiary
        HabitStatus.Skipped -> Color(0xFFFFC857)
        HabitStatus.Failed -> MaterialTheme.colorScheme.error
        HabitStatus.Pending -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(ClayTokens.Radius.Small)) {
        Text(status.name, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
fun ProgressBar(label: String, value: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(ClayTokens.Size.ProgressHeight).clip(RoundedCornerShape(ClayTokens.Radius.Small)))
    }
}

@Composable
fun FormField(value: String, label: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(ClayTokens.Radius.Medium),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(ClayTokens.Size.ButtonHeight),
        shape = RoundedCornerShape(ClayTokens.Radius.Medium),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
    ) {
        Text(label)
    }
}

@Composable
fun VerticalSpacer() {
    Spacer(Modifier.height(12.dp))
}
