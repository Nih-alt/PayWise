package com.nihal.paywise.domain.model

import com.nihal.paywise.data.local.entity.CategoryEntity

enum class CategoryKind {
    EXPENSE, INCOME
}

enum class SpendingGroup {
    FIXED, DISCRETIONARY
}

data class Category(
    val id: String,
    val name: String,
    val color: Long,
    val kind: CategoryKind,
    val spendingGroup: SpendingGroup = SpendingGroup.DISCRETIONARY,
    val parentId: String?
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    color = color,
    kind = kind,
    spendingGroup = spendingGroup,
    parentId = parentId
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    color = color,
    kind = kind,
    spendingGroup = spendingGroup,
    parentId = parentId
)