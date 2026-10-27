package pt.registoev.app.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class VehicleProfile(
    val plate: String,
    val brand: String = "",
    val model: String = ""
)

object VehicleManager {
    private val gson = Gson()

    fun getVehicles(prefs: SharedPreferences): List<VehicleProfile> {
        val json = prefs.getString("vehicles_json", null)
        if (!json.isNullOrBlank()) {
            return try {
                val type = object : TypeToken<List<VehicleProfile>>() {}.type
                val list: List<VehicleProfile> = gson.fromJson(json, type)
                val sanitized = list.map { v ->
                    if (v.plate.trim().uppercase() == "00-AA-00") v.copy(plate = "") else v
                }
                if (sanitized.isNotEmpty()) sanitized else getDefaultFromPrefs(prefs)
            } catch (_: Exception) {
                getDefaultFromPrefs(prefs)
            }
        }
        val defaultList = getDefaultFromPrefs(prefs)
        saveVehicles(prefs, defaultList)
        return defaultList
    }

    private fun getDefaultFromPrefs(prefs: SharedPreferences): List<VehicleProfile> {
        val rawPlate = prefs.getString("vehicle_plate", "")?.uppercase() ?: ""
        val plate = if (rawPlate == "00-AA-00") "" else rawPlate
        val brand = prefs.getString("vehicle_brand", "") ?: ""
        val model = prefs.getString("vehicle_model", "") ?: ""
        return listOf(VehicleProfile(plate = plate, brand = brand, model = model))
    }

    fun saveVehicles(prefs: SharedPreferences, list: List<VehicleProfile>) {
        val json = gson.toJson(list)
        prefs.edit { putString("vehicles_json", json) }
    }

    fun getActivePlate(prefs: SharedPreferences): String {
        val active = prefs.getString("vehicle_plate", "")?.uppercase() ?: ""
        if (active == "00-AA-00") {
            prefs.edit { putString("vehicle_plate", "") }
            return ""
        }
        if (active.isNotBlank()) return active
        val vehicles = getVehicles(prefs)
        val firstPlate = vehicles.firstOrNull()?.plate ?: ""
        setActiveVehicle(prefs, firstPlate)
        return firstPlate
    }

    fun setActiveVehicle(prefs: SharedPreferences, plate: String) {
        val formatted = if (plate.trim().uppercase() == "00-AA-00") "" else plate.trim().uppercase()
        val vehicles = getVehicles(prefs)
        val matched = vehicles.firstOrNull { it.plate.equals(formatted, ignoreCase = true) }
        
        prefs.edit {
            putString("vehicle_plate", formatted)
            if (matched != null) {
                putString("vehicle_brand", matched.brand)
                putString("vehicle_model", matched.model)
            }
        }
    }
}

fun formatVehiclePlateFromRaw(raw: String): String {
    val limited = raw.take(6)
    return when {
        limited.length <= 2 -> limited
        limited.length <= 4 -> "${limited.substring(0, 2)}-${limited.substring(2)}"
        else -> "${limited.substring(0, 2)}-${limited.substring(2, 4)}-${limited.substring(4)}"
    }
}

fun formatVehiclePlate(input: String, previous: String = ""): String {
    // Se o utilizador apagou o hífen via backspace (ex: "AA-" -> "AA")
    if (previous.endsWith("-") && !input.endsWith("-") && input.length == previous.length - 1) {
        val cleanPrev = previous.filter { it.isLetterOrDigit() }
        if (cleanPrev.length > 1) {
            val trimmed = cleanPrev.dropLast(1).uppercase()
            return formatVehiclePlateFromRaw(trimmed)
        }
    }
    
    val raw = input.filter { it.isLetterOrDigit() }.uppercase()
    return formatVehiclePlateFromRaw(raw)
}

