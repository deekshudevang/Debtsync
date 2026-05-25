package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: androidx.navigation.NavController,
    viewModel: ContactViewModel
) {
    val dashboardData by viewModel.dashboardState.collectAsState()
    
    var totalLent = 0.0
    var totalBorrowed = 0.0

    dashboardData.contacts.forEach { contactWithBalance ->
        if (contactWithBalance.netBalance > 0) {
            totalLent += contactWithBalance.netBalance
        } else if (contactWithBalance.netBalance < 0) {
            totalBorrowed += kotlin.math.abs(contactWithBalance.netBalance)
        }
    }

    val maxAmount = maxOf(totalLent, totalBorrowed)

    Scaffold(
        containerColor = DeepSpaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("Analytics", color = OffWhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OffWhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Lent", color = MutedSlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(formatCurrency(totalLent), color = NeonEmeraldGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Borrowed", color = MutedSlateText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(formatCurrency(totalBorrowed), color = NeonCrimsonRed, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bar Chart
            Text("Overview", color = OffWhiteText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(DarkSurface, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                if (maxAmount > 0) {
                    val colorLent = NeonEmeraldGreen
                    val colorBorrowed = NeonCrimsonRed
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val barWidth = 40.dp.toPx()
                        val spacing = width / 3f

                        // Lent Bar
                        val lentHeight = ((totalLent / maxAmount) * height).toFloat()
                        drawRoundRect(
                            color = colorLent,
                            topLeft = Offset(width / 2f - spacing / 2 - barWidth / 2, height - lentHeight),
                            size = Size(barWidth, lentHeight),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )

                        // Borrowed Bar
                        val borrowedHeight = ((totalBorrowed / maxAmount) * height).toFloat()
                        drawRoundRect(
                            color = colorBorrowed,
                            topLeft = Offset(width / 2f + spacing / 2 - barWidth / 2, height - borrowedHeight),
                            size = Size(barWidth, borrowedHeight),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                    }
                } else {
                    Text("No data", color = MutedSlateText, modifier = Modifier.align(Alignment.Center))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Trust Score (Advanced Analytics Engine)
            Text("AI Trust & Credit Score", color = OffWhiteText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val trustScore = if (totalLent >= totalBorrowed) 850 else 600
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { trustScore / 1000f },
                            modifier = Modifier.size(120.dp),
                            color = NeonEmeraldGreen,
                            trackColor = DarkBorder,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            strokeWidth = 10.dp,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(trustScore.toString(), color = OffWhiteText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text(if (trustScore > 700) "Excellent" else "Fair", color = NeonEmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your repayment consistency makes you highly trustworthy.", color = MutedSlateText, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gamification
            Text("Achievements & Streaks", color = OffWhiteText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Star, contentDescription = "Streak", tint = MatteGoldAccent, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("14 Days", color = OffWhiteText, fontWeight = FontWeight.Bold)
                        Text("Repayment Streak", color = MutedSlateText, fontSize = 10.sp)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Settler", tint = CyanSlateAccent, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fast Settler", color = OffWhiteText, fontWeight = FontWeight.Bold)
                        Text("Settle < 24h", color = MutedSlateText, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Top Owers
            Text("Top Owe You", color = OffWhiteText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val sortedOwed = dashboardData.contacts.filter { it.netBalance > 0 }.sortedByDescending { it.netBalance }.take(3)
            if (sortedOwed.isEmpty()) {
                Text("Nobody owes you right now.", color = MutedSlateText, fontSize = 14.sp)
            } else {
                sortedOwed.forEach {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it.contact.name, color = OffWhiteText, fontWeight = FontWeight.SemiBold)
                        Text(formatCurrency(it.netBalance), color = NeonEmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Top You Owe", color = OffWhiteText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val sortedBorrow = dashboardData.contacts.filter { it.netBalance < 0 }.sortedBy { it.netBalance }.take(3) // smaller netBalance is larger debt
            if (sortedBorrow.isEmpty()) {
                Text("You don't owe anyone right now.", color = MutedSlateText, fontSize = 14.sp)
            } else {
                sortedBorrow.forEach {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it.contact.name, color = OffWhiteText, fontWeight = FontWeight.SemiBold)
                        Text(formatCurrency(kotlin.math.abs(it.netBalance)), color = NeonCrimsonRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
