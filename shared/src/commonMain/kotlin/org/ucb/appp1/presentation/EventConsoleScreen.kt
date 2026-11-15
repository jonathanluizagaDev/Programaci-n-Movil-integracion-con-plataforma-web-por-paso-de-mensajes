package org.ucb.appp1.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.ucb.appp1.model.EventLogEntry
import org.ucb.appp1.model.EventLogStage

@Composable
fun EventConsoleScreen(state: EventStateHolder) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Consola de eventos", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Simulación local · API HTTPS aún no conectada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Cada flujo muestra solicitud, respuesta, evento, confirmación y resultado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = state::startFromApp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Iniciar evento desde la app")
        }

        OutlinedButton(
            onClick = state::simulateStartFromWeb,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Simular evento iniciado desde la web")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Registro de actividad", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = state::clearLogs, enabled = state.logs.isNotEmpty()) {
                Text("Limpiar")
            }
        }

        if (state.logs.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = "Aún no hay eventos. Inicia uno desde la app o simula uno desde la web.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.logs, key = EventLogEntry::id) { entry ->
                    EventLogCard(entry)
                }
            }
        }
    }
}

@Composable
private fun EventLogCard(entry: EventLogEntry) {
    val cardColor = when (entry.stage) {
        EventLogStage.REQUEST -> MaterialTheme.colorScheme.primaryContainer
        EventLogStage.RESPONSE -> MaterialTheme.colorScheme.secondaryContainer
        EventLogStage.DELIVERY -> MaterialTheme.colorScheme.tertiaryContainer
        EventLogStage.ACKNOWLEDGEMENT -> MaterialTheme.colorScheme.primaryContainer
        EventLogStage.COMPLETION -> MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(entry.stage.label, style = MaterialTheme.typography.labelLarge)
            Text(entry.source.label, style = MaterialTheme.typography.labelMedium)
        }
        Text(entry.description, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "requestId: ${entry.requestId} · eventId: ${entry.eventId}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
