package com.developersbeeh.pharmaflow.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// COMPONENTE STEPPER (BARRA DE PROGRESSO) - AGORA PÚBLICO E NO MAIN
@Composable
fun OrderTrackingStepper(currentStatus: String) {
    val steps = listOf("Pendente", "Em Separação", "Saiu para Entrega", "Entregue")

    // Ignora case sensitive para achar o status
    val currentStepIndex = steps.indexOfFirst { it.equals(currentStatus, ignoreCase = true) }
        .let { if (it == -1) 0 else it }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, stepName ->
            val isActive = index <= currentStepIndex
            val color by animateColorAsState(
                if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray,
                label = "colorAnim"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < currentStepIndex) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (index == currentStepIndex) {
                        Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val label = when (stepName) {
                    "Pendente" -> "Recebido"
                    "Em Separação" -> "Separando"
                    "Saiu para Entrega" -> "Saiu"
                    "Entregue" -> "Entregue"
                    else -> ""
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = if (index == currentStepIndex) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (index < steps.size - 1) {
                val lineColor = if (index < currentStepIndex) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(bottom = 14.dp)
                        .background(lineColor)
                )
            }
        }
    }
}