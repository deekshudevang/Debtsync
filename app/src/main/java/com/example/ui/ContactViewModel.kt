package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Contact
import com.example.data.model.Transaction
import com.example.data.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data wrapper containing contact and aggregated transactional statistics
data class ContactWithBalance(
    val contact: Contact,
    val totalBorrowed: Double,
    val totalLent: Double,
    val netBalance: Double, // positive = Lent (should receive), negative = Borrowed (owe)
    val lastTransactionTime: Long
)

// Main UI state representing search filters, lock screen checks and stats
data class DashboardState(
    val contacts: List<ContactWithBalance> = emptyList(),
    val searchQuery: String = "",
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netBalance: Double = 0.0,
    val recentTransactions: List<TransactionWithContactName> = emptyList()
)

data class TransactionWithContactName(
    val transaction: Transaction,
    val contactName: String
)

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository: DataRepository

    // Search query flowing from search fields
    val searchQuery = MutableStateFlow("")

    // Transaction filter settings
    val transactionFilterType = MutableStateFlow("All") // "All", "Borrowed", "Lent"
    val transactionSearchQuery = MutableStateFlow("")

    // Secure PIN code properties for local app lock
    private val prefs = context.getSharedPreferences("debtsync_prefs", Context.MODE_PRIVATE)
    val appPinState = MutableStateFlow(prefs.getString("app_pin", "") ?: "")
    val isAppLocked = MutableStateFlow(prefs.getBoolean("is_locked_enabled", false) && (prefs.getString("app_pin", "")?.isNotEmpty() == true))
    val isTempUnlocked = MutableStateFlow(false)
    val isOnboarded = MutableStateFlow(prefs.getBoolean("is_onboarded", false))

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DataRepository(database.contactDao())
        
        // Populate initial mock data in development if the database is entirely empty, for a vibrant preview!
        viewModelScope.launch {
            val existing = repository.allContacts.first()
            if (existing.isEmpty()) {
                loadDemoData()
            }
        }
    }

    // Expose flows representing entities
    val rawContacts: StateFlow<List<Contact>> = repository.allContacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val rawTransactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Aggregate reactive flow computing the primary dashboard statistics and filtered contacts
    val dashboardState: StateFlow<DashboardState> = combine(
        rawContacts,
        rawTransactions,
        searchQuery
    ) { contactList, transactionList, query ->
        
        val contactWithBalances = contactList.map { contact ->
            val txs = transactionList.filter { it.contactId == contact.id }
            val borrowed = txs.filter { it.isBorrowed }.sumOf { it.amount }
            val lent = txs.filter { !it.isBorrowed }.sumOf { it.amount }
            val net = lent - borrowed
            val latestTime = txs.maxByOrNull { it.timestamp }?.timestamp ?: contact.createdAt
            
            ContactWithBalance(
                contact = contact,
                totalBorrowed = borrowed,
                totalLent = lent,
                netBalance = net,
                lastTransactionTime = latestTime
            )
        }.filter {
            query.isEmpty() || it.contact.name.contains(query, ignoreCase = true) || it.contact.phone.contains(query, ignoreCase = true)
        }

        // Totals mapping
        val totalBorrowed = transactionList.filter { it.isBorrowed }.sumOf { it.amount }
        val totalLent = transactionList.filter { !it.isBorrowed }.sumOf { it.amount }
        val netBalance = totalLent - totalBorrowed

        // Compile recent transactions with owner identity
        val recentTxs = transactionList.take(15).map { tx ->
            val owner = contactList.find { it.id == tx.contactId }?.name ?: "Unknown Person"
            TransactionWithContactName(tx, owner)
        }

        DashboardState(
            contacts = contactWithBalances,
            searchQuery = query,
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            netBalance = netBalance,
            recentTransactions = recentTxs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    // PIN lock logic
    fun savePin(pin: String, enabled: Boolean) {
        prefs.edit().apply {
            putString("app_pin", pin)
            putBoolean("is_locked_enabled", enabled)
            apply()
        }
        appPinState.value = pin
        isAppLocked.value = enabled
        Toast.makeText(context, if (enabled) "PIN Lock Enabled" else "PIN Lock Disabled", Toast.LENGTH_SHORT).show()
    }

    fun verifyPin(entered: String): Boolean {
        val saved = prefs.getString("app_pin", "") ?: ""
        val matches = entered == saved
        if (matches) {
            isTempUnlocked.value = true
        }
        return matches
    }

    fun lockApp() {
        if (appPinState.value.isNotEmpty()) {
            isTempUnlocked.value = false
        }
    }

    fun setOnboarded() {
        prefs.edit().putBoolean("is_onboarded", true).apply()
        isOnboarded.value = true
    }

    // Database manipulation functions supporting views
    fun getContactById(id: Long): kotlinx.coroutines.flow.Flow<Contact?> {
        return repository.getContactById(id)
    }

    fun getTransactionsByContact(contactId: Long): kotlinx.coroutines.flow.Flow<List<Transaction>> {
        return repository.getTransactionsByContact(contactId)
    }

    fun addContact(name: String, phone: String, upiId: String?, avatarUrl: String?, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val contact = Contact(
                name = name,
                phone = phone,
                upiId = if (upiId.isNullOrBlank()) null else upiId.trim(),
                avatarUrl = if (avatarUrl.isNullOrBlank()) null else avatarUrl.trim()
            )
            val id = repository.insertContact(contact)
            onComplete(id)
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact)
            Toast.makeText(context, "Contact Updated", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            Toast.makeText(context, "Contact Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun addTransaction(contactId: Long, amount: Double, isBorrowed: Boolean, note: String) {
        viewModelScope.launch {
            val tx = Transaction(
                contactId = contactId,
                amount = amount,
                isBorrowed = isBorrowed,
                note = note.trim()
            )
            repository.insertTransaction(tx)
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    // Export local contacts and transactions into standard comma-separated text CSV
    fun exportReportAsCSV() {
        viewModelScope.launch {
            try {
                val contactList = rawContacts.value
                val txList = rawTransactions.value
                
                val csv = StringBuilder()
                csv.append("Contact,Phone,UPI,Type,Amount (INR),Note,Date\n")
                
                txList.forEach { tx ->
                    val owner = contactList.find { it.id == tx.contactId }
                    val name = owner?.name ?: "Unknown"
                    val phone = owner?.phone ?: ""
                    val upi = owner?.upiId ?: ""
                    val type = if (tx.isBorrowed) "Borrowed" else "Lent"
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    
                    csv.append("\"$name\",\"$phone\",\"$upi\",\"$type\",${tx.amount},\"${tx.note.replace("\"", "\"\"")}\",\"$dateStr\"\n")
                }

                // Share Intent
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, csv.toString())
                    type = "text/csv"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share DebtSync CSV Statement")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
                Toast.makeText(context, "Exported Statement to CSV", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export report: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // JSON Local backup / Simulated cloud Sync (export DB entries to Device as backup file or Clipboard)
    fun backupDatabase(): String {
        return try {
            val payload = JSONObject()
            val contactsArray = JSONArray()
            rawContacts.value.forEach { c ->
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("upiId", c.upiId ?: "")
                    put("avatarUrl", c.avatarUrl ?: "")
                    put("createdAt", c.createdAt)
                }
                contactsArray.put(obj)
            }
            val txArray = JSONArray()
            rawTransactions.value.forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("contactId", t.contactId)
                    put("amount", t.amount)
                    put("isBorrowed", t.isBorrowed)
                    put("note", t.note)
                    put("timestamp", t.timestamp)
                }
                txArray.put(obj)
            }
            payload.put("contacts", contactsArray)
            payload.put("transactions", txArray)
            payload.put("backupTime", System.currentTimeMillis())
            
            val jsonString = payload.toString(4)
            
            // Save inside a backup file locally too
            val file = File(context.filesDir, "debtsync_backup.json")
            file.writeText(jsonString)
            
            jsonString
        } catch (e: Exception) {
            ""
        }
    }

    fun restoreDatabase(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val contacts = root.getJSONArray("contacts")
            val txs = root.getJSONArray("transactions")
            
            viewModelScope.launch {
                // Remove previous elements to replace completely
                rawContacts.value.forEach { repository.deleteContact(it) }
                
                for (i in 0 until contacts.length()) {
                    val c = contacts.getJSONObject(i)
                    val restoredContact = Contact(
                        id = c.optLong("id"),
                        name = c.getString("name"),
                        phone = c.getString("phone"),
                        upiId = if (c.getString("upiId").isEmpty()) null else c.getString("upiId"),
                        avatarUrl = if (c.getString("avatarUrl").isEmpty()) null else c.getString("avatarUrl"),
                        createdAt = c.optLong("createdAt", System.currentTimeMillis())
                    )
                    repository.insertContact(restoredContact)
                }
                
                for (j in 0 until txs.length()) {
                    val t = txs.getJSONObject(j)
                    val restoredTx = Transaction(
                        id = t.optLong("id"),
                        contactId = t.getLong("contactId"),
                        amount = t.getDouble("amount"),
                        isBorrowed = t.getBoolean("isBorrowed"),
                        note = t.getString("note"),
                        timestamp = t.optLong("timestamp", System.currentTimeMillis())
                    )
                    repository.insertTransaction(restoredTx)
                }
            }
            Toast.makeText(context, "Database Restored Successfully!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Invalid Backup Format!", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // Helper to generate dynamic WhatsApp reminder templates
    fun getWhatsAppReminderMessage(name: String, amount: Double): String {
        return if (amount > 0) {
            "Hi $name, just a gentle reminder regarding our pending payment on DebtSync. Outstanding of ₹${String.format(Locale.getDefault(), "%.2f", amount)} is pending to receive. You can pay via UPI. Thanks!"
        } else {
            "Hi $name, this is regarding pending payments on DebtSync. I owe you ₹${String.format(Locale.getDefault(), "%.2f", Math.abs(amount))}. Just checking your preferred UPI details to clear this. Thanks!"
        }
    }

    // Seed realistic demo data for presentation on fresh launch
    fun loadDemoData() {
        viewModelScope.launch {
            val rajeevId = repository.insertContact(Contact(name = "Rajeev Sharma", phone = "+919876543210", upiId = "rajeev.sharma@paytm", avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150"))
            val rahulId = repository.insertContact(Contact(name = "Rahul Verma", phone = "+919998887776", upiId = "9998887776@ybl", avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"))
            val priyaId = repository.insertContact(Contact(name = "Priya Patel", phone = "+919555444332", upiId = "priyampatel@okaxis", avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"))
            val amitId = repository.insertContact(Contact(name = "Amit Mehra", phone = "+919111222333", upiId = "amitmehra@upi", avatarUrl = null))

            // Rajeev Sharma transactions
            repository.insertTransaction(Transaction(contactId = rajeevId, amount = 1200.0, isBorrowed = false, note = "Lent for dinner bill split"))
            repository.insertTransaction(Transaction(contactId = rajeevId, amount = 300.0, isBorrowed = true, note = "Borrowed for tea & snacks"))

            // Rahul Verma transactions
            repository.insertTransaction(Transaction(contactId = rahulId, amount = 5000.0, isBorrowed = true, note = "Borrowed for bike rent"))
            repository.insertTransaction(Transaction(contactId = rahulId, amount = 1500.0, isBorrowed = false, note = "Gave partial repayment"))

            // Priya Patel transactions
            repository.insertTransaction(Transaction(contactId = priyaId, amount = 2500.0, isBorrowed = false, note = "Lent shopping cash"))
            repository.insertTransaction(Transaction(contactId = priyaId, amount = 1000.0, isBorrowed = false, note = "Lent movie ticket price"))

            // Amit Mehra transactions
            repository.insertTransaction(Transaction(contactId = amitId, amount = 800.0, isBorrowed = true, note = "Borrowed uber booking fares"))
        }
    }

    private fun String?.isNull_or_empty(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
