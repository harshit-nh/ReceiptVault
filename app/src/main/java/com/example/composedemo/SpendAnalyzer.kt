package com.example.composedemo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.util.UUID

// Modern and bright category colors
enum class SpendCategory(val displayName: String, val color: Color) {
    FOOD("Food", Color(0xFF3778E1)), // Main theme color
    TRANSPORT("Transport", Color(0xFF00D2FF)),
    RENT("Rent", Color(0xFFFF5252)),
    UTILITIES("Utilities", Color(0xFF4CAF50)),
    ENTERTAINMENT("Entertainment", Color(0xFFFF4081)),
    OTHERS("Others", Color(0xFF9C27B0))
}

data class SpendRecord(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val category: SpendCategory,
    val description: String,
    val date: LocalDate = LocalDate.now(),
    val cardId: Int? = null,
    val cardDisplay: String? = null // e.g. "Visa • 4242"
)

@Composable
fun SpendChart(records: List<SpendRecord>) {
    val categoryTotals = SpendCategory.entries.associateWith { category ->
        records.filter { it.category == category }.sumOf { it.amount }
    }.filter { it.value > 0 }

    val totalSpent = categoryTotals.values.sum()
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(records) {
        animationProgress.animateTo(1f, animationSpec = tween(1500))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Spending Distribution", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
                Text("Category-wise breakdown", fontSize = 12.sp, color = Color.Gray)
            }
            Box(modifier = Modifier.background(Color(0xFF3778E1).copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("Live", color = Color(0xFF3778E1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            if (totalSpent > 0) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    var startAngle = -90f
                    categoryTotals.forEach { (category, amount) ->
                        val sweepAngle = (amount / totalSpent * 360f).toFloat() * animationProgress.value
                        drawArc(
                            color = category.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", fontSize = 14.sp, color = Color.Gray)
                    Text("$${String.format("%.0f", totalSpent)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            } else {
                Text("No data to display", color = Color.LightGray)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        LegendGrid()
    }
}

@Composable
fun LegendGrid() {
    val categories = SpendCategory.entries.toList()
    Column {
        categories.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowItems.forEach { category ->
                    LegendItem(category, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun LegendItem(category: SpendCategory, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.size(10.dp).background(category.color, RoundedCornerShape(3.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(category.displayName, fontSize = 11.sp, maxLines = 1, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}
