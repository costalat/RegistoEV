package pt.registoev.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
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
import androidx.compose.ui.platform.LocalContext
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
    val ctx = LocalContext.current
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
    
    // PDF Export States
    var pdfBaseCalendar by remember { mutableStateOf<Calendar?>(null) }
    var pdfChargesToExport by remember { mutableStateOf<List<EvChargeEntity>>(emptyList()) }

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

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && pdfBaseCalendar != null) {
            val success = exportMonthlyReportToPdf(ctx, uri, pdfChargesToExport, pdfBaseCalendar!!)
            if (success) {
                message = "Relatório PDF gerado com sucesso!"
                isError = false
            } else {
                message = "Erro ao gerar PDF oficial."
                isError = true
            }
            showExportOptions = false
            selectedExportType = null
            pdfBaseCalendar = null
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
                        val weekLabel = remember(start, dateRangePickerState.selectedEndDateMillis) {
                            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                            "Semana ${cal.get(Calendar.WEEK_OF_YEAR)} (${sdf.format(Date(start))} a ${sdf.format(Date(dateRangePickerState.selectedEndDateMillis ?: start))})"
                        }
                        Text(
                            text = weekLabel,
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
                
                if (selectedExportType == "Carregamentos") {
                    pdfBaseCalendar = cal
                    pdfChargesToExport = filterChargesForPeriod(charges, "Mês", cal)
                    // Não lançamos logo o launcher, deixamos o utilizador escolher CSV ou PDF Oficial no Step 3
                } else {
                    csvContentToExport = generateCsvForPeriod(charges, selectedExportType!!, "Mês", cal)
                    pendingFileName = "export_${selectedExportType!!.lowercase()}_mes_${year}_${month + 1}.csv"
                    csvLauncher.launch(pendingFileName)
                }
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

                            // Step 3: Escolha do Formato (apenas após escolher Mês + Carregamentos)
                            if (pdfBaseCalendar != null && selectedExportType == "Carregamentos") {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.05f))
                                Text("FORMATO DO RELATÓRIO", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
                                Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { 
                                            csvContentToExport = generateCsvForPeriod(charges, "Carregamentos", "Mês", pdfBaseCalendar!!)
                                            csvLauncher.launch("export_carregamentos_mes.csv") 
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("CSV Simples", color = Color.White)
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            val monthYear = SimpleDateFormat("MM_yyyy", Locale.getDefault()).format(pdfBaseCalendar!!.time)
                                            pdfLauncher.launch("reporte_mensal_${monthYear}.pdf") 
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("PDF Oficial", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = { 
                            showExportOptions = false
                            selectedExportType = null
                            selectedExportPeriodType = null
                            pdfBaseCalendar = null
                        },
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

fun filterChargesForPeriod(charges: List<EvChargeEntity>, periodType: String, calendar: Calendar): List<EvChargeEntity> {
    val startTime: Long
    val endTime: Long
    val baseCal = calendar.clone() as Calendar
    when (periodType) {
        "Semana" -> {
            baseCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            baseCal.set(Calendar.HOUR_OF_DAY, 0); baseCal.set(Calendar.MINUTE, 0); baseCal.set(Calendar.SECOND, 0); baseCal.set(Calendar.MILLISECOND, 0)
            startTime = baseCal.timeInMillis
            baseCal.add(Calendar.DAY_OF_YEAR, 7)
            endTime = baseCal.timeInMillis
        }
        "Mês" -> {
            baseCal.set(Calendar.DAY_OF_MONTH, 1)
            baseCal.set(Calendar.HOUR_OF_DAY, 0); baseCal.set(Calendar.MINUTE, 0); baseCal.set(Calendar.SECOND, 0); baseCal.set(Calendar.MILLISECOND, 0)
            startTime = baseCal.timeInMillis
            baseCal.add(Calendar.MONTH, 1)
            endTime = baseCal.timeInMillis
        }
        "Ano" -> {
            baseCal.set(Calendar.DAY_OF_YEAR, 1)
            baseCal.set(Calendar.HOUR_OF_DAY, 0); baseCal.set(Calendar.MINUTE, 0); baseCal.set(Calendar.SECOND, 0); baseCal.set(Calendar.MILLISECOND, 0)
            startTime = baseCal.timeInMillis
            baseCal.add(Calendar.YEAR, 1)
            endTime = baseCal.timeInMillis
        }
        else -> { startTime = 0; endTime = Long.MAX_VALUE }
    }
    return charges.filter { it.date in startTime until endTime }.sortedBy { it.date }
}

fun exportMonthlyReportToPdf(
    context: Context,
    uri: Uri,
    charges: List<EvChargeEntity>,
    calendar: Calendar
): Boolean {
    val prefs = context.getSharedPreferences("registoev_prefs", Context.MODE_PRIVATE)
    val unidade = prefs.getString("user_unit", "") ?: ""
    val condutor = prefs.getString("driver_name", "") ?: ""
    val marca = prefs.getString("vehicle_brand", "") ?: ""
    val modelo = prefs.getString("vehicle_model", "") ?: ""
    val matricula = prefs.getString("vehicle_plate", "") ?: ""

    val reportMonth = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT")).format(calendar.time).uppercase()
    val pdfDocument = PdfDocument()
    val textPaint = Paint().apply { textSize = 10f; isAntiAlias = true }
    val titlePaint = Paint().apply { 
        textSize = 12f 
        isFakeBoldText = true 
        isAntiAlias = true 
        textAlign = Paint.Align.CENTER 
    }
    
    // Dimensões A4 (72 DPI)
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f
    
    val filteredCharges = charges.filter { it.chargeType != "Nenhum" }
    var currentItemIndex = 0
    var pageNumber = 1

    while (currentItemIndex < filteredCharges.size || (filteredCharges.isEmpty() && currentItemIndex == 0)) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // --- 1. HEADER (APENAS NA PRIMEIRA PÁGINA) ---
        var currentY = 50f
        if (pageNumber == 1) {
            textPaint.textAlign = Paint.Align.CENTER
            val headerCenterX = pageWidth - margin - 60f
            
            canvas.drawText("VISTO", headerCenterX, currentY, textPaint)
            canvas.drawText("Cmdt/Dir/Chefe", headerCenterX, currentY + 15f, textPaint)
            
            canvas.drawLine(headerCenterX - 50f, currentY + 40f, headerCenterX + 50f, currentY + 40f, textPaint)
            canvas.drawLine(headerCenterX - 15f, currentY + 50f, headerCenterX + 15f, currentY + 50f, textPaint)

            currentY = 130f
            canvas.drawText("REPORTE MENSAL DOS CARREGAMENTOS ELÉTRICOS", pageWidth / 2f, currentY, titlePaint)

            currentY = 170f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("U/E/O: $unidade", margin, currentY, textPaint)
            canvas.drawLine(margin + 35f, currentY + 2f, margin + 180f, currentY + 2f, textPaint)
            
            canvas.drawText("Mês: $reportMonth", margin, currentY + 20f, textPaint)
            canvas.drawLine(margin + 25f, currentY + 22f, margin + 180f, currentY + 22f, textPaint)
            
            val veiculoStr = "Dados viatura (Marca/Modelo/Matrícula): $marca / $modelo / $matricula"
            canvas.drawText(veiculoStr, margin, currentY + 45f, textPaint)
            canvas.drawLine(margin + 175f, currentY + 47f, pageWidth - margin, currentY + 47f, textPaint)
            
            currentY = 235f // Posição de topo da tabela na página 1
        } else {
            currentY = 60f // Posição de topo da tabela nas páginas seguintes
        }

        // --- 4. TABELA ---
        val tableTop = currentY
        val colWidths = floatArrayOf(75f, 80f, 45f, 60f, 90f, 75f, 90f)
        val headers = arrayOf("Data de", "Tipologia de", "KW", "Km da", "Nome/Código", "Localidade", "Condutor")
        val headers2 = arrayOf("carregamento", "Carregamento", "abastecidos", "viatura", "Posto", "", "")
        
        var currentX = margin
        val rowHeight = 25f
        
        canvas.drawLine(margin, tableTop, pageWidth - margin, tableTop, textPaint)
        canvas.drawLine(margin, tableTop + rowHeight * 1.5f, pageWidth - margin, tableTop + rowHeight * 1.5f, textPaint)

        for (i in headers.indices) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(headers[i], currentX + 5f, tableTop + 15f, textPaint)
            canvas.drawText(headers2[i], currentX + 5f, tableTop + 27f, textPaint)
            canvas.drawLine(currentX, tableTop, currentX, tableTop + rowHeight * 1.5f, textPaint)
            currentX += colWidths[i]
        }
        canvas.drawLine(pageWidth - margin, tableTop, pageWidth - margin, tableTop + rowHeight * 1.5f, textPaint)

        // Desenhar Linhas de Dados
        var y = tableTop + rowHeight * 1.5f
        
        // Calcular quantos itens cabem nesta página específica
        val spaceAvailable = 720f - y // Deixar espaço para o rodapé se for a última página
        val itemsOnThisPage = (spaceAvailable / rowHeight).toInt().coerceAtMost(filteredCharges.size - currentItemIndex)
        
        for (i in 0 until itemsOnThisPage) {
            val charge = filteredCharges[currentItemIndex + i]
            currentX = margin
            
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(charge.date))
            val rowData = arrayOf(
                dateStr,
                charge.chargeType.replace("Rede ", ""),
                String.format(Locale.US, "%.1f", charge.kwh),
                charge.odometer.toString(),
                charge.codPosto,
                "", 
                condutor
            )

            for (j in rowData.indices) {
                canvas.drawText(rowData[j], currentX + 5f, y + 18f, textPaint)
                canvas.drawLine(currentX, y, currentX, y + rowHeight, textPaint)
                currentX += colWidths[j]
            }
            canvas.drawLine(pageWidth - margin, y, pageWidth - margin, y + rowHeight, textPaint)
            y += rowHeight
            canvas.drawLine(margin, y, pageWidth - margin, y, textPaint)
        }
        
        currentItemIndex += itemsOnThisPage
        if (filteredCharges.isEmpty()) currentItemIndex = 1
        val isLastPage = currentItemIndex >= filteredCharges.size

        // --- 5. RODAPÉ (APENAS NA ÚLTIMA PÁGINA) ---
        if (isLastPage) {
            val footerY = y + 40f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Tipologia de carregamento:", margin, footerY, textPaint)
            canvas.drawText("(a)  Postos da rede do Exército;", margin, footerY + 15f, textPaint)
            canvas.drawText("(b)  Tomada local da U/E/O;", margin, footerY + 28f, textPaint)
            canvas.drawText("(c)  Posto da rede pública (MOBI.E).", margin, footerY + 41f, textPaint)

            val signatureY = 780f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("O Chefe da SecLog", pageWidth - margin - 80f, signatureY, textPaint)
            canvas.drawLine(pageWidth - margin - 150f, signatureY + 25f, pageWidth - margin - 10f, signatureY + 25f, textPaint)
            canvas.drawLine(pageWidth - margin - 100f, signatureY + 35f, pageWidth - margin - 60f, signatureY + 35f, textPaint)
        }

        pdfDocument.finishPage(page)
        pageNumber++
    }

    return try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        pdfDocument.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        pdfDocument.close()
        false
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
