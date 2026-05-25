package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.data.model.Contact
import com.example.data.model.Transaction
import com.example.ui.theme.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Helper function to format INR currency values nicely
fun formatCurrency(amount: Double): String {
    return "₹" + String.format(Locale.getDefault(), "%,.2f", Math.abs(amount))
}

@Composable
fun DebtSyncNavigation(viewModel: ContactViewModel) {
    val navController = rememberNavController()
    val isLocked by viewModel.isAppLocked.collectAsState()
    val isUnlocked by viewModel.isTempUnlocked.collectAsState()
    val isOnboarded by viewModel.isOnboarded.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {
        
        // --- SPLASH SCREEN ---
        composable("splash") {
            SplashScreen(navController, isLocked, isUnlocked, isOnboarded)
        }

        // --- ONBOARDING SCREEN ---
        composable("onboarding") {
            OnboardingScreen(navController, viewModel)
        }

        // --- PIN LOCK SCREEN ---
        composable("lock_screen") {
            LockScreen(viewModel, onUnlocked = {
                navController.navigate("dashboard") {
                    popUpTo("lock_screen") { inclusive = true }
                }
            })
        }

        // --- DASHBOARD SCREEN ---
        composable("dashboard") {
            DashboardScreen(navController, viewModel)
        }

        // --- ADD CONTACT SCREEN ---
        composable("add_contact") {
            AddContactScreen(navController, viewModel)
        }

        // --- SETTINGS SCREEN ---
        composable("settings") {
            SettingsScreen(navController, viewModel)
        }

        // --- AI ASSISTANT SCREEN ---
        composable("ai_assistant") {
            AIAssistantScreen(onNavigateBack = { navController.popBackStack() })
        }

        // --- ANALYTICS SCREEN ---
        composable("analytics") {
            AnalyticsScreen(navController, viewModel)
        }

        // --- SPLIT BILL SCREEN ---
        composable("split_bills") {
            SplitBillScreen(navController, viewModel)
        }

        // --- CONTACT DETAIL SCREEN ---
        composable(
            route = "contact_detail/{contactId}",
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
            ContactDetailScreen(navController, viewModel, contactId)
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(navController: NavController, isLocked: Boolean, isUnlocked: Boolean, isOnboarded: Boolean) {
    LaunchedEffect(Unit) {
        delay(1200)
        if (!isOnboarded) {
            navController.navigate("onboarding") {
                popUpTo("splash") { inclusive = true }
            }
        } else if (isLocked && !isUnlocked) {
            navController.navigate("lock_screen") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("dashboard") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(2.dp, CyanSlateAccent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Logo",
                    tint = NeonEmeraldGreen,
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "DebtSync X",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhiteText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Track, pay, and reconcile together.",
                fontSize = 14.sp,
                color = MutedSlateText
            )
        }
    }
}

// Removed delay wrapper

// ==========================================
// ONBOARDING SCREEN
// ==========================================
@Composable
fun OnboardingScreen(navController: NavController, viewModel: ContactViewModel) {
    val context = LocalContext.current
    
    var pendingLoginId by remember { mutableStateOf<String?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Contacts Synced", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Contacts permission required for optimal experience.", Toast.LENGTH_SHORT).show()
        }
        viewModel.setOnboarded(pendingLoginId)
        navController.navigate("dashboard") {
            popUpTo("onboarding") { inclusive = true }
        }
    }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(DeepSpaceBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(2.dp, CyanSlateAccent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                     imageVector = Icons.Default.Sync,
                     contentDescription = "Logo",
                     tint = NeonEmeraldGreen,
                     modifier = Modifier.size(54.dp)
                 )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Welcome to DebtSync X",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhiteText
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Seamlessly track, manage and settle your peer-to-peer debts.",
                fontSize = 16.sp,
                color = MutedSlateText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val id = doGoogleSignIn(context)
                            if (id != null) {
                                pendingLoginId = id
                                Toast.makeText(context, "Sign-in successful", Toast.LENGTH_SHORT).show()
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            } else {
                                Toast.makeText(context, "Google Sign-in failed. Please verify your Web Client ID.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Throwable) {
                            Toast.makeText(context, "Sign-in error: \${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OffWhiteText, contentColor = DeepSpaceBackground),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            ) {
                Text("Skip sign-in for now", color = MutedSlateText, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "By continuing, you agree to sync your contacts to automatically match payments.",
                fontSize = 12.sp,
                color = MutedSlateText,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// 2. LOCK SCREEN (PIN ACCESS KEYPAD)
// ==========================================
@Composable
fun LockScreen(viewModel: ContactViewModel, onUnlocked: () -> Unit) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val savedPin by viewModel.appPinState.collectAsState()
    val context = LocalContext.current

    var retryBiometric by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryBiometric) {
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.biometricUnlock()
                    onUnlocked()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Use your fingerprint to access DebtSync X")
            .setDeviceCredentialAllowed(true)
            .build()
        
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Biometric not setup or cancelled
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Banner and prompt
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked State",
                tint = MatteGoldAccent,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "App Safe Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhiteText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (pinError) "Invalid PIN. Try Again!" else "Enter security code to access transactions",
                fontSize = 13.sp,
                color = if (pinError) NeonCrimsonRed else MutedSlateText,
                fontWeight = if (pinError) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Display circles indicating entered digits
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 1..4) {
                    val circleActive = i <= enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (circleActive) CyanSlateAccent else DarkElevatedSurface
                            )
                            .border(1.dp, MutedSlateText.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        }

        // Minimal custom safe numerical keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Fingerprint", "0", "Delete")
            )

            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    row.forEach { digit ->
                        val isSpecial = digit == "Fingerprint" || digit == "Delete"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.3f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSpecial) Color.Transparent else DarkSurface)
                                .clickable {
                                    when (digit) {
                                        "Fingerprint" -> {
                                            retryBiometric++
                                        }
                                        "Delete" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                            pinError = false
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += digit
                                                pinError = false
                                            }
                                            if (enteredPin.length == 4) {
                                                if (viewModel.verifyPin(enteredPin)) {
                                                    onUnlocked()
                                                } else {
                                                    pinError = true
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                }
                                .border(
                                    1.dp,
                                    if (isSpecial) Color.Transparent else DarkBorder,
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (digit == "Delete") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete Icon",
                                    tint = OffWhiteText
                                )
                            } else if (digit == "Fingerprint") {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint",
                                    tint = CyanSlateAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Text(
                                    text = digit,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpecial) MutedSlateText else OffWhiteText
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 3. MAIN DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(navController: NavController, viewModel: ContactViewModel) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    val rawTxList by viewModel.rawTransactions.collectAsState()
    val currentSearchQuery by viewModel.searchQuery.collectAsState()
    var selectedFilterTab by remember { mutableStateOf("All") } // "All", "Owed", "Lent"
    var showUpiSelector by remember { mutableStateOf<ContactWithBalance?>(null) }
    var quickAddContactId by remember { mutableStateOf<Long?>(null) }
    
    val voiceResultLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText: String? = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                android.widget.Toast.makeText(context, "Voice command received: \$spokenText", android.widget.Toast.LENGTH_LONG).show()
                // In a real app, this would be parsed by DebtSync AI and saved.
            }
        }
    }

    // Set status bar lock updates
    LaunchedEffect(Unit) {
        viewModel.lockApp()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepSpaceBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyanSlateAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("D", color = MatteGoldAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(
                            text = "DEBTSYNC SUMMARY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedSlateText,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Overview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OffWhiteText
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(DarkSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, NeonEmeraldGreen, RoundedCornerShape(16.dp))
                            .clickable { navController.navigate("ai_assistant") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = NeonEmeraldGreen, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(DarkSurface, RoundedCornerShape(16.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                            .clickable { navController.navigate("settings") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = OffWhiteText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_contact") },
                containerColor = CyanSlateAccent,
                contentColor = MatteGoldAccent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.offset(y = (-16).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Person", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Summary Item (Net Ring, Stats Cards)
            item {
                DashboardSummarySection(
                    totalLent = dashboardState.totalLent,
                    totalBorrowed = dashboardState.totalBorrowed,
                    netBalance = dashboardState.netBalance
                )
            }

            // Quick Actions (Analytics, Split Bills, Voice)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("analytics") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanSlateAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = DeepSpaceBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stats", color = DeepSpaceBackground, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { navController.navigate("split_bills") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CallSplit, contentDescription = null, tint = DeepSpaceBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Split", color = DeepSpaceBackground, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say 'Rahul owes me 500'")
                            }
                            try {
                                voiceResultLauncher.launch(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Voice input not supported", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MatteGoldAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = DeepSpaceBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice", color = DeepSpaceBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Contact Search input element
            item {
                OutlinedTextField(
                    value = currentSearchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search person name or phone...", color = MutedSlateText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MutedSlateText) },
                    trailingIcon = {
                        if (currentSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel search", tint = MutedSlateText)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanSlateAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = OffWhiteText,
                        unfocusedTextColor = OffWhiteText
                    ),
                    singleLine = true
                )
            }

            // Relationship Categories (Chips filter for Owe vs Receive stats)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("All", "Show Debts (You Owe)", "Show Credits (You Lent)").forEach { tabName ->
                        val shortName = when (tabName) {
                            "All" -> "All"
                            "Show Debts (You Owe)" -> "You Owe"
                            else -> "You Received"
                        }
                        val systemKey = when (tabName) {
                            "All" -> "All"
                            "Show Debts (You Owe)" -> "Owe"
                            else -> "Lent"
                        }
                        val isSelected = selectedFilterTab == systemKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CyanSlateAccent else DarkSurface)
                                .border(1.dp, if (isSelected) Color.Transparent else DarkBorder, RoundedCornerShape(10.dp))
                                .clickable { selectedFilterTab = systemKey }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = shortName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) DeepSpaceBackground else OffWhiteText
                            )
                        }
                    }
                }
            }

            // Filtered list of contacts
            val filteredContacts = dashboardState.contacts.filter { rel ->
                when (selectedFilterTab) {
                    "Owe" -> rel.netBalance < 0
                    "Lent" -> rel.netBalance > 0
                    else -> true
                }
            }

            if (filteredContacts.isEmpty()) {
                item {
                    EmptyListPlaceholder(
                        title = "No relationships found",
                        subtitle = "Try updating your search filters or click 'Add Person' below to track transactions."
                    )
                }
            } else {
                items(filteredContacts, key = { it.contact.id }) { item ->
                    ContactListItemCard(
                        item = item,
                        onClick = {
                            navController.navigate("contact_detail/${item.contact.id}")
                        },
                        onPayClick = {
                            if (item.contact.upiId.isNullOrEmpty()) {
                                Toast.makeText(context, "Please add a UPI ID to pay this contact.", Toast.LENGTH_SHORT).show()
                                navController.navigate("contact_detail/${item.contact.id}")
                            } else {
                                showUpiSelector = item
                            }
                        },
                        onRemindClick = {
                            val textMessage = viewModel.getWhatsAppReminderMessage(item.contact.name, item.netBalance)
                            showNotification(context, "DebtSync Auto-Reminder", "Payment reminder scheduled for ${item.contact.name}")
                            launchWhatsApp(context, item.contact.phone, textMessage)
                        },
                        onAddClick = {
                            quickAddContactId = item.contact.id
                        }
                    )
                }
            }

            // Recent global transactions log section header
            if (rawTxList.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhiteText,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                items(dashboardState.recentTransactions.take(5)) { txWithContact ->
                    val tx = txWithContact.transaction
                    RecentTxDashboardItem(
                        tx = tx,
                        contactName = txWithContact.contactName,
                        onDeleteClick = { viewModel.deleteTransaction(tx) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    if (showUpiSelector != null) {
        UPIAppSelectorBottomSheet(
            onDismissRequest = { showUpiSelector = null },
            onAppSelected = { packageName ->
                val contact = showUpiSelector!!.contact
                val amount = Math.abs(showUpiSelector!!.netBalance)
                launchUPIPaymentApp(context, contact.upiId!!, contact.name, amount, packageName)
                showUpiSelector = null
            }
        )
    }

    quickAddContactId?.let { cid ->
        DialogAddTransaction(
            onDismiss = { quickAddContactId = null },
            onSave = { amount, isBorrowed, note ->
                viewModel.addTransaction(cid, amount, isBorrowed, note)
                quickAddContactId = null
            }
        )
    }
}

// Elegant summary element consisting of custom radial canvas bar diagram and detailed cards
@Composable
fun DashboardSummarySection(
    totalLent: Double,
    totalBorrowed: Double,
    netBalance: Double
) {
    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLoaded = true }

    val animatedNetBalance by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLoaded) netBalance.toFloat() else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "NET BALANCE", 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Medium,
                            color = CyanSlateAccent,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(animatedNetBalance.toDouble()),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (netBalance >= 0) "OVERALL POSITIVE" else "OVERALL NEGATIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (netBalance >= 0) NeonEmeraldGreenBg else NeonCrimsonRedBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (netBalance >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, 
                            contentDescription = null, 
                            tint = if (netBalance >= 0) NeonEmeraldGreen else NeonCrimsonRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (netBalance >= 0) "You are set to receive ${formatCurrency(netBalance)}" else "You owe a total of ${formatCurrency(Math.abs(netBalance))}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Split metrics info cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Lent card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).background(NeonEmeraldGreenBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Lent", tint = NeonEmeraldGreen, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedSlateText, letterSpacing = 0.5.sp)
                }
                Text(
                    text = formatCurrency(totalLent), 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    color = OffWhiteText
                )
            }

            // Borrowed card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).background(NeonCrimsonRedBg, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Borrowed", tint = NeonCrimsonRed, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BORROWED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedSlateText, letterSpacing = 0.5.sp)
                }
                Text(
                    text = formatCurrency(totalBorrowed), 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.SemiBold, 
                    color = OffWhiteText
                )
            }
        }

        // Segmented ratio bar
        if (totalLent > 0 || totalBorrowed > 0) {
            val total = totalLent + totalBorrowed
            val lentRatio = (totalLent / total).toFloat()
            val animatedLentRatio by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isLoaded) lentRatio else 0.5f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "DISTRIBUTION", 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MutedSlateText, 
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                ) {
                    val width = size.width
                    val drawLentRatio = animatedLentRatio.coerceIn(0f, 1f)
                    val lentWidth = width * drawLentRatio
                    val borrowedWidth = (width - lentWidth).coerceAtLeast(0f)
                    if (lentWidth > 0f) {
                        drawRect(color = NeonEmeraldGreen, size = androidx.compose.ui.geometry.Size(lentWidth, size.height))
                    }
                    if (borrowedWidth > 0f) {
                        drawRect(color = NeonCrimsonRed, topLeft = androidx.compose.ui.geometry.Offset(lentWidth, 0f), size = androidx.compose.ui.geometry.Size(borrowedWidth, size.height))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Lent ${(animatedLentRatio.coerceIn(0f, 1f) * 100).toInt()}%", color = NeonEmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Borrowed ${((1f - animatedLentRatio.coerceIn(0f, 1f)) * 100).toInt()}%", color = NeonCrimsonRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ContactListItemCard(
    item: ContactWithBalance,
    onClick: () -> Unit,
    onPayClick: () -> Unit,
    onRemindClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Profile image or initials circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkElevatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.contact.avatarUrl != null && item.contact.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.contact.avatarUrl,
                            contentDescription = item.contact.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        val initials = if (item.contact.name.isNotBlank()) item.contact.name.take(2).uppercase() else "?"
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Medium,
                            color = OffWhiteText,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = item.contact.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = OffWhiteText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.contact.phone.ifBlank { "Contact" },
                        fontSize = 12.sp,
                        color = MutedSlateText
                    )
                }
            }

            // Realtime financial balance summaries
            Column(horizontalAlignment = Alignment.End) {
                val formattedAmt = formatCurrency(item.netBalance)
                if (item.netBalance > 0) {
                    Text("+ $formattedAmt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonEmeraldGreen)
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.border(1.dp, CyanSlateAccent.copy(alpha=0.3f), RoundedCornerShape(8.dp)).clickable { onRemindClick() }.padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text("Remind", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanSlateAccent)
                        }
                        Box(modifier = Modifier.background(DarkElevatedSurface, RoundedCornerShape(8.dp)).clickable { onAddClick() }.padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text("Add +", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OffWhiteText)
                        }
                    }
                } else if (item.netBalance < 0) {
                    Text("- $formattedAmt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonCrimsonRed)
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.background(CyanSlateAccent, RoundedCornerShape(8.dp)).clickable { onPayClick() }.padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text("Pay Now", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepSpaceBackground)
                        }
                        Box(modifier = Modifier.background(DarkElevatedSurface, RoundedCornerShape(8.dp)).clickable { onAddClick() }.padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text("Add +", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OffWhiteText)
                        }
                    }
                } else {
                    Text("+ ₹0.00", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonEmeraldGreen)
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.background(Color.White.copy(alpha=0.05f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text("SETTLED", fontSize = 9.sp, color = OffWhiteText, letterSpacing = (-0.5).sp)
                        }
                        Box(modifier = Modifier.background(DarkElevatedSurface, RoundedCornerShape(8.dp)).clickable { onAddClick() }.padding(horizontal = 8.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
                            Text("Add +", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OffWhiteText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTxDashboardItem(
    tx: Transaction,
    contactName: String,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkElevatedSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.isBorrowed) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (tx.isBorrowed) NeonCrimsonRed else NeonEmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = contactName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = OffWhiteText
                )
                Text(
                    text = tx.note.ifBlank { if (tx.isBorrowed) "Borrowed" else "Lent" },
                    fontSize = 12.sp,
                    color = MutedSlateText
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.isBorrowed) "-" else "+"}${formatCurrency(tx.amount)}",
                    color = if (tx.isBorrowed) NeonCrimsonRed else NeonEmeraldGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDeleteClick, 
                    modifier = Modifier.size(24.dp).background(DarkElevatedSurface, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Tx", tint = MutedSlateText, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

// ==========================================
// 4. ADD CONTACT SCREEN
// ==========================================
fun getContactDetails(context: Context, uri: Uri): Pair<String, String> {
    var name = ""
    var phone = ""
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (nameIndex >= 0) name = cursor.getString(nameIndex)
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
            if (idIndex >= 0 && hasPhoneIndex >= 0) {
                val contactId = cursor.getString(idIndex)
                val hasPhone = cursor.getString(hasPhoneIndex).toInt() > 0
                if (hasPhone) {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            val phoneIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (phoneIndex >= 0) phone = pCursor.getString(phoneIndex)
                        }
                    }
                }
            }
        }
    }
    return Pair(name, phone)
}

