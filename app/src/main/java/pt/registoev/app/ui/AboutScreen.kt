package pt.registoev.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.registoev.app.R

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // App Branding (Reajustado para 85dp - equilíbrio)
        Surface(
            modifier = Modifier
                .size(85.dp)
                .clip(RoundedCornerShape(22.dp)),
            color = Color(0xFF1E1E1E)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = Color(0xFF2196F3)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Registo EV",
            style = MaterialTheme.typography.headlineMedium, // Restaurado
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Versão 1.3",
            style = MaterialTheme.typography.bodySmall, // Restaurado
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp)) // Mais equilibrado
        
        // Main Description Card (Padding reajustado para 16dp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) { 
                Text(
                    text = "Sobre a Aplicação",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "O Registo EV simplifica a gestão da mobilidade elétrica, permitindo o controlo rigoroso de consumos e quilometragem.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    lineHeight = 20.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Features Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Funcionalidades",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                BulletPoint("Registo de movimentos")
                BulletPoint("Cálculo de kWh/100km")
                BulletPoint("Gráficos de energia")
                BulletPoint("Backup de dados")
                BulletPoint("Exportação de relatórios")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Credits Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Equipa e Desenvolvimento",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                CreditItemWithCustomIcon(
                    role = "Prova de conceito", 
                    name = "Copilot Premium", 
                    iconRes = R.drawable.ic_copilot
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))
                CreditItemWithCustomIcon(
                    role = "Desenvolvimento", 
                    name = "Gemini Pro", 
                    iconRes = R.drawable.ic_gemini
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))
                CreditItemWithCustomIcon(
                    role = "Especificação, revisão e controlo de qualidade",
                    name = "TC",
                    iconRes = R.drawable.ic_quality
                )
            }
        }
        
        Spacer(modifier = Modifier.height(110.dp)) // Espaço para a dock
    }
}

@Composable
fun CreditItemWithCustomIcon(role: String, name: String, iconRes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp) // Equilibrado
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
    }
}
