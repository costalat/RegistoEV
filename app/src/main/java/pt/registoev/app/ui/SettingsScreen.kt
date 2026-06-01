package pt.registoev.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("registoev_prefs", Context.MODE_PRIVATE) }

    var matricula by remember { 
        mutableStateOf(sharedPrefs.getString("vehicle_plate", "") ?: "") 
    }
    var marca by remember { 
        mutableStateOf(sharedPrefs.getString("vehicle_brand", "") ?: "") 
    }
    var modelo by remember { 
        mutableStateOf(sharedPrefs.getString("vehicle_model", "") ?: "")
    }
    var condutor by remember { 
        mutableStateOf(sharedPrefs.getString("driver_name", "") ?: "") 
    }
    var unidade by remember { 
        mutableStateOf(sharedPrefs.getString("user_unit", "") ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Definições",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Card Unidade
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("UNIDADE", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }

                OutlinedTextField(
                    value = unidade,
                    onValueChange = { 
                        unidade = it
                        sharedPrefs.edit { putString("user_unit", it) }
                    },
                    label = { Text("Unidade / Estabelecimento / Órgão") },
                    placeholder = { Text("Ex: Comando Territorial") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = settingsTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Card Identificação do Condutor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("CONDUTOR", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }

                OutlinedTextField(
                    value = condutor,
                    onValueChange = { 
                        condutor = it
                        sharedPrefs.edit { putString("driver_name", it) }
                    },
                    label = { Text("Nome do Condutor") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = settingsTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Card Identificação do Veículo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("VEÍCULO", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }

                OutlinedTextField(
                    value = marca,
                    onValueChange = { 
                        marca = it
                        sharedPrefs.edit { putString("vehicle_brand", it) }
                    },
                    label = { Text("Marca") },
                    placeholder = { Text("Ex: Tesla") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = settingsTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = { 
                            modelo = it
                            sharedPrefs.edit { putString("vehicle_model", it) }
                        },
                        label = { Text("Modelo") },
                        placeholder = { Text("Ex: Model 3") },
                        modifier = Modifier.weight(1f),
                        colors = settingsTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = matricula,
                        onValueChange = { 
                            matricula = it
                            sharedPrefs.edit { putString("vehicle_plate", it) }
                        },
                        label = { Text("Matrícula") },
                        placeholder = { Text("00-AA-00") },
                        modifier = Modifier.weight(1f),
                        colors = settingsTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
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
