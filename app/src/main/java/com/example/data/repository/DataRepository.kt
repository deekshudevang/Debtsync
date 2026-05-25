package com.example.data.repository

import com.example.data.db.ContactDao
import com.example.data.model.Contact
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class DataRepository(private val contactDao: ContactDao) {
    val allContacts: Flow<List<Contact>> = contactDao.getContacts()
    val allTransactions: Flow<List<Transaction>> = contactDao.getAllTransactions()

    fun getContactById(id: Long): Flow<Contact?> {
        return contactDao.getContactById(id)
    }

    fun getTransactionsByContact(contactId: Long): Flow<List<Transaction>> {
        return contactDao.getTransactionsByContact(contactId)
    }

    suspend fun insertContact(contact: Contact): Long {
        return contactDao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return contactDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        contactDao.deleteTransaction(transaction)
    }
}
