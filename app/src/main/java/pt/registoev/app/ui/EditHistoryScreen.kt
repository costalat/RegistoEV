package pt.registoev.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            title = { Text("Eliminar Registos", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Deseja remover ${selectedIds.size} registos?", color = Color.LightGray) },
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
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- TOP BAR / SELECTION TOOLBAR ---
        Crossfade(targetState = isSelectionMode, label = "toolbar") { isModeActive ->
            if (isModeActive) {
                Surface(
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { isSelectionMode = false; selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                        
                        Text(text = "${selectedIds.size} selecionados", color = Color.White, style = MaterialTheme.typography.titleMedium)

                        Row {
                            IconButton(onClick = { 
                                selectedIds = if (selectedIds.size == charges.size) emptySet() else charges.map { it.id }.toSet()
                            }) {
                                Icon(Icons.Default.SelectAll, null, tint = Color.White)
                            }
                            IconButton(onClick = { showDeleteConfirm = true }, enabled = selectedIds.isNotEmpty()) {
                                Icon(Icons.Default.Delete, null, tint = if (selectedIds.isNotEmpty()) Color(0xFFE53935) else Color.DarkGray)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gestão de Histórico",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    // BOTÃO DE APAGAR (CANTO SUPERIOR DIREITO)
                    IconButton(
                        onClick = { isSelectionMode = true },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedCharges, key = { it.id }) { charge ->
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
    val accentColor = when (charge.chargeType) {
        "Portátil" -> Color(0xFFFFB300)
        "Rede MOBI.E" -> Color(0xFF26C6DA)
        "Rede Exército" -> Color(0xFF046A38)
        else -> Color(0xFF2196F3)
    }

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
            containerColor = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.15f) else Color(0xFF141416)
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isSelected) Color(0xFF2196F3) else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // CABEÇALHO: Data e Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(charge.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3)),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CORPO: Trajeto (Horizontal) e Métricas (R)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Bloco Trajeto
                Row(
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${charge.origin} → ${charge.destination}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Lado Direito: Apenas Odómetro
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${charge.odometer}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = " km",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // RODAPÉ: Badges e kWh (Alinhados na base)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge: Rede
                        if (charge.chargeType != "Nenhum") {
                            Surface(color = Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = charge.chargeType, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                }
                            }
                        }

                        // Badge: Código do Posto
                        if (charge.codPosto.isNotEmpty()) {
                            Surface(color = Color.White.copy(alpha = 0.04f), shape = RoundedCornerShape(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(Icons.Default.EvStation, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = charge.codPosto, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    // Energia Baixa para a linha do posto
                    if (charge.kwh > 0) {
                        val kwhVal = remember(charge.kwh) { String.format(Locale.getDefault(), "%.1f", charge.kwh) }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = kwhVal, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                            Text(
                                text = " kWh",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
