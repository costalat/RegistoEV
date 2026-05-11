package pt.registoev.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.registoev.app.AppTab

@Composable
fun FloatingDockPreview() {
    var selectedTab by remember { mutableStateOf(AppTab.KM) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Fundo Dark Profundo
    ) {
        // Exemplo de conteúdo de fundo (cartões estilo a imagem)
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Movimentos Recentes", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            repeat(3) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2196F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("E", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Entroncamento", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("26 Abr 2024", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // A BARRA FLUTUANTE (Design sugerido)
        Surface(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E).copy(alpha = 0.95f), // Glassmorphism dark
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF2196F3) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { selectedTab = tab }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Color.White else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tab.title.split(" ").first(), 
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PreviewFloatingDock() {
    RegistoEVTheme(darkTheme = true) {
        FloatingDockPreview()
    }
}