@Composable
fun AddContactScreen(navController: NavController, viewModel: ContactViewModel) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val pickContactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        uri?.let {
            val (fetchedName, fetchedPhone) = getContactDetails(context, it)
            if (fetchedName.isNotEmpty()) name = fetchedName
            if (fetchedPhone.isNotEmpty()) phone = fetchedPhone
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) pickContactLauncher.launch(null)
        else Toast.makeText(context, "Permission Denied to Read Contacts", Toast.LENGTH_SHORT).show()
    }
    
    // Choose preconfigured avatars matching aesthetic
    val defaultAvatars = listOf(
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
        "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150"
    )
    var selectedAvatar by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = DeepSpaceBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(DarkSurface, CircleShape)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OffWhiteText)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Add New Person", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OffWhiteText)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Avatar profile picker
            Text("Select Profile Avatar (Optional)", fontSize = 14.sp, color = MutedSlateText)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                defaultAvatars.forEach { url ->
                    val isChosen = selectedAvatar == url
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isChosen) 3.dp else 1.dp,
                                color = if (isChosen) CyanSlateAccent else DarkBorder,
                                shape = CircleShape
                            )
                            .clickable { selectedAvatar = url }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Contact Information", fontSize = 14.sp, color = MutedSlateText)
            }
            
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanSlateAccent.copy(alpha = 0.2f), contentColor = OffWhiteText),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Contacts, contentDescription = null, tint = CyanSlateAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import from Phone Contacts", color = OffWhiteText, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Person Full Name", color = MutedSlateText) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanSlateAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = OffWhiteText,
                    unfocusedTextColor = OffWhiteText
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number", color = MutedSlateText) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanSlateAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = OffWhiteText,
                    unfocusedTextColor = OffWhiteText
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("Paytm / PhonePe UPI Address (Optional)", color = MutedSlateText) },
                placeholder = { Text("9876543210@paytm or upiid@okaxis", color = MutedSlateText.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanSlateAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = OffWhiteText,
                    unfocusedTextColor = OffWhiteText
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        Toast.makeText(navController.context, "Name and Phone are mandatory fields", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addContact(
                            name = name.trim(),
                            phone = phone.trim(),
                            upiId = upiId.trim(),
                            avatarUrl = selectedAvatar
                        ) { newId ->
                            // Redirect directly to contact details to start logging transactions
                            navController.navigate("contact_detail/$newId") {
                                popUpTo("add_contact") { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanSlateAccent)
            ) {
                Text("Create Contact Profile", color = OffWhiteText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ==========================================
// 5. CONTACT DETAIL SCREEN
// ==========================================
@Composable
fun ContactDetailScreen(
    navController: NavController,
    viewModel: ContactViewModel,
    contactId: Long
) {
    val context = LocalContext.current
    val contactFlow = remember(contactId) { viewModel.getContactById(contactId) }
    val contact by contactFlow.collectAsState(initial = null)
    
    val txFlow = remember(contactId) { viewModel.getTransactionsByContact(contactId) }
    val transactions by txFlow.collectAsState(initial = emptyList())

    // Calculations local
    val borrowedSum = transactions.filter { it.isBorrowed }.sumOf { it.amount }
    val lentSum = transactions.filter { !it.isBorrowed }.sumOf { it.amount }
    val netBalance = lentSum - borrowedSum

    var showAddTxSheet by remember { mutableStateOf(false) }
    var editUpiDialogState by remember { mutableStateOf(false) }
    var showUpiSelector by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepSpaceBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(DarkSurface, CircleShape)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OffWhiteText)
                }
                
                // Quick contact deletion action
                IconButton(
                    onClick = {
                        contact?.let {
                            viewModel.deleteContact(it)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.background(DarkSurface, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Contact", tint = NeonCrimsonRed)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTxSheet = true },
                containerColor = NeonEmeraldGreen,
                contentColor = DeepSpaceBackground,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PostAdd, contentDescription = "Add transaction")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Transaction", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        if (contact == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val contactProfile = contact!!
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profiler Details Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurface)
                            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(20.dp))
                            .clickable { showAddTxSheet = true }
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DarkElevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (contactProfile.avatarUrl != null && contactProfile.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = contactProfile.avatarUrl,
                                    contentDescription = contactProfile.name,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = contactProfile.name.take(2).uppercase(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanSlateAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = contactProfile.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OffWhiteText
                        )
                        Text(
                            text = contactProfile.phone,
                            fontSize = 12.sp,
                            color = MutedSlateText
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = CyanSlateAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = contactProfile.upiId ?: "No UPI linked",
                                fontSize = 11.sp,
                                color = if (contactProfile.upiId != null) MutedSlateText else NeonCrimsonRed.copy(alpha = 0.7f),
                                modifier = Modifier.clickable { editUpiDialogState = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(CyanSlateAccent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, CyanSlateAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = null,
                                tint = CyanSlateAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TAP CARD TO QUICK ADD TRANSACTION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanSlateAccent,
                                letterSpacing = 0.5.sp
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = DarkBorder)

                        // Relative Ledger Balance Display
                        Text(
                            text = if (netBalance > 0) "Your Outstanding Receivable" else if (netBalance < 0) "Your Outstanding Payable" else "All Balances Are Reconciled",
                            fontSize = 12.sp,
                            color = MutedSlateText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(netBalance),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = if (netBalance > 0) NeonEmeraldGreen else if (netBalance < 0) NeonCrimsonRed else OffWhiteText
                        )
                        
                        // Human Statement details (Requested sentence)
                        if (netBalance > 0) {
                            Text(
                                text = "You should receive ${formatCurrency(netBalance)} from ${contactProfile.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmeraldGreen,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else if (netBalance < 0) {
                            Text(
                                text = "You owe ${formatCurrency(Math.abs(netBalance))} to ${contactProfile.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCrimsonRed,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Grid of Instant actions (Pay, Call, WhatsApp Reminder UI)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action: PAY NOW (Active only if You Owe Money, i.e., netBalance < 0)
                        val isOwed = netBalance < 0
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = isOwed) {
                                    if (contactProfile.upiId.isNullOrEmpty()) {
                                        Toast.makeText(context, "Add person's UPI ID first!", Toast.LENGTH_SHORT).show()
                                        editUpiDialogState = true
                                    } else {
                                        showUpiSelector = true
                                    }
                                },
                            color = if (isOwed) MatteGoldAccent.copy(alpha = 0.15f) else DarkSurface.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isOwed) MatteGoldAccent else DarkBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = "PayNow",
                                    tint = if (isOwed) MatteGoldAccent else MutedSlateText.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Pay Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isOwed) MatteGoldAccent else MutedSlateText.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Call Shortcut
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    launchCall(context, contactProfile.phone)
                                },
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Phone call",
                                    tint = CyanSlateAccent
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Call Mobile", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanSlateAccent)
                            }
                        }

                        // WhatsApp Reminder Shortcut
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val textMessage = viewModel.getWhatsAppReminderMessage(contactProfile.name, netBalance)
                                    showNotification(context, "DebtSync Auto-Reminder", "Payment reminder scheduled for ${contactProfile.name}")
                                    launchWhatsApp(context, contactProfile.phone, textMessage)
                                },
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "WhatsApp reminder",
                                    tint = NeonEmeraldGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Remind WA", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeonEmeraldGreen)
                            }
                        }
                    }
                }

                // Transaction ledger list view
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ledger History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OffWhiteText)
                        Text("${transactions.size} records", fontSize = 11.sp, color = MutedSlateText)
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        EmptyListPlaceholder(
                            title = "Perfectly balanced ledger!",
                            subtitle = "Use the green plus button to create a new Borrowed (debited) or Given (credited) record."
                        )
                    }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionListItem(
                            tx = tx,
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Modal Sheet implementation simulated for Kotlin/Compose Compatibility
    if (showAddTxSheet) {
        DialogAddTransaction(
            onDismiss = { showAddTxSheet = false },
            onSave = { amount, isBorrowed, note ->
                viewModel.addTransaction(contactId, amount, isBorrowed, note)
                showAddTxSheet = false
            }
        )
    }

    if (editUpiDialogState) {
        DialogEditUPI(
            currentUpi = contact?.upiId ?: "",
            onDismiss = { editUpiDialogState = false },
            onSave = { newUpi ->
                contact?.let {
                    viewModel.updateContact(it.copy(upiId = newUpi))
                }
                editUpiDialogState = false
            }
        )
    }

    if (showUpiSelector && contact != null) {
        UPIAppSelectorBottomSheet(
            onDismissRequest = { showUpiSelector = false },
            onAppSelected = { packageName ->
                launchUPIPaymentApp(context, contact!!.upiId ?: "", contact!!.name, Math.abs(netBalance), packageName)
                showUpiSelector = false
            }
        )
    }
}

