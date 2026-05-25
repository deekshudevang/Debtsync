package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    navController: androidx.navigation.NavController,
    viewModel: ContactViewModel
) {
    val dashboardData by viewModel.dashboardState.collectAsState()
    var billName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    val selectedContacts = remember { mutableStateListOf<Long>() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = DeepSpaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("Split Bill", color = OffWhiteText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OffWhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = billName,
                onValueChange = { billName = it },
                label = { Text("What was this for?", color = MutedSlateText) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OffWhiteText,
                    unfocusedTextColor = OffWhiteText,
                    focusedBorderColor = CyanSlateAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = totalAmount,
                onValueChange = { totalAmount = it },
                label = { Text("Total Amount", color = MutedSlateText) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeonEmeraldGreen,
                    unfocusedTextColor = NeonEmeraldGreen,
                    focusedBorderColor = NeonEmeraldGreen,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Split With (Select Friends)",
                color = OffWhiteText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(dashboardData.contacts) { contactWithBalance ->
                    val isSelected = selectedContacts.contains(contactWithBalance.contact.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(if (isSelected) DarkElevatedSurface else DarkSurface, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedContacts.add(contactWithBalance.contact.id)
                                } else {
                                    selectedContacts.remove(contactWithBalance.contact.id)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = CyanSlateAccent)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = contactWithBalance.contact.name,
                            color = OffWhiteText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val amount = totalAmount.toDoubleOrNull()
                        if (billName.isEmpty() || amount == null || amount <= 0 || selectedContacts.isEmpty()) {
                            snackbarHostState.showSnackbar("Please fill all details and select at least one contact.")
                            return@launch
                        }

                        // Split includes you (the user) + selected contacts
                        val splitCount = selectedContacts.size + 1
                        val individualAmount = amount / splitCount

                        // Create a transaction for each selected contact (they owe you)
                        selectedContacts.forEach { contactId ->
                            viewModel.addTransaction(
                                contactId = contactId,
                                amount = individualAmount,
                                isBorrowed = false, // You paid, they owe you
                                note = "Split: $billName"
                            )
                        }

                        snackbarHostState.showSnackbar("Bill split successfully!")
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanSlateAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Split ₹${totalAmount.takeIf { it.isNotEmpty() } ?: "0"}", color = DeepSpaceBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
