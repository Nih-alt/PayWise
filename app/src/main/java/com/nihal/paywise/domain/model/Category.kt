package com.nihal.paywise.domain.model

import com.nihal.paywise.data.local.entity.CategoryEntity

enum class CategoryKind {
    EXPENSE, INCOME
}

data class Category(
    val id: String,
    val name: String,
    val color: Long,
    val kind: CategoryKind,
    val parentId: String?
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    color = color,
    kind = kind,
    parentId = parentId
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    color = color,
    kind = kind,
    parentId = parentId
)