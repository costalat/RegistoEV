package pt.registoev.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Navigation
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
import java.util.Date
import java.util.Locale

@Composable
fun KmHistoryScreen(charges: List<EvChargeEntity>) {
    val sortedCharges = charges.sortedBy { it.date }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            DashboardSection(charges = charges)
        }
        
        item {
            Text(
                text = "MOVIMENTOS",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = Color.Gray,
                letterSpacing = 1.sp
            )
        }

        if (sortedCharges.isEmpty()) {
            item {
                EmptyState("Sem movimentos registados.")
            }
        } else {
            val listWithDiffs = sortedCharges.zipWithNext { a, b -> b to (b.odometer - a.odometer) }
            val kmItems = listWithDiffs.asReversed()
            
            items(kmItems) { (charge, diff) ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    KmItem(charge, diff)
                }
            }
            
            if (sortedCharges.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        KmItem(sortedCharges.first(), null)
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun KmItem(charge: EvChargeEntity, diff: Int?) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Cantos muito arredondados estilo iOS
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${charge.origin} → ${charge.destination}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Valor em destaque no canto superior direito (Estilo iOS)
                Text(
                    text = if (diff != null) "$diff km" else "Inicial",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF2C94C) // Amarelo/Dourado do exemplo
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Odómetro: ${charge.odometer} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = dateFormat.format(Date(charge.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ChargeHistoryScreen(charges: List<EvChargeEntity>) {
    val chargeRecords = charges.filter { it.chargeType != "Nenhum" }.sortedByDescending { it.odometer }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            NetworkEnergySection(charges = charges)
        }

        item {
            Text(
                text = "HISTÓRICO DE CARGAS",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = Color.Gray,
                letterSpacing = 1.sp
            )
        }
        
        if (chargeRecords.isEmpty()) {
            item {
                EmptyState("Sem carregamentos registados.")
            }
        } else {
            items(chargeRecords) { charge ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ChargeHistoryItem(charge)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun ChargeHistoryItem(charge: EvChargeEntity) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Cantos amplos estilo iOS Premium
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)), // Harmonizado com os movimentos
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // LINHA SUPERIOR: Local de Carga | Localidade | Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCAL DE CARGA",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray, // Harmonizado com a cor da data
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = charge.destination,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(charge.date)),
                    style = MaterialTheme.typography.bodyMedium, // Aumentado de labelSmall para bodyMedium
                    color = Color.LightGray, // Mudado de DarkGray para LightGray para maior visibilidade
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp)) // Reduzido de 28.dp para 16.dp para um look mais compacto
            
            // LINHA INFERIOR: Rede | kWh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Info Badge: Rede utilizada com fundo discreto
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = charge.chargeType,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Resultado Master: Destaque equilibrado para kWh
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", charge.kwh),
                        style = MaterialTheme.typography.headlineSmall, // Reduzido de displaySmall para headlineMedium
                        fontWeight = FontWeight.W900,
                        color = Color(0xFF4CAF50),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = " kWh",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4CAF50).copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
    }
}
