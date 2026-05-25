package pt.registoev.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.registoev.app.data.EvChargeEntity
import java.text.SimpleDateFormat
import java.util.*
import java.util.*

@Composable
fun EditHistoryScreen(
    charges: List<EvChargeEntity>,
    onEditClick: (EvChargeEntity) -> Unit,
    onDeleteSelected: (List<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val sortedCharges = remember(charges) {
        // Ordenar primeiro por Dia (descendente) e depois por Odómetro (descendente) para total consistência
        charges.sortedWith(
            compareByDescending<EvChargeEntity> { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis 
            }.thenByDescending { it.odometer }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(28.dp),
            title = { 
                Text(
                    "Eliminar Registos", 
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "Deseja remover ${selectedIds.size} registos?",
                    color = Color.LightGray
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSelected(selectedIds.toList())
                        isSelectionMode = false
                        selectedIds = emptySet()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AnimatedVisibility(
            visible = charges.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    IconButton(onClick = { 
                        isSelectionMode = false
                        selectedIds = emptySet()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                    Text(
                        text = "${selectedIds.size} selecionados",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Button(
                        onClick = { showDeleteConfirm = true },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            disabledContainerColor = Color(0xFFE53935).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apagar")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f)) // Empurra o conteúdo para a direita
                    Text(
                        text = "Apagar  ->",
                        style = MaterialTheme.typography.titleMedium, // Aumentado de labelLarge para titleMedium
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold, // Aumentado peso para Bold para acompanhar o tamanho
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = { isSelectionMode = true }) {
                        Icon(Icons.Default.SelectAll, null, tint = Color(0xFFF5E1A4)) // Beje Suave
                    }
                }
            }
        }

        if (charges.isEmpty()) {
            EmptyState("Sem registos para gerir.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedCharges) { charge ->
                    EditCard(
                        charge = charge,
                        isSelected = selectedIds.contains(charge.id),
                        isSelectionMode = isSelectionMode,
                        dateFormat = dateFormat,
                        onToggleSelection = {
                            selectedIds = if (selectedIds.contains(charge.id)) {
                                selectedIds - charge.id
                            } else {
                                selectedIds + charge.id
                            }
                        },
                        onEditClick = { onEditClick(charge) }
                    )
                }
                item { Spacer(modifier = Modifier.height(110.dp)) }
            }
        }
    }
}

@Composable
fun EditCard(
    charge: EvChargeEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    dateFormat: SimpleDateFormat,
    onToggleSelection: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.1f) else Color(0xFF1C1C1E)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) Color(0xFF2196F3) else Color.White.copy(alpha = 0.1f)
        ),
        onClick = { if (isSelectionMode) onToggleSelection() else onEditClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "${charge.origin} → ${charge.destination}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = dateFormat.format(Date(charge.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    Text(
                        text = "${charge.odometer} km",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                
                if (!isSelectionMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Toque para editar registo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
