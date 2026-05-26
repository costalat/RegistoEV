package pt.registoev.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EvStation
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
                        selectedIds = emptySet()
                        isSelectionMode = false
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Toolbar de seleção
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                color = Color(0xFF1C1C1E),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { isSelectionMode = false; selectedIds = emptySet() }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                    
                    Text(
                        text = "${selectedIds.size} selecionados",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row {
                        IconButton(onClick = { 
                            selectedIds = if (selectedIds.size == charges.size) emptySet() else charges.map { it.id }.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, null, tint = Color.White)
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete, 
                                null, 
                                tint = if (selectedIds.isNotEmpty()) Color(0xFFE53935) else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
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
                        onEditClick = { onEditClick(charge) },
                        onLongClick = { 
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds = setOf(charge.id)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(110.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditCard(
    charge: EvChargeEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    dateFormat: SimpleDateFormat,
    onToggleSelection: () -> Unit,
    onEditClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() else onEditClick() },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.1f) else Color(0xFF1C1C1E)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) Color(0xFF2196F3) else Color.White.copy(alpha = 0.1f)
        )
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
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Badge: Tipo de Carga
                            if (charge.chargeType != "Nenhum") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFF26C6DA),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = charge.chargeType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }

                            // Badge: Código do Posto
                            if (charge.codPosto.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EvStation,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = charge.codPosto,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }

                        // Valor kWh Alinhado na horizontal com os badges (à Direita) - Tamanho aumentado
                        if (charge.kwh > 0) {
                            val kwhText = remember(charge.kwh) { String.format(Locale.getDefault(), "%.1f kWh", charge.kwh) }
                            Text(
                                text = kwhText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

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