fun calculatePlateCursorPosition(
    @Suppress("UNUSED_PARAMETER") oldText: String,
    @Suppress("UNUSED_PARAMETER") oldCursor: Int,
    formattedText: String,
    rawNewText: String,
    rawCursor: Int,
): Int {
    if (rawCursor >= rawNewText.length) {
        return formattedText.length
    }
    
    val newAlphanumericCount = rawNewText.take(rawCursor).count { it.isLetterOrDigit() }
    
    var count = 0
    var formattedIndex = 0
    while (formattedIndex < formattedText.length && count < newAlphanumericCount) {
        if (formattedText[formattedIndex].isLetterOrDigit()) {
            count++
        }
        formattedIndex++
    }
    
    if (formattedIndex < formattedText.length && formattedText[formattedIndex] == '-') {
        formattedIndex++
    }
    
    return formattedIndex.coerceIn(0, formattedText.length)
}

fun formatVehiclePlateTextFieldValue(newValue: TextFieldValue, oldValue: TextFieldValue): TextFieldValue {
    val oldText = oldValue.text
    val newText = newValue.text
    val formattedText = formatVehiclePlate(newText, oldText)
    
    if (formattedText == oldText && newText == oldText) {
        return newValue
    }
    
    val cursorIndex = calculatePlateCursorPosition(oldText, oldValue.selection.end, formattedText, newText, newValue.selection.end)
    return TextFieldValue(
        text = formattedText,
        selection = TextRange(cursorIndex.coerceIn(0, formattedText.length)),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onPlateActivated: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("registoev_prefs", Context.MODE_PRIVATE) }

    var vehicles by remember { mutableStateOf(VehicleManager.getVehicles(sharedPrefs)) }
    var activePlate by remember { mutableStateOf(VehicleManager.getActivePlate(sharedPrefs)) }

    var unidade by remember { 
        mutableStateOf(sharedPrefs.getString("user_unit", "") ?: "")
    }
    var condutor by remember { 
        mutableStateOf(sharedPrefs.getString("driver_name", "") ?: "") 
    }

    // Estado do formulário de veículo (Adicionar / Editar)
    var showVehicleDialog by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<VehicleProfile?>(null) }
    var inputPlateState by remember { mutableStateOf(TextFieldValue("")) }
    val inputPlate = inputPlateState.text
    var inputBrand by remember { mutableStateOf("") }
    var inputModel by remember { mutableStateOf("") }

    fun openAddDialog() {
        editingVehicle = null
        inputPlateState = TextFieldValue("")
        inputBrand = ""
        inputModel = ""
        showVehicleDialog = true
    }

    fun openEditDialog(v: VehicleProfile) {
        editingVehicle = v
        val plateClean = if (v.plate == "00-AA-00") "" else v.plate
        val formatted = formatVehiclePlate(plateClean)
        inputPlateState = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        inputBrand = v.brand
        inputModel = v.model
        showVehicleDialog = true
    }

    fun saveVehicleForm() {
        val rawClean = inputPlate.trim().uppercase()
        val cleanPlate = if (rawClean == "00-AA-00") "" else rawClean

        val newProfile = VehicleProfile(
            plate = cleanPlate,
            brand = inputBrand.trim(),
            model = inputModel.trim()
        )

        val updatedList = vehicles.toMutableList()
        if (editingVehicle != null) {
            val idx = updatedList.indexOfFirst { it.plate.equals(editingVehicle!!.plate, ignoreCase = true) }
            if (idx >= 0) updatedList[idx] = newProfile else updatedList.add(newProfile)
        } else {
            updatedList.removeAll { it.plate.equals(cleanPlate, ignoreCase = true) }
            updatedList.add(newProfile)
        }

        vehicles = updatedList
        VehicleManager.saveVehicles(sharedPrefs, updatedList)
        
        // Se for o único ou se estivermos a editar o ativo
        if (vehicles.size == 1 || (editingVehicle != null && editingVehicle!!.plate == activePlate)) {
            activePlate = cleanPlate
            VehicleManager.setActiveVehicle(sharedPrefs, cleanPlate)
        }
        showVehicleDialog = false
    }

    fun deleteVehicle(v: VehicleProfile) {
        if (vehicles.size <= 1) return // Manter pelo menos um veículo
        val updatedList = vehicles.filterNot { it.plate.equals(v.plate, ignoreCase = true) }
        vehicles = updatedList
        VehicleManager.saveVehicles(sharedPrefs, updatedList)

        if (activePlate.equals(v.plate, ignoreCase = true)) {
            val newActive = updatedList.first().plate
            activePlate = newActive
            VehicleManager.setActiveVehicle(sharedPrefs, newActive)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Definições",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // Card Unidade
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("UNIDADE / ESTABELECIMENTO", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }

                OutlinedTextField(
                    value = unidade,
                    onValueChange = { 
                        unidade = it
                        sharedPrefs.edit { putString("user_unit", it) }
                    },
                    label = { Text("Unidade / Estabelecimento / Órgão") },
                    placeholder = { Text("Ex: UAGME") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = settingsTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        // Card Identificação do Condutor (Global / Independente)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONDUTOR", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }

                OutlinedTextField(
                    value = condutor,
                    onValueChange = { 
                        condutor = it
                        sharedPrefs.edit { putString("driver_name", it) }
                    },
                    label = { Text("Nome do Condutor") },
                    placeholder = { Text("Ex: João Silva") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = settingsTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        // Card Gestão de Frota / Veículos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GESTÃO DE VEÍCULOS / FROTA", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { openAddDialog() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Veículo", tint = Color(0xFF4CAF50))
                    }
                }

                Text(
                    text = "Selecione o veículo ativo para registo e consulta de dados nas abas da aplicação:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                vehicles.forEach { v ->
                    val isActive = v.plate.equals(activePlate, ignoreCase = true)
                    val plateClean = if (v.plate == "00-AA-00") "" else v.plate
                    val displayPlate = if (plateClean.filter { it.isLetterOrDigit() }.length >= 6) plateClean else "??-??-??"
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activePlate = v.plate
                                VehicleManager.setActiveVehicle(sharedPrefs, v.plate)
                                onPlateActivated?.invoke(v.plate)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) Color(0xFF2C2C2E) else Color(0xFF141416)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isActive) 1.5.dp else 0.5.dp,
                            color = if (isActive) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // LINHA SUPERIOR: Nome do Veículo + Badge ATIVO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val titleText = listOf(v.brand, v.model).filter { it.isNotBlank() }.joinToString(" ")
                                Text(
                                    text = if (titleText.isNotBlank()) titleText else "Veículo sem nome",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ATIVO", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // LINHA INFERIOR: Matrícula + Botões de Ação
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Badge estilizado de Matrícula
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color.Black, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = displayPlate,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontSize = 13.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { openEditDialog(v) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Editar", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }

                                    if (vehicles.size > 1) {
                                        IconButton(
                                            onClick = { deleteVehicle(v) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { openAddDialog() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADICIONAR NOVO VEÍCULO", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }

    // Modal para Adicionar / Editar Veículo
    if (showVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showVehicleDialog = false },
            title = {
                Text(
                    text = if (editingVehicle != null) "Editar Veículo" else "Adicionar Veículo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputPlateState,
                        onValueChange = { newValue ->
                            val updatedState = formatVehiclePlateTextFieldValue(newValue, inputPlateState)
                            inputPlateState = updatedState
                            val updatedText = updatedState.text
                            // Atualizar em tempo real se este for o veículo ativo
                            if (editingVehicle != null && editingVehicle!!.plate == activePlate) {
                                activePlate = updatedText
                                VehicleManager.setActiveVehicle(sharedPrefs, updatedText)
                            }
                        },
                        label = { Text("Matrícula") },
                        placeholder = { Text("Ex: AA-23-CV") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputBrand,
                        onValueChange = { inputBrand = it },
                        label = { Text("Marca") },
                        placeholder = { Text("Ex: Tesla / Nissan") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputModel,
                        onValueChange = { inputModel = it },
                        label = { Text("Modelo") },
                        placeholder = { Text("Ex: Model 3 / Leaf") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveVehicleForm() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVehicleDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }
}

@Composable
fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color.White.copy(alpha = 0.3f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
    focusedLabelColor = Color.LightGray,
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color.White
)
