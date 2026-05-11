package pt.registoev.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import pt.registoev.app.data.EvChargeEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onImport: (List<EvChargeEntity>) -> Unit,
    onExportToFile: (Uri) -> Unit,
    onImportFromFile: (Uri) -> Unit,
    charges: List<EvChargeEntity>,
    onExportCsv: (Uri, String) -> Unit
) {
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // State for CSV Export Workflow
    var showExportOptions by remember { mutableStateOf(false) }
    var selectedExportType by remember { mutableStateOf<String?>(null) } // "Movimentos" or "Carregamentos"
    var selectedExportPeriodType by remember { mutableStateOf<String?>(null) } // "Semana", "Mês", "Ano"
    
    // Pickers State
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    
    var csvContentToExport by remember { mutableStateOf("") }
    var pendingFileName by remember { mutableStateOf("") }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportCsv(uri, csvContentToExport)
            message = "Exportação CSV concluída!"
            isError = false
            showExportOptions = false
            selectedExportType = null
            selectedExportPeriodType = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportToFile(uri)
            message = "Backup (JSON) concluído!"
            isError = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportFromFile(uri)
            message = "Backup restaurado com sucesso!"
            isError = false
        }
    }

    // --- PICKERS ---
    
    // Day Picker (For Week - selects start date)
    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        
        // Auto-selecionar a semana completa ao tocar num dia
        LaunchedEffect(dateRangePickerState.selectedStartDateMillis) {
            dateRangePickerState.selectedStartDateMillis?.let { start ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { 
                    timeInMillis = start
                }
                
                // Calcular quantos dias recuar até ao Domingo (Domingo = 1)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = dayOfWeek - Calendar.SUNDAY
                
                cal.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                
                val sunday = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val saturday = cal.timeInMillis
                
                // Só atualiza se for diferente para evitar loop infinito
                if (dateRangePickerState.selectedStartDateMillis != sunday || dateRangePickerState.selectedEndDateMillis != saturday) {
                    dateRangePickerState.setSelection(sunday, saturday)
                }
            }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { start ->
                        val cal = Calendar.getInstance().apply { 
                            timeInMillis = start
                            firstDayOfWeek = Calendar.SUNDAY
                        }
                        val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
                        val year = cal.get(Calendar.YEAR)
                        
                        csvContentToExport = generateCsvForPeriod(charges, selectedExportType!!, "Semana", cal)
                        pendingFileName = "export_${selectedExportType!!.lowercase()}_semana_${weekNum}_${year}.csv"
                        csvLauncher.launch(pendingFileName)
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            }
        ) { 
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Selecionar Semana", modifier = Modifier.padding(16.dp)) },
                headline = { 
                    val start = dateRangePickerState.selectedStartDateMillis
                    if (start != null) {
                        val cal = Calendar.getInstance().apply { timeInMillis = start }
                        Text(
                            text = "Semana ${cal.get(Calendar.WEEK_OF_YEAR)} (${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(start))} a ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(dateRangePickerState.selectedEndDateMillis ?: start))})",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2196F3)
                        )
                    } else {
                        Text("Toque num dia para escolher a semana", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                },
                showModeToggle = false,
                modifier = Modifier.height(450.dp) // Ajuste para caber no diálogo
            )
        }
    }

    // Month Picker
    if (showMonthPicker) {
        MonthYearPickerDialog(
            onDismiss = { showMonthPicker = false },
            onConfirm = { month, year ->
                val cal = Calendar.getInstance().apply { 
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                }
                csvContentToExport = generateCsvForPeriod(charges, selectedExportType!!, "Mês", cal)
                pendingFileName = "export_${selectedExportType!!.lowercase()}_mes_${year}_${month + 1}.csv"
                csvLauncher.launch(pendingFileName)
                showMonthPicker = false
            }
        )
    }

    // Year Picker
    if (showYearPicker) {
        YearPickerDialog(
            onDismiss = { showYearPicker = false },
            onConfirm = { year ->
                val cal = Calendar.getInstance().apply { set(Calendar.YEAR, year) }
                csvContentToExport = generateCsvForPeriod(charges, selectedExportType!!, "Ano", cal)
                pendingFileName = "export_${selectedExportType!!.lowercase()}_ano_$year.csv"
                csvLauncher.launch(pendingFileName)
                showYearPicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SECÇÃO EXPORTAR (CSV) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.IosShare, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Relatórios", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!showExportOptions) {
                    Button(
                        onClick = { showExportOptions = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Configurar Exportação", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Step 1: Tipo
                    Text("TIPO DE DADOS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                    Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Movimentos", "Carregamentos").forEach { type ->
                            FilterChip(
                                selected = selectedExportType == type,
                                onClick = { selectedExportType = type; selectedExportPeriodType = null },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = Color.Black,
                                    labelColor = Color.LightGray,
                                    containerColor = Color.White.copy(alpha = 0.05f)
                                ),
                                border = null
                            )
                        }
                    }

                    // Step 2: Escolha do Período
                    AnimatedVisibility(visible = selectedExportType != null) {
                        Column {
                            Text("INTERVALO TEMPORAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                            Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExportOptionChip("Semana", Icons.Default.CalendarViewWeek) { showDatePicker = true }
                                ExportOptionChip("Mês", Icons.Default.CalendarMonth) { showMonthPicker = true }
                                ExportOptionChip("Ano", Icons.Default.CalendarToday) { showYearPicker = true }
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = { showExportOptions = false; selectedExportType = null; selectedExportPeriodType = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = Color.DarkGray)
                    }
                }
            }
        }

        // --- SECÇÃO BACKUP (JSON) ---
        BackupActionCard(
            title = "Arquivo de Segurança",
            description = "Gere um ficheiro comprimido com a totalidade da sua base de dados.",
            icon = Icons.Default.Inventory2,
            buttonText = "Criar Backup",
            buttonColor = Color.White.copy(alpha = 0.08f),
            textColor = Color.White,
            onClick = {
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                exportLauncher.launch("registoev_full_backup_$dateStr.json")
            }
        )

        BackupActionCard(
            title = "Restauro",
            description = "Importe um ficheiro de salvaguarda para recuperar todos os seus registos.",
            icon = Icons.Default.History,
            buttonText = "Selecionar Ficheiro",
            buttonColor = Color.White.copy(alpha = 0.08f),
            textColor = Color.White,
            onClick = { importLauncher.launch(arrayOf("application/json")) }
        )

        // FEEDBACK MESSAGE
        if (message != null) {
            Surface(
                color = if (isError) Color(0xFFF44336).copy(alpha = 0.1f) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = if (isError) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = message!!, color = if (isError) Color(0xFFE57373) else Color(0xFF81C784), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
fun ExportOptionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
        colors = InputChipDefaults.inputChipColors(
            containerColor = Color(0xFF2C2C2E),
            labelColor = Color.White,
            leadingIconColor = Color.White
        ),
        border = null,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun MonthYearPickerDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val months = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        title = { Text("Selecionar Mês", color = Color.White) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedYear-- }) { Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
                    Text(text = selectedYear.toString(), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = { selectedYear++ },
                        enabled = selectedYear < currentYear
                    ) { 
                        Icon(
                            Icons.Default.ChevronRight, 
                            null, 
                            tint = if (selectedYear < currentYear) Color.White else Color.DarkGray
                        ) 
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(months) { index, month ->
                        val isFutureMonth = selectedYear == currentYear && index > currentMonth
                        val isSelected = selectedMonth == index
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> Color(0xFF2196F3)
                                        isFutureMonth -> Color.Transparent
                                        else -> Color.White.copy(alpha = 0.05f)
                                    }
                                )
                                .clickable(enabled = !isFutureMonth) { selectedMonth = index }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = month, 
                                color = when {
                                    isSelected -> Color.White
                                    isFutureMonth -> Color.DarkGray
                                    else -> Color.Gray
                                }, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedMonth, selectedYear) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun YearPickerDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        title = { Text("Selecionar Ano", color = Color.White) },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedYear-- }) { Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }
                Text(text = selectedYear.toString(), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { selectedYear++ },
                    enabled = selectedYear < currentYear
                ) { 
                    Icon(
                        Icons.Default.ChevronRight, 
                        null, 
                        tint = if (selectedYear < currentYear) Color.White else Color.DarkGray
                    ) 
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedYear) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun BackupActionCard(title: String, description: String, icon: ImageVector, buttonText: String, buttonColor: Color, textColor: Color = Color.Black, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = buttonColor), shape = RoundedCornerShape(12.dp)) {
                Text(buttonText, fontWeight = FontWeight.Bold, color = textColor)
            }
        }
    }
}

