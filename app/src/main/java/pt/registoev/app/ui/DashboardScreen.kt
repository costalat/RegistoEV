package pt.registoev.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.registoev.app.data.EvChargeEntity
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardSection(charges: List<EvChargeEntity>) {
    var selectedPeriodType by remember { mutableStateOf(HistoryPeriod.MONTH) }
    var baseCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showSelectors by remember { mutableStateOf(value = false) }
    
    // Pickers State
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    // --- PICKERS LOGIC ---
    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        LaunchedEffect(dateRangePickerState.selectedStartDateMillis) {
            dateRangePickerState.selectedStartDateMillis?.let { start ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = start }
                // ISO 8601: Segunda-feira como primeiro dia
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                
                cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                
                val monday = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val sunday = cal.timeInMillis
                
                if (dateRangePickerState.selectedStartDateMillis != monday || dateRangePickerState.selectedEndDateMillis != sunday) {
                    dateRangePickerState.setSelection(monday, sunday)
                }
            }
        }
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { start ->
                        baseCalendar = Calendar.getInstance().apply { timeInMillis = start }
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            }
        ) { 
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Selecionar Semana", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.height(450.dp)
            )
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            onDismiss = { showMonthPicker = false },
            onConfirm = { m: Int, y: Int ->
                baseCalendar = Calendar.getInstance().apply { 
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                }
                showMonthPicker = false
            }
        )
    }

    if (showYearPicker) {
        YearPickerDialog(
            onDismiss = { showYearPicker = false },
            onConfirm = { y: Int ->
                baseCalendar = Calendar.getInstance().apply { set(Calendar.YEAR, y) }
                showYearPicker = false
            }
        )
    }

    // Filtragem baseada no período escolhido
    val calculations = remember(charges, selectedPeriodType, baseCalendar) {
        val cal = baseCalendar.clone() as Calendar
        val startTime: Long
        val endTime: Long
        
        // Normalizar o calendário base para o início do dia local
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        
        when (selectedPeriodType) {
            HistoryPeriod.WEEK -> {
                // ISO 8601: Segunda-feira como primeiro dia da semana
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                startTime = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                endTime = cal.timeInMillis
            }
            HistoryPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                startTime = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                endTime = cal.timeInMillis
            }
            HistoryPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                startTime = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                endTime = cal.timeInMillis
            }
        }
        
        // 1. Calcular a distância de cada viagem em todo o histórico
        val sortedAll = charges.sortedBy { it.date }
        val trips = sortedAll.zipWithNext { a, b -> b to (b.odometer - a.odometer) }
        
        // 2. Filtrar apenas as viagens que TERMINARAM dentro do período selecionado
        val periodTrips = trips.filter { (record, _) -> record.date in startTime until endTime }
        val totalKm = periodTrips.sumOf { it.second }
        
        // 3. Filtrar os registos (para kWh) que pertencem ao período
        val periodCharges = charges.filter { it.date in startTime until endTime }

        // 4. Calcular Consumo Médio
        val totalKwh = periodCharges.filter { it.chargeType != "Nenhum" }.sumOf { it.kwh }
        val avgConsumption = if (totalKm > 0) (totalKwh * 100.0) / totalKm else 0.0

        Triple(totalKm, avgConsumption, periodCharges)
    }

    val totalKm = calculations.first
    val avgConsumption = calculations.second

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "PT")) }
        val periodDisplay = when(selectedPeriodType) {
            HistoryPeriod.WEEK -> "Semana ${baseCalendar.get(Calendar.WEEK_OF_YEAR)}"
            HistoryPeriod.MONTH -> monthFormat.format(baseCalendar.time)
            HistoryPeriod.YEAR -> baseCalendar.get(Calendar.YEAR).toString()
        }

        Text(
            text = "Estatísticas ($periodDisplay)",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showSelectors = !showSelectors },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Total Km",
                value = totalKm.toString(),
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Consumo Médio",
                value = remember(avgConsumption) { String.format(Locale.getDefault(), "%.1f", avgConsumption) },
                unit = "kWh/100",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1.2f)
            )
        }

        AnimatedVisibility(visible = showSelectors) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryPeriod.entries.forEach { period ->
                    val isSelected = selectedPeriodType == period
                    val label = when(period) {
                        HistoryPeriod.WEEK -> "Semana"
                        HistoryPeriod.MONTH -> "Mês"
                        HistoryPeriod.YEAR -> "Ano"
                    }
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            selectedPeriodType = period
                            when(period) {
                                HistoryPeriod.WEEK -> showDatePicker = true
                                HistoryPeriod.MONTH -> showMonthPicker = true
                                HistoryPeriod.YEAR -> showYearPicker = true
                            }
                            showSelectors = false
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2196F3),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1E1E),
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 0.dp,
                            enabled = true,
                            selected = isSelected
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

