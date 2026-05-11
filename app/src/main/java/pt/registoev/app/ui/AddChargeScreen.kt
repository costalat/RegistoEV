package pt.registoev.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.onFocusChanged
import android.content.Context
import androidx.core.content.edit
import pt.registoev.app.data.EvChargeEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddChargeScreen(
    existingCharges: List<EvChargeEntity>,
    editingRecord: EvChargeEntity? = null,
    onSave: (String, String, Int, String, Double, Long, Long?, String) -> Unit,
    onCancelEdit: () -> Unit = {},
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("registoev_prefs", Context.MODE_PRIVATE) }
    
    // Lista de sugestões dinâmica baseada nas preferências do utilizador
    var locations by remember { 
        mutableStateOf(
            sharedPrefs.getStringSet("custom_locations", setOf("Entroncamento", "Benavente"))
                ?.toList()?.sorted() ?: listOf("Benavente", "Entroncamento")
        )
    }

    var stationCodes by remember {
        mutableStateOf<List<String>>(
            sharedPrefs.getStringSet("custom_station_codes", emptySet())
                ?.toList()?.sorted() ?: emptyList()
        )
    }

    // Usamos um identificador do registo para forçar o reset do formulário
    val editId = editingRecord?.id ?: -1L
    
    var origin by remember(editId) { mutableStateOf(editingRecord?.origin ?: "") }
    var destination by remember(editId) { mutableStateOf(editingRecord?.destination ?: "") }
    var odometer by remember(editId) { mutableStateOf(editingRecord?.odometer?.toString() ?: "") }
    var chargeType by remember(editId) { mutableStateOf(editingRecord?.chargeType ?: "Nenhum") }
    var kwh by remember(editId) { mutableStateOf(TextFieldValue(editingRecord?.kwh?.toString() ?: "")) }
    var codPosto by remember(editId) { mutableStateOf(editingRecord?.codPosto ?: "") }
    var selectedDate by remember(editId) { mutableLongStateOf(editingRecord?.date ?: System.currentTimeMillis()) }
    
    var showDatePicker by remember { mutableStateOf(value = false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("pt", "PT")) }

    val odometerFocusRequester = remember { FocusRequester() }
    val kwhFocusRequester = remember { FocusRequester() }
    val codPostoFocusRequester = remember { FocusRequester() }

    val chargeTypes = listOf("Nenhum", "Portátil", "Rede MOBI.E", "Rede Exército")

    // Diálogos para gerir localizações
    var pendingLocationToAdd by remember { mutableStateOf<String?>(null) }
    var pendingLocationToDelete by remember { mutableStateOf<String?>(null) }
    
    var pendingStationCodeToAdd by remember { mutableStateOf<String?>(null) }
    var pendingStationCodeToDelete by remember { mutableStateOf<String?>(null) }

    // --- LOGICA DE VERIFICAÇÃO EM CADEIA ---
    fun checkAndSave(
        step: Int,
        currentLocs: List<String> = locations,
        currentCodes: List<String> = stationCodes
    ) {
        val tOrigin = origin.trim()
        val tDest = destination.trim()
        val tCode = codPosto.trim()
        val isMobie = chargeType == "Rede MOBI.E"

        when (step) {
            0 -> { // Passo 1: Verificar Origem
                if (tOrigin.isNotEmpty() && !currentLocs.any { it.equals(tOrigin, ignoreCase = true) }) {
                    pendingLocationToAdd = tOrigin
                } else {
                    checkAndSave(1, currentLocs, currentCodes)
                }
            }
            1 -> { // Passo 2: Verificar Destino
                if (tDest.isNotEmpty() && !currentLocs.any { it.equals(tDest, ignoreCase = true) }) {
                    pendingLocationToAdd = tDest
                } else {
                    checkAndSave(2, currentLocs, currentCodes)
                }
            }
            2 -> { // Passo 3: Verificar Código do Posto
                if (isMobie && tCode.isNotEmpty() && !currentCodes.any { it.equals(tCode, ignoreCase = true) }) {
                    pendingStationCodeToAdd = tCode
                } else {
                    checkAndSave(3) // Finalizar
                }
            }
            3 -> { // Passo Final: Guardar Efetivamente
                val odoVal = odometer.toIntOrNull() ?: 0
                val kwhVal = if (chargeType == "Nenhum") 0.0 else (kwh.text.toDoubleOrNull() ?: 0.0)
                onSave(origin, destination, odoVal, chargeType, kwhVal, selectedDate, editingRecord?.id, codPosto)
                if (editingRecord == null) {
                    origin = ""; destination = ""; odometer = ""; kwh = TextFieldValue(""); chargeType = "Nenhum"; codPosto = ""
                }
            }
        }
    }

    // Validação de Odómetro
    val odometerValue = odometer.toIntOrNull() ?: 0
    val otherCharges = existingCharges.filter { it.id != editingRecord?.id }
    val odoError = remember(odometerValue, selectedDate, otherCharges, editingRecord) {
        if (odometerValue <= 0) return@remember null
        if (editingRecord == null) {
            val absoluteMaxOdo = otherCharges.maxOfOrNull { it.odometer } ?: 0
            if (odometerValue <= absoluteMaxOdo) return@remember "Deve ser superior ao último registo: $absoluteMaxOdo km"
        } else {
            val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val startOfCurrentDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val startOfNextDay = calendar.timeInMillis
            val maxPrev = otherCharges.filter { it.date < startOfCurrentDay }.maxOfOrNull { it.odometer }
            if ((maxPrev != null) && (odometerValue < maxPrev)) return@remember "Mínimo permitido: $maxPrev km"
            val minNext = otherCharges.filter { it.date >= startOfNextDay }.minOfOrNull { it.odometer }
            if ((minNext != null) && (odometerValue > minNext)) return@remember "Máximo permitido: $minNext km"
        }
        null
    }

    // --- DIÁLOGOS DE CONFIRMAÇÃO ---
    if (pendingLocationToAdd != null) {
        val isOrigin = origin.trim() == pendingLocationToAdd
        AlertDialog(
            onDismissRequest = { pendingLocationToAdd = null },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Nova Localização", color = Color.White) },
            text = { Text("Deseja adicionar \"$pendingLocationToAdd\" às sugestões?", color = Color.LightGray) },
            confirmButton = {
                Button(onClick = {
                    val newSet = locations.toMutableSet().apply { add(pendingLocationToAdd!!) }
                    sharedPrefs.edit { putStringSet("custom_locations", newSet) }
                    val nextLocs = newSet.toList().sorted()
                    locations = nextLocs
                    pendingLocationToAdd = null
                    // Prosseguir: se era origem (passo 0), vai para destino (passo 1). Se era destino, vai para posto (passo 2).
                    checkAndSave(if (isOrigin) 1 else 2, nextLocs)
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    pendingLocationToAdd = null 
                    checkAndSave(if (isOrigin) 1 else 2)
                }) { Text("Não", color = Color.Gray) }
            }
        )
    }

    if (pendingStationCodeToAdd != null) {
        AlertDialog(
            onDismissRequest = { pendingStationCodeToAdd = null },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Novo Código de Posto", color = Color.White) },
            text = { Text("Deseja adicionar \"$pendingStationCodeToAdd\" às sugestões?", color = Color.LightGray) },
            confirmButton = {
                Button(onClick = {
                    val newSet = stationCodes.toMutableSet().apply { add(pendingStationCodeToAdd!!) }
                    sharedPrefs.edit { putStringSet("custom_station_codes", newSet) }
                    val nextCodes = newSet.toList().sorted()
                    stationCodes = nextCodes
                    pendingStationCodeToAdd = null
                    checkAndSave(3, currentCodes = nextCodes)
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    pendingStationCodeToAdd = null 
                    checkAndSave(3)
                }) { Text("Não", color = Color.Gray) }
            }
        )
    }

    // Diálogos de Apagar
    if (pendingLocationToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingLocationToDelete = null },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Apagar Localização", color = Color.White) },
            text = { Text("Remover \"$pendingLocationToDelete\" das sugestões?", color = Color.LightGray) },
            confirmButton = {
                Button(onClick = {
                    val newSet = locations.toMutableSet().apply { remove(pendingLocationToDelete!!) }
                    sharedPrefs.edit { putStringSet("custom_locations", newSet) }
                    locations = newSet.toList().sorted()
                    pendingLocationToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { pendingLocationToDelete = null }) { Text("Cancelar", color = Color.Gray) } }
        )
    }

    if (pendingStationCodeToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingStationCodeToDelete = null },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Apagar Código", color = Color.White) },
            text = { Text("Remover \"$pendingStationCodeToDelete\" das sugestões?", color = Color.LightGray) },
            confirmButton = {
                Button(onClick = {
                    val newSet = stationCodes.toMutableSet().apply { remove(pendingStationCodeToDelete!!) }
                    sharedPrefs.edit { putStringSet("custom_station_codes", newSet) }
                    stationCodes = newSet.toList().sorted()
                    pendingStationCodeToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { pendingStationCodeToDelete = null }) { Text("Cancelar", color = Color.Gray) } }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = Color.Gray) }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- DATA ---
        IOSRowCard {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Data", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                Text(text = dateFormatter.format(Date(selectedDate)), style = MaterialTheme.typography.titleLarge, color = Color.LightGray, fontWeight = FontWeight.Bold)
            }
        }

        // --- TRAJETO ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("TRAJETO", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, modifier = Modifier.padding(start = 12.dp))
            IOSRowCard {
                Column {
                    CompactInputField(label = "Origem", value = origin, onValueChange = { origin = it }, suggestions = locations, onLongPress = { pendingLocationToDelete = it })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                    CompactInputField(label = "Destino", value = destination, onValueChange = { destination = it }, suggestions = locations, onLongPress = { pendingLocationToDelete = it })
                }
            }
        }

        // --- VEÍCULO ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("VEÍCULO", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, modifier = Modifier.padding(start = 12.dp))
            IOSRowCard {
                Row(modifier = Modifier.fillMaxWidth().clickable { odometerFocusRequester.requestFocus() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Odómetro", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.width(120.dp), contentAlignment = Alignment.CenterEnd) {
                        BasicTextField(
                            value = odometer, onValueChange = { if (it.length <= 7) odometer = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(odometerFocusRequester),
                            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = Color.White),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(Color.White), singleLine = true,
                            decorationBox = { innerTextField ->
                                if (odometer.isEmpty()) Text("0", color = Color.DarkGray, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                innerTextField()
                            }
                        )
                    }
                    Text("km", color = Color.DarkGray, modifier = Modifier.padding(start = 4.dp))
                }
            }
            if (odoError != null) Text(text = odoError, color = Color(0xFFE53935), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
        }

        // --- CARREGAMENTO ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("TIPO DE CARGA", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray, modifier = Modifier.padding(start = 12.dp))
            IOSRowCard {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chargeTypes.forEach { type ->
                            val isSelected = chargeType == type
                            val hasEnergy = kwh.text.isNotBlank() && type != "Nenhum"
                            val activeColor = when (type) {
                                "Portátil" -> Color(0xFFFFB300)
                                "Rede MOBI.E" -> Color(0xFF26C6DA)
                                "Rede Exército" -> Color(0xFF046A38)
                                else -> Color.White.copy(alpha = 0.15f)
                            }
                            Box(
                                modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected && hasEnergy) activeColor else if (isSelected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                    .border(width = 1.dp, color = if (isSelected && hasEnergy) activeColor.copy(alpha = 0.5f) else if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent, shape = RoundedCornerShape(10.dp))
                                    .clickable { chargeType = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = type.split(" ").last(), color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    AnimatedVisibility(visible = chargeType != "Nenhum", enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    kwhFocusRequester.requestFocus()
                                    kwh = kwh.copy(selection = TextRange(0, kwh.text.length))
                                }, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Energia", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                                Box(modifier = Modifier.width(120.dp), contentAlignment = Alignment.CenterEnd) {
                                    BasicTextField(
                                        value = kwh, onValueChange = { kwh = it }, 
                                        modifier = Modifier.fillMaxWidth().focusRequester(kwhFocusRequester).onFocusChanged { if(it.isFocused) kwh = kwh.copy(selection = TextRange(0, kwh.text.length)) },
                                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), cursorBrush = SolidColor(Color(0xFF4CAF50)), singleLine = true,
                                        decorationBox = { innerTextField ->
                                            if (kwh.text.isEmpty()) Text("0.0", color = Color.DarkGray, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                            innerTextField()
                                        }
                                    )
                                }
                                Text("kWh", color = Color.DarkGray, modifier = Modifier.padding(start = 4.dp))
                            }
                            
                            if (chargeType == "Rede MOBI.E") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth().clickable { codPostoFocusRequester.requestFocus() }, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Código do Posto", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                                        Box(modifier = Modifier.width(140.dp), contentAlignment = Alignment.CenterEnd) {
                                            BasicTextField(
                                                value = codPosto, onValueChange = { codPosto = it.uppercase() }, modifier = Modifier.fillMaxWidth().focusRequester(codPostoFocusRequester),
                                                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = Color.White),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), cursorBrush = SolidColor(Color.White), singleLine = true,
                                                decorationBox = { innerTextField ->
                                                    if (codPosto.isEmpty()) Text("ABC-00000", color = Color.DarkGray, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                                    innerTextField()
                                                }
                                            )
                                        }
                                    }
                                    if (stationCodes.isNotEmpty()) {
                                        Row(modifier = Modifier.padding(top = 4.dp).fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.End) {
                                            val haptic = LocalHapticFeedback.current
                                            stationCodes.forEach { code ->
                                                Text(text = code, color = if (codPosto == code) Color.Black else Color.LightGray, fontSize = 11.sp,
                                                    modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(6.dp)).background(if (codPosto == code) Color.White else Color.White.copy(alpha = 0.08f))
                                                        .combinedClickable(onClick = { codPosto = code }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); pendingStationCodeToDelete = code })
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- BOTÃO GUARDAR ---
        Spacer(modifier = Modifier.height(8.dp))
        val isFormValid = origin.isNotBlank() && destination.isNotBlank() && odometer.isNotBlank() && odoError == null && (chargeType == "Nenhum" || kwh.text.isNotBlank())
        Button(
            onClick = { checkAndSave(0) },
            modifier = Modifier.fillMaxWidth().height(52.dp), enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(containerColor = if (isFormValid) Color(0xFF4CAF50) else Color.White, contentColor = if (isFormValid) Color.White else Color.Black, disabledContainerColor = Color.White.copy(alpha = 0.1f), disabledContentColor = Color.DarkGray),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (editingRecord != null) "ATUALIZAR REGISTO" else "GUARDAR REGISTO", fontWeight = FontWeight.Bold) }

        if (editingRecord != null) TextButton(onClick = onCancelEdit, modifier = Modifier.fillMaxWidth()) { Text("Cancelar", color = Color.LightGray) }
        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
fun IOSRowCard(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFF1C1C1E)) { content() }
}

@Composable
fun CompactInputField(label: String, value: String, onValueChange: (String) -> Unit, suggestions: List<String>, onLongPress: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.width(70.dp))
            TextField(value = value, onValueChange = onValueChange, placeholder = { Text("Obrigatório", color = Color.DarkGray, fontSize = 14.sp) }, colors = transparentTextFieldColors(), textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.padding(top = 2.dp, start = 70.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { loc ->
                val isSelected = value == loc
                Text(text = loc, color = if (isSelected) Color.Black else Color.LightGray, fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isSelected) Color.White else Color.White.copy(alpha = 0.08f))
                        .combinedClickable(onClick = { onValueChange(loc) }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress(loc) })
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White
)
