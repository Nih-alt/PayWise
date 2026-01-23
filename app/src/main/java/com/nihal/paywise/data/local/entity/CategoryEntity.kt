package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nihal.paywise.domain.model.CategoryKind
import com.nihal.paywise.domain.model.SpendingGroup

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: Long,
    val kind: CategoryKind,
    val spendingGroup: SpendingGroup,
    val parentId: String?
)