fun generateCsvForPeriod(charges: List<EvChargeEntity>, type: String, periodType: String, calendar: Calendar): String {
    val startTime: Long
    val endTime: Long
    
    val baseCal = calendar.clone() as Calendar
    when (periodType) {
        "Semana" -> {
            // Ajustar explicitamente para o Domingo anterior (00:00:00)
            baseCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            baseCal.set(Calendar.HOUR_OF_DAY, 0)
            baseCal.set(Calendar.MINUTE, 0)
            baseCal.set(Calendar.SECOND, 0)
            baseCal.set(Calendar.MILLISECOND, 0)
            startTime = baseCal.timeInMillis
            
            // Adicionar 7 dias para chegar ao próximo Domingo (exclui o início do próximo Domingo)
            baseCal.add(Calendar.DAY_OF_YEAR, 7)
            endTime = baseCal.timeInMillis
        }
        "Mês" -> {
            baseCal.set(Calendar.DAY_OF_MONTH, 1)
            baseCal.set(Calendar.HOUR_OF_DAY, 0); baseCal.set(Calendar.MINUTE, 0); baseCal.set(Calendar.SECOND, 0)
            startTime = baseCal.timeInMillis
            baseCal.add(Calendar.MONTH, 1)
            endTime = baseCal.timeInMillis
        }
        "Ano" -> {
            baseCal.set(Calendar.DAY_OF_YEAR, 1)
            baseCal.set(Calendar.HOUR_OF_DAY, 0); baseCal.set(Calendar.MINUTE, 0); baseCal.set(Calendar.SECOND, 0)
            startTime = baseCal.timeInMillis
            baseCal.add(Calendar.YEAR, 1)
            endTime = baseCal.timeInMillis
        }
        else -> { startTime = 0; endTime = Long.MAX_VALUE }
    }

    val filtered = charges.filter { it.date in startTime until endTime }
    val sb = StringBuilder()
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (type == "Movimentos") {
        sb.append("Data;Origem;Destino;Odometer\n")
        filtered.sortedBy { it.date }.forEach {
            sb.append("${df.format(Date(it.date))};${it.origin};${it.destination};${it.odometer}\n")
        }
    } else {
        sb.append("Data;Local;Tipo Carga;Cod. Posto;kWh\n")
        filtered.filter { it.chargeType != "Nenhum" }.sortedBy { it.date }.forEach {
            sb.append("${df.format(Date(it.date))};${it.destination};${it.chargeType};${it.codPosto};${it.kwh}\n")
        }
    }
    return sb.toString()
}