enum class HistoryPeriod { WEEK, MONTH, YEAR }

@Composable
fun NetworkEnergySection(charges: List<EvChargeEntity>) {
    var selectedPeriodType by remember { mutableStateOf(HistoryPeriod.MONTH) }
    var baseCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showSelectors by remember { mutableStateOf(value = false) }
    
    // Pickers State
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    // --- PICKERS LOGIC ---
    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        LaunchedEffect(dateRangePickerState.selectedStartDateMillis) {
            dateRangePickerState.selectedStartDateMillis?.let { start ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = start }
                // ISO 8601: Segunda-feira como primeiro dia
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                
                cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                
                val monday = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val sunday = cal.timeInMillis
                
                if (dateRangePickerState.selectedStartDateMillis != monday || dateRangePickerState.selectedEndDateMillis != sunday) {
                    dateRangePickerState.setSelection(monday, sunday)
                }
            }
        }
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { start ->
                        baseCalendar = Calendar.getInstance().apply { timeInMillis = start }
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            }
        ) { 
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Selecionar Semana", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.height(450.dp)
            )
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            onDismiss = { showMonthPicker = false },
            onConfirm = { m: Int, y: Int ->
                baseCalendar = Calendar.getInstance().apply { 
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                }
                showMonthPicker = false
            }
        )
    }

    if (showYearPicker) {
        YearPickerDialog(
            onDismiss = { showYearPicker = false },
            onConfirm = { y: Int ->
                baseCalendar = Calendar.getInstance().apply { set(Calendar.YEAR, y) }
                showYearPicker = false
            }
        )
    }

    // Filtragem baseada no período
    val filteredCharges = remember(charges, selectedPeriodType, baseCalendar) {
        val cal = baseCalendar.clone() as Calendar
        val startTime: Long
        val endTime: Long
        
        // Normalizar para o início do dia local
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)

        when (selectedPeriodType) {
            HistoryPeriod.WEEK -> {
                // ISO 8601: Segunda-feira como primeiro dia da semana
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                startTime = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 7)
                endTime = cal.timeInMillis
            }
            HistoryPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                startTime = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                endTime = cal.timeInMillis
            }
            HistoryPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                startTime = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                endTime = cal.timeInMillis
            }
        }
        
        charges.filter { it.date in startTime until endTime && it.chargeType != "Nenhum" }
    }

    val consumptionByType = filteredCharges.groupBy { it.chargeType }
        .mapValues { it.value.sumOf { c -> c.kwh }.toFloat() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "PT")) }
        val periodDisplay = when(selectedPeriodType) {
            HistoryPeriod.WEEK -> "Semana ${baseCalendar.get(Calendar.WEEK_OF_YEAR)}"
            HistoryPeriod.MONTH -> monthFormat.format(baseCalendar.time)
            HistoryPeriod.YEAR -> baseCalendar.get(Calendar.YEAR).toString()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Energia por Rede ($periodDisplay)",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.clickable { showSelectors = !showSelectors }) {
            BarChart(data = consumptionByType)
        }

        AnimatedVisibility(visible = showSelectors) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryPeriod.entries.forEach { period ->
                    val isSelected = selectedPeriodType == period
                    val label = when(period) {
                        HistoryPeriod.WEEK -> "Semana"
                        HistoryPeriod.MONTH -> "Mês"
                        HistoryPeriod.YEAR -> "Ano"
                    }
                    
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            selectedPeriodType = period
                            when(period) {
                                HistoryPeriod.WEEK -> showDatePicker = true
                                HistoryPeriod.MONTH -> showMonthPicker = true
                                HistoryPeriod.YEAR -> showYearPicker = true
                            }
                            showSelectors = false
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2196F3),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1E1E),
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 0.dp,
                            enabled = true,
                            selected = isSelected
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String = "",
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = unit, color = color, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun BarChart(data: Map<String, Float>) {
    val maxVal = (data.values.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { (label, value) ->
            val animatedWidth by animateFloatAsState(
                targetValue = value / maxVal,
                animationSpec = tween(durationMillis = 1000),
                label = ""
            )

            val barColor = when {
                label.contains("Portátil", ignoreCase = true) -> Color(0xFFFFB300) // Âmbar
                label.contains("MOBI.E", ignoreCase = true) -> Color(0xFF26C6DA) // Ciano
                label.contains("Exército", ignoreCase = true) -> Color(0xFF046A38) // Verde Exército
                else -> Color(0xFF2196F3) // Azul padrão
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, color = Color.White, fontSize = 12.sp)
                    val valueText = remember(value) { String.format(Locale.getDefault(), "%.1f kWh", value) }
                    Text(text = valueText, color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedWidth)
                            .fillMaxHeight()
                            .background(
                                color = barColor,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
        
        if (data.isEmpty()) {
            Text(
                text = "Sem dados de carga disponíveis",
                color = Color.DarkGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