// Dialog implementation for adding transactions
@Composable
fun DialogAddTransaction(
    onDismiss: () -> Unit,
    onSave: (Double, Boolean, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var isBorrowed by remember { mutableStateOf(false) } // true = Borrowed, false = Lent
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Transaction", fontWeight = FontWeight.Bold, color = OffWhiteText) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector Button tabs: Borrowed or Given
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isBorrowed = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isBorrowed) NeonEmeraldGreen else DarkElevatedSurface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Gave / Lent Cash",
                            color = if (!isBorrowed) DeepSpaceBackground else OffWhiteText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { isBorrowed = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBorrowed) NeonCrimsonRed else DarkElevatedSurface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Borrowed Cash",
                            color = if (isBorrowed) OffWhiteText else OffWhiteText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (INR)", color = MutedSlateText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OffWhiteText,
                        unfocusedTextColor = OffWhiteText,
                        focusedBorderColor = CyanSlateAccent,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Transaction Note / Reason", color = MutedSlateText) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Repayment, lunch bill, shopping", color = MutedSlateText.copy(alpha = 0.4f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OffWhiteText,
                        unfocusedTextColor = OffWhiteText,
                        focusedBorderColor = CyanSlateAccent,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val doubleAmt = amount.toDoubleOrNull() ?: 0.0
                    if (doubleAmt > 0) {
                        onSave(doubleAmt, isBorrowed, note)
                    }
                }
            ) {
                Text("Confirm Entry", color = CyanSlateAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlateText)
            }
        },
        containerColor = DarkSurface
    )
}