fun parseJsonBackup(json: String): List<EvChargeEntity> {
    val result = mutableListOf<EvChargeEntity>()
    val array = JSONArray(json)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val dateValue = if (obj.has("data") && obj.get("data") is String) {
            dateFormat.parse(obj.getString("data"))?.time ?: System.currentTimeMillis()
        } else if (obj.has("date")) {
            obj.getLong("date")
        } else {
            System.currentTimeMillis()
        }
        result.add(EvChargeEntity(
            id = if (obj.has("id")) obj.getLong("id") else 0,
            origin = if (obj.has("origem")) obj.getString("origem") else obj.optString("origin", ""),
            destination = if (obj.has("destino")) obj.getString("destino") else obj.optString("destination", ""),
            odometer = if (obj.has("odo")) obj.getInt("odo") else obj.optInt("odometer", 0),
            chargeType = if (obj.has("tipoCarga")) obj.getString("tipoCarga") else obj.optString("chargeType", "Nenhum"),
            kwh = if (obj.has("kw")) obj.getDouble("kw") else obj.optDouble("kwh", 0.0),
            date = dateValue,
            codPosto = if (obj.has("codPosto")) obj.getString("codPosto") else obj.optString("stationCode", "")
        ))
    }
    return result
}

fun exportRecordsToJson(charges: List<EvChargeEntity>): String {
    val array = JSONArray()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    for (charge in charges) {
        val obj = org.json.JSONObject()
        obj.put("id", charge.id)
        obj.put("data", dateFormat.format(Date(charge.date)))
        obj.put("origem", charge.origin)
        obj.put("destino", charge.destination)
        obj.put("odo", charge.odometer)
        obj.put("kw", charge.kwh)
        obj.put("tipoCarga", charge.chargeType)
        obj.put("codPosto", charge.codPosto)
        array.put(obj)
    }
    return array.toString(2)
}
