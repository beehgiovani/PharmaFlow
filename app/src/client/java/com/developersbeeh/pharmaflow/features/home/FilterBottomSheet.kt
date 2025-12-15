package com.developersbeeh.pharmaflow.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersbeeh.pharmaflow.domain.sort.SortOption

data class FilterOptions(
    val sortBy: SortOption = SortOption.DEFAULT,
    val onlyOnSale: Boolean = false,
    val onlyAvailable: Boolean = false
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentOptions: FilterOptions,
    onApply: (FilterOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var sortBy by remember { mutableStateOf(currentOptions.sortBy) }
    var onlyOnSale by remember { mutableStateOf(currentOptions.onlyOnSale) }
    var onlyAvailable by remember { mutableStateOf(currentOptions.onlyAvailable) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtros e Ordenação", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Sort Options
            Text("Ordenar por", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Column {
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (sortBy == option),
                                onClick = { sortBy = option }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (sortBy == option),
                            onClick = { sortBy = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Filter Toggles
            Text("Filtros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onlyOnSale = !onlyOnSale }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = onlyOnSale, onCheckedChange = { onlyOnSale = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Apenas Ofertas", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("Exibir apenas produtos com desconto", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onlyAvailable = !onlyAvailable }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = onlyAvailable, onCheckedChange = { onlyAvailable = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Somente Disponíveis", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("Ocultar produtos sem estoque", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { 
                        // Reset
                        sortBy = SortOption.DEFAULT
                        onlyOnSale = false
                        onlyAvailable = false
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Limpar")
                }
                
                Button(
                    onClick = { onApply(FilterOptions(sortBy, onlyOnSale, onlyAvailable)) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Aplicar Filtros")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