// Dialog for editing user UPI ID in-context
@Composable
fun DialogEditUPI(
    currentUpi: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var upi by remember { mutableStateOf(currentUpi) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit UPI ID", fontWeight = FontWeight.Bold, color = OffWhiteText) },
        text = {
            OutlinedTextField(
                value = upi,
                onValueChange = { upi = it },
                label = { Text("UPI Link Name", color = MutedSlateText) },
                placeholder = { Text("e.g. upi_id@paytm", color = MutedSlateText.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OffWhiteText,
                    unfocusedTextColor = OffWhiteText,
                    focusedBorderColor = CyanSlateAccent,
                    unfocusedBorderColor = DarkBorder
                ),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(upi.trim()) }) {
                Text("Save UPI", color = CyanSlateAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedSlateText)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun TransactionListItem(
    tx: Transaction,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (tx.isBorrowed) NeonCrimsonRed.copy(alpha = 0.12f)
                        else NeonEmeraldGreen.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.isBorrowed) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (tx.isBorrowed) NeonCrimsonRed else NeonEmeraldGreen,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = tx.note.ifBlank { if (tx.isBorrowed) "Borrowed Cash" else "Settled / Given Cash" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhiteText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MutedSlateText
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${if (tx.isBorrowed) "-" else "+"}${formatCurrency(tx.amount)}",
                color = if (tx.isBorrowed) NeonCrimsonRed else NeonEmeraldGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Tx",
                    tint = MutedSlateText.copy(alpha = 0.35f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// Placeholder shown when searches/ledgers are completely null
@Composable
fun EmptyListPlaceholder(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = "Empty",
            tint = MutedSlateText.copy(alpha = 0.3f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = OffWhiteText.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = MutedSlateText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ==========================================
// 6. SETTINGS & APP CONTROL
// ==========================================
@Composable
fun SettingsScreen(navController: NavController, viewModel: ContactViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedPin by viewModel.appPinState.collectAsState()
    val isLockedEnabled by viewModel.isAppLocked.collectAsState()
    val connectedGoogleAccount by viewModel.googleAccountId.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogCode by remember { mutableStateOf("") }
    
    var showImportDialog by remember { mutableStateOf(false) }
    var rawRestoreJsonString by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DeepSpaceBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.background(DarkSurface, CircleShape)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OffWhiteText)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Application Configurations", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OffWhiteText)
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // CLOUD SYNC & ACCOUNT BLOCK
            Text("Cloud Synchronization", fontSize = 14.sp, color = MutedSlateText, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (connectedGoogleAccount == null) {
                                    coroutineScope.launch {
                                        try {
                                            val id = doGoogleSignIn(context)
                                            if (id != null) {
                                                viewModel.saveGoogleAccount(id)
                                                Toast.makeText(context, "Google Account Linked!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Google Sign-in failed.", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Sign-in error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    viewModel.clearGoogleAccount()
                                    Toast.makeText(context, "Google Account Disconnected.", Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (connectedGoogleAccount != null) NeonEmeraldGreen.copy(alpha = 0.12f) else DarkElevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Sync",
                                tint = if (connectedGoogleAccount != null) NeonEmeraldGreen else OffWhiteText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (connectedGoogleAccount != null) "Google Account Connected" else "Link Google Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = OffWhiteText
                            )
                            Text(
                                if (connectedGoogleAccount != null) "Sync is active for ${connectedGoogleAccount!!.take(10)}..." else "Enable seamless cloud backups",
                                fontSize = 11.sp,
                                color = MutedSlateText
                            )
                        }
                        Text(
                            text = if (connectedGoogleAccount != null) "UNLINK" else "LINK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (connectedGoogleAccount != null) MutedSlateText else CyanSlateAccent
                        )
                    }
                }
            }

            // SECURITY CONTROL BLOCK
            Text("Security & PIN Protection", fontSize = 14.sp, color = MutedSlateText, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Secure Lock Protection", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OffWhiteText)
                            Text("Asks for numerical key code on startup", fontSize = 11.sp, color = MutedSlateText)
                        }
                        Switch(
                            checked = isLockedEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    showPinDialog = true
                                } else {
                                    viewModel.savePin("", false)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanSlateAccent,
                                checkedTrackColor = CyanSlateAccent.copy(alpha = 0.4f),
                                uncheckedThumbColor = MutedSlateText,
                                uncheckedTrackColor = DarkElevatedSurface
                            )
                        )
                    }

                    if (savedPin.isNotEmpty()) {
                        HorizontalDivider(color = DarkBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Passcode PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OffWhiteText)
                                Text("Active 4-digit lockout protection", fontSize = 11.sp, color = MutedSlateText)
                            }
                            Text(
                                text = "****",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MatteGoldAccent
                            )
                        }
                    }
                }
            }

            // BACKUP, CSV REPORT & SYNCHRONIZATION DATA BLOCKS
            Text("Data Export, Reports & Backups", fontSize = 14.sp, color = MutedSlateText, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Export Statement Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.exportReportAsCSV() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonEmeraldGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "CSV", tint = NeonEmeraldGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export Ledger Report (CSV)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OffWhiteText)
                            Text("Share fully formatted CSV summaries to any messaging app", fontSize = 11.sp, color = MutedSlateText)
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    // Export Backup JSON Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val jsonStr = viewModel.backupDatabase()
                                if (jsonStr.isNotEmpty()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("DebtSync Backup Code", jsonStr)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Full Backup copied to Clipboard & written as debtsync_backup.json!", Toast.LENGTH_LONG).show()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanSlateAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "JSON backup", tint = CyanSlateAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Create JSON Cloud Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OffWhiteText)
                            Text("Copies internal database sync script to transfer records easily", fontSize = 11.sp, color = MutedSlateText)
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    // Import Backup JSON Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showImportDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MatteGoldAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Restore backup", tint = MatteGoldAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Restore Database Script", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OffWhiteText)
                            Text("Paste standard exported JSON blocks to recover profile histories", fontSize = 11.sp, color = MutedSlateText)
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    // Demo Reload Data Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadDemoData()
                                Toast.makeText(context, "Realtime demo ledger profile reloaded!", Toast.LENGTH_SHORT).show()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LightCrimsonRed.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = "Demo data", tint = LightCrimsonRed, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Reload Realistic Mock Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OffWhiteText)
                            Text("Fills dashboard with transactions for immediate playground testing", fontSize = 11.sp, color = MutedSlateText)
                        }
                    }
                }
            }

            // APPEARANCE & THEMES
            Text("Appearance", fontSize = 14.sp, color = MutedSlateText, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsRow(icon = Icons.Default.DarkMode, title = "Dark Mode", subtitle = "Midnight Obsidian", tint = MutedSlateText, hasToggle = true, defaultToggle = true)
                    SettingsRow(icon = Icons.Default.FontDownload, title = "System Font", subtitle = "Variable sans-serif", tint = MutedSlateText, hasToggle = true, defaultToggle = true)
                    SettingsRow(icon = Icons.Default.Animation, title = "Reduced Motion", subtitle = "Disable parallax and glass blur", tint = MutedSlateText, hasToggle = true, defaultToggle = false)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Version 1.0.0 (DebtSync X Native Build)",
                fontSize = 11.sp,
                color = MutedSlateText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal dialog for entering lock key code PIN
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configure Security Code", color = OffWhiteText, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinDialogCode,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinDialogCode = it },
                    label = { Text("Enter 4 Digit Code", color = MutedSlateText) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OffWhiteText,
                        unfocusedTextColor = OffWhiteText,
                        focusedBorderColor = CyanSlateAccent,
                        unfocusedBorderColor = DarkBorder
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinDialogCode.length == 4) {
                            viewModel.savePin(pinDialogCode, true)
                            showPinDialog = false
                            pinDialogCode = ""
                        } else {
                            Toast.makeText(context, "Passcode must be exactly 4 numerical digits", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Enable Safe Mode", color = CyanSlateAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        pinDialogCode = ""
                    }
                ) {
                    Text("Cancel", color = MutedSlateText)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Modal dialog for pasting json sync strings
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Synchronize Database Local script", color = OffWhiteText, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = rawRestoreJsonString,
                    onValueChange = { rawRestoreJsonString = it },
                    label = { Text("Paste JSON Payload", color = MutedSlateText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OffWhiteText,
                        unfocusedTextColor = OffWhiteText,
                        focusedBorderColor = CyanSlateAccent,
                        unfocusedBorderColor = DarkBorder
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rawRestoreJsonString.isNotBlank()) {
                            val ok = viewModel.restoreDatabase(rawRestoreJsonString)
                            if (ok) {
                                showImportDialog = false
                                rawRestoreJsonString = ""
                            }
                        }
                    }
                ) {
                    Text("Apply Backup", color = CyanSlateAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportDialog = false
                        rawRestoreJsonString = ""
                    }
                ) {
                    Text("Cancel", color = MutedSlateText)
                }
            },
            containerColor = DarkSurface
        )
    }
}

