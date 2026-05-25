package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val upiId: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