// ==========================================
// UPI PAYMENT INTENT AND SHORTCUT ACTION ENGINE
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UPIAppSelectorBottomSheet(
    onDismissRequest: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val upiApps = listOf(
        Triple("Google Pay", "com.google.android.apps.nbu.paisa.user", Color(0xFFFFFFFF)),
        Triple("Paytm", "net.one97.paytm", Color(0xFF002970)),
        Triple("PhonePe", "com.phonepe.app", Color(0xFF5F259F)),
        Triple("Cred", "com.dreamplug.androidapp", Color(0xFF141414)),
        Triple("Amazon Pay", "in.amazon.mShop.android.shopping", Color(0xFF000000)),
        Triple("Other Apps", "", DarkElevatedSurface)
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = DeepSpaceBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MutedSlateText.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Complete Payment",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OffWhiteText
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(upiApps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAppSelected(app.second) }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(app.third)
                                .border(
                                    width = 1.dp,
                                    color = if (app.third == DarkElevatedSurface || app.third == Color(0xFF141414) || app.third == Color(0xFF000000)) MutedSlateText.copy(alpha = 0.2f) else app.third.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app.first == "Google Pay") {
                                Text("GPay", color = Color(0xFF1A73E8), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            } else if (app.first == "Paytm") {
                                Text("Paytm", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            } else if (app.first == "PhonePe") {
                                Text("पे", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                            } else if (app.first == "Cred") {
                                Text("CRED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            } else if (app.first == "Amazon Pay") {
                                Text("pay", color = Color(0xFFFF9900), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                            } else {
                                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = OffWhiteText)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = app.first,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MutedSlateText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

fun launchUPIPaymentApp(context: Context, upiId: String, name: String, amount: Double, packageName: String) {
    try {
        val cleanName = URLEncoder.encode(name, "UTF-8")
        val uriString = "upi://pay?pa=$upiId&pn=$cleanName&am=$amount&cu=INR"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(uriString)
        }
        if (packageName.isNotEmpty()) {
            intent.setPackage(packageName)
        }
        val chooser = if (packageName.isEmpty()) Intent.createChooser(intent, "Complete UPI payment") else intent
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
         // Safe device fallback: copy to clipboard
         val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
         val clip = android.content.ClipData.newPlainText("UPI payment deep link", "upi://pay?pa=$upiId&pn=$name&am=$amount&cu=INR")
         clipboard.setPrimaryClip(clip)
         Toast.makeText(context, "UPI clipboard copied. Payment app not detected.", Toast.LENGTH_LONG).show()
    }
}

fun launchCall(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open phone dialer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun launchWhatsApp(context: Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        val query = URLEncoder.encode(message, "UTF-8")
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$query"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Default text share fallback
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(textIntent, "Send Reminder Profile Detail"))
    }
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, tint: androidx.compose.ui.graphics.Color, hasToggle: Boolean = false, defaultToggle: Boolean = false) {
    var isChecked by remember { mutableStateOf(defaultToggle) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { if(hasToggle) isChecked = !isChecked },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OffWhiteText)
                Text(subtitle, fontSize = 11.sp, color = MutedSlateText)
            }
        }
        if (hasToggle) {
            Switch(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyanSlateAccent,
                    checkedTrackColor = CyanSlateAccent.copy(alpha = 0.4f),
                    uncheckedThumbColor = MutedSlateText,
                    uncheckedTrackColor = DarkElevatedSurface
                )
            )
        } else {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Open", tint = MutedSlateText)
        }
    }
}
